package com.primecrm.api.controller;

import com.primecrm.core.dto.auth.ChangeOwnPasswordRequest;
import com.primecrm.core.dto.auth.LoginRequest;
import com.primecrm.core.dto.auth.LoginResponse;
import com.primecrm.core.dto.auth.MeResponse;
import com.primecrm.core.dto.auth.RefreshTokenRequest;
import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.core.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Login, refresh, logout e usuario autenticado")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Autentica com login/email + senha e retorna o par de tokens (access + refresh)")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Troca um refresh token valido por um novo par de tokens (rotacao)")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token informado")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna os dados, roles e permissoes do usuario autenticado")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(authService.me(currentUser.id()));
    }

    @PatchMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Troca a senha do proprio usuario, exigindo a senha atual. Revoga os refresh tokens ativos")
    public ResponseEntity<Void> changeOwnPassword(@Valid @RequestBody ChangeOwnPasswordRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        authService.changeOwnPassword(currentUser.id(), request);
        return ResponseEntity.noContent().build();
    }
}
