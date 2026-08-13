package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.audit.AuditLogFilter;
import com.primecrm.core.dto.audit.AuditLogResponse;
import com.primecrm.core.mapper.AuditLogMapper;
import com.primecrm.core.specification.AuditLogSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.audit.AuditLog;
import com.primecrm.infra.repository.AuditLogRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    public static final int MAX_EXPORT_ROWS = 5000;

    private static final String AUDIT_ENTITY = "AuditLog";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(AuditLogFilter filter, Pageable pageable) {
        return auditLogRepository.findAll(toSpecification(filter), pageable).map(auditLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> timeline(String entityName, UUID entityId) {
        AuditLogFilter filter = new AuditLogFilter(null, entityName, entityId, null, null, null, null);
        return auditLogRepository
                .findAll(toSpecification(filter), PageRequest.of(0, MAX_EXPORT_ROWS, NEWEST_FIRST))
                .map(auditLogMapper::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<String> entityNames() {
        return auditLogRepository.findDistinctEntityNames();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> export(AuditLogFilter filter) {
        List<AuditLogResponse> rows = auditLogRepository
                .findAll(toSpecification(filter), PageRequest.of(0, MAX_EXPORT_ROWS, NEWEST_FIRST))
                .map(auditLogMapper::toResponse)
                .getContent();
        auditService.recordChange(AuditAction.EXPORT, AUDIT_ENTITY, null, Map.of("rows", rows.size()));
        return rows;
    }

    private Specification<AuditLog> toSpecification(AuditLogFilter filter) {
        return SpecificationUtils.and(
                AuditLogSpecifications.textSearch(filter.search()),
                AuditLogSpecifications.hasEntityName(filter.entityName()),
                AuditLogSpecifications.hasEntityId(filter.entityId()),
                AuditLogSpecifications.hasAction(filter.action()),
                AuditLogSpecifications.hasUser(filter.userId()),
                AuditLogSpecifications.from(filter.from()),
                AuditLogSpecifications.to(filter.to()));
    }
}
