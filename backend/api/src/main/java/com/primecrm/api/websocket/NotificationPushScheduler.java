package com.primecrm.api.websocket;

import com.primecrm.core.dto.notification.NotificationListResponse;
import com.primecrm.core.service.NotificationService;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushScheduler {

    private static final String DESTINATION = "/queue/notifications";
    private static final long PUSH_INTERVAL_MS = 20_000;

    private final NotificationSocketRegistry socketRegistry;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = PUSH_INTERVAL_MS)
    public void pushToConnectedUsers() {
        socketRegistry.connectedUserIds().forEach(this::pushToUser);
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal user = StompHeaderAccessor.wrap(event.getMessage()).getUser();
        if (user != null) {
            pushToUser(user.getName());
        }
    }

    private void pushToUser(String userId) {
        try {
            NotificationListResponse response = notificationService.list(UUID.fromString(userId));
            messagingTemplate.convertAndSendToUser(userId, DESTINATION, response);
        } catch (RuntimeException ex) {
            log.warn("Falha ao enviar notificacoes via WebSocket para o usuario {}", userId, ex);
        }
    }
}
