-- À exécuter dans votre base (ex. hpaytest_devhpaycash)
-- Création de la table beneficiaries (sans contraintes FK pour éviter l'Errcode 150)
-- L'appli gère la cohérence côté métier ; owner_client_id et beneficiary_client_id
-- pointent logiquement vers clients(id).

CREATE TABLE IF NOT EXISTS beneficiaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_client_id BIGINT NOT NULL,
    beneficiary_client_id BIGINT NULL,
    phone VARCHAR(20) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_beneficiaries_owner (owner_client_id),
    INDEX idx_beneficiaries_owner_phone (owner_client_id, phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
