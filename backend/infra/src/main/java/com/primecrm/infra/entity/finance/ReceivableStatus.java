package com.primecrm.infra.entity.finance;

public enum ReceivableStatus {
    PENDING,
    PAID,
    CANCELED;

    public boolean isClosed() {
        return this == PAID || this == CANCELED;
    }
}
