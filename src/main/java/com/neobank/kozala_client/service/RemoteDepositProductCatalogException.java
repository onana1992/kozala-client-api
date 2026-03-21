package com.neobank.kozala_client.service;

/** Erreur lors de l’appel distant au catalogue produits de dépôt. */
public class RemoteDepositProductCatalogException extends RuntimeException {

    public RemoteDepositProductCatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
