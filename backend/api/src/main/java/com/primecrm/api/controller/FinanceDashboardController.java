package com.primecrm.api.controller;

import com.primecrm.core.dto.dashboard.FinanceDashboardResponse;
import com.primecrm.core.service.FinanceDashboardService;
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
@RequestMapping("/api/v1/dashboard/financeiro")
@RequiredArgsConstructor
@Tag(name = "Dashboard Financeiro", description = "Indicadores consolidados de contas a receber e a pagar")
public class FinanceDashboardController {

    private final FinanceDashboardService financeDashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCEIRO_VIEW')")
    @Operation(summary = "Indicadores do periodo (padrao: ultimos 30 dias) de contas a receber e a pagar "
            + "(em aberto, em atraso, movimentado no periodo com variacao contra o periodo anterior) e "
            + "serie mensal dos ultimos 12 meses de recebido x pago")
    public ResponseEntity<FinanceDashboardResponse> load(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(financeDashboardService.load(from, to));
    }
}
