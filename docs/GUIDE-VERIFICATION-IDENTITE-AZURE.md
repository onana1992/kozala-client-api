# Guide de mise en œuvre – Vérification d’identité (Document + Selfie) avec Azure AI

Ce guide décrit comment implémenter la **vérification du compte (étape 3)** : **Document et vérification de l’identité** en s’appuyant sur les services IA d’Azure (stockage, Custom Vision, Face API), en cohérence avec le flux existant de l’app (écrans `verification-identity-*`).

---

## 1. Vue d’ensemble du flux

Le parcours suit le schéma suivant :

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Upload document + stockage Azure (Blob Storage)               │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Détection de faux document (Custom Vision / ML)               │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Capture selfie avec guide liveness (mobile)                  │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Azure Face API : Liveness + Face Match                       │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Tout OK ?  ─── Oui ──► Onboarding validé                     │
│                ─── Non ──► En attente / Rejet, refaire selfie   │
│                           ou document                            │
└─────────────────────────────────────────────────────────────────┘
```

**Composants à mettre en place :**

| Étape | Composant | Rôle |
|-------|-----------|------|
| 1 | **Azure Blob Storage** | Stockage sécurisé des images (recto/verso CNI ou passeport, selfie) |
| 2 | **Azure Custom Vision** | Modèle ML pour détecter les faux documents (authenticité) |
| 3 | **Mobile (Expo)** | Capture selfie avec instructions liveness (bouger la tête, cligner, etc.) |
| 4 | **Azure Face API** | Liveness (détection de vivant) + comparaison visage document ↔ selfie |

---

## 2. Prérequis Azure

### 2.1 Ressources à créer dans le portail Azure

1. **Compte de stockage (Storage Account)**  
   - Créer un compte de stockage (ex. `kozalaidentity`).  
   - Créer un **conteneur** dédié aux documents d’identité (ex. `identity-documents`), accès **privé** (pas d’accès anonyme).  
   - Récupérer : **Connection string** ou **Account key** (pour le backend).

2. **Custom Vision**  
   - Créer une ressource **Custom Vision** (Training + Prediction si séparés, ou ressource unifiée).  
   - Créer un **projet** (ex. « Détection faux documents »).  
   - Entraîner un modèle avec des images de **documents authentiques** et **faux / contrefaits** (étiquettes du type `authentic` / `fake` ou par type de faux).  
   - Récupérer : **Endpoint** (prediction) et **clé API (Prediction key)**.

3. **Face API**  
   - Créer une ressource **Face** (Cognitive Services).  
   - Récupérer : **Endpoint** (ex. `https://<nom>.cognitiveservices.azure.com/`) et **clé API**.

### 2.2 Variables d’environnement / configuration backend

À ne pas commiter. Exemple dans `application.properties` (ou variables d’environnement en production) :

```properties
# Azure Storage (documents d'identité)
azure.storage.connection-string=${AZURE_STORAGE_CONNECTION_STRING}
azure.storage.container-identity=identity-documents

# Azure Custom Vision (détection faux documents)
azure.custom-vision.endpoint=${AZURE_CUSTOM_VISION_ENDPOINT}
azure.custom-vision.prediction-key=${AZURE_CUSTOM_VISION_PREDICTION_KEY}
azure.custom-vision.project-id=${AZURE_CUSTOM_VISION_PROJECT_ID}
azure.custom-vision.published-name=Iteration1

# Azure Face API (liveness + face match)
azure.face.endpoint=${AZURE_FACE_ENDPOINT}
azure.face.subscription-key=${AZURE_FACE_SUBSCRIPTION_KEY}
```

---

## 3. Backend (Spring Boot) – Structure recommandée

### 3.1 Dépendances Maven (`pom.xml`)

```xml
<!-- Azure Storage Blob -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
    <version>12.25.0</version>
</dependency>

<!-- Client HTTP pour Custom Vision et Face API (RestTemplate ou WebClient) -->
<!-- Déjà fourni par Spring Boot -->
```

Pour Custom Vision et Face API : appels HTTP avec `RestTemplate` ou `WebClient` (pas de SDK Java obligatoire pour une première version).

### 3.2 Packages et classes à créer

```
com.neobank.kozala_client
├── config
│   ├── AzureStorageProperties.java    # Configuration Blob (connection string, container)
│   ├── AzureCustomVisionProperties.java
│   └── AzureFaceProperties.java
├── service
│   ├── IdentityDocumentStorageService.java   # Upload / sauvegarde Blob
│   ├── FakeDocumentDetectionService.java     # Appel Custom Vision
│   ├── FaceVerificationService.java          # Liveness + Face Match (Face API)
│   └── IdentityVerificationService.java      # Orchestration du flux complet
├── controller
│   └── IdentityVerificationController.java   # Endpoints upload document, selfie, statut
└── dto
    └── identity/
        ├── UploadDocumentRequest.java        # (multipart: recto, verso, type CNI/passport)
        ├── UploadDocumentResponse.java       # storageKey, documentId, fakeCheckResult
        ├── UploadSelfieRequest.java         # (multipart: selfie image)
        ├── LivenessSessionResponse.java      # sessionId pour Face API liveness (optionnel)
        └── IdentityVerificationResultResponse.java  # success, message, identityCompleted
```

### 3.3 Entités existantes à réutiliser

- **`Document`** : déjà présent (`client_id`, `type`, `storage_key`, `status`, etc.).  
  - Utiliser `DocumentType.ID_CARD`, `PASSPORT`, `SELFIE`.  
  - `storage_key` = chemin Blob (ex. `clients/{clientId}/documents/{uuid}-recto.jpg`).  
- **`Client`** : champ ou statut « identité vérifiée » (ex. `identityVerified` ou dérivé des `Document` en statut `APPROVED`).

### 3.4 Flux métier backend (orchestration)

**IdentityVerificationService** (résumé) :

1. **Upload document (recto + verso si CNI)**  
   - Recevoir les multipart.  
   - Appeler **IdentityDocumentStorageService** : upload vers Blob, retour des `storage_key`.  
   - Créer les entrées **Document** en base (type ID_CARD ou PASSPORT, status PENDING).  
   - Pour chaque image : appeler **FakeDocumentDetectionService** (Custom Vision).  
   - Si « faux » détecté : mettre le document en REJECTED, retourner erreur.  
   - Sinon : laisser PENDING (ou APPROVED si pas de revue manuelle).

2. **Upload selfie**  
   - Recevoir l’image selfie (multipart).  
   - Upload Blob + création Document type SELFIE.  
   - Appeler **FaceVerificationService** :  
     - **Liveness** : vérifier que l’image est une vraie personne (Face API liveness).  
     - **Face Match** : comparer le visage de la selfie aux visages extraits des documents (recto CNI/passeport).  
   - Si liveness OK et face match OK : marquer l’identité comme vérifiée (ex. mettre à jour Client ou statut des documents).  
   - Sinon : retourner erreur (rejet ou « refaire selfie / document »).

---

## 4. Détail des services Azure côté backend

### 4.1 Upload et sauvegarde (Azure Blob Storage)

- **IdentityDocumentStorageService**  
  - Méthode du type : `String uploadDocument(Long clientId, String documentType, String side, MultipartFile file)`.  
  - Générer un nom de blob unique (ex. `clients/{clientId}/documents/{uuid}-{side}.jpg`).  
  - Utiliser `BlobClient.upload()` (flux du fichier).  
  - Retourner le **storage key** (ou l’URL SAS limitée dans le temps si le front a besoin de lire).  
  - En base : sauvegarder ce `storage_key` dans `Document.storageKey`.

### 4.2 Détection de faux document (Custom Vision)

- **FakeDocumentDetectionService**  
  - Entrée : image (fichier ou URL du blob).  
  - Appel HTTP vers l’endpoint Custom Vision **Prediction** (POST, avec `Prediction-Key` et `Content-Type: application/octet-stream` ou multipart).  
  - Réponse : probabilités par tag (ex. `authentic`, `fake`).  
  - Si la probabilité « fake » dépasse un seuil (ex. 0.7) → considérer comme faux, retourner un résultat « rejeté » pour cette image.

Référence : [Custom Vision Prediction API](https://docs.microsoft.com/azure/cognitive-services/custom-vision-service/use-prediction-api).

### 4.3 Capture selfie avec guide liveness (mobile)

Côté **mobile** (Expo / React Native) :

- Utiliser la **caméra frontale** (selfie).  
- Afficher un **guide liveness** : instructions à l’utilisateur (ex. « Placez votre visage dans le cadre », « Clignez des yeux », « Tournez la tête légèrement »).  
- Capturer l’image (ou une courte séquence) une fois les consignes respectées.  
- Envoyer l’image au backend (multipart) vers un endpoint dédié (ex. `POST /api/identity-verification/selfie`).

Le « guide » est surtout UX ; la **vraie** détection de vivant (anti-photo, anti-replay) se fait côté **Azure Face API** (liveness).

### 4.4 Face API : Liveness + Face Match

- **FaceVerificationService**  

1. **Liveness**  
   - Utiliser **Face API – Liveness** (session-based ou pas selon la version).  
   - Envoi de l’image selfie (ou participation à une session liveness si vous utilisez le flux avec session).  
   - Vérifier que le résultat indique « live » (personne réelle).  

2. **Face Match**  
   - **Détecter les visages** :  
     - Sur l’image selfie (Face API Detect).  
     - Sur les images document (recto CNI/passeport) : détecter le visage sur la photo d’identité.  
   - **Comparer** : utiliser l’API **Face - Verify** (comparaison faceId à faceId) entre le visage selfie et le visage du document.  
   - Si similarité au-dessus du seuil (ex. 0.8) → même personne.

Référence : [Face API - Liveness](https://docs.microsoft.com/azure/cognitive-services/computer-vision/concept-face-api-liveness), [Face - Verify](https://docs.microsoft.com/azure/cognitive-services/computer-vision/concept-face-api-verify).

---

## 5. API REST proposée (backend)

| Méthode | Endpoint | Rôle |
|---------|----------|------|
| POST | `/api/identity-verification/documents` | Upload document(s) : multipart (recto, verso optionnel, type CNI/passport). Stockage Blob + Custom Vision. Retour : documentIds, résultat détection faux. |
| POST | `/api/identity-verification/selfie` | Upload selfie : multipart. Stockage Blob + Face API (liveness + face match). Retour : succès ou erreur (rejet). |
| GET | `/api/identity-verification/status` | Statut de la vérification identité du client connecté (pending / approved / rejected). Peut s’appuyer sur `VerificationStatusResponse.identityCompleted` ou une table dédiée. |

Sécurité : tous ces endpoints doivent être **protégés** (JWT, client identifié). Ne jamais exposer les clés Azure au mobile ; tout passe par le backend.

---

## 6. Mobile (Expo / React Native) – Points clés

### 6.1 Écrans existants à brancher

- **verification-identity-document** : déjà prévu (recto/verso). À connecter à la caméra ou au sélecteur de fichiers, puis appel `POST /api/identity-verification/documents` avec les images.
- **verification-identity-selfie** : déjà prévu. À implémenter :  
  - Guide liveness (texte + éventuellement animation ou étapes).  
  - Capture caméra frontale.  
  - Envoi `POST /api/identity-verification/selfie`.

### 6.2 Upload des fichiers

- Utiliser `expo-image-picker` (prendre une photo) ou `expo-document-picker` (fichier).  
- Envoyer en **multipart/form-data** avec Axios (ou fetch) vers le backend.  
- Gérer le token JWT dans l’en-tête `Authorization`.

### 6.3 Guide liveness (UX)

- Afficher un cadre pour positionner le visage.  
- Instructions courtes : « Gardez le visage dans le cadre », « Clignez des yeux », « Tournez la tête à gauche/droite ».  
- Bouton « Prendre la photo » (ou capture auto quand le visage est bien détecté si vous ajoutez une pré-détection côté client).  
- Après capture, afficher un indicateur de chargement pendant l’appel au backend (liveness + face match).

### 6.4 Gestion des erreurs

- Si le backend retourne « document potentiellement faux » : afficher un message clair et proposer de reprendre le document.  
- Si « liveness échoué » ou « face match échoué » : afficher un message et proposer de reprendre la selfie ou de revérifier le document.

---

## 7. Ordre de mise en œuvre recommandé

1. **Azure**  
   - Créer le Storage Account + conteneur, Custom Vision (projet + entraînement basique), ressource Face.  
   - Noter les endpoints et clés.

2. **Backend – Stockage**  
   - Config (properties) Azure Storage.  
   - `IdentityDocumentStorageService` (upload Blob).  
   - Endpoint d’upload document qui enregistre en base (`Document`) et retourne le `storage_key`.

3. **Backend – Détection faux**  
   - Config Custom Vision.  
   - `FakeDocumentDetectionService` (appel API Prediction).  
   - Intégrer dans le flux d’upload document : si faux → REJECTED et réponse 4xx.

4. **Backend – Face API**  
   - Config Face API.  
   - `FaceVerificationService` : liveness puis face match (detect sur document + selfie, puis verify).  
   - Endpoint upload selfie qui appelle ce service et met à jour le statut identité.

5. **Backend – Orchestration**  
   - `IdentityVerificationService` qui enchaîne : stockage → Custom Vision (documents) ; stockage → Face (selfie).  
   - Endpoint GET statut vérification identité.

6. **Mobile**  
   - Brancher la caméra / sélection de fichiers sur l’écran document.  
   - Appels API upload documents avec gestion erreurs (faux document).  
   - Écran selfie : guide liveness + capture + upload selfie.  
   - Affichage du statut (succès / en attente / rejet) et redirection vers `verification-identity-success` ou message d’erreur.

7. **Sécurité et production**  
   - Toutes les clés Azure en variables d’environnement.  
   - Limiter la taille des fichiers (ex. 5 Mo par image).  
   - Logs sans stocker les images ; conserver les `storage_key` et résultats (authentic/fake, liveness, face match) pour audit.

---

## 8. Exemples d’appels Azure (backend)

### 8.1 Custom Vision – Prediction

```http
POST {endpoint}/customvision/v3.0/Prediction/{projectId}/classify/iterations/{publishedName}/image
Prediction-Key: {predictionKey}
Content-Type: application/octet-stream

<corps binaire de l'image>
```

Réponse : liste de tags avec probabilité (ex. `"tagName": "fake", "probability": 0.92`). Adapter le seuil (ex. rejet si probability("fake") > 0.7).

### 8.2 Face API – Detect (pour extraire faceId)

```http
POST {endpoint}/face/v1.0/detect?returnFaceId=true&returnFaceLandmarks=false
Ocp-Apim-Subscription-Key: {subscriptionKey}
Content-Type: application/octet-stream

<corps binaire de l'image>
```

Réponse : tableau d’objets avec `faceId`. Utiliser le premier pour la comparaison.

### 8.3 Face API – Verify (comparaison deux visages)

```http
POST {endpoint}/face/v1.0/verify
Ocp-Apim-Subscription-Key: {subscriptionKey}
Content-Type: application/json

{
  "faceId1": "{faceId du document}",
  "faceId2": "{faceId de la selfie}"
}
```

Réponse : `"isIdentical": true/false`, `"confidence": 0.9`. Considérer identique si confidence ≥ 0.8.

### 8.4 Face API – Liveness

Suivre la documentation officielle pour le flux **Liveness** (session ou one-shot selon la version). En général : création de session, envoi d’images ou de frames, récupération du résultat « live » ou « spoof ».

---

## 9. Fichiers à créer / modifier (résumé)

| Fichier | Action |
|---------|--------|
| `config/AzureStorageProperties.java` | Créer (connection string, container) |
| `config/AzureCustomVisionProperties.java` | Créer |
| `config/AzureFaceProperties.java` | Créer |
| `service/IdentityDocumentStorageService.java` | Créer (Blob upload) |
| `service/FakeDocumentDetectionService.java` | Créer (Custom Vision) |
| `service/FaceVerificationService.java` | Créer (Liveness + Verify) |
| `service/IdentityVerificationService.java` | Créer (orchestration) |
| `controller/IdentityVerificationController.java` | Créer (endpoints) |
| `entity/Document.java` | Déjà présent – vérifier types (ID_CARD, PASSPORT, SELFIE) |
| `application.properties` | Ajouter propriétés Azure (ou utiliser env) |
| Mobile : `verification-identity-document.tsx` | Brancher caméra + upload API |
| Mobile : `verification-identity-selfie.tsx` | Guide liveness + capture + upload selfie |
| Mobile : `api/endpoints/identityVerification.api.ts` | Créer (appels backend) |

---

## 10. Actions à faire pour compléter l’implémentation

Checklist pour finaliser la mise en production et les tests de la vérification d’identité.

### 10.1 Azure – Ressources et configuration

1. **Créer les ressources Azure**
   - **Storage** : compte de stockage + conteneur (ex. `identity-documents`). Récupérer la chaîne de connexion.
   - **Custom Vision** : projet + entraînement avec au moins deux tags (ex. `authentic`, `fake`). Publier une itération et noter l’endpoint, la clé de prédiction, l’ID projet et le nom publié (ex. `Iteration1`).
   - **Face** : ressource Cognitive Services (Face). Noter l’endpoint et la clé d’abonnement.

2. **Configurer le backend**
   - Dans `application.properties` (ou variables d’environnement), renseigner :
     - `azure.storage.connection-string`
     - `azure.storage.container-identity`
     - `azure.custom-vision.endpoint`, `azure.custom-vision.prediction-key`, `azure.custom-vision.project-id`, `azure.custom-vision.published-name`
     - `azure.face.endpoint`, `azure.face.subscription-key`
   - Décommenter ou ajouter les propriétés si elles sont commentées.

3. **Sans Azure (développement)**  
   Si vous ne configurez pas Azure, le stockage document lèvera une exception à l’upload. Pour tester sans Azure, il faudrait soit mocker les services, soit ajouter un mode « stockage local » pour les documents (comme pour la photo de profil).

### 10.2 Custom Vision – Modèle anti-fraude

- Entraîner le modèle avec des images de documents **authentiques** (tag `authentic`) et **faux / contrefaits** (tag `fake`).
- Le code backend considère qu’un document est « faux » si le tag `fake` a une probabilité ≥ 0,7. Adapter le seuil dans `FakeDocumentDetectionService` si besoin.
- Tester l’appel Prediction API (Postman ou un script) pour vérifier les réponses avant de brancher le flux complet.

### 10.3 Face API – Liveness (optionnel)

- Actuellement, `UploadSelfieResponse.livenessPassed` est mis à `true` par défaut (non branché au Liveness Azure).
- Pour activer le Liveness : suivre la doc Microsoft (session Liveness), exposer un endpoint ou un service qui appelle l’API Liveness, et mettre à jour `FaceVerificationService` / `IdentityVerificationService` pour utiliser le résultat et remplir `livenessPassed` en conséquence.

### 10.4 Mobile

- **Permissions** : l’app demande l’accès à la galerie (document) et à la caméra (selfie). Vérifier le comportement sur iOS (Info.plist) et Android (AndroidManifest / permissions) si les messages de permission ne s’affichent pas.
- **Base URL** : `EXPO_PUBLIC_API_URL` doit pointer vers le backend (ex. `http://<IP>:8082/api`). Voir `kozala-mobile/docs/NETWORK-SETUP.md`.
- **Tests** : parcourir le flux document (recto/verso si CNI) puis selfie, et vérifier que le statut (`GET /api/identity-verification/status`) reflète bien `documentsUploaded`, `selfieUploaded`, `identityCompleted`.

### 10.5 Vérification du flux end-to-end

1. Se connecter avec un client (JWT valide).
2. Aller à la vérification d’identité → choisir type de document (CNI ou Passeport).
3. Document : ajouter recto (et verso pour CNI) via galerie, puis « Continuer ». Vérifier que l’API renvoie 200 et que les documents sont enregistrés (Blob + BDD).
4. Selfie : prendre un selfie (caméra frontale), puis « Envoyer la vérification ». Vérifier que le face match réussit (ou simuler en désactivant Face pour accepter par défaut).
5. Arriver sur l’écran de succès et contrôler `GET /api/identity-verification/status` (ex. `identityCompleted: true`).

### 10.6 Optionnel – Profil et statut global

- Si le profil utilisateur affiche un statut « identité complétée », s’assurer qu’il utilise soit `GET /api/identity-verification/status`, soit que le backend expose ce statut dans l’endpoint profil existant (`/api/profile/verification-status` ou équivalent) en cohérence avec les documents/selfie.

---

## 11. Références

- [Azure Blob Storage – Java](https://docs.microsoft.com/azure/storage/blobs/storage-quickstart-blobs-java)  
- [Custom Vision – Prediction API](https://docs.microsoft.com/azure/cognitive-services/custom-vision-service/use-prediction-api)  
- [Face API – Liveness](https://docs.microsoft.com/azure/cognitive-services/computer-vision/concept-face-api-liveness)  
- [Face API – Verify](https://docs.microsoft.com/azure/cognitive-services/computer-vision/concept-face-api-verify)  

Ce document peut être complété au fil de l’implémentation (ex. schéma de base de données, codes d’erreur précis, ou exemples de requêtes HTTP pour Custom Vision et Face API).
