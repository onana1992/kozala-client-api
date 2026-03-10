# Redis – OTP et signup tokens

L’API utilise **Redis** pour stocker les codes OTP et les tokens d’inscription (signup), avec expiration automatique (TTL).

> **Prise en main** : pour un tutoriel structuré (installation, commandes, Spring Boot), voir **[TUTORIEL-REDIS.md](./TUTORIEL-REDIS.md)**.

## Configuration

Dans `application.properties` (ou variables d’environnement) :

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=5000
```

- **host** : adresse du serveur Redis.
- **port** : port Redis (défaut 6379).
- **password** : laisser vide si Redis n’a pas de mot de passe.
- **timeout** : délai de connexion en ms.

## Clés utilisées

| Préfixe | Exemple | Valeur | TTL |
|--------|---------|--------|-----|
| `otp:` | `otp:+237600000000` | Code OTP 6 chiffres | 5 min |
| `signup:` | `signup:a1b2c3d4e5...` | `phone\|clientId` (clientId optionnel) | 30 min |

- **OTP** : une clé par numéro de téléphone normalisé (E.164). Supprimée après validation réussie ou à l’expiration.
- **Signup token** : une clé par token (UUID). La valeur est `phone` puis `phone|clientId` après `complete-signup`. Supprimée après `set-password` ou à l’expiration.

## Dépendance et configuration

- **Maven** : `spring-boot-starter-data-redis`.
- **Config** : `config/RedisConfig.java` documente l’usage Redis (OTP + signup tokens). Le `StringRedisTemplate` est fourni par l’auto-configuration Spring Boot.
- **Lancement** : Redis doit être démarré avant l’API (sinon erreur de connexion au démarrage). En local : `redis-server` ou Docker `docker run -p 6379:6379 redis`.

## Santé Redis (Actuator)

- **Dépendance** : `spring-boot-starter-actuator`.
- **Endpoint** : `GET /actuator/health` expose le statut Redis (entrée `redis`: `UP` ou `DOWN`). Utile pour les déploiements et le monitoring.

## Démarrage sans Redis

Pour désactiver Redis (retour au stockage en mémoire), il faudrait soit exclure le starter Redis et réintroduire les implémentations en mémoire, soit configurer un profil Spring (ex. `spring.autoconfigure.exclude=...`) et fournir des beans de fallback. L’implémentation actuelle suppose Redis disponible.
