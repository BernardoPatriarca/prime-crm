package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.task.TaskListFilter;
import com.primecrm.core.dto.task.TaskRequest;
import com.primecrm.core.dto.task.TaskResponse;
import com.primecrm.core.mapper.TaskMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.core.specification.TaskSpecifications;
import com.primecrm.infra.entity.task.Task;
import com.primecrm.infra.entity.task.TaskStatus;
import com.primecrm.infra.repository.TaskRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(TaskListFilter filter, Pageable pageable) {
        return taskRepository.findAll(toSpecification(filter), pageable).map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(UUID id) {
        return taskMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = taskMapper.toEntity(request);
        applyReferences(task, request);
        applyStatus(task, request.status() == null ? TaskStatus.PENDING : request.status());

        task = taskRepository.save(task);
        auditService.recordCreate(task);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse update(UUID id, TaskRequest request) {
        Task task = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(task);

        taskMapper.updateEntity(task, request);
        applyReferences(task, request);
        if (request.status() != null) {
            applyStatus(task, request.status());
        }

        task = taskRepository.save(task);
        auditService.recordUpdate(task, previousState);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse changeStatus(UUID id, TaskStatus status) {
        if (status == null) {
            throw new BusinessException("TASK_STATUS_REQUIRED", "Informe o novo status da tarefa");
        }
        Task task = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(task);

        applyStatus(task, status);

        task = taskRepository.save(task);
        auditService.recordUpdate(task, previousState);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void delete(UUID id) {
        Task task = getActiveOrThrow(id);
        task.setDeletedAt(Instant.now());
        taskRepository.save(task);
        auditService.recordDelete(task);
    }

    private Specification<Task> toSpecification(TaskListFilter filter) {
        return SpecificationUtils.and(
                TaskSpecifications.notDeleted(),
                TaskSpecifications.withReferencesFetched(),
                TaskSpecifications.textSearch(filter.search()),
                TaskSpecifications.hasStatus(filter.status()),
                TaskSpecifications.hasType(filter.typeId()),
                TaskSpecifications.hasPriority(filter.priorityId()),
                TaskSpecifications.hasAssignee(filter.assignedUserId()),
                TaskSpecifications.hasCustomer(filter.customerId()),
                TaskSpecifications.hasLead(filter.leadId()),
                TaskSpecifications.hasOpportunity(filter.opportunityId()),
                TaskSpecifications.dueFrom(filter.dueFrom()),
                TaskSpecifications.dueTo(filter.dueTo()),
                TaskSpecifications.onlyOverdue(filter.overdue()));
    }

    private void applyReferences(Task task, TaskRequest request) {
        task.setType(referenceResolver.domainValue(request.typeId(), "Tipo de tarefa"));
        task.setPriority(referenceResolver.domainValue(request.priorityId(), "Prioridade"));
        task.setAssignee(referenceResolver.user(request.assignedUserId()));
        task.setCustomer(referenceResolver.customer(request.customerId()));
        task.setContact(referenceResolver.contact(request.contactId()));
        task.setLead(referenceResolver.lead(request.leadId()));
        task.setOpportunity(referenceResolver.opportunity(request.opportunityId()));
    }

    private void applyStatus(Task task, TaskStatus status) {
        task.setStatus(status);
        if (status != TaskStatus.DONE) {
            task.setCompletedAt(null);
        } else if (task.getCompletedAt() == null) {
            task.setCompletedAt(Instant.now());
        }
    }

    private Task getActiveOrThrow(UUID id) {
        return taskRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa", id));
    }
}
