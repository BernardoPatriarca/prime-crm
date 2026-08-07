package com.primecrm.core.dto.customfield;

import com.primecrm.infra.entity.config.FieldType;
import java.util.Map;
import java.util.UUID;

public record CustomFieldResponse(
        UUID id,
        String targetEntity,
        String fieldKey,
        String label,
        FieldType fieldType,
        Map<String, Object> options,
        boolean required,
        int displayOrder,
        boolean active
) {
}
