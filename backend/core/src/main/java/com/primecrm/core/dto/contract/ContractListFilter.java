package com.primecrm.core.dto.contract;

import com.primecrm.infra.entity.contract.ContractStatus;
import java.util.UUID;

public record ContractListFilter(
        String search,
        ContractStatus status,
        UUID customerId,
        UUID opportunityId,
        UUID ownerUserId,
        Boolean expired
) {
}
