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
	 * Jeton JWT pour l’en-tête Authorization: Bearer … (vide = pas d’en-tête).
	 * Préférez une variable d’environnement ou application-local.properties (gitignored).
	 */
	private String bearerToken = "";

	/** Code produit compte courant (POST open-checking-and-savings). Env : REMOTE_CURRENT_ACCOUNT_PRODUCT_CODE */
	private String currentAccountProductCode = "CURR-STD-XAF";

	/** Code produit compte d’épargne. Env : REMOTE_SAVINGS_ACCOUNT_PRODUCT_CODE */
	private String savingsAccountProductCode = "SAV-STD-XAF";

	/**
	 * Nom du query param pour cibler le client sur GET /api/client/accounts (appel avec jeton service).
	 * Si l’API distante n’accepte pas ce paramètre, laisser vide : requête sans param (second essai : JWT utilisateur).
	 */
	private String accountsClientIdQueryParam = "clientId";

	/**
	 * En-tête HTTP optionnel pour transmettre l’ID client (ex. {@code X-Client-Id}) en plus du query param.
	 * Vide = pas d’en-tête supplémentaire.
	 */
	private String accountsClientIdHeader = "";
}
