package com.neobank.kozala_client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Fournit un {@link RestClient} pointant sur {@code app.remote-api.base-url} avec
 * {@code Authorization: Bearer} issu de {@code app.remote-api.bearer-token}.
 * Les services ne doivent pas surcharger cet en-tête avec le JWT utilisateur : l’API distante
 * s’authentifie uniquement avec ce jeton service.
 * <p>Injection : {@code @Qualifier(RemoteApiConfig.REMOTE_API_REST_CLIENT) RestClient client}
 */
@Configuration
public class RemoteApiConfig {

	public static final String REMOTE_API_REST_CLIENT = "remoteApiRestClient";

	@Bean(name = REMOTE_API_REST_CLIENT)
	RestClient remoteApiRestClient(RemoteApiProperties props) {
		RestClient.Builder builder = RestClient.builder().baseUrl(trimTrailingSlash(props.getBaseUrl()));
		if (StringUtils.hasText(props.getBearerToken())) {
			builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getBearerToken().trim());
		}
		return builder.build();
	}

	private static String trimTrailingSlash(String url) {
		if (url == null || url.isEmpty()) {
			return "http://localhost:8080";
		}
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
