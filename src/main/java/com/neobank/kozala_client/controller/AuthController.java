package com.neobank.kozala_client.controller;

import com.neobank.kozala_client.dto.ApiResponse;
import com.neobank.kozala_client.dto.auth.*;
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
@Tag(name = "Authentification", description = "Login, signup (OTP, complete, set-password), refresh et logout JWT")
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

    @PostMapping("/send-otp")
    @Operation(summary = "Envoyer l'OTP", description = "Envoie un code OTP au numéro (SMS ou log en dev). Étape 1 du signup.")
    public ResponseEntity<ApiResponse<SendOtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String phone = com.neobank.kozala_client.service.OtpService.normalizePhone(request.getPhone());
        SendOtpResponse response = authService.sendOtp(phone);
        return ResponseEntity.ok(ApiResponse.success("Code envoyé", response));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Vérifier l'OTP", description = "Vérifie le code OTP et retourne un signupToken pour les étapes suivantes. Étape 2 du signup.")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String phone = com.neobank.kozala_client.service.OtpService.normalizePhone(request.getPhone());
        VerifyOtpResponse response = authService.verifyOtp(phone, request.getCode());
        return ResponseEntity.ok(ApiResponse.success("Code validé", response));
    }

    @PostMapping("/complete-signup")
    @Operation(summary = "Compléter l'inscription", description = "Enregistre les infos légales (nom, prénom, etc.) avec le signupToken. Étape 3 du signup.")
    public ResponseEntity<ApiResponse<Void>> completeSignup(@Valid @RequestBody CompleteSignupRequest request) {
        authService.completeSignup(
                request.getSignupToken(),
                request.getFirstName(),
                request.getLastName(),
                request.getGender(),
                request.getBirthDate()
        );
        return ResponseEntity.ok(ApiResponse.success("Inscription complétée", null));
    }

    @PostMapping("/set-password")
    @Operation(summary = "Définir le mot de passe", description = "Définit le mot de passe du compte et retourne les tokens de session. Étape 4 du signup.")
    public ResponseEntity<ApiResponse<AuthResponse>> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        AuthResponse response = authService.setPassword(request.getSignupToken(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Mot de passe défini", response));
    }
}
