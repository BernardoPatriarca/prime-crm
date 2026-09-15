package com.primecrm.infra.entity.finance;

public enum PayableStatus {
    PENDING,
    PAID,
    CANCELED;

    public boolean isClosed() {
        return this == PAID || this == CANCELED;
    }
}
