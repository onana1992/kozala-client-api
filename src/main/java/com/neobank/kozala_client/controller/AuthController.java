package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.auth.AuthResponse;
import com.neobank.kozala_client.dto.auth.LoginRequest;
import com.neobank.kozala_client.dto.auth.RefreshRequest;
import com.neobank.kozala_client.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Login, refresh et logout JWT (téléphone + mot de passe)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Connexion", description = "Authentification par téléphone et mot de passe. Retourne access et refresh tokens.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request.getPhone(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir les tokens", description = "Échange le refresh token contre un nouvel access token et un nouveau refresh token (rotation).")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Tokens renouvelés", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Révoque le refresh token fourni. Corps : { \"refreshToken\": \"...\" }.")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie", null));
    }
}
