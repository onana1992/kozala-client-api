package com.neobank.kozala_client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.remote-api")
public class RemoteApiProperties {

	/**
	 * URL de base de l’API distante (ex. http://localhost:8080).
	 */
	private String baseUrl = "http://localhost:8080";

	/**
	 * Jeton JWT service pour tous les appels sortants vers l’API bancaire ({@code Authorization: Bearer …}).
	 * Obligatoire en pratique pour les routes protégées. Préférez {@code REMOTE_API_BEARER_TOKEN} ou
	 * {@code application-local.properties} (gitignored).
	 */
	private String bearerToken = "";

	/** Code produit compte courant (POST open-checking-and-savings). Env : REMOTE_CURRENT_ACCOUNT_PRODUCT_CODE */
	private String currentAccountProductCode = "CURR-STD-XAF";

	/** Code produit compte d’épargne. Env : REMOTE_SAVINGS_ACCOUNT_PRODUCT_CODE */
	private String savingsAccountProductCode = "SAV-STD-XAF";

	/**
	 * Nom du query param pour cibler le client sur GET /api/client/accounts (jeton service + {@code clientId}).
	 * Vide = requête sans paramètre (si l’API distante le permet).
	 */
	private String accountsClientIdQueryParam = "clientId";

	/**
	 * En-tête HTTP optionnel pour transmettre l’ID client (ex. {@code X-Client-Id}) en plus du query param.
	 * Vide = pas d’en-tête supplémentaire.
	 */
	private String accountsClientIdHeader = "";
}
