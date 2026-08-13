package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.report.ReportFilter;
import com.primecrm.core.dto.report.ReportGroupRow;
import com.primecrm.core.dto.report.ReportResponse;
import com.primecrm.core.report.CustomerReportGroupBy;
import com.primecrm.core.report.OpportunityReportGroupBy;
import com.primecrm.core.report.ReportAggregator;
import com.primecrm.core.report.ReportCsvWriter;
import com.primecrm.core.report.ReportQuery;
import com.primecrm.core.report.TaskReportGroupBy;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.task.Task;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportAggregator aggregator;
    @Mock
    private ReportCsvWriter csvWriter;
    @Mock
    private AuditService auditService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(aggregator, csvWriter, auditService);
    }

    @Test
    void customers_groupsCustomersWithoutMeasureAndFiltersByCreationDate() {
        when(aggregator.aggregate(any())).thenReturn(emptyReport("CUSTOMERS"));

        reportService.customers(CustomerReportGroupBy.SEGMENT, ReportFilter.empty());

        ReportQuery<?> query = captureQuery();
        assertThat(query.entityType()).isEqualTo(Customer.class);
        assertThat(query.groupBy()).isEqualTo("SEGMENT");
        assertThat(query.measured()).isFalse();
    }

    @Test
    void opportunities_sumsTheAmountColumn() {
        when(aggregator.aggregate(any())).thenReturn(emptyReport("OPPORTUNITIES"));

        reportService.opportunities(OpportunityReportGroupBy.STAGE, ReportFilter.empty());

        ReportQuery<?> query = captureQuery();
        assertThat(query.entityType()).isEqualTo(Opportunity.class);
        assertThat(query.measureAttribute()).isEqualTo("amount");
        assertThat(query.measured()).isTrue();
    }

    @Test
    void tasks_groupsTasksWithoutMeasure() {
        when(aggregator.aggregate(any())).thenReturn(emptyReport("TASKS"));

        reportService.tasks(TaskReportGroupBy.STATUS, new ReportFilter(Instant.now(), null, null));

        ReportQuery<?> query = captureQuery();
        assertThat(query.entityType()).isEqualTo(Task.class);
        assertThat(query.measured()).isFalse();
    }

    @Test
    void toCsv_registersAnExportEntryInTheAuditTrail() {
        ReportResponse report = new ReportResponse("TASKS", "STATUS", false, 3, null, Instant.now(),
                List.of(new ReportGroupRow("PENDING", 3, null, new BigDecimal("100.00"))));
        when(csvWriter.write(report)).thenReturn("csv");

        assertThat(reportService.toCsv(report)).isEqualTo("csv");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordChange(eq(AuditAction.EXPORT), eq("Report"), isNull(), captor.capture());
        assertThat(captor.getValue()).containsEntry("report", "TASKS").containsEntry("groupBy", "STATUS");
    }

    private ReportQuery<?> captureQuery() {
        ArgumentCaptor<ReportQuery<?>> captor = ArgumentCaptor.forClass(ReportQuery.class);
        verify(aggregator).aggregate(captor.capture());
        return captor.getValue();
    }

    private ReportResponse emptyReport(String report) {
        return new ReportResponse(report, "ANY", false, 0, null, Instant.now(), List.of());
    }
}
