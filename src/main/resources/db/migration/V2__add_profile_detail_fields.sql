-- Champs détail du profil (genre, date de naissance, situation familiale)
ALTER TABLE clients ADD COLUMN gender VARCHAR(20);
ALTER TABLE clients ADD COLUMN birth_date DATE;
ALTER TABLE clients ADD COLUMN marital_status VARCHAR(20);
