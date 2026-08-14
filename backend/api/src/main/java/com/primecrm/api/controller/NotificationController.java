package com.primecrm.api.controller;

import com.primecrm.core.dto.notification.NotificationListResponse;
import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.core.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificacoes", description = "Alertas derivados da operacao: tarefas, oportunidades e leads")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Alertas do usuario logado: tarefas atrasadas e do dia, oportunidades com previsao de "
            + "fechamento vencida e leads sem responsavel")
    public ResponseEntity<NotificationListResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(notificationService.list(currentUser == null ? null : currentUser.id()));
    }
}
