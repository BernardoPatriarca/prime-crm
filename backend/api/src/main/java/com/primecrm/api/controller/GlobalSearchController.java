package com.primecrm.api.controller;

import com.primecrm.core.dto.search.GlobalSearchResponse;
import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.core.service.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Busca global", description = "Busca unica sobre clientes, contatos, leads, oportunidades e tarefas")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca o termo em todos os modulos que o usuario tem permissao de visualizar, "
            + "devolvendo ate 5 resultados por modulo")
    public ResponseEntity<GlobalSearchResponse> search(@RequestParam String query,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Set<String> permissions = currentUser == null ? Set.of() : Set.copyOf(permissionsOf(currentUser));
        return ResponseEntity.ok(globalSearchService.search(query, permissions::contains));
    }

    private List<String> permissionsOf(AuthenticatedUser currentUser) {
        return currentUser.permissions() == null ? List.of() : currentUser.permissions();
    }
}
