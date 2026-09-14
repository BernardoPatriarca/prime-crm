package com.primecrm.core.specification;

import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.entity.order.OrderStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class OrderSpecifications {

    private static final String[] TO_ONE_PATHS = {"customer", "proposal", "opportunity", "owner"};

    private OrderSpecifications() {
    }

    public static Specification<Order> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Order> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Order> hasCustomer(UUID customerId) {
        return byReferenceId("customer", customerId);
    }

    public static Specification<Order> hasOpportunity(UUID opportunityId) {
        return byReferenceId("opportunity", opportunityId);
    }

    public static Specification<Order> hasOwner(UUID ownerUserId) {
        return byReferenceId("owner", ownerUserId);
    }

    public static Specification<Order> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("notes")), pattern));
    }

    private static Specification<Order> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
