package com.primecrm.core.specification;

import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.CustomerTag;
import com.primecrm.infra.entity.commercial.PersonType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CustomerSpecifications {

    private static final String[] TO_ONE_PATHS = {
            "clientType", "segment", "activityBranch", "category", "origin", "status",
            "owner", "team", "parentCustomer"
    };

    private CustomerSpecifications() {
    }

    public static Specification<Customer> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Customer> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Customer> hasActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Customer> hasPersonType(PersonType personType) {
        if (personType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("personType"), personType);
    }

    public static Specification<Customer> hasClientType(UUID clientTypeId) {
        return byReferenceId("clientType", clientTypeId);
    }

    public static Specification<Customer> hasSegment(UUID segmentId) {
        return byReferenceId("segment", segmentId);
    }

    public static Specification<Customer> hasOwner(UUID ownerUserId) {
        return byReferenceId("owner", ownerUserId);
    }

    public static Specification<Customer> hasParent(UUID parentCustomerId) {
        return byReferenceId("parentCustomer", parentCustomerId);
    }

    public static Specification<Customer> hasAnyTag(Collection<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            var tagRoot = subquery.from(CustomerTag.class);
            subquery.select(tagRoot.get("customer").get("id"));
            subquery.where(cb.and(
                    cb.equal(tagRoot.get("customer").get("id"), root.get("id")),
                    tagRoot.get("domainValue").get("id").in(tagIds)));
            return cb.exists(subquery);
        };
    }

    public static Specification<Customer> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byName = cb.like(cb.lower(root.get("name")), pattern);
            Predicate byTradeName = cb.like(cb.lower(root.get("tradeName")), pattern);
            Predicate byDocument = cb.like(cb.lower(root.get("document")), pattern);
            Predicate byEmail = cb.like(cb.lower(root.get("email")), pattern);
            Predicate byCode = cb.like(cb.lower(root.get("code")), pattern);
            return cb.or(byName, byTradeName, byDocument, byEmail, byCode);
        };
    }

    private static Specification<Customer> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
