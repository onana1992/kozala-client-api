package com.neobank.kozala_client.repository;

/**
 * Projection pour le lookup répertoire : uniquement id, phone, displayName.
 * Évite de charger toutes les colonnes (password_hash, relations, etc.).
 */
public interface ClientLookupProjection {

    Long getId();

    String getPhone();

    String getDisplayName();
}
