package com.primecrm.infra.entity.agenda;

public enum CalendarEventStatus {
    SCHEDULED,
    DONE,
    CANCELED;

    public boolean isClosed() {
        return this == DONE || this == CANCELED;
    }
}
