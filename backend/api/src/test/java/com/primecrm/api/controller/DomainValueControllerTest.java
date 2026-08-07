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
import com.primecrm.core.dto.domain.DomainValueRequest;
import com.primecrm.core.dto.domain.DomainValueResponse;
import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.core.security.JwtTokenProvider;
import com.primecrm.core.service.DomainValueService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DomainValueController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class})
class DomainValueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DomainValueService domainValueService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static UsernamePasswordAuthenticationToken authenticationWith(String... authorities) {
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "admin@primecrm.local", "admin",
                "Administrador", List.of("Administrador"), List.of(authorities));
        List<GrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities);
    }

    @Test
    void list_withDominiosViewAuthority_returns200() throws Exception {
        DomainValueResponse response = new DomainValueResponse(UUID.randomUUID(), "PRIORITY", "Prioridade", "ALTA",
                "Alta", null, "#F97316", null, 3, null, true);
        when(domainValueService.list(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/domain-values").param("type", "PRIORITY")
                        .with(authentication(authenticationWith("DOMINIOS_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("ALTA"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_withoutDominiosViewAuthority_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/domain-values")
                        .with(authentication(authenticationWith("OUTRA_PERMISSAO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/domain-values"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withMissingRequiredFields_returns400() throws Exception {
        DomainValueRequest invalidRequest = new DomainValueRequest("", "", "", null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/domain-values")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(authentication(authenticationWith("DOMINIOS_CREATE"))))
                .andExpect(status().isBadRequest());
    }
}
