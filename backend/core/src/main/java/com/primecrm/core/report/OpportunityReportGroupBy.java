package com.primecrm.core.report;

import com.primecrm.infra.entity.commercial.Opportunity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OpportunityReportGroupBy implements ReportDimension<Opportunity> {

    PIPELINE(ReportDimension.referenceName("pipeline")),
    STAGE(ReportDimension.referenceName("stage")),
    OUTCOME(ReportDimension.attribute("outcome")),
    OWNER(ReportDimension.referenceName("owner")),
    TEAM(ReportDimension.referenceName("team")),
    CUSTOMER(ReportDimension.referenceName("customer")),
    WIN_REASON(ReportDimension.referenceName("winReason")),
    LOSS_REASON(ReportDimension.referenceName("lossReason")),
    COMPETITOR(ReportDimension.attribute("competitor")),
    OPENED_MONTH(ReportDimension.month("openedAt")),
    EXPECTED_CLOSE_MONTH(ReportDimension.month("expectedCloseDate")),
    CLOSED_MONTH(ReportDimension.month("closedAt"));

    private final ReportDimension<Opportunity> dimension;

    @Override
    public Expression<?> expression(Root<Opportunity> root, CriteriaBuilder cb) {
        return dimension.expression(root, cb);
    }
}
