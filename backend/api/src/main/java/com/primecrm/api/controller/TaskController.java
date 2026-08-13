package com.primecrm.api.controller;

import com.primecrm.api.support.SortGuard;
import com.primecrm.core.dto.task.TaskListFilter;
import com.primecrm.core.dto.task.TaskRequest;
import com.primecrm.core.dto.task.TaskResponse;
import com.primecrm.core.dto.task.TaskStatusUpdateRequest;
import com.primecrm.core.service.TaskService;
import com.primecrm.infra.entity.task.TaskStatus;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tarefas", description = "Atividades de follow-up ligadas a clientes, leads e oportunidades")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @PreAuthorize("hasAuthority('TAREFAS_VIEW')")
    @Operation(summary = "Lista tarefas paginadas, com busca textual (titulo/codigo/descricao) e filtros por "
            + "status, tipo, prioridade, responsavel, cliente, lead, oportunidade, periodo de vencimento e atraso")
    public ResponseEntity<PageResponse<TaskResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) UUID typeId,
            @RequestParam(required = false) UUID priorityId,
            @RequestParam(required = false) UUID assignedUserId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID leadId,
            @RequestParam(required = false) UUID opportunityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueTo,
            @RequestParam(required = false) Boolean overdue,
            @PageableDefault(size = 20, sort = "dueAt") Pageable pageable) {
        TaskListFilter filter = new TaskListFilter(search, status, typeId, priorityId, assignedUserId, customerId,
                leadId, opportunityId, dueFrom, dueTo, overdue);
        return ResponseEntity.ok(
                PageResponse.from(taskService.list(filter, SortGuard.requireSafeSort(pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TAREFAS_VIEW')")
    @Operation(summary = "Busca uma tarefa pelo id")
    public ResponseEntity<TaskResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TAREFAS_CREATE')")
    @Operation(summary = "Cria uma tarefa. O codigo (TAR-######) e gerado pelo banco e nao aceito no request")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TAREFAS_EDIT')")
    @Operation(summary = "Atualiza uma tarefa existente")
    public ResponseEntity<TaskResponse> update(@PathVariable UUID id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TAREFAS_EDIT')")
    @Operation(summary = "Altera apenas o status da tarefa. Concluir preenche a data de conclusao; "
            + "sair de concluida limpa a data")
    public ResponseEntity<TaskResponse> changeStatus(@PathVariable UUID id,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ResponseEntity.ok(taskService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TAREFAS_DELETE')")
    @Operation(summary = "Exclui (soft delete) uma tarefa")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
