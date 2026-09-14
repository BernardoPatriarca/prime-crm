package com.primecrm.api.websocket;

import com.primecrm.core.security.AuthenticatedUser;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Component
public class NotificationHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        AuthenticatedUser user = (AuthenticatedUser) attributes.get(JwtHandshakeInterceptor.ATTR_AUTHENTICATED_USER);
        return user == null ? null : new StompPrincipal(user.id().toString());
    }
}
