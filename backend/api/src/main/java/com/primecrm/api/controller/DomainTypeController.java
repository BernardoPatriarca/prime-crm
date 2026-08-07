package com.primecrm.api.controller;

import com.primecrm.core.dto.domain.DomainTypeResponse;
import com.primecrm.core.service.DomainTypeService;
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
@RequestMapping("/api/v1/domain-types")
@RequiredArgsConstructor
@Tag(name = "Tipos de Dominio", description = "Catalogo tecnico fixo dos tipos suportados pelo engine generico de dominios (somente leitura)")
public class DomainTypeController {

    private final DomainTypeService domainTypeService;

    @GetMapping
    @PreAuthorize("hasAuthority('DOMINIOS_VIEW')")
    @Operation(summary = "Lista todos os tipos de dominio disponiveis (ex: CLIENT_TYPE, PRIORITY, TAG)")
    public ResponseEntity<List<DomainTypeResponse>> list() {
        return ResponseEntity.ok(domainTypeService.findAll());
    }
}
