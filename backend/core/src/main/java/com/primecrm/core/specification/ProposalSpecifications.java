package com.primecrm.core.specification;

import com.primecrm.infra.entity.proposal.Proposal;
import com.primecrm.infra.entity.proposal.ProposalStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProposalSpecifications {

    private static final String[] TO_ONE_PATHS = {"customer", "contact", "opportunity", "owner"};

    private ProposalSpecifications() {
    }

    public static Specification<Proposal> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Proposal> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Proposal> hasStatus(ProposalStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Proposal> hasCustomer(UUID customerId) {
        return byReferenceId("customer", customerId);
    }

    public static Specification<Proposal> hasOpportunity(UUID opportunityId) {
        return byReferenceId("opportunity", opportunityId);
    }

    public static Specification<Proposal> hasOwner(UUID ownerUserId) {
        return byReferenceId("owner", ownerUserId);
    }

    public static Specification<Proposal> onlyExpired(Boolean expired) {
        if (expired == null) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate isExpired = cb.and(
                    cb.equal(root.get("status"), ProposalStatus.SENT),
                    cb.isNotNull(root.get("validUntil")),
                    cb.lessThan(root.get("validUntil"), LocalDate.now()));
            return expired ? isExpired : cb.not(isExpired);
        };
    }

    public static Specification<Proposal> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("notes")), pattern));
    }

    private static Specification<Proposal> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
