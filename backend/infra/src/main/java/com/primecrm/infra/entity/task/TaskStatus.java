package com.primecrm.infra.entity.task;

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    CANCELED;

    public boolean isClosed() {
        return this == DONE || this == CANCELED;
    }
}
