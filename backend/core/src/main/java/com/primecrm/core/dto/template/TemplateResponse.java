package com.primecrm.core.dto.template;

import com.primecrm.infra.entity.config.TemplateType;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        TemplateType type,
        String name,
        String subject,
        String content,
        boolean active
) {
}
