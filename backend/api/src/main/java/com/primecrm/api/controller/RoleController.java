package com.primecrm.api.controller;

import com.primecrm.core.dto.role.AssignPermissionsRequest;
import com.primecrm.core.dto.role.RoleRequest;
import com.primecrm.core.dto.role.RoleResponse;
import com.primecrm.core.service.RoleService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Perfis de Acesso", description = "Gestao de roles (perfis) e seus vinculos com permissoes")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERFIS_VIEW')")
    @Operation(summary = "Lista perfis paginados, com filtro textual e por status ativo/inativo")
    public ResponseEntity<PageResponse<RoleResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(roleService.list(search, active, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERFIS_VIEW')")
    @Operation(summary = "Busca um perfil pelo id, incluindo suas permissoes")
    public ResponseEntity<RoleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERFIS_CREATE')")
    @Operation(summary = "Cria um novo perfil de acesso")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        RoleResponse response = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERFIS_EDIT')")
    @Operation(summary = "Atualiza nome/descricao/status ativo de um perfil")
    public ResponseEntity<RoleResponse> update(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERFIS_DELETE')")
    @Operation(summary = "Exclui (soft delete) um perfil de acesso")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('PERFIS_EDIT')")
    @Operation(summary = "Substitui o conjunto de permissoes vinculadas ao perfil")
    public ResponseEntity<RoleResponse> assignPermissions(@PathVariable UUID id,
                                                            @Valid @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(roleService.assignPermissions(id, request));
    }
}
