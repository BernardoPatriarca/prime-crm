package com.primecrm.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.primecrm.api.config.RateLimitProperties;
import com.primecrm.shared.dto.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final InMemoryRateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isLoginPath = LOGIN_PATH.equals(request.getRequestURI());
        RateLimitProperties.Limit limit = isLoginPath ? properties.getLogin() : properties.getGeneral();
        String key = (isLoginPath ? "login:" : "general:") + request.getRemoteAddr();

        if (!rateLimiter.tryConsume(key, limit.getCapacity(), limit.getWindowSeconds())) {
            writeRateLimitExceeded(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitExceeded(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "RATE_LIMIT_EXCEEDED",
                "Numero maximo de requisicoes excedido. Tente novamente em instantes.",
                request.getRequestURI(),
                null
        );
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
