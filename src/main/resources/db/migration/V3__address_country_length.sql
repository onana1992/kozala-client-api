-- Permettre noms de pays (ex: formulaire détail profil)
ALTER TABLE addresses MODIFY COLUMN country VARCHAR(100);
