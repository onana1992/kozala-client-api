-- Statuts de revue KYC (email, profil) sur clients. Identité = status (PENDING_REVIEW, VERIFIED, etc.)
ALTER TABLE clients ADD COLUMN email_review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE clients ADD COLUMN profile_review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
