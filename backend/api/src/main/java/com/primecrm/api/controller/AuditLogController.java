package com.primecrm.api.controller;

import com.primecrm.api.support.SortGuard;
import com.primecrm.core.audit.AuditLogCsvWriter;
import com.primecrm.core.dto.audit.AuditLogFilter;
import com.primecrm.core.dto.audit.AuditLogResponse;
import com.primecrm.core.service.AuditLogService;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Auditoria", description = "Trilha de auditoria de alteracoes, sessoes e extracoes de dados")
public class AuditLogController {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    private final AuditLogService auditLogService;
    private final AuditLogCsvWriter csvWriter;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDITORIA_VIEW')")
    @Operation(summary = "Lista o log de auditoria paginado, com busca textual (entidade/usuario/IP) e filtros "
            + "por entidade, registro, acao, usuario e periodo")
    public ResponseEntity<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AuditLogFilter filter = new AuditLogFilter(search, entityName, entityId, action, userId, from, to);
        return ResponseEntity.ok(
                PageResponse.from(auditLogService.list(filter, SortGuard.requireSafeSort(pageable))));
    }

    @GetMapping("/entities")
    @PreAuthorize("hasAuthority('AUDITORIA_VIEW')")
    @Operation(summary = "Nomes de entidade ja registrados no log, para alimentar o filtro da tela")
    public ResponseEntity<List<String>> entityNames() {
        return ResponseEntity.ok(auditLogService.entityNames());
    }

    @GetMapping("/{entityName}/{entityId}")
    @PreAuthorize("hasAuthority('AUDITORIA_VIEW')")
    @Operation(summary = "Linha do tempo de auditoria de um registro especifico, do mais recente para o mais antigo")
    public ResponseEntity<List<AuditLogResponse>> timeline(@PathVariable String entityName,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(auditLogService.timeline(entityName, entityId));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('AUDITORIA_EXPORT')")
    @Operation(summary = "Exporta em CSV o log de auditoria filtrado (limitado aos registros mais recentes)")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        AuditLogFilter filter = new AuditLogFilter(search, entityName, entityId, action, userId, from, to);
        byte[] content = csvWriter.write(auditLogService.export(filter)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria.csv\"")
                .contentType(MediaType.parseMediaType(CSV_CONTENT_TYPE))
                .body(content);
    }
}
