package com.primecrm.core.specification;

import com.primecrm.infra.entity.config.SystemSetting;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class SystemSettingSpecifications {

    private SystemSettingSpecifications() {
    }

    public static Specification<SystemSetting> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<SystemSetting> byKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("settingKey"), key.trim());
    }
}
