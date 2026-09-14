package com.primecrm.core.dto.proposal;

import com.primecrm.infra.entity.proposal.ProposalStatus;
import java.util.UUID;

public record ProposalListFilter(
        String search,
        ProposalStatus status,
        UUID customerId,
        UUID opportunityId,
        UUID ownerUserId,
        Boolean expired
) {
}
