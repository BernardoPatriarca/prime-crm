package com.primecrm.core.dto.proposal;

import com.primecrm.infra.entity.proposal.ProposalStatus;
import jakarta.validation.constraints.NotNull;

public record ProposalStatusUpdateRequest(

        @NotNull(message = "Status e obrigatorio")
        ProposalStatus status
) {
}
