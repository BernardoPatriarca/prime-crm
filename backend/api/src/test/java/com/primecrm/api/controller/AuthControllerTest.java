package com.primecrm.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.primecrm.api.config.SecurityConfig;
import com.primecrm.api.security.JwtAuthenticationFilter;
import com.primecrm.api.security.RestAccessDeniedHandler;
import com.primecrm.api.security.RestAuthenticationEntryPoint;
import com.primecrm.core.dto.auth.LoginRequest;
import com.primecrm.core.dto.auth.LoginResponse;
import com.primecrm.core.dto.auth.MeResponse;
import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.core.security.JwtTokenProvider;
import com.primecrm.core.service.AuthService;
import com.primecrm.infra.entity.auth.UserStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void login_withValidCredentials_returns200AndTokenPair() throws Exception {
        MeResponse me = new MeResponse(UUID.randomUUID(), "Administrador", "admin@primecrm.local", "admin",
                UserStatus.ACTIVE, null, List.of("Administrador"), List.of("USUARIOS_VIEW"));
        LoginResponse response = LoginResponse.of("access-token", "refresh-token", 900L, me);

        when(authService.login(any())).thenReturn(response);

        LoginRequest request = new LoginRequest("admin", "Admin@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.user.login").value("admin"));
    }

    @Test
    void login_withBlankBody_returns400() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_withAuthenticatedPrincipal_returnsUserData() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser(userId, "admin@primecrm.local", "admin",
                "Administrador", List.of("Administrador"), List.of("USUARIOS_VIEW"));
        MeResponse me = new MeResponse(userId, "Administrador", "admin@primecrm.local", "admin",
                UserStatus.ACTIVE, null, List.of("Administrador"), List.of("USUARIOS_VIEW"));

        when(authService.me(userId)).thenReturn(me);

        mockMvc.perform(get("/api/v1/auth/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("admin"));
    }

    @Test
    void me_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
