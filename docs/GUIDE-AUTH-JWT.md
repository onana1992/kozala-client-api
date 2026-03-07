# Guide d’authentification JWT (téléphone + mot de passe)

Ce guide décrit comment mettre en place une authentification JWT dans l’API Kozala Client, en utilisant **numéro de téléphone** et **mot de passe**, avec **access token** et **refresh token**.

---

## 1. Vue d’ensemble

### 1.1 Access token vs Refresh token

| Token | Rôle | Durée de vie | Où est-il envoyé ? |
|-------|------|----------------|---------------------|
| **Access token** | Accéder aux ressources (API protégées) | Courte (ex. 15 min – 1 h) | En-tête `Authorization: Bearer <accessToken>` |
| **Refresh token** | Obtenir un nouvel access token sans retaper le mot de passe | Longue (ex. 7–30 jours) | Uniquement vers `POST /api/auth/refresh` (body ou cookie HttpOnly) |

- L’**access token** expire souvent ; au lieu de redemander le mot de passe, le client envoie le **refresh token** pour en obtenir un nouveau (et éventuellement un nouveau refresh token).
- Le **refresh token** est soit stocké en base (révocable, plus sécurisé), soit émis comme JWT long (sans DB mais moins facile à révoquer).

### 1.2 Flux d’authentification (login)

```
┌─────────────┐     POST /api/auth/login      ┌─────────────┐
│   Client    │  { "phone": "...", "password": "..." }  │   API      │
│  (Mobile)   │ ──────────────────────────────►│   Backend   │
└─────────────┘                                 └──────┬──────┘
       ▲                                                │
       │                                                │ 1. Vérifier phone + password
       │                                                │ 2. Générer access + refresh
       │                                                │ 3. Persister le refresh (si DB)
       │                                                ▼
       │  { "accessToken": "eyJ...", "refreshToken": "eyJ...",   │
       │    "expiresIn": 900, "refreshExpiresIn": 604800 }      │
       │◄──────────────────────────────────────────────────────│
       │
       │  Requêtes API : Authorization: Bearer <accessToken>
       ▼
┌─────────────┐     GET /api/clients/1        ┌─────────────┐
│   Client    │  Authorization: Bearer <accessToken>   │   API       │
└─────────────┘ ─────────────────────────────►│   Backend   │
                                               └─────────────┘
```

### 1.3 Flux de rafraîchissement (refresh)

```
┌─────────────┐     POST /api/auth/refresh    ┌─────────────┐
│   Client    │  { "refreshToken": "eyJ..." }  │   API       │
│  (Mobile)   │ ─────────────────────────────►│   Backend   │
└─────────────┘                                 └──────┬──────┘
       ▲                                                │
       │                                                │ 1. Valider le refresh token
       │                                                │ 2. (Si DB) vérifier qu’il existe et n’est pas révoqué
       │                                                │ 3. Générer nouvel access (+ optionnellement nouveau refresh)
       │                                                ▼
       │  { "accessToken": "eyJ...", "refreshToken": "eyJ...", ... }
       │◄──────────────────────────────────────────────────────
```

Quand l’**access token** expire (401), le client appelle `/api/auth/refresh` avec le **refresh token** pour obtenir un nouvel access token (sans redemander phone/password).

### 1.4 Rôles des composants

| Composant | Rôle |
|-----------|------|
| **Login (phone + password)** | Vérifier les identifiants, émettre access + refresh tokens |
| **Access token (JWT)** | Identité (`sub`), courte durée ; utilisé pour les appels API |
| **Refresh token** | Longue durée ; utilisé uniquement pour obtenir un nouvel access token (endpoint dédié) |
| **Endpoint /api/auth/refresh** | Accepter un refresh token valide, renvoyer un nouvel access (et optionnellement un nouveau refresh) |
| **Filtre JWT** | Vérifier **uniquement l’access token** dans `Authorization`, injecter le contexte |
| **Spring Security** | Public : `/api/auth/login`, `/api/auth/refresh` ; protégé : reste de l’API avec access token |

---

## 2. Dépendances Maven

Ajouter la dépendance pour générer et parser les JWT (JJWT) dans `pom.xml` :

```xml
<!-- JWT (JJWT) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

## 3. Configuration (`application.properties`)

```properties
# JWT - Clé de signature (partagée ou distincte pour refresh)
jwt.secret=VotreCleSecreteTresLongueEtComplexeMinimum256BitsPourHS256
# Optionnel : clé dédiée au refresh (recommandé si refresh en JWT)
# jwt.refresh-secret=UneAutreCleLonguePourRefreshToken

# Access token : courte durée (15 min à 1 h)
jwt.access-expiration-ms=900000
# Refresh token : longue durée (7 à 30 jours)
jwt.refresh-expiration-ms=604800000
```

| Propriété | Rôle | Exemple |
|-----------|------|--------|
| **jwt.secret** | Signer access (et refresh si même clé) | Clé HS256, 256 bits min |
| **jwt.refresh-secret** | (Optionnel) Signer uniquement les refresh tokens | Idem |
| **jwt.access-expiration-ms** | Durée de vie de l’access token (ms) | 900000 = 15 min |
| **jwt.refresh-expiration-ms** | Durée de vie du refresh token (ms) | 604800000 = 7 jours |

En production : secrets en variables d’environnement ou coffre (Vault). Génération d’une clé :

```bash
openssl rand -hex 32
```

---

## 4. Modèle : un client = un compte de connexion

Dans ce projet, **un client = un compte de connexion**. L’entité `Client` sert à la fois de profil métier et de compte d’authentification.

Sur l’entité **`Client`** (déjà présente) :

- **phone** : identifiant de connexion (unique), déjà présent.
- **passwordHash** : hash BCrypt du mot de passe, stocké dans la colonne `password_hash`.

Le login consiste à :

1. Charger le `Client` par `phone` (ex. `ClientRepository.findByPhone(phone)`).
2. Vérifier le mot de passe avec `BCryptPasswordEncoder.matches(rawPassword, client.getPasswordHash())`.
3. Si le client n’existe pas ou n’a pas encore de mot de passe (`passwordHash` null), renvoyer 401.

Extrait de l’entité `Client` :

```java
@Column(nullable = false, unique = true, length = 50)
private String phone;

@Column(name = "password_hash", length = 255)
private String passwordHash;
```

---

## 5. Sécurité des mots de passe (BCrypt)

- À l’**inscription** : `BCryptPasswordEncoder.encode(plainPassword)` puis enregistrer le hash en base.
- À la **connexion** : `BCryptPasswordEncoder.matches(plainPassword, passwordHash)`.
- Ne jamais logger ni renvoyer le mot de passe en clair.

Configuration du bean :

```java
@Configuration
public class SecurityBeansConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## 6. Service JWT (access + refresh)

Responsabilités :

1. **Générer l’access token** : claims `sub` (phone ou userId), `exp`, `iat`, et **`type: "access"`** pour ne pas accepter un refresh token dans le filtre API.
2. **Générer le refresh token** : mêmes infos d’identité, **`type: "refresh"`**, durée plus longue ; optionnellement un claim `jti` (ID unique) pour la révocation en base.
3. **Valider l’access token** : signature + expiration + claim `type == "access"` ; utilisé par le filtre JWT sur les routes protégées.
4. **Valider le refresh token** : signature + expiration + `type == "refresh"` ; utilisé uniquement dans le endpoint `/api/auth/refresh` (et si stocké en DB : vérifier que le token existe et n’est pas révoqué).

Exemple (pseudo-code) :

```java
// Génération access token
String accessToken = Jwts.builder()
    .subject(user.getPhone())
    .claim("type", "access")
    .issuedAt(now)
    .expiration(new Date(now.getTime() + accessExpirationMs))
    .signWith(accessSecretKey)
    .compact();

// Génération refresh token (avec jti si stockage en DB)
String jti = UUID.randomUUID().toString();
String refreshToken = Jwts.builder()
    .subject(user.getPhone())
    .claim("type", "refresh")
    .claim("jti", jti)
    .issuedAt(now)
    .expiration(new Date(now.getTime() + refreshExpirationMs))
    .signWith(refreshSecretKey)  // peut être la même clé que access
    .compact();
// Persister en DB : jti, userId, expiry, révocable

// Validation (filtre : access uniquement)
Claims claims = Jwts.parser().verifyWith(accessSecretKey).build().parseSignedClaims(token).getPayload();
if (!"access".equals(claims.get("type", String.class))) throw new InvalidTokenException();
String phone = claims.getSubject();

// Validation refresh (dans AuthService.refresh)
Claims refreshClaims = Jwts.parser().verifyWith(refreshSecretKey).build().parseSignedClaims(refreshToken).getPayload();
if (!"refresh".equals(refreshClaims.get("type", String.class))) throw new InvalidTokenException();
// Si refresh en DB : vérifier que jti existe et n’est pas révoqué
```

Les clés sont dérivées de `jwt.secret` (et éventuellement `jwt.refresh-secret`) pour HS256.

---

## 7. Endpoint de login

### 7.1 Requête

- **URL** : `POST /api/auth/login`
- **Body** (JSON) :

```json
{
  "phone": "+243812345678",
  "password": "MonMotDePasse123"
}
```

- **Contraintes** : `phone` et `password` obligatoires (validation Bean Validation).

### 7.2 Logique

1. Valider le body (phone, password non vides).
2. Charger l’utilisateur par `phone` (User ou Client selon votre modèle).
3. Si absent → 401 ou message “Identifiants invalides”.
4. Si présent : `passwordEncoder.matches(password, user.getPasswordHash())`.
5. Si échec → 401.
6. Si succès :
   - Générer **access token** et **refresh token** (service JWT).
   - Si vous stockez les refresh en base : persister le refresh (jti, userId, expiry).
   - Renvoyer la réponse ci-dessous.

### 7.3 Réponse login (access + refresh)

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 604800
}
```

- **expiresIn** : durée de vie de l’access token en **secondes** (ex. 900 = 15 min).
- **refreshExpiresIn** : durée de vie du refresh token en **secondes** (ex. 604800 = 7 jours).
- Ne jamais renvoyer le mot de passe.

### 7.4 Sécurité login

- Endpoint **public** (pas de JWT requis).
- Limiter les tentatives de login (rate limiting) pour éviter le brute-force.
- Utiliser HTTPS en production.

---

## 7bis. Endpoint refresh et stockage des refresh tokens

### 7bis.1 Requête refresh

- **URL** : `POST /api/auth/refresh`
- **Body** (JSON) :

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

- **Public** : pas d’Authorization requise ; seul le body (ou un cookie HttpOnly) contient le refresh token.

### 7bis.2 Logique refresh

1. Extraire le `refreshToken` du body (ou du cookie).
2. Valider le JWT (signature, expiration, claim `type == "refresh"`).
3. **Si refresh stockés en base** : vérifier que le `jti` existe en table et n’est pas révoqué (champ `revoked = true` ou ligne supprimée). Si révoqué → 401.
4. Générer un **nouvel access token** (et optionnellement un **nouveau refresh token**).
5. **Rotation recommandée** : invalider l’ancien refresh (révoquer en DB ou le remplacer par le nouveau) et renvoyer le nouveau refresh dans la réponse. Ainsi un refresh ne sert qu’une fois.
6. Réponse : même format que le login (`accessToken`, `refreshToken`, `expiresIn`, `refreshExpiresIn`).

### 7bis.3 Stockage des refresh tokens (optionnel mais recommandé)

Pour pouvoir **révoquer** les sessions (logout, vol de token, changement de mot de passe), persister les refresh tokens en base.

**Entité exemple** :

```java
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_tokens_jti", columnList = "jti", unique = true))
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 36)
    private String jti;           // ID unique du token (claim jti dans le JWT)
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean revoked;
    private Instant revokedAt;
    // ...
}
```

- À l’émission : créer une ligne avec `jti`, `userId`, `expiresAt`, `revoked = false`.
- Au refresh : vérifier que la ligne existe et `revoked == false` ; si rotation, révoquer l’ancien (`revoked = true`, `revokedAt = now`) et insérer le nouveau.
- Au **logout** : révoquer le refresh token associé à la session (ex. en gardant un lien user ↔ jti ou en révoquant tous les refresh d’un user).
- **Nettoyage** : job périodique pour supprimer les lignes expirées (ou révoquées depuis longtemps).

---

## 8. Filtre JWT (protéger les routes avec l’access token)

Un filtre Spring (ordre avant `UsernamePasswordAuthenticationFilter`) :

1. Lire l’en-tête `Authorization`.
2. Si absent ou pas `Bearer <token>` : laisser passer ; Spring Security renverra 401 pour les routes protégées.
3. Si présent : extraire le token et le **valider comme access token** (signature + expiration + claim **`type == "access"`**). Rejeter tout refresh token utilisé ici → 401.
4. Si invalide ou type ≠ access : répondre 401 et ne pas continuer la chaîne.
5. Si valide : extraire l’identité (`sub`), créer un `UsernamePasswordAuthenticationToken`, le mettre dans `SecurityContextHolder.getContext().setAuthentication(...)`, puis `chain.doFilter(request, response)`.

Les contrôleurs récupèrent l’utilisateur courant via `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` ou `@AuthenticationPrincipal`.

---

## 9. Configuration Spring Security

Règles à appliquer :

- **Public** : `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/register` (si présent), Swagger, health, etc.
- **Protégé** : tout le reste de l’API (ex. `/api/**` sauf `/api/auth/**`), en exigeant un **access token** valide.

Exemple de configuration (à adapter à votre filtre JWT) :

```java
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
.requestMatchers("/api/**").authenticated()
.anyRequest().authenticated()
```

Et ajouter le filtre JWT dans la chaîne (dans l’ordre qui convient, en général avant le filtre d’authentification par formulaire).

---

## 10. Exemples d’appels

### 10.1 Login

```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"+243812345678\",\"password\":\"MonMotDePasse123\"}"
```

Réponse attendue (ex.) :

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 604800
}
```

### 10.2 Refresh (obtenir un nouvel access token)

```bash
curl -X POST http://localhost:8082/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"<VOTRE_REFRESH_TOKEN>\"}"
```

Réponse : même structure que le login (nouvel `accessToken`, et souvent nouveau `refreshToken` si rotation).

### 10.3 Appel à une route protégée (avec access token)

```bash
curl -X GET http://localhost:8082/api/clients/1 \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Utiliser **l’access token** dans `Authorization`, pas le refresh token. Sans token, token expiré ou token invalide : **401 Unauthorized**.

---

## 11. Checklist de mise en œuvre

- [ ] Ajouter les dépendances JJWT dans `pom.xml`
- [ ] Configuration : `jwt.secret`, `jwt.access-expiration-ms`, `jwt.refresh-expiration-ms` (et optionnellement `jwt.refresh-secret`)
- [ ] Entité User (ou champs sur Client) avec `phone` et `passwordHash`
- [ ] Bean `BCryptPasswordEncoder` et hash à l’inscription
- [ ] Service JWT : génération **access** (claim `type: "access"`) et **refresh** (claim `type: "refresh"`, optionnel `jti`), validation en distinguant access / refresh
- [ ] (Optionnel) Entité `RefreshToken` et table pour stocker les refresh (jti, userId, expiresAt, revoked) ; révoquer au logout / rotation
- [ ] DTO : `LoginRequest`, `RefreshRequest` (refreshToken), `AuthResponse` (accessToken, refreshToken, tokenType, expiresIn, refreshExpiresIn)
- [ ] Endpoint `POST /api/auth/login` (public) → renvoyer access + refresh
- [ ] Endpoint `POST /api/auth/refresh` (public) → valider refresh, renvoyer nouvel access (+ optionnel nouveau refresh avec rotation)
- [ ] Filtre JWT : valider **uniquement** l’access token (type == "access") dans `Authorization`
- [ ] SecurityConfig : public `/api/auth/**`, protégé le reste avec access token
- [ ] Tests : login, refresh, accès protégé avec access token ; 401 si refresh utilisé comme Bearer ou token expiré

---

## 12. Bonnes pratiques (access + refresh)

- **Secrets** : clés longues (256 bits min), jamais en dur ; variables d’environnement ou coffre (Vault). Clé refresh distincte recommandée si refresh en JWT.
- **HTTPS** : obligatoire en production pour éviter le vol des tokens.
- **Access token** : courte durée (15 min – 1 h) pour limiter la fenêtre d’abus en cas de vol.
- **Refresh token** : longue durée (7–30 jours) ; stockage en base + révocation pour logout et changement de mot de passe.
- **Rotation** : à chaque refresh, émettre un **nouveau** refresh et invalider l’ancien (un refresh = un usage). Réduit le risque si un refresh est volé.
- **Côté client** : stocker l’access token en mémoire (ou court terme) ; le refresh en stockage plus persistant (ex. keychain / keystore), jamais en clair dans une URL.
- **Filtre** : n’accepter que les tokens avec `type == "access"` ; refuser un refresh token dans `Authorization`.
- **Phone** : format normalisé (ex. E.164) pour éviter doublons.
- **Rate limiting** : sur `/api/auth/login` et `/api/auth/refresh` pour limiter brute-force et abus.
- **Réponses d’erreur** : message générique (“Identifiants invalides”, “Token invalide ou expiré”) sans détail technique.

Ce guide couvre l’authentification JWT avec **access token** et **refresh token** (téléphone + mot de passe) pour l’API Kozala Client. Pour une mise en œuvre pas à pas dans le code (entités, DTO, service JWT, filtre, SecurityConfig, endpoints login/refresh), on peut détailler fichier par fichier.
