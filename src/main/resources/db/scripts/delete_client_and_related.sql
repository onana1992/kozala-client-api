-- Supprime un client et toutes ses données liées (ordre respectant les clés étrangères).
-- Usage : remplacer @client_id par l'ID du client à supprimer, puis exécuter le script.
-- Base : core_auth_test (ou celle définie dans application.properties).

SET @client_id = 6;  -- TODO: remplacer par l'ID du client à supprimer

-- Enfants : tables qui référencent clients
DELETE FROM beneficiaries WHERE owner_client_id = @client_id;
DELETE FROM refresh_tokens WHERE client_id = @client_id;
DELETE FROM documents WHERE client_id = @client_id;
DELETE FROM addresses WHERE client_id = @client_id;
DELETE FROM kyc_checks WHERE client_id = @client_id;
DELETE FROM related_persons WHERE client_id = @client_id;

-- Parent
DELETE FROM clients WHERE id = @client_id;

-- Vérification (optionnel) : doit retourner 0 ligne
-- SELECT COUNT(*) FROM clients WHERE id = @client_id;
