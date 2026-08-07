package com.primecrm.core.audit;

import com.primecrm.infra.entity.audit.AuditLog;
import com.primecrm.infra.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuditLogPersister {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }
}
