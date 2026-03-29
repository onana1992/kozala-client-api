package com.neobank.kozala_client.service;

/** Erreur lors de l’appel distant {@code GET /api/client/transactions}. */
public class RemoteClientTransactionsException extends RuntimeException {

    public RemoteClientTransactionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
