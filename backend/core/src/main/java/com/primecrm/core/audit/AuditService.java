package com.primecrm.core.audit;

import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.shared.util.TenantContext;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<AuditRequestContext> requestContextProvider;

    public Map<String, Object> snapshot(Object entity) {
        return AuditChanges.snapshot(entity);
    }

    public void recordCreate(Object entity) {
        recordChange(AuditAction.CREATE, AuditChanges.entityName(entity), AuditChanges.entityId(entity),
                AuditChanges.snapshot(entity));
    }

    public void recordUpdate(Object entity, Map<String, Object> previousState) {
        recordChange(AuditAction.UPDATE, AuditChanges.entityName(entity), AuditChanges.entityId(entity),
                AuditChanges.diff(previousState, AuditChanges.snapshot(entity)));
    }

    public void recordDelete(Object entity) {
        recordChange(AuditAction.DELETE, AuditChanges.entityName(entity), AuditChanges.entityId(entity),
                AuditChanges.snapshot(entity));
    }

    public void recordChange(AuditAction action, String entityName, UUID entityId, Map<String, Object> changes) {
        try {
            AuthenticatedUser currentUser = currentUser();
            AuditRequestContext requestContext = currentRequestContext();
            eventPublisher.publishEvent(new AuditEntry(
                    entityName,
                    entityId,
                    action,
                    changes,
                    currentUser == null ? null : currentUser.id(),
                    currentUser == null ? null : currentUser.email(),
                    requestContext == null ? null : requestContext.currentIpAddress(),
                    requestContext == null ? null : requestContext.currentUserAgent(),
                    TenantContext.getCurrentTenant()));
        } catch (RuntimeException ex) {
            log.error("Falha ao preparar registro de auditoria {} de {} [{}]", action, entityName, entityId, ex);
        }
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof AuthenticatedUser user ? user : null;
    }

    private AuditRequestContext currentRequestContext() {
        return requestContextProvider == null ? null : requestContextProvider.getIfAvailable();
    }
}
