package com.primecrm.core.service;

import com.primecrm.core.audit.AuditChanges;
import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.user.AssignRolesRequest;
import com.primecrm.core.dto.user.ChangePasswordRequest;
import com.primecrm.core.dto.user.UserCreateRequest;
import com.primecrm.core.dto.user.UserResponse;
import com.primecrm.core.dto.user.UserStatusUpdateRequest;
import com.primecrm.core.dto.user.UserUpdateRequest;
import com.primecrm.core.mapper.UserMapper;
import com.primecrm.core.service.support.UserAuthorityResolver;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.core.specification.UserSpecifications;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.auth.Role;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.auth.UserRole;
import com.primecrm.infra.entity.auth.UserStatus;
import com.primecrm.infra.repository.RoleRepository;
import com.primecrm.infra.repository.UserRepository;
import com.primecrm.infra.repository.UserRoleRepository;
import com.primecrm.core.specification.UserRoleSpecifications;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ConflictException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String AUDIT_ENTITY = "User";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserAuthorityResolver authorityResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String search, UserStatus status, Pageable pageable) {
        var spec = SpecificationUtils.<User>and(
                UserSpecifications.notDeleted(),
                UserSpecifications.textSearch(search),
                UserSpecifications.hasStatus(status)
        );
        Page<User> page = userRepository.findAll(spec, pageable);
        Map<UUID, List<Role>> rolesByUser = authorityResolver.resolveActiveRolesByUser(
                page.getContent().stream().map(User::getId).toList());
        return page.map(user -> userMapper.toResponse(user,
                userMapper.toRoleSummaryList(rolesByUser.getOrDefault(user.getId(), List.of()))));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return toResponse(getActiveUserOrThrow(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        ensureEmailAndLoginAvailable(request.email(), request.login(), null);

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        auditService.recordCreate(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = getActiveUserOrThrow(id);
        ensureEmailAndLoginAvailable(request.email(), request.login(), id);

        Map<String, Object> previousState = auditService.snapshot(user);
        userMapper.updateEntity(user, request);
        user = userRepository.save(user);
        auditService.recordUpdate(user, previousState);
        return toResponse(user);
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException("SELF_DELETE_FORBIDDEN", "Nao e possivel excluir o proprio usuario.");
        }
        User user = getActiveUserOrThrow(id);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
        auditService.recordDelete(user);
    }

    @Transactional
    public UserResponse updateStatus(UUID id, UserStatusUpdateRequest request, UUID currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException("SELF_STATUS_CHANGE_FORBIDDEN",
                    "Nao e possivel alterar o status do proprio usuario.");
        }
        User user = getActiveUserOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(user);
        user.setStatus(request.status());
        user = userRepository.save(user);
        auditService.recordUpdate(user, previousState);
        return toResponse(user);
    }

    @Transactional
    public UserResponse assignRoles(UUID id, AssignRolesRequest request) {
        User user = getActiveUserOrThrow(id);

        List<Role> roles = roleRepository.findAllById(request.roleIds());
        if (roles.size() != request.roleIds().size()) {
            throw new ResourceNotFoundException("Um ou mais roles informados nao foram encontrados");
        }

        List<UserRole> current = userRoleRepository.findAll(UserRoleSpecifications.byUserId(id));
        List<String> previousRoleNames = current.stream().map(userRole -> userRole.getRole().getName()).sorted().toList();
        userRoleRepository.deleteAll(current);

        List<UserRole> updated = roles.stream().map(role -> {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            return userRole;
        }).toList();
        userRoleRepository.saveAll(updated);

        List<String> currentRoleNames = roles.stream().map(Role::getName).sorted().toList();
        auditService.recordChange(AuditAction.UPDATE, AUDIT_ENTITY, id,
                Map.of("roles", AuditChanges.of(previousRoleNames, currentRoleNames)));

        return toResponse(user);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = getActiveUserOrThrow(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.recordChange(AuditAction.UPDATE, AUDIT_ENTITY, id, Map.of("passwordChanged", true));
    }

    private User getActiveUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    private void ensureEmailAndLoginAvailable(String email, String login, UUID excludingId) {
        userRepository.findOne(UserSpecifications.byLoginOrEmail(email))
                .filter(u -> excludingId == null || !u.getId().equals(excludingId))
                .filter(u -> !u.isDeleted())
                .ifPresent(u -> {
                    throw new ConflictException("Ja existe um usuario cadastrado com este e-mail");
                });
        if (!StringUtils.hasText(login)) {
            return;
        }
        userRepository.findOne(UserSpecifications.byLoginOrEmail(login))
                .filter(u -> excludingId == null || !u.getId().equals(excludingId))
                .filter(u -> !u.isDeleted())
                .ifPresent(u -> {
                    throw new ConflictException("Ja existe um usuario cadastrado com este login");
                });
    }

    private UserResponse toResponse(User user) {
        List<Role> roles = authorityResolver.resolveActiveRoles(user.getId());
        return userMapper.toResponse(user, userMapper.toRoleSummaryList(roles));
    }
}
