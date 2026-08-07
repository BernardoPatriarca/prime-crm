package com.primecrm.api.controller;

import com.primecrm.core.dto.common.ReorderRequest;
import com.primecrm.core.dto.domain.DomainValueRequest;
import com.primecrm.core.dto.domain.DomainValueResponse;
import com.primecrm.core.service.DomainValueService;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/domain-values")
@RequiredArgsConstructor
@Tag(name = "Valores de Dominio", description = "CRUD generico dos valores de cada tipo de dominio (ex: prioridades, origens de lead, tags)")
public class DomainValueController {

    private final DomainValueService domainValueService;

    @GetMapping
    @PreAuthorize("hasAuthority('DOMINIOS_VIEW')")
    @Operation(summary = "Lista valores de um tipo de dominio, paginados, com busca textual e filtro por ativo/inativo")
    public ResponseEntity<PageResponse<DomainValueResponse>> list(
            @RequestParam(name = "type", required = false) String domainTypeCode,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "displayOrder") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(domainValueService.list(domainTypeCode, search, active, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOMINIOS_VIEW')")
    @Operation(summary = "Busca um valor de dominio pelo id")
    public ResponseEntity<DomainValueResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(domainValueService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DOMINIOS_CREATE')")
    @Operation(summary = "Cria um novo valor de dominio para um domainTypeCode existente")
    public ResponseEntity<DomainValueResponse> create(@Valid @RequestBody DomainValueRequest request) {
        DomainValueResponse response = domainValueService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOMINIOS_EDIT')")
    @Operation(summary = "Atualiza um valor de dominio existente")
    public ResponseEntity<DomainValueResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody DomainValueRequest request) {
        return ResponseEntity.ok(domainValueService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOMINIOS_DELETE')")
    @Operation(summary = "Exclui (soft delete) um valor de dominio")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        domainValueService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAuthority('DOMINIOS_EDIT')")
    @Operation(summary = "Reordena em lote o displayOrder de um conjunto de valores de dominio")
    public ResponseEntity<List<DomainValueResponse>> reorder(@Valid @RequestBody ReorderRequest request) {
        return ResponseEntity.ok(domainValueService.reorder(request));
    }
}
