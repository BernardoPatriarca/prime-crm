package com.primecrm.api.controller;

import com.primecrm.core.dto.commercial.ContactResponse;
import com.primecrm.core.dto.commercial.CustomerListFilter;
import com.primecrm.core.dto.commercial.CustomerRequest;
import com.primecrm.core.dto.commercial.CustomerResponse;
import com.primecrm.core.service.CustomerService;
import com.primecrm.infra.entity.commercial.PersonType;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro unico de clientes e empresas (pessoa fisica e juridica)")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENTES_VIEW')")
    @Operation(summary = "Lista clientes paginados, com busca textual (nome, nome fantasia, documento, e-mail, "
            + "codigo) e filtros por tipo de pessoa, tipo de cliente, segmento, responsavel, ativo e tags")
    public ResponseEntity<PageResponse<CustomerResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PersonType personType,
            @RequestParam(required = false) UUID clientTypeId,
            @RequestParam(required = false) UUID segmentId,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) List<UUID> tagIds,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        CustomerListFilter filter = new CustomerListFilter(search, personType, clientTypeId, segmentId,
                ownerUserId, active, tagIds);
        return ResponseEntity.ok(PageResponse.from(customerService.list(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_VIEW')")
    @Operation(summary = "Busca um cliente pelo id")
    public ResponseEntity<CustomerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @GetMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('CONTATOS_VIEW')")
    @Operation(summary = "Lista os contatos vinculados a um cliente, ordenados por nome")
    public ResponseEntity<List<ContactResponse>> contacts(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.findContacts(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTES_CREATE')")
    @Operation(summary = "Cria um novo cliente. O codigo (CLI-######) e gerado pelo banco e nao aceito no request")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_EDIT')")
    @Operation(summary = "Atualiza um cliente existente")
    public ResponseEntity<CustomerResponse> update(@PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_DELETE')")
    @Operation(summary = "Exclui (soft delete) um cliente")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
