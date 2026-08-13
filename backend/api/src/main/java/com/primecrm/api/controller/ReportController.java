package com.primecrm.api.controller;

import com.primecrm.core.dto.report.ReportFilter;
import com.primecrm.core.dto.report.ReportResponse;
import com.primecrm.core.report.CustomerReportGroupBy;
import com.primecrm.core.report.OpportunityReportGroupBy;
import com.primecrm.core.report.TaskReportGroupBy;
import com.primecrm.core.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Relatorios", description = "Extracao de informacoes agrupadas de clientes, oportunidades e tarefas")
public class ReportController {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    private final ReportService reportService;

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('RELATORIOS_VIEW')")
    @Operation(summary = "Clientes agrupados pela dimensao escolhida. O periodo filtra a data de cadastro "
            + "e userId filtra o responsavel pela conta")
    public ResponseEntity<ReportResponse> customers(
            @RequestParam CustomerReportGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(reportService.customers(groupBy, new ReportFilter(from, to, userId)));
    }

    @GetMapping("/opportunities")
    @PreAuthorize("hasAuthority('RELATORIOS_VIEW')")
    @Operation(summary = "Oportunidades agrupadas pela dimensao escolhida, com soma de valores. O periodo "
            + "filtra a data de abertura e userId filtra o responsavel")
    public ResponseEntity<ReportResponse> opportunities(
            @RequestParam OpportunityReportGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(reportService.opportunities(groupBy, new ReportFilter(from, to, userId)));
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('RELATORIOS_VIEW')")
    @Operation(summary = "Tarefas agrupadas pela dimensao escolhida. O periodo filtra a data de vencimento "
            + "e userId filtra o responsavel pela tarefa")
    public ResponseEntity<ReportResponse> tasks(
            @RequestParam TaskReportGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(reportService.tasks(groupBy, new ReportFilter(from, to, userId)));
    }

    @GetMapping("/customers/export")
    @PreAuthorize("hasAuthority('RELATORIOS_EXPORT')")
    @Operation(summary = "Exporta em CSV o relatorio de clientes agrupado pela dimensao escolhida")
    public ResponseEntity<byte[]> exportCustomers(
            @RequestParam CustomerReportGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID userId) {
        return csv("clientes", reportService.customers(groupBy, new ReportFilter(from, to, userId)), groupBy.name());
    }

    @GetMapping("/opportunities/export")
    @PreAuthorize("hasAuthority('RELATORIOS_EXPORT')")
    @Operation(summary = "Exporta em CSV o relatorio de oportunidades agrupado pela dimensao escolhida")
    public ResponseEntity<byte[]> exportOpportunities(
            @RequestParam OpportunityReportGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID userId) {
        return csv("oportunidades", reportService.opportunities(groupBy, new ReportFilter(from, to, userId)),
                groupBy.name());
    }

    @GetMapping("/tasks/export")
    @PreAuthorize("hasAuthority('RELATORIOS_EXPORT')")
    @Operation(summary = "Exporta em CSV o relatorio de tarefas agrupado pela dimensao escolhida")
    public ResponseEntity<byte[]> exportTasks(
            @RequestParam TaskReportGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID userId) {
        return csv("tarefas", reportService.tasks(groupBy, new ReportFilter(from, to, userId)), groupBy.name());
    }

    private ResponseEntity<byte[]> csv(String prefix, ReportResponse report, String groupBy) {
        byte[] content = reportService.toCsv(report).getBytes(StandardCharsets.UTF_8);
        String fileName = "%s-%s.csv".formatted(prefix, groupBy.toLowerCase());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(CSV_CONTENT_TYPE))
                .body(content);
    }
}
