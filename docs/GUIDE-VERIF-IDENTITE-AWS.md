# Guide pratique – Vérification d’identité avec AWS

Ce guide décrit **étape par étape** comment activer et utiliser la **vérification d’identité avec AWS** (stockage S3 + Rekognition CompareFaces) dans **kozala-client-api**.

---

## 1. Prérequis

- Un **compte AWS** avec accès à la console.
- Un **utilisateur IAM** avec clé d’accès (Access Key ID + Secret Access Key) ayant les droits **S3** (PutObject, GetObject, ListBucket) et **Rekognition** (CompareFaces).

---

## 2. Créer les ressources AWS

### 2.1 Bucket S3

1. Console AWS → **S3** → **Créer un bucket**.
2. **Nom** : unique (ex. `kozala-identity-docs-VOTRE_COMPTE`).
3. **Région** : ex. `eu-west-1` (Irlande) ou `eu-west-3` (Paris).
4. **Bloquer tout accès public** : activé.
5. Créer le bucket et noter **nom** et **région**.

Ce bucket est utilisé pour **toutes les images** de l’application :
- **Documents d’identité** et **selfies** : `clients/{clientId}/documents/...`
- **Photos de profil** : `clients/{clientId}/profile/{filename}`

### 2.2 Utilisateur IAM et politique

1. **IAM** → **Utilisateurs** → **Créer un utilisateur** (ex. `kozala-backend-identity`).
2. **Attacher une politique** (inline ou gérée). Exemple de politique :

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::VOTRE_BUCKET_IDENTITY",
        "arn:aws:s3:::VOTRE_BUCKET_IDENTITY/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": ["rekognition:CompareFaces", "rekognition:DetectFaces"],
      "Resource": "*"
    }
  ]
}
```

3. **Accès par programmation** : créer une **clé d’accès** pour cet utilisateur. Noter **Access Key ID** et **Secret Access Key** (à ne jamais commiter).

---

## 3. Configurer l’application

### 3.1 Choisir le fournisseur

Dans `application.properties` (ou variables d’environnement) :

```properties
# Comparaison document/selfie : aws (Rekognition) | none (accepter sans vérification)
app.face-verification.provider=aws
```

- `aws` : Rekognition CompareFaces (nécessite les propriétés AWS ci-dessous).
- `none` : pas de comparaison (selfie acceptée sans vérification).

### 3.2 Propriétés AWS

```properties
# AWS – région (ex. eu-west-1, us-east-1)
aws.region=eu-west-1

# S3 – bucket pour documents et selfies
aws.s3.bucket-identity=kozala-identity-docs-VOTRE_COMPTE

# Rekognition – seuil de similarité (0.0 à 1.0) pour considérer une correspondance
aws.rekognition.face-match-threshold=0.8
```

### 3.3 Credentials (recommandé : variables d’environnement)

En production, **ne pas** mettre les clés dans le fichier de config. Utiliser :

- **Variables d’environnement** (reconnues automatiquement par le SDK AWS) :
  - `AWS_ACCESS_KEY_ID`
  - `AWS_SECRET_ACCESS_KEY`
  - `AWS_REGION` (optionnel si `aws.region` est défini)

Ou un **profil AWS** (`~/.aws/credentials`).

Pour tester en local, vous pouvez temporairement ajouter (à ne pas commiter) :

```properties
# Uniquement en dev local – utiliser des variables d’env en prod
# aws.access-key-id=AKIA...
# aws.secret-access-key=...
```

(Le code lit `aws.access-key-id` et `aws.secret-access-key` si présents, sinon le SDK utilise les variables d’environnement ou le profil.)

---

## 4. Vérifier le flux

1. **Démarrer** l’API avec `app.identity-storage.provider=aws` et `app.face-verification.provider=aws`.
2. **Upload document** : envoyer recto (et verso si CNI) → les fichiers sont stockés dans S3.
3. **Upload selfie** : envoyer la selfie → stockée dans S3, puis Rekognition **CompareFaces** compare le visage du document (recto) au visage de la selfie.
4. Si la similarité ≥ seuil (ex. 80 %) → document selfie **APPROVED** ; sinon **REJECTED** avec message d’erreur.

---

## 5. Dépannage

| Problème | Vérification |
|----------|--------------|
| Erreur au démarrage (S3/Rekognition) | Credentials (env ou `aws.access-key-id` / `aws.secret-access-key`), région, nom du bucket. |
| « Aucun visage détecté » sur la selfie | Qualité de l’image, luminosité, visage bien cadré. Rekognition exige un visage détectable. |
| « Aucun visage détecté » sur le document | Utiliser le **recto** du document (carte d’identité ou page photo du passeport). |
| « La selfie ne correspond pas au visage du document » | CompareFaces a retourné une similarité < seuil. Vérifier que la même personne est sur le document et la selfie. |

---

## 6. Références

- [GUIDE-MIGRATION-AWS-VERIFICATION-IDENTITE.md](./GUIDE-MIGRATION-AWS-VERIFICATION-IDENTITE.md) – architecture et migration détaillée.
- [AWS Rekognition – CompareFaces](https://docs.aws.amazon.com/rekognition/latest/dg/compare-faces.html)
- [AWS S3 – Java SDK](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3.html)

---

**Implémentation** : stockage S3 via `IdentityDocumentStorageService` / `AwsS3DocumentStorageService` ; comparaison visage via `FaceVerificationService.verifyDocumentAndSelfie` et `AwsRekognitionFaceVerificationService`.
