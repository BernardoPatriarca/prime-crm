package com.primecrm.core.audit;

public interface AuditRequestContext {

    String currentIpAddress();

    String currentUserAgent();
}
