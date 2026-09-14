package com.primecrm.infra.entity.contract;

public enum ContractStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    TERMINATED;

    public boolean isClosed() {
        return this == TERMINATED;
    }
}
