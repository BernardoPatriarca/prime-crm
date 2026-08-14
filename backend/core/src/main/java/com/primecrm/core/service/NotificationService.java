package com.primecrm.core.service;

import com.primecrm.core.dto.notification.NotificationListResponse;
import com.primecrm.core.dto.notification.NotificationResponse;
import com.primecrm.core.dto.notification.NotificationSeverity;
import com.primecrm.core.dto.notification.NotificationType;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.entity.task.Task;
import com.primecrm.infra.entity.task.TaskStatus;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.TaskRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final int MAX_ITEMS = 20;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final List<TaskStatus> OPEN_TASK_STATUSES = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
    private static final String TASKS_LINK = "/tarefas";
    private static final String OPPORTUNITIES_LINK = "/oportunidades";
    private static final String LEADS_LINK = "/leads";

    private final TaskRepository taskRepository;
    private final OpportunityRepository opportunityRepository;
    private final LeadRepository leadRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse list(UUID currentUserId) {
        List<NotificationResponse> items = new ArrayList<>();
        items.addAll(overdueTasks(currentUserId));
        items.addAll(tasksDueToday(currentUserId));
        items.addAll(lateOpportunities(currentUserId));
        items.addAll(leadsWithoutOwner());

        List<NotificationResponse> sorted = items.stream()
                .sorted(Comparator.comparing(NotificationResponse::severity)
                        .thenComparing(NotificationResponse::date, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_ITEMS)
                .toList();

        return new NotificationListResponse(items.size(), sorted);
    }

    private List<NotificationResponse> overdueTasks(UUID currentUserId) {
        if (currentUserId == null) {
            return List.of();
        }
        return taskRepository
                .findTop10ByAssignee_IdAndStatusInAndDueAtLessThanAndDeletedAtIsNullOrderByDueAtAsc(
                        currentUserId, OPEN_TASK_STATUSES, Instant.now())
                .stream()
                .map(task -> toNotification(task, NotificationType.TASK_OVERDUE, NotificationSeverity.DANGER))
                .toList();
    }

    private List<NotificationResponse> tasksDueToday(UUID currentUserId) {
        if (currentUserId == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        return taskRepository
                .findTop10ByAssignee_IdAndStatusInAndDueAtGreaterThanEqualAndDueAtLessThanAndDeletedAtIsNullOrderByDueAtAsc(
                        currentUserId, OPEN_TASK_STATUSES, startOfDay(today), startOfDay(today.plusDays(1)))
                .stream()
                .map(task -> toNotification(task, NotificationType.TASK_DUE_TODAY, NotificationSeverity.WARN))
                .toList();
    }

    private List<NotificationResponse> lateOpportunities(UUID currentUserId) {
        if (currentUserId == null) {
            return List.of();
        }
        return opportunityRepository
                .findTop10ByOwner_IdAndOutcomeAndExpectedCloseDateLessThanAndDeletedAtIsNullOrderByExpectedCloseDateAsc(
                        currentUserId, OpportunityOutcome.OPEN, LocalDate.now(BUSINESS_ZONE))
                .stream()
                .map(this::toNotification)
                .toList();
    }

    private List<NotificationResponse> leadsWithoutOwner() {
        return leadRepository.findTop10ByOwnerIsNullAndConvertedAtIsNullAndDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .map(this::toNotification)
                .toList();
    }

    private NotificationResponse toNotification(Task task, NotificationType type, NotificationSeverity severity) {
        return new NotificationResponse(type, severity, task.getId(), task.getTitle(),
                task.getCustomer() == null ? null : task.getCustomer().getName(), TASKS_LINK, task.getDueAt());
    }

    private NotificationResponse toNotification(Opportunity opportunity) {
        return new NotificationResponse(NotificationType.OPPORTUNITY_CLOSE_DATE_PASSED, NotificationSeverity.WARN,
                opportunity.getId(), opportunity.getTitle(),
                opportunity.getCustomer() == null ? null : opportunity.getCustomer().getName(),
                OPPORTUNITIES_LINK, startOfDay(opportunity.getExpectedCloseDate()));
    }

    private NotificationResponse toNotification(Lead lead) {
        return new NotificationResponse(NotificationType.LEAD_WITHOUT_OWNER, NotificationSeverity.INFO, lead.getId(),
                lead.getName(), lead.getCompanyName(), LEADS_LINK, lead.getCreatedAt());
    }

    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }
}
