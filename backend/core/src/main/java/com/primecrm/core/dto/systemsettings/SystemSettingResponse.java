package com.primecrm.core.dto.systemsettings;

import java.util.UUID;

public record SystemSettingResponse(
        UUID id,
        String settingKey,
        String settingValue,
        String description
) {
}
