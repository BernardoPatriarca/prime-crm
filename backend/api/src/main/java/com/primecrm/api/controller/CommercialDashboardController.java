package com.primecrm.api.controller;

import com.primecrm.core.dto.dashboard.CommercialDashboardResponse;
import com.primecrm.core.service.CommercialDashboardService;
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
@RequestMapping("/api/v1/dashboard/comercial")
@RequiredArgsConstructor
@Tag(name = "Dashboard Comercial", description = "Indicadores consolidados de propostas, pedidos e contratos")
public class CommercialDashboardController {

    private final CommercialDashboardService commercialDashboardService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PROPOSTAS_VIEW', 'PEDIDOS_VIEW', 'CONTRATOS_VIEW')")
    @Operation(summary = "Indicadores do periodo (padrao: ultimos 30 dias) de propostas emitidas e pedidos "
            + "feitos no periodo (com taxa de conversao para aceita/entregue), contratos ativos (valor "
            + "recorrente total) e contratos vencendo nos proximos 30 dias, alem da serie mensal dos "
            + "ultimos 12 meses de valor de propostas x pedidos")
    public ResponseEntity<CommercialDashboardResponse> load(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(commercialDashboardService.load(from, to));
    }
}
