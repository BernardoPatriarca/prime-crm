package com.primecrm.infra.entity.order;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    DELIVERED,
    CANCELED;

    public boolean isClosed() {
        return this == DELIVERED || this == CANCELED;
    }
}
