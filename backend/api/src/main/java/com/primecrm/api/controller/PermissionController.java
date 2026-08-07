package com.primecrm.api.controller;

import com.primecrm.core.dto.permission.PermissionResponse;
import com.primecrm.core.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissoes", description = "Catalogo tecnico fixo de permissoes (somente leitura)")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSOES_VIEW')")
    @Operation(summary = "Lista o catalogo completo de permissoes disponiveis no sistema")
    public ResponseEntity<List<PermissionResponse>> list() {
        return ResponseEntity.ok(permissionService.findAll());
    }
}
