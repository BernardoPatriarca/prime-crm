package com.primecrm.core.report;

import com.primecrm.infra.entity.commercial.Customer;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CustomerReportGroupBy implements ReportDimension<Customer> {

    CLIENT_TYPE(ReportDimension.referenceName("clientType")),
    SEGMENT(ReportDimension.referenceName("segment")),
    ACTIVITY_BRANCH(ReportDimension.referenceName("activityBranch")),
    CATEGORY(ReportDimension.referenceName("category")),
    ORIGIN(ReportDimension.referenceName("origin")),
    STATUS(ReportDimension.referenceName("status")),
    TEAM(ReportDimension.referenceName("team")),
    OWNER(ReportDimension.referenceName("owner")),
    PERSON_TYPE(ReportDimension.attribute("personType")),
    STATE(ReportDimension.attribute("state")),
    CITY(ReportDimension.attribute("city")),
    ACTIVE(ReportDimension.attribute("active")),
    CREATED_MONTH(ReportDimension.month("createdAt"));

    private final ReportDimension<Customer> dimension;

    @Override
    public Expression<?> expression(Root<Customer> root, CriteriaBuilder cb) {
        return dimension.expression(root, cb);
    }
}
