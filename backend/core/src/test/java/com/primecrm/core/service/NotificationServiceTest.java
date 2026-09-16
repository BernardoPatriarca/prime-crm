package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.dto.notification.NotificationListResponse;
import com.primecrm.core.dto.notification.NotificationSeverity;
import com.primecrm.core.dto.notification.NotificationType;
import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.entity.task.Task;
import com.primecrm.infra.repository.CalendarEventRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.TaskRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private CalendarEventRepository calendarEventRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(taskRepository, opportunityRepository, leadRepository,
                calendarEventRepository);
    }

    @Test
    void list_withNullUser_skipsUserScopedSourcesButKeepsLeadsWithoutOwner() {
        when(leadRepository.findTop10ByOwnerIsNullAndConvertedAtIsNullAndDeletedAtIsNullOrderByCreatedAtDesc())
                .thenReturn(List.of());

        NotificationListResponse response = notificationService.list(null);

        assertThat(response.total()).isZero();
        assertThat(response.items()).isEmpty();
        verify(taskRepository, never()).findTop10ByAssignee_IdAndStatusInAndDueAtLessThanAndDeletedAtIsNullOrderByDueAtAsc(
                any(), anyList(), any());
        verify(opportunityRepository, never())
                .findTop10ByOwner_IdAndOutcomeAndExpectedCloseDateLessThanAndDeletedAtIsNullOrderByExpectedCloseDateAsc(
                        any(), any(), any());
        verify(calendarEventRepository, never()).findOverdueByAssignee(any(), any(), any());
    }

    @Test
    void list_ordersBySeverityThenByDate() {
        UUID userId = UUID.randomUUID();

        Task overdueTask = new Task();
        overdueTask.setId(UUID.randomUUID());
        overdueTask.setTitle("Tarefa atrasada");
        overdueTask.setDueAt(Instant.now().minusSeconds(3600));

        Lead leadWithoutOwner = new Lead();
        leadWithoutOwner.setId(UUID.randomUUID());
        leadWithoutOwner.setName("Lead sem dono");
        leadWithoutOwner.setCreatedAt(Instant.now());

        Opportunity lateOpportunity = new Opportunity();
        lateOpportunity.setId(UUID.randomUUID());
        lateOpportunity.setTitle("Oportunidade atrasada");
        lateOpportunity.setExpectedCloseDate(LocalDate.now().minusDays(2));

        when(taskRepository.findTop10ByAssignee_IdAndStatusInAndDueAtLessThanAndDeletedAtIsNullOrderByDueAtAsc(
                any(), anyList(), any())).thenReturn(List.of(overdueTask));
        when(taskRepository.findTop10ByAssignee_IdAndStatusInAndDueAtGreaterThanEqualAndDueAtLessThanAndDeletedAtIsNullOrderByDueAtAsc(
                any(), anyList(), any(), any())).thenReturn(List.of());
        when(opportunityRepository
                .findTop10ByOwner_IdAndOutcomeAndExpectedCloseDateLessThanAndDeletedAtIsNullOrderByExpectedCloseDateAsc(
                        any(), any(OpportunityOutcome.class), any())).thenReturn(List.of(lateOpportunity));
        when(leadRepository.findTop10ByOwnerIsNullAndConvertedAtIsNullAndDeletedAtIsNullOrderByCreatedAtDesc())
                .thenReturn(List.of(leadWithoutOwner));
        when(calendarEventRepository.findOverdueByAssignee(any(), any(CalendarEventStatus.class), any()))
                .thenReturn(List.of());

        NotificationListResponse response = notificationService.list(userId);

        assertThat(response.total()).isEqualTo(3);
        assertThat(response.items()).hasSize(3);
        assertThat(response.items().get(0).type()).isEqualTo(NotificationType.TASK_OVERDUE);
        assertThat(response.items().get(0).severity()).isEqualTo(NotificationSeverity.DANGER);
        assertThat(response.items().get(1).type()).isEqualTo(NotificationType.OPPORTUNITY_CLOSE_DATE_PASSED);
        assertThat(response.items().get(1).severity()).isEqualTo(NotificationSeverity.WARN);
        assertThat(response.items().get(2).type()).isEqualTo(NotificationType.LEAD_WITHOUT_OWNER);
        assertThat(response.items().get(2).severity()).isEqualTo(NotificationSeverity.INFO);
    }

    @Test
    void list_truncatesToMaxItemsButKeepsFullTotalCount() {
        UUID userId = UUID.randomUUID();
        List<Task> overdueTasks = java.util.stream.IntStream.range(0, 25)
                .mapToObj(i -> {
                    Task task = new Task();
                    task.setId(UUID.randomUUID());
                    task.setTitle("Tarefa " + i);
                    task.setDueAt(Instant.now().minusSeconds(i));
                    return task;
                })
                .toList();

        when(taskRepository.findTop10ByAssignee_IdAndStatusInAndDueAtLessThanAndDeletedAtIsNullOrderByDueAtAsc(
                any(), anyList(), any())).thenReturn(overdueTasks);
        when(taskRepository.findTop10ByAssignee_IdAndStatusInAndDueAtGreaterThanEqualAndDueAtLessThanAndDeletedAtIsNullOrderByDueAtAsc(
                any(), anyList(), any(), any())).thenReturn(List.of());
        when(opportunityRepository
                .findTop10ByOwner_IdAndOutcomeAndExpectedCloseDateLessThanAndDeletedAtIsNullOrderByExpectedCloseDateAsc(
                        any(), any(OpportunityOutcome.class), any())).thenReturn(List.of());
        when(leadRepository.findTop10ByOwnerIsNullAndConvertedAtIsNullAndDeletedAtIsNullOrderByCreatedAtDesc())
                .thenReturn(List.of());
        when(calendarEventRepository.findOverdueByAssignee(any(), any(CalendarEventStatus.class), any()))
                .thenReturn(List.of());

        NotificationListResponse response = notificationService.list(userId);

        assertThat(response.total()).isEqualTo(25);
        assertThat(response.items()).hasSize(NotificationService.MAX_ITEMS);
    }
}
