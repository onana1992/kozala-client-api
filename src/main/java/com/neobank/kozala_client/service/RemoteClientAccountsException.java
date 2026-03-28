package com.neobank.kozala_client.service;

/** Erreur lors de l’appel distant GET /api/client/accounts. */
public class RemoteClientAccountsException extends RuntimeException {

    public RemoteClientAccountsException(String message, Throwable cause) {
        super(message, cause);
    }
}
