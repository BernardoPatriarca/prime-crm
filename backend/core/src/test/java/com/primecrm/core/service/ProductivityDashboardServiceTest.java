package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.primecrm.core.dto.dashboard.ProductivityDashboardResponse;
import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import com.primecrm.infra.entity.task.TaskStatus;
import com.primecrm.infra.repository.CalendarEventRepository;
import com.primecrm.infra.repository.TaskRepository;
import com.primecrm.infra.repository.projection.LabeledCountAggregate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductivityDashboardServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private CalendarEventRepository calendarEventRepository;

    private ProductivityDashboardService productivityDashboardService;

    @BeforeEach
    void setUp() {
        productivityDashboardService = new ProductivityDashboardService(taskRepository, calendarEventRepository);

        when(taskRepository.countByStatusAndDeletedAtIsNull(any())).thenReturn(0L);
        when(taskRepository.countByStatusInAndDueAtLessThanAndDeletedAtIsNull(anyList(), any())).thenReturn(0L);
        when(taskRepository.countByStatusInAndDueAtGreaterThanEqualAndDueAtLessThanAndDeletedAtIsNull(
                anyList(), any(), any())).thenReturn(0L);
        when(taskRepository.countByStatusAndCompletedAtGreaterThanEqualAndDeletedAtIsNull(any(), any()))
                .thenReturn(0L);
        when(taskRepository.rankAssigneesByCompleted(any(), any(), any(), any())).thenReturn(List.of());
        when(calendarEventRepository.countByStatusAndStartAtGreaterThanEqualAndStartAtLessThanAndDeletedAtIsNull(
                any(), any(), any())).thenReturn(0L);
        when(calendarEventRepository.countOverdue(any(), any())).thenReturn(0L);
    }

    @Test
    void load_countsOverdueTasksOverOpenStatusesOnly() {
        when(taskRepository.countByStatusInAndDueAtLessThanAndDeletedAtIsNull(anyList(), any())).thenReturn(7L);

        assertThat(load().tasks().overdue()).isEqualTo(7L);
    }

    @Test
    void load_computesTaskRankingSharesOverTheTotalCompleted() {
        when(taskRepository.rankAssigneesByCompleted(eq(TaskStatus.DONE), any(), any(), any()))
                .thenReturn(List.of(counted("Ana", 3), counted("Bruno", 1)));

        var ranking = load().taskRanking();

        assertThat(ranking).extracting(row -> row.share().toPlainString()).containsExactly("75.00", "25.00");
    }

    @Test
    void load_withoutAnyCompletedTasks_returnsAnEmptyRankingInsteadOfFailing() {
        assertThat(load().taskRanking()).isEmpty();
    }

    @Test
    void load_summarizesTodayThisWeekAndOverdueAgendaEvents() {
        when(calendarEventRepository.countByStatusAndStartAtGreaterThanEqualAndStartAtLessThanAndDeletedAtIsNull(
                eq(CalendarEventStatus.SCHEDULED), any(), any())).thenReturn(3L);
        when(calendarEventRepository.countOverdue(eq(CalendarEventStatus.SCHEDULED), any())).thenReturn(2L);

        var agenda = load().agenda();

        assertThat(agenda.scheduledToday()).isEqualTo(3L);
        assertThat(agenda.scheduledThisWeek()).isEqualTo(3L);
        assertThat(agenda.overdue()).isEqualTo(2L);
    }

    private ProductivityDashboardResponse load() {
        return productivityDashboardService.load(null, null);
    }

    private LabeledCountAggregate counted(String label, long count) {
        return new LabeledCountAggregate() {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public long getItemCount() {
                return count;
            }
        };
    }
}
