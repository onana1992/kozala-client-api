# Guide de déploiement — Redis pour kozala-client-api

**Redis** est **obligatoire** pour l’API client : codes **OTP** et **tokens d’inscription (signup)**, avec expiration (TTL). Sans Redis joignable, l’application ne démarre pas correctement ou l’actuator remonte Redis en **DOWN**.

Documentation fonctionnelle : [REDIS-OTP-SIGNUP.md](./REDIS-OTP-SIGNUP.md) · tutoriel : [TUTORIEL-REDIS.md](./TUTORIEL-REDIS.md).

**Approche retenue** : **Docker Compose** sur la même EC2 que **core-backend** et **kozala-client-api** — fichier **`docker-compose.yml`** à la racine du dépôt, déployé automatiquement par le workflow GitHub Actions vers **`~/kozala-client-deploy/`** (utilisateur **`ubuntu`**, chemin home **`/home/ubuntu`**).

---

## 1. Variables côté application

Dans **`/home/ubuntu/docker-run-kozala-client.env`** (référencé par Compose) :

| Variable | Valeur avec Compose |
|----------|---------------------|
| `SPRING_DATA_REDIS_HOST` | `redis-kozala` (nom du **service** dans `docker-compose.yml`) |
| `SPRING_DATA_REDIS_PORT` | `6379` |
| `SPRING_DATA_REDIS_PASSWORD` | vide si pas de mot de passe |
| `SPRING_DATA_REDIS_TIMEOUT` | `5000` (ms) |

Le modèle **`docker-run-kozala-client.env.example`** du dépôt définit **`SPRING_DATA_REDIS_HOST=redis-kozala`** ; recopie vers **`docker-run-kozala-client.env`** (local + EC2).

---

## 2. Fichier `docker-compose.yml` (dépôt)

À la racine de **kozala-client-api** :

- **Service `redis-kozala`** : image `redis:7-alpine`, persistance **AOF** sur le volume **`redis-kozala-data`**, réseau **`kozala-net`**.
- **Service `kozala-client-api`** : image ECR **`633510959273.dkr.ecr.us-east-1.amazonaws.com/kozala-client-api:latest`**, port **8082**, `depends_on: redis-kozala`, `env_file` pointant vers **`/home/ubuntu/docker-run-kozala-client.env`**.

Réseau Docker nommé **`kozala-net`** (partagé uniquement entre ces deux services).  
**`container_name`** explicites : `redis-kozala` et `kozala-client-api`.

Si l’utilisateur SSH n’est pas **`ubuntu`**, adapte dans **`docker-compose.yml`** le chemin **`env_file`** (ex. `/home/ec2-user/docker-run-kozala-client.env`) et la variable **`DEPLOY_DIR`** dans **`.github/workflows/kozala-client-api-ci-cd.yml`** (répertoire de déploiement sur l’EC2).

---

## 3. Prérequis sur l’EC2

1. **Docker** avec le plugin **Compose v2** (`docker compose version`, pas seulement l’ancien binaire `docker-compose`).
2. Fichier **`/home/ubuntu/docker-run-kozala-client.env`** présent et renseigné (MySQL, JWT, AWS, **`APP_REMOTE_API_*`**, etc.) — voir [GUIDE-DEPLOIEMENT.md](./GUIDE-DEPLOIEMENT.md).
3. **AWS CLI** + droits **`aws ecr get-login-password`** pour tirer l’image **kozala-client-api** (comme pour le déploiement manuel ECR).

---

## 4. Déploiement avec GitHub Actions (recommandé)

Le workflow **`.github/workflows/kozala-client-api-ci-cd.yml`** :

1. Build & push l’image vers ECR.
2. Crée **`/home/ubuntu/kozala-client-deploy`** si besoin.
3. Copie **`docker-compose.yml`** depuis le dépôt vers ce répertoire (**`appleboy/scp-action`**).
4. En SSH : login ECR, **`docker pull`** de l’image API, puis dans le répertoire de déploiement :
   - **`docker compose down --remove-orphans`** (ignore les erreurs si premier run),
   - **`docker rm -f kozala-client-api redis-kozala`** pour éviter les conflits avec d’anciens conteneurs lancés à la main,
   - **`docker compose up -d --remove-orphans`**.

Les PR qui modifient **`docker-compose.yml`** déclenchent aussi le workflow (filtre **`paths`**).

---

## 5. Premier déploiement ou test manuel sur l’EC2

En SSH :

```bash
mkdir -p ~/kozala-client-deploy
# Copier docker-compose.yml depuis ton poste (exemple) :
# scp -i ~/.ssh/ta-cle.pem docker-compose.yml ubuntu@ec2-100-56-14-195.compute-1.amazonaws.com:~/kozala-client-deploy/

aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 633510959273.dkr.ecr.us-east-1.amazonaws.com

cd ~/kozala-client-deploy
docker compose pull
docker compose up -d --remove-orphans
```

Vérifications :

```bash
docker compose ps
docker exec -it redis-kozala redis-cli ping
curl -sS http://127.0.0.1:8082/actuator/health
```

La réponse health doit contenir **`redis`** : **`UP`**.

---

## 6. Opérations courantes (Compose)

| Action | Commande (depuis `~/kozala-client-deploy`) |
|--------|--------------------------------------------|
| État | `docker compose ps` |
| Logs API | `docker compose logs -f kozala-client-api` |
| Logs Redis | `docker compose logs -f redis-kozala` |
| Redémarrer tout | `docker compose restart` |
| Arrêter | `docker compose down` |
| Mettre à jour l’image API seule | `docker compose pull kozala-client-api && docker compose up -d kozala-client-api` |

Shell Redis : `docker exec -it redis-kozala redis-cli` (ajouter **`-a motdepasse`** si tu actives **`requirepass`**).

---

## 7. Redis avec mot de passe

Dans **`docker-compose.yml`**, adapte le service **`redis-kozala`**, par exemple :

```yaml
  redis-kozala:
    command: redis-server --appendonly yes --requirepass 'VOTRE_SECRET'
```

Puis dans **`docker-run-kozala-client.env`** :

```env
SPRING_DATA_REDIS_PASSWORD=VOTRE_SECRET
```

Redéploie avec **`docker compose up -d`**. Ne commite pas le secret.

---

## 8. Sécurité

- **Ne pas** exposer le port **6379** sur Internet (security group). Avec Compose tel que défini, Redis n’a **pas** de **`ports:`** : il n’est joignable que depuis les conteneurs sur **`kozala-net`**.
- En production : **`requirepass`** (§ 7).

---

## 9. Dépannage

| Symptôme | Piste |
|----------|--------|
| Redis **DOWN** / connection refused | `docker compose ps` ; **`SPRING_DATA_REDIS_HOST=redis-kozala`** dans l’env ; service **redis-kozala** **healthy** / up. |
| `env_file not found` | Chemin **`/home/ubuntu/docker-run-kozala-client.env`** : fichier absent ou mauvais utilisateur dans **`docker-compose.yml`**. |
| Échec **`docker compose`** sur l’EC2 | Installer le plugin Compose v2 (paquet **`docker-compose-plugin`** ou Docker Engine récent). |
| Conflit de noms | Le script CI fait **`docker rm -f kozala-client-api redis-kozala`** avant **`up`** ; à la main : **`docker compose down`** puis **`up`**. |
| `NOAUTH` | Mot de passe Redis activé : renseigner **`SPRING_DATA_REDIS_PASSWORD`**. |

---

## 10. Alternatives (sans Compose)

- **Deux `docker run`** sur le réseau **`kozala-net`** : possible mais moins pratique ; le CI ne les gère plus seul.
- **`host.docker.internal`** + Redis publié sur **6379** : voir l’ancienne variante dans l’historique git ou [TUTORIEL-REDIS.md](./TUTORIEL-REDIS.md) ; il faut alors **`SPRING_DATA_REDIS_HOST=host.docker.internal`** et adapter le lancement de l’API.

---

## 11. Checklist

- [ ] **`docker compose version`** OK sur l’EC2.
- [ ] **`/home/ubuntu/docker-run-kozala-client.env`** complet (**`SPRING_DATA_REDIS_HOST=redis-kozala`**).
- [ ] **`~/kozala-client-deploy/docker-compose.yml`** présent (copié par le CI ou à la main).
- [ ] **`docker compose up -d`** → **`GET /actuator/health`** → **redis : UP**.

---

## 12. Guide principal

[Déploiement global kozala-client-api](./GUIDE-DEPLOIEMENT.md).
