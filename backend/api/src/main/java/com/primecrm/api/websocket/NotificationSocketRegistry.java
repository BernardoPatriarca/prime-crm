package com.primecrm.api.websocket;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class NotificationSocketRegistry {

    private final Map<String, AtomicInteger> connectionsByUserId = new ConcurrentHashMap<>();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        String userId = userIdOf(event.getMessage());
        if (userId != null) {
            connectionsByUserId.computeIfAbsent(userId, id -> new AtomicInteger()).incrementAndGet();
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String userId = userIdOf(event.getMessage());
        if (userId != null) {
            connectionsByUserId.computeIfPresent(userId, (id, count) -> count.decrementAndGet() <= 0 ? null : count);
        }
    }

    public Set<String> connectedUserIds() {
        return Set.copyOf(connectionsByUserId.keySet());
    }

    private String userIdOf(org.springframework.messaging.Message<byte[]> message) {
        Principal user = StompHeaderAccessor.wrap(message).getUser();
        return user == null ? null : user.getName();
    }
}
