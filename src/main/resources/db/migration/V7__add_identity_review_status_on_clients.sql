-- Statut de revue identité (documents + selfie), dissocié du statut client (acceptation manuelle par le reviewer).
ALTER TABLE clients ADD COLUMN identity_review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
