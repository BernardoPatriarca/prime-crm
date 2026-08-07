package com.primecrm.api.controller;

import com.primecrm.core.dto.template.TemplateRequest;
import com.primecrm.core.dto.template.TemplateResponse;
import com.primecrm.core.service.TemplateService;
import com.primecrm.infra.entity.config.TemplateType;
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
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Templates de comunicacao (e-mail, proposta, contrato, whatsapp)")
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAuthority('TEMPLATES_VIEW')")
    @Operation(summary = "Lista templates paginados, filtraveis por type, busca textual e ativo/inativo")
    public ResponseEntity<PageResponse<TemplateResponse>> list(
            @RequestParam(required = false) TemplateType type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(templateService.list(type, search, active, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATES_VIEW')")
    @Operation(summary = "Busca um template pelo id")
    public ResponseEntity<TemplateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEMPLATES_CREATE')")
    @Operation(summary = "Cria um novo template")
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateRequest request) {
        TemplateResponse response = templateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATES_EDIT')")
    @Operation(summary = "Atualiza um template existente")
    public ResponseEntity<TemplateResponse> update(@PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATES_DELETE')")
    @Operation(summary = "Exclui (soft delete) um template")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
