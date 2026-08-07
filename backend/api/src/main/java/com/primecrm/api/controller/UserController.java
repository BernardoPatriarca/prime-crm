package com.primecrm.api.controller;

import com.primecrm.core.dto.user.AssignRolesRequest;
import com.primecrm.core.dto.user.ChangePasswordRequest;
import com.primecrm.core.dto.user.UserCreateRequest;
import com.primecrm.core.dto.user.UserResponse;
import com.primecrm.core.dto.user.UserStatusUpdateRequest;
import com.primecrm.core.dto.user.UserUpdateRequest;
import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.core.service.UserService;
import com.primecrm.infra.entity.auth.UserStatus;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestao de usuarios do sistema")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIOS_VIEW')")
    @Operation(summary = "Lista usuarios paginados, com filtro textual (nome/email/login) e por status")
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(userService.list(search, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_VIEW')")
    @Operation(summary = "Busca um usuario pelo id")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_CREATE')")
    @Operation(summary = "Cria um novo usuario")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_EDIT')")
    @Operation(summary = "Atualiza os dados cadastrais de um usuario")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_DELETE')")
    @Operation(summary = "Exclui (soft delete) um usuario. Nao e possivel excluir o proprio usuario autenticado")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        userService.delete(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USUARIOS_EDIT')")
    @Operation(summary = "Ativa, inativa ou bloqueia um usuario. Nao e possivel alterar o proprio status")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable UUID id,
                                                       @Valid @RequestBody UserStatusUpdateRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(userService.updateStatus(id, request, currentUser.id()));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USUARIOS_EDIT')")
    @Operation(summary = "Substitui o conjunto de roles atribuidos ao usuario")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable UUID id, @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(userService.assignRoles(id, request));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAuthority('USUARIOS_EDIT')")
    @Operation(summary = "Redefine administrativamente a senha de um usuario")
    public ResponseEntity<Void> changePassword(@PathVariable UUID id, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
