package com.primecrm.core.specification;

import com.primecrm.infra.entity.product.Product;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private static final String[] TO_ONE_PATHS = {"category", "unit"};

    private ProductSpecifications() {
    }

    public static Specification<Product> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Product> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Product> hasCategory(UUID categoryId) {
        return byReferenceId("category", categoryId);
    }

    public static Specification<Product> hasUnit(UUID unitId) {
        return byReferenceId("unit", unitId);
    }

    public static Specification<Product> isService(Boolean service) {
        if (service == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("service"), service);
    }

    public static Specification<Product> isActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Product> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("sku")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    private static Specification<Product> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
