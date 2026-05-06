# KMS (Key Management Service) API Documentation

## Overview

Ce document décrit todas les endpoints de gestion de clés disponibles dans le service KMS. Les APIs sont organisées en 7 catégories de fonctionnalités principales.

**Base URL:** `/api/v1/private/key`

---

## 1. Key Management APIs

### 1.1 Create Key
**Endpoint:** `POST /keys`

Crée une nouvelle clé cryptographique avec métadonnées et version initiale active.

**Request Body:**
```json
{
  "keySpec": "AES_256 | RSA_2048 | EC_P256",
  "purpose": "ENCRYPT_DECRYPT | SIGN_VERIFY",
  "alias": "optional-key-alias",
  "description": "optional key description"
}
```

**Response (200 OK):**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "arn": "arn:kms:tenant-id:key/key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status": "ENABLED",
  "createdAt": "2024-05-06T10:30:00"
}
```

---

### 1.2 Get Key Metadata
**Endpoint:** `GET /keys/{keyId}`

Récupère les métadonnées d'une clé spécifique.

**Path Parameters:**
- `keyId` (string, required): L'identifiant unique de la clé

**Response (200 OK):**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status": "ENABLED | DISABLED | PENDING_DELETION",
  "keySpec": "AES_256",
  "purpose": "ENCRYPT_DECRYPT",
  "currentVersion": "v-1",
  "createdAt": "2024-05-06T10:30:00",
  "alias": "optional-key-alias",
  "description": "optional description"
}
```

---

### 1.3 List Keys
**Endpoint:** `GET /keys`

Retourne une liste paginée des clés.

**Query Parameters:**
- `limit` (integer, optional): Nombre maximum de clés à retourner (defaut: 100, max: 1000)
- `nextToken` (string, optional): Token pour pagination

**Response (200 OK):**
```json
{
  "keys": [
    {
      "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "alias": "optional-alias",
      "status": "ENABLED"
    }
  ],
  "nextToken": "token-for-next-page"
}
```

---

### 1.4 Enable Key
**Endpoint:** `PATCH /keys/{keyId}/enable`

Active une clé désactivée pour les opérations cryptographiques.

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé

**Response (200 OK):**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status": "ENABLED"
}
```

---

### 1.5 Disable Key
**Endpoint:** `PATCH /keys/{keyId}/disable`

Désactive immédiatement l'utilisation d'une clé.

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé

**Response (200 OK):**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status": "DISABLED"
}
```

---

### 1.6 Schedule Key Deletion
**Endpoint:** `DELETE /keys/{keyId}`

Programme la suppression d'une clé (soft delete avec période de grâce).

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé

**Query Parameters:**
- `pendingWindowInDays` (integer, optional): Nombre de jours avant suppression (7-30, défaut: 7)

**Response (200 OK):**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status": "PENDING_DELETION",
  "deletionDate": "2024-05-13T10:30:00"
}
```

---

### 1.7 Rotate Key
**Endpoint:** `POST /keys/{keyId}/rotate`

Crée une nouvelle version cryptographique de la clé.

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé

**Response (200 OK):**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "newVersion": "v-2",
  "rotationDate": "2024-05-06T10:35:00"
}
```

---

## 2. Cryptographic Operations

### 2.1 Encrypt
**Endpoint:** `POST /encrypt`

Chiffre du texte brut avec une clé gérée par KMS.

**Request Body:**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "plaintext": "base64-encoded-plaintext",
  "encryptionContext": {
    "department": "finance",
    "purpose": "backup"
  }
}
```

**Response (200 OK):**
```json
{
  "ciphertext": "base64-encoded-ciphertext",
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "keyVersion": "v-1"
}
```

---

### 2.2 Decrypt
**Endpoint:** `POST /decrypt`

Déchiffre le texte chiffré avec la version de clé correcte automatiquement.

**Request Body:**
```json
{
  "ciphertext": "base64-encoded-ciphertext"
}
```

**Response (200 OK):**
```json
{
  "plaintext": "base64-encoded-plaintext",
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "keyVersion": "v-1"
}
```

---

### 2.3 Re-encrypt
**Endpoint:** `POST /reencrypt`

Rewrappe le texte chiffré d'une clé à une autre sans exposer le texte brut.

**Request Body:**
```json
{
  "ciphertext": "base64-encoded-ciphertext",
  "destinationKeyId": "dest-key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

**Response (200 OK):**
```json
{
  "ciphertext": "new-base64-encoded-ciphertext",
  "sourceKeyId": "source-key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "destinationKeyId": "dest-key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

---

## 3. Signing APIs (Asymmetric Keys)

### 3.1 Sign
**Endpoint:** `POST /sign`

Génère une signature numérique pour un message.

**Request Body:**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "message": "base64-encoded-message",
  "algorithm": "RSASSA_PSS_SHA256 | ECDSA_SHA256"
}
```

**Response (200 OK):**
```json
{
  "signature": "base64-encoded-signature",
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

---

### 3.2 Verify
**Endpoint:** `POST /verify`

Vérifie une signature numérique.

**Request Body:**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "message": "base64-encoded-message",
  "signature": "base64-encoded-signature"
}
```

**Response (200 OK):**
```json
{
  "valid": true
}
```

---

## 4. Key Policy & Access Control

### 4.1 Set Key Policy
**Endpoint:** `PUT /keys/{keyId}/policy`

Définit des règles d'accès de type IAM pour l'utilisation des clés.

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé

**Request Body:**
```json
{
  "policy": {
    "Version": "2023-01-01",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "AWS": "arn:aws:iam::123456789012:user/alice"
        },
        "Action": "kms:Decrypt",
        "Resource": "*"
      }
    ]
  }
}
```

**Response (200 OK):**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status": "UPDATED"
}
```

---

### 4.2 Get Key Policy
**Endpoint:** `GET /keys/{keyId}/policy`

Récupère la politique actuelle d'une clé.

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé

**Response (200 OK):**
```json
{
  "Version": "2023-01-01",
  "Statement": [ ... ]
}
```

---

### 4.3 Create Grant
**Endpoint:** `POST /keys/{keyId}/grants`

Délègue un accès limité à une clé.

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé

**Request Body:**
```json
{
  "principal": "arn:aws:iam::123456789012:user/bob",
  "operations": ["encrypt", "decrypt", "sign"]
}
```

**Response (200 OK):**
```json
{
  "grantId": "grant-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

---

### 4.4 Revoke Grant
**Endpoint:** `DELETE /keys/{keyId}/grants/{grantId}`

Supprime les permissions accordées précédemment.

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé
- `grantId` (string, required): L'identifiant du grant

**Response (200 OK):**
```json
{
  "status": "REVOKED"
}
```

---

## 5. Key Versioning APIs

### 5.1 List Key Versions
**Endpoint:** `GET /keys/{keyId}/versions`

Liste toutes les versions d'une clé.

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé

**Response (200 OK):**
```json
{
  "versions": [
    {
      "versionId": "v-1",
      "createdAt": "2024-05-01T10:00:00",
      "status": "ACTIVE"
    },
    {
      "versionId": "v-2",
      "createdAt": "2024-05-06T10:30:00",
      "status": "INACTIVE"
    }
  ]
}
```

---

### 5.2 Get Active Version
**Endpoint:** `GET /keys/{keyId}/active-version`

Retourne la version actuellement active d'une clé.

**Path Parameters:**
- `keyId` (string, required): L'identifiant de la clé

**Response (200 OK):**
```json
{
  "versionId": "v-2"
}
```

---

## 6. Data Key API (Envelope Encryption)

### 6.1 Generate Data Key
**Endpoint:** `POST /datakey/generate`

Génère une clé de chiffrement de données (DEK) pour le chiffrement côté client.

**Request Body:**
```json
{
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "keySize": 256
}
```

**Response (200 OK):**
```json
{
  "plaintextKey": "base64-encoded-plaintext-key",
  "encryptedKey": "base64-encoded-encrypted-key",
  "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

---

## 7. Audit APIs

### 7.1 Get Audit Logs
**Endpoint:** `GET /audit/logs`

Retourne l'historique d'utilisation cryptographique pour la conformité et la surveillance.

**Query Parameters:**
- `keyId` (string, optional): Filtrer par identifiant de clé
- `fromDate` (string, optional): Date de début (format ISO 8601: YYYY-MM-DDTHH:mm:ss)
- `toDate` (string, optional): Date de fin (format ISO 8601: YYYY-MM-DDTHH:mm:ss)
- `limit` (integer, optional): Nombre maximum de logs à retourner (défaut: 100)

**Response (200 OK):**
```json
{
  "logs": [
    {
      "action": "ENCRYPT",
      "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "timestamp": "2024-05-06T10:35:00",
      "principal": "user@example.com",
      "ip": "192.168.1.1"
    },
    {
      "action": "DECRYPT",
      "keyId": "key-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "timestamp": "2024-05-06T10:36:00",
      "principal": "user@example.com",
      "ip": "192.168.1.1"
    }
  ]
}
```

---

## Error Handling

Tous les endpoints retournent les codes HTTP standards :

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Requête traitée avec succès |
| 400 | Bad Request | Paramètres invalides ou malformés |
| 404 | Not Found | Ressource non trouvée (clé, grant, etc.) |
| 409 | Conflict | Conflit (ex: clé déjà existe) |
| 500 | Internal Server Error | Erreur du serveur |

### Error Response Format
```json
{
  "error": "ERROR_CODE",
  "message": "Description de l'erreur",
  "timestamp": "2024-05-06T10:35:00"
}
```

---

## Authentication & Authorization

Tous les endpoints requièrent une authentification :
- **Header:** `Authorization: Bearer {token}`

L'autorisation est basée sur :
- Tenant (isolement multi-locataires)
- Permissions utilisateur
- Politiques de clé et grants

---

## Rate Limiting

- **Limit:** 1000 requêtes par minute par tenant
- **Header Response:** `X-RateLimit-Remaining`, `X-RateLimit-Reset`

---

## Best Practices

1. **Rotation de Clés:** Rotez vos clés régulièrement (recommandé: mensuellement)
2. **Audit Logs:** Consultez régulièrement les logs d'audit pour la conformité
3. **Politique de Clé:** Définissez des politiques d'accès restrictives
4. **Backup:** Sauvegardez les données chiffrées avec les clés de déchiffrement
5. **Grants:** Utilisez les grants pour le principe du moindre privilège
6. **Contexte de Chiffrement:** Utilisez encryptionContext pour la dérivation de clés

---

## Examples

### Example 1: Chiffrement de données sensibles
```bash
curl -X POST http://localhost:8080/api/v1/private/key/encrypt \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "keyId": "key-12345",
    "plaintext": "dGVzdCBkYXRhIHRvIGVuY3J5cHQ=",
    "encryptionContext": {
      "department": "finance",
      "compliance": "gdpr"
    }
  }'
```

### Example 2: Génération de signature numérique
```bash
curl -X POST http://localhost:8080/api/v1/private/key/sign \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "keyId": "key-rsa-2048",
    "message": "bWVzc2FnZSB0byBzaWdu",
    "algorithm": "RSASSA_PSS_SHA256"
  }'
```

### Example 3: Génération de clé de données
```bash
curl -X POST http://localhost:8080/api/v1/private/key/datakey/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "keyId": "key-aes-256",
    "keySize": 256
  }'
```

---

## Versions API

**Version actuelle:** v1
- Release Date: 2024-05-06
- Support: Long-term support

---

## Support & Feedback

Pour toute question ou feedback, contactez le support KMS ou consultez la documentation interne.

