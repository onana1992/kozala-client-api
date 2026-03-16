# Guide de migration – Vérification d’identité sur AWS

Ce document décrit comment **passer d’Azure à AWS** pour le flux de vérification d’identité (stockage des documents/selfies, reconnaissance faciale, et autres services nécessaires). Il sert de **documentation de référence** pour l’architecture, la configuration et la migration du backend **kozala-client-api**.

---

## 1. Vue d’ensemble

### 1.1 Objectif

- **Stockage** : remplacer **Azure Blob Storage** par **Amazon S3**.
- **Reconnaissance faciale** : remplacer **Azure Face API** (detect + verify) par **Amazon Rekognition** (CompareFaces).
- **Optionnel – détection de faux documents** : aujourd’hui assuré par Azure Custom Vision (désactivé). Sur AWS, possible avec **Rekognition Custom Labels** ou **Rekognition DetectLabels / Moderation** selon le besoin.

Aucune modification du flux métier côté mobile : les mêmes endpoints (upload document, upload selfie, statut) restent utilisés ; seul le backend change de fournisseur cloud.

### 1.2 Équivalence des services

| Rôle | Azure (actuel) | AWS (cible) |
|------|----------------|-------------|
| Stockage des images (documents, selfies) | Azure Blob Storage (compte + conteneur) | **Amazon S3** (bucket + préfixes/clés) |
| Détection de visage sur une image | Azure Face API – Detect | **Amazon Rekognition – DetectFaces** (ou non utilisé si on fait uniquement CompareFaces) |
| Comparaison document ↔ selfie (1:1) | Azure Face API – Verify (faceId1, faceId2) | **Amazon Rekognition – CompareFaces** (image source + image cible) |
| Détection de faux documents (optionnel) | Azure Custom Vision | **Rekognition Custom Labels** ou **DetectModerationLabels** (selon cas d’usage) |

**Remarque** : Avec Rekognition **CompareFaces**, on envoie directement les deux images (document + selfie). Il n’est pas obligatoire d’appeler DetectFaces séparément pour obtenir des faceId, ce qui simplifie le flux par rapport à Azure (detect → verify).

---

## 2. Services AWS utilisés

### 2.1 Amazon S3 (stockage)

- **Rôle** : stocker les images des documents d’identité (recto/verso) et les selfies.
- **Concepts** :
  - **Bucket** : conteneur racine (équivalent du « compte de stockage » + « conteneur » Azure).
  - **Clé d’objet (key)** : chemin logique du fichier (ex. `clients/123/documents/id_card/uuid-recto.jpg`).
- **Sécurité** : bucket privé (pas d’accès public) ; accès via **credentials IAM** (clé d’accès + secret) ou **IAM Role** (EC2, ECS, Lambda).
- **Région** : choisir une région conforme à vos exigences (ex. `eu-west-1`, `eu-central-1`).

### 2.2 Amazon Rekognition (reconnaissance faciale)

- **APIs utilisées** :
  - **CompareFaces** : compare le visage d’une image source (document) avec le visage d’une image cible (selfie). Retourne une similarité (confidence) et permet de décider si c’est la même personne.
  - **DetectFaces** (optionnel) : détection de visages dans une image ; utile si vous voulez valider « au moins un visage » avant de comparer.
- **Entrées** : images fournies en **bytes** (Binary) ou référencées dans **S3** (Bucket + Key). Pour un backend qui a déjà les octets en mémoire, l’envoi en binaire est simple.
- **Pas d’approbation entreprise** : contrairement à Azure Face (Identification/Verification), Rekognition CompareFaces est utilisable dès l’ouverture d’un compte AWS.

### 2.3 Optionnel – Liveness (détection de vivant)

- **Rekognition Face Liveness** : vérification qu’une selfie provient d’une personne réelle (anti-photo, anti-replay). Disponible pour React Web, iOS et Android. Facturation par « check » (voir [Rekognition Face Liveness](https://aws.amazon.com/rekognition/face-liveness/)). Peut être ajouté plus tard sans changer le stockage ni CompareFaces.

### 2.4 Optionnel – Détection de faux documents

- **Rekognition Custom Labels** : entraîner un modèle sur des images de documents authentiques / faux (équivalent Custom Vision). Coût : formation + inférence.
- **Rekognition DetectModerationLabels** : détection de contenu inapproprié ; peut compléter une stratégie de contrôle.
- **Alternative** : garder une logique métier simple (ex. règles, revue manuelle) sans ML dédié au faux document, comme actuellement avec Custom Vision désactivé.

---

## 3. Prérequis et création des ressources AWS

### 3.1 Compte AWS

- Créer un compte sur [aws.amazon.com](https://aws.amazon.com) si besoin.
- Activer **MFA** sur le compte root et utiliser un **utilisateur IAM** pour les opérations quotidiennes (pas le root).

### 3.2 IAM – Utilisateur et droits

Créer un **utilisateur IAM** dédié au backend (ex. `kozala-backend-identity`) avec :

- **Accès par programmation** (clé d’accès + secret) pour que Spring Boot s’authentifie.
- **Politique (policy)** autorisant :
  - **S3** : `PutObject`, `GetObject`, `ListBucket` sur le bucket des documents d’identité (et uniquement ce bucket).
  - **Rekognition** : `rekognition:CompareFaces`, et éventuellement `rekognition:DetectFaces`.

Exemple de politique (à adapter avec votre `BUCKET_NAME` et `REGION`) :

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::VOTRE_BUCKET_IDENTITY",
        "arn:aws:s3:::VOTRE_BUCKET_IDENTITY/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "rekognition:CompareFaces",
        "rekognition:DetectFaces"
      ],
      "Resource": "*"
    }
  ]
}
```

Noter les **Access Key ID** et **Secret Access Key** ; ne pas les commiter (variables d’environnement ou secret manager).

### 3.3 S3 – Bucket pour les documents d’identité

1. Dans la **Console AWS** → **S3** → **Créer un bucket**.
2. **Nom** : globalement unique (ex. `kozala-identity-documents-VOTRE_COMPTE`).
3. **Région** : ex. Europe (Paris) `eu-west-3` ou Europe (Irlande) `eu-west-1`.
4. **Paramètres** :
   - Bloquer tout accès public (recommandé).
   - Activer **versionnement** si vous avez besoin d’historique (optionnel).
   - Chiffrement : optionnel (S3-SSE par défaut ou KMS).
5. Créer le bucket et noter son **nom** et la **région**.

Structure des clés (compatible avec le flux actuel) :

- `clients/{clientId}/documents/{documentType}/{uuid}-{side}.jpg`  
  Ex. : `clients/42/documents/id_card/a1b2c3d4-recto.jpg`, `clients/42/documents/selfie/uuid-selfie.jpg`.

### 3.4 Rekognition – Aucune ressource à créer

Rekognition est un service API : pas de « ressource » à créer dans la console. Il suffit d’appeler les APIs avec les mêmes credentials IAM que pour S3 (ou un utilisateur ayant les droits Rekognition). Choisir une **région** où Rekognition est disponible (ex. `eu-west-1`, `us-east-1`).

---

## 4. Tarification (ordre de grandeur)

### 4.1 Amazon S3

- **Free Tier** (12 mois) : 5 Go de stockage, 20 000 requêtes GET, 2 000 requêtes PUT/POST/LIST par mois.
- **Au-delà** : stockage Standard (quelques centimes par Go/mois), requêtes PUT ~0,005 USD / 1 000 requêtes, GET ~0,0004 USD / 1 000 requêtes.  
Référence : [AWS S3 Pricing](https://aws.amazon.com/s3/pricing/).

Pour un volume modéré (milliers de documents + selfies), le coût S3 reste faible.

### 4.2 Amazon Rekognition (Image)

- **CompareFaces** fait partie des APIs **Groupe 1**.
- **Free Tier** (12 mois) : 1 000 images analysées par mois gratuites (Groupe 1).
- **Payant** : à partir de 0,001 USD par image (premier million/mois). Chaque appel à CompareFaces avec 2 images = 2 images facturées.
- Détail : [Amazon Rekognition Pricing](https://aws.amazon.com/rekognition/pricing/).

Exemple : 5 000 vérifications/mois (2 images par vérification = 10 000 images) → environ 10 USD/mois hors free tier.

### 4.3 Rekognition Custom Labels (optionnel)

- Coût formation + inférence (heures de training et d’inférence). À évaluer si vous réactivez une détection de faux documents type Custom Vision.

---

## 5. Configuration backend (Spring Boot)

### 5.1 Variables et propriétés

Le backend devra exposer une configuration **AWS** en plus ou à la place d’Azure. Exemple de propriétés :

```properties
# --- AWS (vérification d'identité) ---
# Région (ex. eu-west-1, us-east-1)
aws.region=eu-west-1

# S3 – bucket pour documents d'identité et selfies
aws.s3.bucket-identity=kozala-identity-documents-VOTRE_COMPTE
# Préfixe optionnel dans le bucket (ex. "identity/" ou vide)
aws.s3.prefix-identity=

# Credentials : en production, utiliser des variables d'environnement
# aws.access-key-id=${AWS_ACCESS_KEY_ID}
# aws.secret-access-key=${AWS_SECRET_ACCESS_KEY}
# Ou IAM Role si l'app tourne sur EC2/ECS (pas de clé dans la config)

# Rekognition – activer la comparaison document/selfie
aws.rekognition.enabled=true
# Seuil de similarité (0.0 à 1.0) pour considérer que les visages correspondent
aws.rekognition.face-match-threshold=0.8
```

En production, ne **jamais** mettre les clés en clair dans `application.properties` ; utiliser par exemple :

- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` (SDK AWS les lit automatiquement),  
ou  
- **IAM Role** si l’application tourne sur EC2, ECS ou Lambda.

### 5.2 Dépendances Maven

Remplacer ou compléter les dépendances Azure par le SDK AWS pour Java :

```xml
<!-- AWS SDK pour S3 et Rekognition -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.0</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>rekognition</artifactId>
    <version>2.20.0</version>
</dependency>
```

Le SDK utilise par défaut la chaîne de credentials standard (variables d’environnement, fichier `~/.aws/credentials`, IAM Role).

---

## 6. Étapes de migration du code

Les étapes ci-dessous décrivent **quoi** modifier, sans imposer l’ordre exact des fichiers.

### 6.1 Stockage (Azure Blob → S3)

- **Créer** une interface ou un service d’abstraction pour le stockage (ex. `IdentityDocumentStorageService` actuel), puis :
  - **Implémenter** une version **S3** : upload via `S3Client.putObject()`, téléchargement via `S3Client.getObject()`, clé d’objet = `storageKey` (même schéma `clients/{id}/documents/...`).
  - Garder la même signature métier : `uploadDocument(clientId, documentType, side, file)` → retourne `storageKey` ; `downloadDocument(storageKey)` → retourne `Optional<byte[]>`.
- **Config** : lire bucket (et préfixe si utilisé) depuis les propriétés AWS ; construire le `S3Client` avec la région et les credentials (ou laisser le SDK les résoudre).
- **Option** : conserver l’implémentation Azure et choisir l’implémentation (Azure ou S3) via un profil Spring ou un flag (ex. `app.identity-storage.provider=s3`).

### 6.2 Reconnaissance faciale (Azure Face → Rekognition)

- **Remplacer ou compléter** `FaceVerificationService` :
  - Au lieu d’appeler Azure Detect (pour obtenir faceId) puis Verify (faceId1, faceId2), appeler **Rekognition CompareFaces** avec :
    - **Source** : octets de l’image du document (recto avec visage).
    - **Target** : octets de l’image selfie.
  - L’API retourne une liste de comparaisons avec un **score de similarité**. Si au moins une comparaison a un score ≥ seuil (ex. 80 %), considérer que le face match est réussi.
- **Config** : `aws.rekognition.enabled`, `aws.rekognition.face-match-threshold`, et les credentials/région AWS (partagés avec S3 ou dédiés).
- **Comportement si désactivé** : si `aws.rekognition.enabled=false`, même logique qu’aujourd’hui avec `azure.face.enabled=false` (accepter la selfie sans comparaison ou retourner une erreur selon la politique métier).

### 6.3 Orchestration (IdentityVerificationService)

- **Upload document** : appeler le nouveau stockage S3 au lieu d’Azure Blob. La logique Custom Vision (faux document) reste optionnelle ; si vous l’activez sur AWS, brancher un service Rekognition Custom Labels ou équivalent.
- **Upload selfie** :
  - Stocker la selfie dans S3 (même service de stockage).
  - Récupérer l’image du document (recto) via `downloadDocument(storageKey)` (déjà en bytes).
  - Appeler le service de comparaison Rekognition (CompareFaces) avec image document + image selfie.
  - Selon le résultat : APPROVED ou REJECTED + message d’erreur.
- Aucun changement nécessaire aux **DTO** ou aux **endpoints** côté API si vous gardez les mêmes contrats (upload multipart ou base64, réponses identiques).

### 6.4 Nettoyage optionnel

- Retirer ou désactiver les dépendances **Azure** (`azure-storage-blob`, propriétés `azure.*`) si vous basculez entièrement sur AWS.
- Supprimer les classes de configuration **Azure** (`AzureStorageProperties`, `AzureFaceProperties`, etc.) si plus utilisées, ou les garder pour une période de double run (Azure + AWS).

---

## 7. Résumé des ressources AWS à avoir

| Ressource | Type | Rôle |
|-----------|------|------|
| **Compte AWS** | Compte | Accès à la console et aux APIs |
| **Utilisateur IAM** | IAM User | Credentials pour le backend (Access Key + Secret) |
| **Policy IAM** | Policy | Droits S3 (PutObject, GetObject, ListBucket) + Rekognition (CompareFaces, optionnel DetectFaces) |
| **Bucket S3** | S3 Bucket | Stockage des images (documents + selfies), privé |
| **Rekognition** | Service API | Aucune ressource à créer ; appels API avec les mêmes credentials |

---

## 8. Références

- [Amazon S3 – Documentation](https://docs.aws.amazon.com/s3/)
- [Amazon S3 – Tarification](https://aws.amazon.com/s3/pricing/)
- [Amazon Rekognition – Documentation](https://docs.aws.amazon.com/rekognition/)
- [Amazon Rekognition – CompareFaces](https://docs.aws.amazon.com/rekognition/latest/dg/compare-faces.html)
- [Amazon Rekognition – Tarification](https://aws.amazon.com/rekognition/pricing/)
- [AWS SDK for Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Configuration des credentials AWS (env, profile, IAM)](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html)

---

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)  
2. [Services AWS utilisés](#2-services-aws-utilisés)  
3. [Prérequis et création des ressources AWS](#3-prérequis-et-création-des-ressources-aws)  
4. [Tarification](#4-tarification-ordre-de-grandeur)  
5. [Configuration backend (Spring Boot)](#5-configuration-backend-spring-boot)  
6. [Étapes de migration du code](#6-étapes-de-migration-du-code)  
7. [Résumé des ressources AWS](#7-résumé-des-ressources-aws-à-avoir)  
8. [Références](#8-références)

---

*Document rédigé pour le projet **kozala-client-api** (vérification d’identité). Mise en œuvre pratique : [GUIDE-VERIF-IDENTITE-AWS.md](./GUIDE-VERIF-IDENTITE-AWS.md).*
