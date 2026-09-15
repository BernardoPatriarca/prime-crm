package com.primecrm.infra.repository;

import com.primecrm.infra.entity.agenda.CalendarEvent;
import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarEventRepository
        extends JpaRepository<CalendarEvent, UUID>, JpaSpecificationExecutor<CalendarEvent> {

    Optional<CalendarEvent> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT e FROM CalendarEvent e WHERE e.assignee.id = :assigneeId AND e.status = :status "
            + "AND e.deletedAt IS NULL AND COALESCE(e.endAt, e.startAt) < :now ORDER BY e.startAt ASC")
    List<CalendarEvent> findOverdueByAssignee(
            @Param("assigneeId") UUID assigneeId, @Param("status") CalendarEventStatus status,
            @Param("now") Instant now);

    long countByStatusAndStartAtGreaterThanEqualAndStartAtLessThanAndDeletedAtIsNull(
            CalendarEventStatus status, Instant from, Instant to);

    @Query("SELECT count(e) FROM CalendarEvent e WHERE e.status = :status AND e.deletedAt IS NULL "
            + "AND COALESCE(e.endAt, e.startAt) < :now")
    long countOverdue(@Param("status") CalendarEventStatus status, @Param("now") Instant now);
}
