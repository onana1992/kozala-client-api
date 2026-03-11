-- Photo de profil : nom du fichier stocké (ex: uuid.jpg)
ALTER TABLE clients ADD COLUMN profile_photo_path VARCHAR(255) NULL;
