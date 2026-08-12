package com.primecrm.api.controller;

import com.primecrm.core.dto.commercial.ContactRequest;
import com.primecrm.core.dto.commercial.ContactResponse;
import com.primecrm.core.service.ContactService;
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
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Tag(name = "Contatos", description = "Contatos (pessoas) vinculados aos clientes")
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONTATOS_VIEW')")
    @Operation(summary = "Lista contatos paginados, com filtro por cliente, busca textual e ativo/inativo")
    public ResponseEntity<PageResponse<ContactResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(contactService.list(customerId, search, active, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTATOS_VIEW')")
    @Operation(summary = "Busca um contato pelo id")
    public ResponseEntity<ContactResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(contactService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTATOS_CREATE')")
    @Operation(summary = "Cria um contato. Marcar como principal desmarca os demais contatos do mesmo cliente")
    public ResponseEntity<ContactResponse> create(@Valid @RequestBody ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTATOS_EDIT')")
    @Operation(summary = "Atualiza um contato. Marcar como principal desmarca os demais contatos do mesmo cliente")
    public ResponseEntity<ContactResponse> update(@PathVariable UUID id,
            @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTATOS_DELETE')")
    @Operation(summary = "Exclui (soft delete) um contato")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
