package com.primecrm.infra.repository;

import com.primecrm.infra.entity.task.Task;
import com.primecrm.infra.entity.task.TaskStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndDeletedAtIsNull(UUID id);

    long countByStatusAndDeletedAtIsNull(TaskStatus status);

    long countByStatusInAndDueAtLessThanAndDeletedAtIsNull(Collection<TaskStatus> statuses, Instant limit);

    long countByStatusInAndDueAtGreaterThanEqualAndDueAtLessThanAndDeletedAtIsNull(
            Collection<TaskStatus> statuses, Instant from, Instant to);

    long countByStatusAndCompletedAtGreaterThanEqualAndDeletedAtIsNull(TaskStatus status, Instant from);

    List<Task> findTop10ByAssignee_IdAndStatusInAndDueAtLessThanAndDeletedAtIsNullOrderByDueAtAsc(
            UUID assigneeId, Collection<TaskStatus> statuses, Instant limit);

    List<Task> findTop10ByAssignee_IdAndStatusInAndDueAtGreaterThanEqualAndDueAtLessThanAndDeletedAtIsNullOrderByDueAtAsc(
            UUID assigneeId, Collection<TaskStatus> statuses, Instant from, Instant to);
}
