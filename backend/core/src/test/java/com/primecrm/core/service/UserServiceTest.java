package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.user.RoleSummaryResponse;
import com.primecrm.core.dto.user.UserCreateRequest;
import com.primecrm.core.dto.user.UserResponse;
import com.primecrm.core.mapper.UserMapper;
import com.primecrm.core.service.support.UserAuthorityResolver;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.auth.UserStatus;
import com.primecrm.infra.repository.RoleRepository;
import com.primecrm.infra.repository.UserRepository;
import com.primecrm.infra.repository.UserRoleRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ConflictException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserAuthorityResolver authorityResolver;
    @Mock
    private AuditService auditService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, userRoleRepository, passwordEncoder,
                userMapper, authorityResolver, auditService);
    }

    @Test
    void create_withNewEmailAndLogin_savesUser() {
        UserCreateRequest request = new UserCreateRequest("New User", "new@primecrm.local", "newuser", "Password123", null);

        User newEntity = new User();
        newEntity.setName(request.name());
        newEntity.setEmail(request.email());
        newEntity.setLogin(request.login());

        User savedEntity = new User();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setName(request.name());
        savedEntity.setEmail(request.email());
        savedEntity.setLogin(request.login());
        savedEntity.setStatus(UserStatus.ACTIVE);

        UserResponse expectedResponse = new UserResponse(savedEntity.getId(), request.name(), request.email(),
                request.login(), null, UserStatus.ACTIVE, null, null, List.of());

        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(newEntity);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedEntity);
        when(authorityResolver.resolveActiveRoles(savedEntity.getId())).thenReturn(List.of());
        when(userMapper.toRoleSummaryList(List.of())).thenReturn(List.<RoleSummaryResponse>of());
        when(userMapper.toResponse(savedEntity, List.of())).thenReturn(expectedResponse);

        UserResponse response = userService.create(request);

        assertThat(response).isEqualTo(expectedResponse);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_withDuplicateEmail_throwsConflict() {
        UserCreateRequest request = new UserCreateRequest("New User", "admin@primecrm.local", "newuser", "Password123", null);

        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("admin@primecrm.local");
        existing.setLogin("admin");

        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_ownUser_throwsBusinessException() {
        UUID currentUserId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.delete(currentUserId, currentUserId))
                .isInstanceOf(BusinessException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void delete_unknownUser_throwsResourceNotFound() {
        UUID targetId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(targetId, currentUserId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
