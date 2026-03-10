# Tutoriel Redis – Prise en main

Ce document est un tutoriel structuré pour bien démarrer avec **Redis** dans le contexte du projet Kozala (stockage OTP et signup tokens). Il couvre les concepts de base, l’installation, les commandes utiles et l’usage avec Spring Boot.

---

## 1. Qu’est-ce que Redis ?

### 1.1 En bref

- **Redis** = *RE*mote *DI*ctionary *S*erver.
- Base de données **clé-valeur** en mémoire, très rapide.
- Données stockées en **RAM** (avec possibilité de persistance sur disque).
- Supporte des **structures** : chaînes, listes, ensembles, hashes, etc.
- Idéal pour : **cache**, **sessions**, **données temporaires avec expiration** (OTP, tokens).

### 1.2 Pourquoi Redis dans ce projet ?

| Besoin | Solution Redis |
|--------|----------------|
| Codes OTP valides 5 min | Clé `otp:phone` → valeur code, TTL 5 min. |
| Tokens d’inscription 30 min | Clé `signup:token` → valeur phone\|clientId, TTL 30 min. |
| Plusieurs instances d’API | Données partagées entre serveurs (pas seulement en mémoire locale). |
| Expiration automatique | TTL natif : plus besoin de nettoyer soi-même. |

---

## 2. Installation et démarrage

### 2.1 Option 1 : Redis en local

**Windows (WSL2 ou Redis pour Windows)**  
- Télécharger depuis [redis.io](https://redis.io/download) ou utiliser un installateur Windows.  
- Ou dans WSL : `sudo apt install redis-server` puis `redis-server`.

**Linux / macOS**  
```bash
# macOS (Homebrew)
brew install redis
brew services start redis

# Linux (Debian/Ubuntu)
sudo apt update && sudo apt install redis-server
sudo systemctl start redis-server
```

**Vérifier que Redis tourne**  
```bash
redis-cli ping
# Réponse attendue : PONG
```

### 2.2 Option 2 : Docker

```bash
# Lancer un conteneur Redis (port 6379)
docker run -d --name redis -p 6379:6379 redis:latest

# Arrêter
docker stop redis

# Démarrer à nouveau
docker start redis
```

### 2.3 Connexion depuis la machine

- **Port par défaut** : 6379.
- **Client en ligne de commande** : `redis-cli` (connexion à `localhost:6379`).
- Pour un serveur distant : `redis-cli -h <host> -p <port> -a <password>` (si mot de passe).

---

## 3. Concepts de base

### 3.1 Modèle clé-valeur

- Chaque donnée est associée à une **clé** (chaîne de caractères).
- Une clé pointe vers une **valeur** (chaîne, nombre, structure…).
- Pas de schéma : vous choisissez la convention de nommage (ex. `otp:+237600000000`).

### 3.2 TTL (Time To Live)

- **TTL** = durée de vie d’une clé en secondes (ou millisecondes).
- À l’expiration, Redis **supprime automatiquement** la clé.
- Commandes utiles :
  - `EXPIRE clé secondes` : fixe un TTL en secondes.
  - `TTL clé` : retourne le nombre de secondes restantes (-1 = pas d’expiration, -2 = clé inexistante).
  - `SET clé valeur EX secondes` : créer une clé avec TTL directement.

### 3.3 Types de données (les plus utilisés)

| Type | Description | Exemple d’usage |
|------|-------------|------------------|
| **String** | Chaîne ou nombre | OTP, token, compteur |
| **Hash** | Ensemble de champs/valeurs | Objet (ex. user:123 → name, email) |
| **List** | Liste ordonnée | File d’attente, historique |
| **Set** | Ensemble non ordonné, sans doublon | Tags, IDs |

Dans ce projet, on n’utilise que des **strings** (via `StringRedisTemplate`).

---

## 4. Commandes CLI essentielles

À taper dans `redis-cli`.

### 4.1 Chaînes (strings)

```bash
# Créer / remplacer une clé
SET ma:cle "ma valeur"

# Créer avec expiration (ex. 300 secondes = 5 min)
SET otp:+237600000000 "123456" EX 300

# Lire une valeur
GET otp:+237600000000

# Supprimer une clé
DEL otp:+237600000000

# Vérifier si une clé existe
EXISTS otp:+237600000000
# 1 = existe, 0 = n’existe pas
```

### 4.2 TTL et expiration

```bash
# Définir un TTL après coup (en secondes)
EXPIRE ma:cle 600

# Voir le TTL restant (secondes)
TTL ma:cle
# -1 = pas d’expiration, -2 = clé supprimée ou inexistante
```

### 4.3 Exploration

```bash
# Lister les clés correspondant à un motif (à utiliser avec modération en prod)
KEYS otp:*
KEYS signup:*

# Nombre de clés en base
DBSIZE

# Tout supprimer (ATTENTION : efface toute la base courante)
FLUSHDB
```

### 4.4 Exemple de scénario OTP (à la main)

```bash
# 1. Simuler l’envoi d’un OTP
SET otp:+237600000000 "123456" EX 300

# 2. Vérifier la valeur
GET otp:+237600000000
# "123456"

# 3. Vérifier le TTL
TTL otp:+237600000000
# (nombre de secondes restantes)

# 4. Après validation réussie : supprimer
DEL otp:+237600000000
```

---

## 5. Redis avec Spring Boot

### 5.1 Dépendance Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Spring Boot configure automatiquement une connexion Redis à partir des propriétés.

### 5.2 Configuration (`application.properties`)

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=5000
```

- **password** : laisser vide si Redis n’a pas de mot de passe.
- **timeout** : délai d’attente de la connexion (ms).

### 5.3 Utiliser `StringRedisTemplate`

Le bean `StringRedisTemplate` permet d’envoyer des **clés et valeurs de type String** (idéal pour OTP et tokens).

**Injection :**

```java
private final StringRedisTemplate redisTemplate;
```

**Opérations de base :**

| Méthode | Équivalent Redis | Description |
|---------|------------------|-------------|
| `opsForValue().set(key, value)` | `SET key value` | Écrire une valeur. |
| `opsForValue().set(key, value, timeout, unit)` | `SET key value EX n` | Écrire avec TTL. |
| `opsForValue().get(key)` | `GET key` | Lire une valeur. |
| `delete(key)` | `DEL key` | Supprimer une clé. |

**Exemple (équivalent OtpService) :**

```java
// Stocker un OTP 5 minutes
String key = "otp:" + phone;
redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

// Lire et valider
String stored = redisTemplate.opsForValue().get(key);
boolean valid = code.equals(stored);
if (valid) {
    redisTemplate.delete(key);
}
```

---

## 6. Cas d’usage dans le projet Kozala

### 6.1 OTP (OtpService)

- **Clé** : `otp:{phone}` (ex. `otp:+237600000000`).
- **Valeur** : code à 6 chiffres.
- **TTL** : 5 minutes.
- **Flux** :
  1. `send-otp` → `SET otp:phone code EX 300`.
  2. `verify-otp` → `GET otp:phone`, comparer au code saisi, puis `DEL otp:phone` si ok.

Fichier : `service/OtpService.java`.

### 6.2 Signup tokens (SignupTokenStore)

- **Clé** : `signup:{token}` (token = UUID).
- **Valeur** : `phone` puis `phone|clientId` après complétion du profil.
- **TTL** : 30 minutes.
- **Flux** :
  1. Après `verify-otp` → `SET signup:token phone| EX 1800`.
  2. Après `complete-signup` → `SET signup:token phone|clientId EX 1800`.
  3. À `set-password` → `GET signup:token` puis `DEL signup:token`.

Fichier : `service/SignupTokenStore.java`.

### 6.3 Vérifier en direct (redis-cli)

Pendant les tests, après un appel `send-otp` :

```bash
redis-cli
KEYS otp:*
GET otp:+237600000000
TTL otp:+237600000000
```

Après un `verify-otp` :

```bash
KEYS signup:*
GET signup:<token_affiché_dans_les_logs_ou_reponse_api>
```

---

## 7. Bonnes pratiques et pièges

### 7.1 Conventions de clés

- **Préfixe par fonction** : `otp:`, `signup:`, `session:` pour éviter les collisions.
- **Clés explicites** : `otp:+237600000000` plutôt qu’un ID opaque sans sens.

### 7.2 TTL

- Toujours définir un **TTL** pour les données temporaires (OTP, tokens) pour éviter de remplir la mémoire.
- Redis supprime les clés expirées automatiquement (stratégie configurable).

### 7.3 KEYS en production

- **Éviter** `KEYS *` ou `KEYS prefix*` sur de grosses bases : la commande bloque Redis.
- Pour parcourir des clés en prod, privilégier **SCAN** (itératif).

### 7.4 Mot de passe et réseau

- En production : utiliser un **mot de passe** (`requirepass` côté Redis, `spring.data.redis.password` côté app).
- Préférer un **réseau privé** (Redis non exposé sur Internet).

### 7.5 Persistance (optionnel)

- Par défaut Redis peut perdre les données en cas d’arrêt (tout en RAM).
- Pour de la persistance : activer **RDB** et/ou **AOF** dans la config Redis (hors scope de ce tutoriel).

---

## 8. Résumé et checklist

| Étape | Action |
|-------|--------|
| 1 | Installer et démarrer Redis (local ou Docker). |
| 2 | Vérifier avec `redis-cli ping` → `PONG`. |
| 3 | Configurer `application.properties` (host, port, password si besoin). |
| 4 | Démarrer l’API Kozala : les services utilisent Redis pour OTP et signup. |
| 5 | Déboguer avec `redis-cli` : `KEYS otp:*`, `KEYS signup:*`, `GET`, `TTL`, `DEL`. |

---

## 9. Ressources

- [Documentation officielle Redis](https://redis.io/docs/)
- [Commandes Redis (reference)](https://redis.io/commands/)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- Dans ce projet : `docs/REDIS-OTP-SIGNUP.md` pour le détail des clés et de la configuration.
