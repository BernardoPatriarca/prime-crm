package com.primecrm.api.controller;

import com.primecrm.core.dto.customfield.CustomFieldRequest;
import com.primecrm.core.dto.customfield.CustomFieldResponse;
import com.primecrm.core.service.CustomFieldService;
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
@RequestMapping("/api/v1/custom-fields")
@RequiredArgsConstructor
@Tag(name = "Campos Personalizados", description = "Metadados de campos personalizados por entidade alvo (ex: CLIENTE, LEAD)")
public class CustomFieldController {

    private final CustomFieldService customFieldService;

    @GetMapping
    @PreAuthorize("hasAuthority('CAMPOS_PERSONALIZADOS_VIEW')")
    @Operation(summary = "Lista campos personalizados paginados, filtraveis por targetEntity, busca textual e ativo/inativo")
    public ResponseEntity<PageResponse<CustomFieldResponse>> list(
            @RequestParam(required = false) String targetEntity,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "displayOrder") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(customFieldService.list(targetEntity, search, active, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPOS_PERSONALIZADOS_VIEW')")
    @Operation(summary = "Busca um campo personalizado pelo id")
    public ResponseEntity<CustomFieldResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(customFieldService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAMPOS_PERSONALIZADOS_CREATE')")
    @Operation(summary = "Cria um novo campo personalizado")
    public ResponseEntity<CustomFieldResponse> create(@Valid @RequestBody CustomFieldRequest request) {
        CustomFieldResponse response = customFieldService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPOS_PERSONALIZADOS_EDIT')")
    @Operation(summary = "Atualiza um campo personalizado existente")
    public ResponseEntity<CustomFieldResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody CustomFieldRequest request) {
        return ResponseEntity.ok(customFieldService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPOS_PERSONALIZADOS_DELETE')")
    @Operation(summary = "Exclui (soft delete) um campo personalizado")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customFieldService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
