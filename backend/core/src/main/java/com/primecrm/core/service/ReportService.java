package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.report.ReportFilter;
import com.primecrm.core.dto.report.ReportResponse;
import com.primecrm.core.report.CustomerReportGroupBy;
import com.primecrm.core.report.OpportunityReportGroupBy;
import com.primecrm.core.report.ReportAggregator;
import com.primecrm.core.report.ReportCsvWriter;
import com.primecrm.core.report.ReportFilters;
import com.primecrm.core.report.ReportQuery;
import com.primecrm.core.report.TaskReportGroupBy;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.task.Task;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String CUSTOMERS_REPORT = "CUSTOMERS";
    private static final String OPPORTUNITIES_REPORT = "OPPORTUNITIES";
    private static final String TASKS_REPORT = "TASKS";
    private static final String REPORT_ENTITY = "Report";

    private final ReportAggregator aggregator;
    private final ReportCsvWriter csvWriter;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ReportResponse customers(CustomerReportGroupBy groupBy, ReportFilter filter) {
        return aggregator.aggregate(new ReportQuery<>(CUSTOMERS_REPORT, groupBy.name(), Customer.class, groupBy,
                null, ReportFilters.of(filter, "createdAt", "owner")));
    }

    @Transactional(readOnly = true)
    public ReportResponse opportunities(OpportunityReportGroupBy groupBy, ReportFilter filter) {
        return aggregator.aggregate(new ReportQuery<>(OPPORTUNITIES_REPORT, groupBy.name(), Opportunity.class,
                groupBy, "amount", ReportFilters.of(filter, "openedAt", "owner")));
    }

    @Transactional(readOnly = true)
    public ReportResponse tasks(TaskReportGroupBy groupBy, ReportFilter filter) {
        return aggregator.aggregate(new ReportQuery<>(TASKS_REPORT, groupBy.name(), Task.class, groupBy,
                null, ReportFilters.of(filter, "dueAt", "assignee")));
    }

    public String toCsv(ReportResponse report) {
        auditService.recordChange(AuditAction.EXPORT, REPORT_ENTITY, null, Map.of(
                "report", report.report(),
                "groupBy", report.groupBy(),
                "rows", report.rows().size(),
                "totalCount", report.totalCount()));
        return csvWriter.write(report);
    }
}
