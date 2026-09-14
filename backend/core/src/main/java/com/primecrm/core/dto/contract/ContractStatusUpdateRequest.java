package com.primecrm.core.dto.contract;

import com.primecrm.infra.entity.contract.ContractStatus;
import jakarta.validation.constraints.NotNull;

public record ContractStatusUpdateRequest(

        @NotNull(message = "Status e obrigatorio")
        ContractStatus status
) {
}
