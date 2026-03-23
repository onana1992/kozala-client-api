package com.neobank.kozala_client.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /** Nom du schéma référencé par les SecurityRequirement OpenAPI (bouton Authorize dans Swagger UI). */
    public static final String BEARER_JWT = "bearer-jwt";

    @Bean
    public OpenAPI kozalaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kozala Client API")
                        .version("1.0")
                        .description("API client Kozala. Cliquez sur **Authorize**, saisissez l’access token JWT (sans le préfixe « Bearer ») pour appeler les routes protégées."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Access token obtenu via `POST /api/auth/login`, `POST /api/auth/verify-login-otp` ou `POST /api/auth/refresh`.")));
    }
}
