package com.primecrm.api.controller;

import com.primecrm.core.dto.dashboard.DashboardResponse;
import com.primecrm.core.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores consolidados da operacao comercial")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Indicadores do periodo (padrao: ultimos 30 dias), funil aberto do pipeline escolhido, "
            + "serie mensal dos ultimos 12 meses, ranking de responsaveis e resumo de tarefas")
    public ResponseEntity<DashboardResponse> load(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID pipelineId) {
        return ResponseEntity.ok(dashboardService.load(from, to, pipelineId));
    }
}
