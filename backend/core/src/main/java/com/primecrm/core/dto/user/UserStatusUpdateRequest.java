package com.primecrm.core.dto.user;

import com.primecrm.infra.entity.auth.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(

        @NotNull(message = "Status e obrigatorio")
        UserStatus status
) {
}
