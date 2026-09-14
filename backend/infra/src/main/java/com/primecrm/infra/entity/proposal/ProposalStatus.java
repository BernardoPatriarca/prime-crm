package com.primecrm.infra.entity.proposal;

public enum ProposalStatus {
    DRAFT,
    SENT,
    ACCEPTED,
    REJECTED;

    public boolean isClosed() {
        return this == ACCEPTED || this == REJECTED;
    }
}
