-- Identifiant client sur l’API / base bancaire « core » quand il diffère de clients.id (auto-increment local).
-- Ex. : UPDATE clients SET core_bank_client_id = 16 WHERE phone = '+237699494370';
ALTER TABLE clients ADD COLUMN core_bank_client_id BIGINT NULL;
