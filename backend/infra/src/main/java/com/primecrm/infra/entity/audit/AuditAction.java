package com.primecrm.infra.entity.audit;

public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    PASSWORD_CHANGED,
    EXPORT
}
