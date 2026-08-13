package com.primecrm.core.mapper;

import com.primecrm.core.dto.audit.AuditLogResponse;
import com.primecrm.infra.entity.audit.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);
}
