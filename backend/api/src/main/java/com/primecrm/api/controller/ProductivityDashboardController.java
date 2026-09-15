package com.primecrm.api.controller;

import com.primecrm.core.dto.dashboard.ProductivityDashboardResponse;
import com.primecrm.core.service.ProductivityDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/produtividade")
@RequiredArgsConstructor
@Tag(name = "Dashboard Produtividade", description = "Indicadores consolidados de tarefas e agenda")
public class ProductivityDashboardController {

    private final ProductivityDashboardService productivityDashboardService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TAREFAS_VIEW', 'AGENDA_VIEW')")
    @Operation(summary = "Resumo de tarefas (pendentes, em andamento, em atraso, vencendo hoje, concluidas "
            + "nos ultimos 7 dias), ranking dos 5 responsaveis que mais concluiram tarefas no periodo "
            + "(padrao: ultimos 30 dias) e resumo da agenda (eventos hoje, na semana e em atraso)")
    public ResponseEntity<ProductivityDashboardResponse> load(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(productivityDashboardService.load(from, to));
    }
}
