# Guide de déploiement — kozala-client-api & stack associée

Ce document décrit le déploiement de l’**API client mobile** (ce dépôt) avec **Docker**, **Amazon ECR** et **EC2**, en cohérence avec le **core banking backend**.

**Instance EC2** : **kozala-client-api** et **core-backend** tournent sur **la même EC2** — SSH : **`ubuntu@ec2-100-56-14-195.compute-1.amazonaws.com`**. Deux conteneurs Docker (`core-backend` sur **8080**, `kozala-client-api` sur **8082**), deux fichiers d’environnement dans le home de l’utilisateur SSH.

---

## 1. Vue d’ensemble

| Composant | Dépôt / image ECR | Port conteneur | Fichier d’environnement sur l’EC2 |
|-----------|-------------------|----------------|-----------------------------------|
| Core banking API | `core_banking_backend` → ECR **`kozala`** | **8080** | `~/docker-run.env` |
| API client (ce projet) | ECR **`kozala-client-api`** (voir URI ci-dessous) | **8082** | `~/docker-run-kozala-client.env` |

**URI du dépôt ECR** : `633510959273.dkr.ecr.us-east-1.amazonaws.com/kozala-client-api`  
Les images sont taguées **`latest`** et **`<sha-du-commit>`** ; pour `docker pull` / `docker run`, utiliser par exemple `…/kozala-client-api:latest`.

L’API client appelle le core via **`APP_REMOTE_API_BASE_URL`**. Sur cette instance, le DNS public **`http://ec2-100-56-14-195.compute-1.amazonaws.com:8080`** convient souvent depuis le conteneur client (même host). En alternative : IP privée VPC **`http://<IP_PRIVÉE>:8080`**, ou **`http://host.docker.internal:8080`** avec `--add-host=host.docker.internal:host-gateway` sur le `docker run` du client.

**CI/CD** : un workflow GitHub Actions build l’image, la pousse vers ECR, puis se connecte en **SSH** à l’EC2 pour copier **`docker-compose.yml`** dans **`~/kozala-client-deploy/`** et exécuter **`docker compose up -d`** (**Redis** + **kozala-client-api**). Les secrets applicatifs ne passent **pas** par GitHub : ils restent dans **`~/docker-run-kozala-client.env`** sur le serveur.

Documentation détaillée du workflow core-backend : dans le dépôt **`core_banking_backend`**, fichier **`doc/infrastructure/github-actions-ci-cd.md`**.

---

## 2. Prérequis

### 2.1 AWS

- Compte et région **`us-east-1`** (alignée sur les workflows actuels).
- **Dépôt ECR** **`kozala-client-api`** : `633510959273.dkr.ecr.us-east-1.amazonaws.com/kozala-client-api` (le créer dans la console ECR ou en CLI s’il n’existe pas encore).
- **Utilisateur IAM** (ou rôle) avec au minimum :
  - `ecr:GetAuthorizationToken` (ressource `*`),
  - droits `ecr:BatchCheckLayerAvailability`, `ecr:GetDownloadUrlForLayer`, `ecr:BatchGetImage`, `ecr:PutImage`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload` sur le dépôt **`kozala-client-api`** (et le même modèle pour **`kozala`** si une seule paire de clés sert aux deux pipelines).

### 2.2 Instance EC2 (partagée avec le core-backend)

- **Hôte** : `ec2-100-56-14-195.compute-1.amazonaws.com`, utilisateur SSH **`ubuntu`**.
- **Une seule EC2** héberge **`core-backend`** et **`kozala-client-api`** : vérifier que **`~/docker-run.env`** (core) et **`~/docker-run-kozala-client.env`** (client) coexistent.
- **Docker** + plugin **Compose v2** (`docker compose`) installés ; l’utilisateur SSH (ex. `ubuntu`) peut exécuter `docker` (sinon préfixer avec `sudo` et adapter le workflow).
- **AWS CLI** et droits de **`aws ecr get-login-password`** (souvent via **rôle IAM** attaché à l’instance — recommandé).
- Fichier **`~/docker-run-kozala-client.env`** présent et renseigné (voir section 4).
- **Security group** :
  - **TCP 22** depuis les IP des runners GitHub Actions ([`https://api.github.com/meta`](https://api.github.com/meta), clé `actions`), sauf runner self-hosted ;
  - **TCP 8082** (et **8080** pour le core) selon qui doit joindre l’API (ALB, clients mobiles, VPN, etc.).

### 2.3 Données et dépendances

- **MySQL** : base utilisée par l’app (schéma / migrations Flyway dans le projet). L’URL JDBC dans l’env doit pointer vers un hôte accessible depuis le conteneur (**RDS**, autre host, etc.).
- **Redis** : OTP et jetons d’inscription — hôte/port dans `SPRING_DATA_REDIS_*`. Déploiement du conteneur Redis sur l’EC2 : **[GUIDE-DEPLOIEMENT-REDIS.md](./GUIDE-DEPLOIEMENT-REDIS.md)**.
- **Core banking** : déployé et joignable à l’URL configurée dans **`APP_REMOTE_API_BASE_URL`**, avec un **JWT service** valide dans **`APP_REMOTE_API_BEARER_TOKEN`**.

---

## 3. Secrets GitHub (dépôt kozala-client-api)

**Settings** → **Secrets and variables** → **Actions** :

| Secret | Rôle |
|--------|------|
| `AWS_ACCESS_KEY_ID` | Clé IAM pour push ECR (droits minimaux). |
| `AWS_SECRET_ACCESS_KEY` | Secret associé. |
| `EC2_HOST` | **`ec2-100-56-14-195.compute-1.amazonaws.com`** (**la même** valeur que pour le workflow **core-backend**). |
| `EC2_USER` | **`ubuntu`** (**identique** au dépôt core). |
| `EC2_SSH_PRIVATE_KEY` | Contenu de la clé `.pem` (lignes `BEGIN` / `END` incluses). |

Les PR **depuis un fork** ne exécutent pas le déploiement (pas d’exposition des secrets).

**Même EC2 que le core** : tu peux **réutiliser les mêmes valeurs** `EC2_HOST`, `EC2_USER` et `EC2_SSH_PRIVATE_KEY` que sur le dépôt `core_banking_backend` ; seuls les dépôts ECR et les conteneurs diffèrent.

---

## 4. Fichier `~/docker-run-kozala-client.env` sur l’EC2

1. Le dépôt versionne **`docker-run-kozala-client.env.example`** (sans secrets). En local : `cp docker-run-kozala-client.env.example docker-run-kozala-client.env`, remplis les valeurs, puis envoie **`~/docker-run-kozala-client.env`** sur l’EC2 (le fichier **`docker-run-kozala-client.env`** est **gitignored**).

   **Windows (PowerShell)** — copier le modèle puis éditer sur le serveur, ou envoyer ta copie locale remplie :

   ```powershell
   scp -i "C:\Users\Personnel\kozala_keys.pem" "C:\Users\Personnel\Desktop\project\Mobile-Api\kozala-client-api\docker-run-kozala-client.env.example" ubuntu@ec2-100-56-14-195.compute-1.amazonaws.com:~/docker-run-kozala-client.env
   ```

   Puis en SSH sur l’EC2 : `nano ~/docker-run-kozala-client.env` et remplacer les **`CHANGEME`**.

   **Linux / macOS** — depuis la racine du dépôt :

   ```bash
   scp -i ~/chemin/vers/cle.pem docker-run-kozala-client.env.example ubuntu@ec2-100-56-14-195.compute-1.amazonaws.com:~/docker-run-kozala-client.env
   ```

2. Édite-le sur l’EC2 (`nano`, `vi`, etc.) et remplace **tous** les placeholders par des valeurs de production :
   - JDBC MySQL (`SPRING_DATASOURCE_*`),
   - Redis,
   - secrets JWT (`jwt.secret` / `jwt.refresh-secret` ou équivalent `JWT_*` selon ce que tu utilises),
   - AWS (préférer un **rôle IAM** sur l’EC2 et laisser clés vides si possible),
   - **`APP_REMOTE_API_BASE_URL`** (ex. **`http://ec2-100-56-14-195.compute-1.amazonaws.com:8080`** depuis le conteneur client sur la même instance),
   - **`APP_REMOTE_API_BEARER_TOKEN`**.

3. **Ne commite pas** ce fichier une fois rempli de secrets réels. Garde une copie chiffrée ou dans un gestionnaire de secrets si besoin.

Le fichier **`docker-compose.yml`** référence **`/home/ubuntu/docker-run-kozala-client.env`** ; le workflow copie le compose dans **`/home/ubuntu/kozala-client-deploy/`** (voir `.github/workflows/kozala-client-api-ci-cd.yml`).

### Photos de profil sur EC2 (pas de disque local)

Les photos de profil ne sont **pas** stockées sur le disque de l’instance : elles vont dans **S3** (`aws.s3.bucket-identity`). Le fichier `docker-run-kozala-client.env` définit **`APP_PROFILE_PHOTO_REQUIRE_AWS_STORAGE=true`** : si le bucket S3 n’est pas configuré, l’upload est refusé avec une erreur explicite (aucun répertoire à monter sur l’EC2).

En développement local sans S3, laisser **`app.profile-photo.require-aws-storage=false`** (défaut dans `application.properties`) pour conserver le fallback disque.

---

## 5. Déclenchement du pipeline

**Fichier** : `.github/workflows/kozala-client-api-ci-cd.yml`

- **Événement** : **pull request** vers la branche **`main`**.
- **Filtre de chemins** : `src/**`, `pom.xml`, `Dockerfile`, **`docker-compose.yml`**, ou le YAML du workflow.
- **Image** : tags **`latest`** et **`<sha-du-commit>`** sur `633510959273.dkr.ecr.us-east-1.amazonaws.com/kozala-client-api`.

Ouvre une PR interne qui touche l’un de ces chemins ; après succès des jobs **Build & push ECR** et **Deploy EC2 (SSH)**, le conteneur **`kozala-client-api`** est recréé avec la nouvelle image.

---

## 6. Ordre de déploiement recommandé

1. MySQL (RDS ou autre) et Redis disponibles, règles réseau OK.
2. Déployer et valider le **core-backend** (port **8080**, `~/docker-run.env`).
3. Obtenir un **JWT service** pour l’API client (selon la procédure du core banking).
4. Renseigner **`docker-run-kozala-client.env`** puis laisser le workflow déployer la stack **Compose** (Redis + **kozala-client-api**, port **8082**).
5. Vérifier **actuator** : `GET http://ec2-100-56-14-195.compute-1.amazonaws.com:8082/actuator/health` (si le port **8082** est ouvert sur le security group).

---

## 7. Dépannage

| Symptôme | Piste |
|----------|--------|
| `Could not load credentials from any providers` dans GitHub Actions | Secrets `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` absents ou vides sur le **dépôt** (ou repo non autorisé pour des secrets d’organisation). |
| Échec `docker pull` sur l’EC2 | Rôle IAM instance ou credentials pour ECR ; région **us-east-1** ; nom du dépôt **kozala-client-api**. |
| Jobs ignorés | PR depuis un fork, ou branche de base ≠ **main**, ou aucun fichier dans les **paths** du workflow. |
| Échec SSH | `EC2_SSH_PRIVATE_KEY` tronqué ; port 22 fermé vers GitHub Actions. |
| 401 / erreurs vers l’API bancaire | `APP_REMOTE_API_BASE_URL` incorrect depuis le conteneur ; **`APP_REMOTE_API_BEARER_TOKEN`** expiré ou invalide. |
| Erreur MySQL / Redis | Hôtes et ports depuis le conteneur (pas `localhost` du host sauf configuration réseau explicite). |

---

## 8. Checklist rapide

- [ ] Dépôt ECR **`kozala-client-api`** créé en **us-east-1**.
- [ ] Secrets GitHub `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_PRIVATE_KEY` configurés sur ce dépôt.
- [ ] **Même EC2** que le core : Docker, accès ECR, **`~/docker-run.env`** + **`~/docker-run-kozala-client.env`** présents.
- [ ] Security group : SSH + ports **8080** / **8082** selon besoin.
- [ ] Core-backend déployé et URL + JWT service testés depuis l’EC2.
- [ ] PR vers **`main`** avec changement pertinent → pipeline vert → smoke test sur **8082**.

---

## 9. Références dans ce dépôt

| Élément | Chemin |
|---------|--------|
| Workflow CI/CD | `.github/workflows/kozala-client-api-ci-cd.yml` (`ECR_REPOSITORY_URI`, `ECR_REGISTRY`, déploiement Compose) |
| Image Docker | `Dockerfile` |
| Stack EC2 (Redis + API) | `docker-compose.yml` → copié vers **`~/kozala-client-deploy/`** par le CI |
| Modèle variables (Git) | **`docker-run-kozala-client.env.example`** — copier vers **`~/docker-run-kozala-client.env`** sur l’EC2 (fichier réel non versionné) |
| Redis & Compose | [GUIDE-DEPLOIEMENT-REDIS.md](./GUIDE-DEPLOIEMENT-REDIS.md) |

Pour le **core-backend** : dépôt `core_banking_backend`, workflow `.github/workflows/core-backend-ci-cd.yml`, doc `doc/infrastructure/github-actions-ci-cd.md`.
