-- Email laissé null jusqu'à la vérification email (signup sans email généré)
ALTER TABLE clients MODIFY COLUMN email VARCHAR(255) NULL;
