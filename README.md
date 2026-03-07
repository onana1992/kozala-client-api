# Kozala Client API

API REST pour la gestion des clients (Neobank Kozala).

## Prérequis

- Java 17+
- Maven
- MySQL 8+

## Configuration

Voir `src/main/resources/application.properties` pour la base de données et le port du serveur.

## Lancement

```bash
mvn spring-boot:run
```

L’application écoute sur le port **8082** par défaut.

## Documentation API (Swagger)

Une fois l’application démarrée, la documentation interactive est disponible ici :

- **Swagger UI** : [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- **OpenAPI JSON** : [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs)

Sur Swagger UI vous pouvez tester tous les endpoints de l’API.
