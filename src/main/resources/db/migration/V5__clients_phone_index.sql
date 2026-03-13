-- Index sur phone pour optimiser findByPhone, findByPhoneIn et les lookups répertoire
CREATE INDEX idx_clients_phone ON clients(phone);
