package com.primecrm.core.dto.systemsettings;

import jakarta.validation.constraints.NotNull;

public record SystemSettingUpdateRequest(

        @NotNull(message = "value e obrigatorio")
        String value
) {
}
