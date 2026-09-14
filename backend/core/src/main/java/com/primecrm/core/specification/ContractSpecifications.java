package com.primecrm.core.specification;

import com.primecrm.infra.entity.contract.Contract;
import com.primecrm.infra.entity.contract.ContractStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ContractSpecifications {

    private static final String[] TO_ONE_PATHS = {"customer", "order", "opportunity", "owner", "billingCycle"};

    private ContractSpecifications() {
    }

    public static Specification<Contract> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Contract> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Contract> hasStatus(ContractStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Contract> hasCustomer(UUID customerId) {
        return byReferenceId("customer", customerId);
    }

    public static Specification<Contract> hasOpportunity(UUID opportunityId) {
        return byReferenceId("opportunity", opportunityId);
    }

    public static Specification<Contract> hasOwner(UUID ownerUserId) {
        return byReferenceId("owner", ownerUserId);
    }

    public static Specification<Contract> onlyExpired(Boolean expired) {
        if (expired == null) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate isExpired = cb.and(
                    cb.equal(root.get("status"), ContractStatus.ACTIVE),
                    cb.isNotNull(root.get("endDate")),
                    cb.lessThan(root.get("endDate"), LocalDate.now()));
            return expired ? isExpired : cb.not(isExpired);
        };
    }

    public static Specification<Contract> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("notes")), pattern));
    }

    private static Specification<Contract> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
