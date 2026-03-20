package com.neobank.kozala_client.service;

/**
 * Échec de l’ouverture des comptes courant / épargne auprès de l’API distante.
 */
public class RemoteAccountOpeningException extends RuntimeException {

    public RemoteAccountOpeningException(String message) {
        super(message);
    }

    public RemoteAccountOpeningException(String message, Throwable cause) {
        super(message, cause);
    }
}
