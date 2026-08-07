package com.primecrm.api.controller;

import com.primecrm.core.dto.systemsettings.SystemSettingResponse;
import com.primecrm.core.dto.systemsettings.SystemSettingUpdateRequest;
import com.primecrm.core.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system-settings")
@RequiredArgsConstructor
@Tag(name = "Configuracoes Gerais", description = "Configuracoes gerais do sistema, no formato chave/valor. As chaves sao fixas (seed)")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIGURACOES_GERAIS_VIEW')")
    @Operation(summary = "Lista todas as chaves de configuracao geral e seus valores atuais")
    public ResponseEntity<List<SystemSettingResponse>> list() {
        return ResponseEntity.ok(systemSettingService.findAll());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('CONFIGURACOES_GERAIS_EDIT')")
    @Operation(summary = "Atualiza o valor de uma chave de configuracao existente. Nao cria chaves novas")
    public ResponseEntity<SystemSettingResponse> update(@PathVariable String key,
                                                          @Valid @RequestBody SystemSettingUpdateRequest request) {
        return ResponseEntity.ok(systemSettingService.updateByKey(key, request));
    }
}
