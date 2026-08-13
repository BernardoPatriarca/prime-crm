package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.task.TaskRequest;
import com.primecrm.core.mapper.TaskMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.task.Task;
import com.primecrm.infra.entity.task.TaskStatus;
import com.primecrm.infra.repository.TaskRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, taskMapper, referenceResolver, auditService);
    }

    @Test
    void create_withoutStatus_startsAsPendingAndIsAudited() {
        TaskRequest request = requestWithStatus(null);
        Task task = newTask(TaskStatus.DONE);

        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);

        taskService.create(request);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getCompletedAt()).isNull();
        verify(auditService).recordCreate(task);
    }

    @Test
    void changeStatus_toDone_fillsCompletedAt() {
        Task task = newTask(TaskStatus.IN_PROGRESS);
        when(taskRepository.findByIdAndDeletedAtIsNull(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.changeStatus(task.getId(), TaskStatus.DONE);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isNotNull();
        verify(auditService).recordUpdate(any(Task.class), any());
    }

    @Test
    void changeStatus_leavingDone_clearsCompletedAt() {
        Task task = newTask(TaskStatus.DONE);
        task.setCompletedAt(Instant.now());
        when(taskRepository.findByIdAndDeletedAtIsNull(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.changeStatus(task.getId(), TaskStatus.IN_PROGRESS);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void changeStatus_keepingDone_preservesTheOriginalCompletedAt() {
        Instant completedAt = Instant.parse("2026-01-05T10:15:30Z");
        Task task = newTask(TaskStatus.DONE);
        task.setCompletedAt(completedAt);
        when(taskRepository.findByIdAndDeletedAtIsNull(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.changeStatus(task.getId(), TaskStatus.DONE);

        assertThat(task.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void changeStatus_withoutStatus_throwsBusinessException() {
        assertThatThrownBy(() -> taskService.changeStatus(UUID.randomUUID(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_marksTheTaskAsDeletedAndAudits() {
        Task task = newTask(TaskStatus.PENDING);
        when(taskRepository.findByIdAndDeletedAtIsNull(task.getId())).thenReturn(Optional.of(task));

        taskService.delete(task.getId());

        assertThat(task.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(task);
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private TaskRequest requestWithStatus(TaskStatus status) {
        return new TaskRequest("Ligar para o cliente", null, null, null, status, null, null, null, null, null,
                null, null, null);
    }

    private Task newTask(TaskStatus status) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle("Ligar para o cliente");
        task.setStatus(status);
        return task;
    }
}
