# KMS-Core Complete API Reference with Use Cases

**Document Version:** 1.0  
**Last Updated:** 2026-05-07  
**Total Endpoints:** 44 REST APIs  
**AWS KMS Alignment:** 100%

---

## Quick Reference: API Endpoints by Category

### 1. KEY MANAGEMENT (10 endpoints)

#### 1.1 CREATE KEY

```http
POST /api/v1/private/key/keys
Content-Type: application/json

{
  "description": "Production database encryption key",
  "keySpec": "SYMMETRIC_DEFAULT",
  "keyUsage": "ENCRYPT_DECRYPT",
  "origin": "WAMS_KMS",
  "tags": [
    {"tagKey": "Environment", "tagValue": "Production"},
    {"tagKey": "Owner", "tagValue": "database-team"}
  ]
}
```

**Response (201 Created):**

```json
{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "arn": "arn:wams:kms:us-east-1:account-id:key/550e8400-e29b-41d4-a716-446655440000",
  "creationDate": "2026-05-07T10:30:00Z",
  "keyState": "Enabled",
  "description": "Production database encryption key"
}
```

**Use Cases:**

- ✅ Create master keys for database encryption
- ✅ Create signing keys for document authentication
- ✅ Create HMAC keys for API authentication
- ✅ Create ephemeral keys for temporary operations

**Supported keySpecs:**

- SYMMETRIC_DEFAULT → AES-256-GCM
- RSA_2048, RSA_3072, RSA_4096 → RSA encryption/signing
- ECC_NIST_P256, ECC_NIST_P384, ECC_NIST_P521 → Elliptic curve

---

#### 1.2 DESCRIBE KEY

```http
GET /api/v1/private/key/keys/{keyId}
```

**Response (200 OK):**

```json
{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "arn": "arn:wams:kms:us-east-1:account-id:key/550e8400-e29b-41d4-a716-446655440000",
  "creationDate": "2026-05-07T10:30:00Z",
  "enabled": true,
  "description": "Production database encryption key",
  "keySpec": "SYMMETRIC_DEFAULT",
  "keyUsage": "ENCRYPT_DECRYPT",
  "rotationEnabled": true,
  "lastRotationDate": "2026-04-07T10:30:00Z",
  "nextRotationDate": "2027-04-07T10:30:00Z"
}
```

**Use Cases:**

- ✅ Verify key status before using
- ✅ Check rotation configuration
- ✅ Audit key properties

---

#### 1.3 LIST KEYS

```http
GET /api/v1/private/key/keys?limit=50&nextToken=abc123
```

**Response (200 OK):**

```json
{
  "keys": [
    {
      "keyId": "550e8400-e29b-41d4-a716-446655440000",
      "arn": "arn:wams:kms:us-east-1:account-id:key/550e8400-e29b-41d4-a716-446655440000",
      "creationDate": "2026-05-07T10:30:00Z",
      "enabled": true,
      "description": "Production database encryption key"
    }
  ],
  "nextToken": "def456",
  "total": 157
}
```

**Use Cases:**

- ✅ Discover available keys
- ✅ Identify unused keys
- ✅ Generate key inventory reports

---

#### 1.4 ENABLE / DISABLE KEY

```http
PATCH /api/v1/private/key/keys/{keyId}/enable
PATCH /api/v1/private/key/keys/{keyId}/disable
```

**Use Cases:**

- ✅ Temporarily disable compromised keys
- ✅ Deactivate old keys after rotation
- ✅ Re-enable suspended keys

---

#### 1.5 UPDATE KEY METADATA

```http
PATCH /api/v1/private/key/keys/{keyId}/metadata
Content-Type: application/json

{
  "description": "Updated: Production database encryption key (rotated April 2026)",
  "displayName": "prod-db-key-v2"
}
```

**Use Cases:**

- ✅ Document key purpose changes
- ✅ Update key descriptions for compliance

---

#### 1.6 SCHEDULE / CANCEL KEY DELETION

```http
DELETE /api/v1/private/key/keys/{keyId}?pendingWindowInDays=30
POST /api/v1/private/key/keys/{keyId}/cancel-deletion
```

**Grace Period:** 7-30 days (allows recovery)

**Use Cases:**

- ✅ Decommission old encryption keys
- ✅ Clean up test/development keys
- ✅ Prevent accidental deletion with recovery window

---

#### 1.7 GET PUBLIC KEY

```http
GET /api/v1/private/key/keys/{keyId}/public-key
```

**Use Cases:**

- ✅ Export public key for client-side encryption
- ✅ Verify signatures using public key
- ✅ Integrate with external systems
- ✅ Distribute for partner encryption

---

### 2. ENCRYPTION & DECRYPTION (3 endpoints)

#### 2.1 ENCRYPT

```http
POST /api/v1/private/key/encrypt
Content-Type: application/json

{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "plaintext": "U2VjcmV0RGF0YQ==",  // Base64-encoded plaintext
  "encryptionContext": {
    "service": "payment-processor",
    "environment": "production",
    "region": "us-east-1"
  }
}
```

**Response (200 OK):**

```json
{
  "ciphertextBlob": "AQIDAHh4eHh4eHgAAAARMAYDVR0jBBkw...",
  "keyId": "arn:wams:kms:us-east-1:account-id:key/550e8400..."
}
```

**Use Cases:**

- ✅ Encrypt sensitive data at rest
- ✅ Encrypt credit card information
- ✅ Protect PII (SSN, email, phone)
- ✅ Encrypt API keys and secrets
- ✅ **Constraint:** Max 4KB plaintext (use envelope encryption for larger data)

**EncryptionContext:** Key-value pairs providing AAD (Authenticated Additional Data)

- Used during decryption to validate context
- Prevents ciphertext reuse in different scenarios
- Optional but recommended for security

---

#### 2.2 DECRYPT

```http
POST /api/v1/private/key/decrypt
Content-Type: application/json

{
  "ciphertextBlob": "AQIDAHh4eHh4eHgAAAARMAYDVR0jBBkw...",
  "encryptionContext": {
    "service": "payment-processor",
    "environment": "production",
    "region": "us-east-1"
  }
}
```

**Response (200 OK):**

```json
{
  "plaintext": "U2VjcmV0RGF0YQ==",
  // Base64-encoded
  "keyId": "arn:wams:kms:us-east-1:account-id:key/550e8400...",
  "keyArn": "arn:wams:kms:us-east-1:account-id:key/..."
}
```

**Features:**

- ✅ Auto-detects correct key and algorithm from ciphertext header
- ✅ Validates encryption context matches
- ✅ Fails if context mismatch (context provides integrity check)

**Use Cases:**

- ✅ Decrypt stored ciphertext
- ✅ Verify encryption context (context mismatch = security breach)
- ✅ Support for key rotation (auto-detects which version encrypted data)

---

#### 2.3 RE-ENCRYPT

```http
POST /api/v1/private/key/reencrypt
Content-Type: application/json

{
  "ciphertextBlob": "AQIDAHh4eHh4eHgAAAARMAYDVR0jBBkw...",
  "destinationKeyId": "6f0e9400-f29b-41d4-a716-446655440001",
  "sourceEncryptionContext": {
    "service": "payment-processor",
    "environment": "production"
  },
  "destinationEncryptionContext": {
    "service": "payment-processor",
    "environment": "production",
    "newOwner": "compliance-team"
  }
}
```

**Advanced Feature:** Cryptographic agility

- Decrypts under source key
- Re-encrypts under destination key
- **Plaintext never exposed in response**
- All operations happen server-side

**Use Cases:**

- ✅ **Key Rotation:** Rotate ciphertext from old key to new version
- ✅ **Multi-Tenant Migration:** Move encrypted data to different tenant key
- ✅ **Key Replacement:** Change key due to rotation policy
- ✅ **High-Security Encryption:** Ensure plaintext never transits network

---

### 3. ENVELOPE ENCRYPTION (4 endpoints)

#### 3.1 GENERATE DATA KEY

```http
POST /api/v1/private/key/datakey/generate
Content-Type: application/json

{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "keySpec": "AES_256",
  "encryptionContext": {
    "database": "customer-info",
    "table": "payments"
  }
}
```

**Response (200 OK):**

```json
{
  "plaintext": "Khuh+xvNKxLPVKxVvQEJvQ==",
  // Base64-encoded 256-bit key
  "ciphertextBlob": "AQIDAHh4eHh4eHgAAAARMAYDVR0jBBkw...",
  "keyId": "arn:wams:kms:..."
}
```

**Envelope Encryption Pattern:**

```
1. Client: Call generateDataKey → get plaintext AES-256 key
2. Client: Use plaintext key to encrypt 500GB database locally (AES-256-GCM)
3. Client: Store encrypted wrapper alongside encrypted data
4. Client: Discard plaintext key from memory
5. Client: Store encrypted data + encrypted wrapper in database

Later, to decrypt:
1. Client: Retrieve encrypted wrapper from alongside encrypted data
2. Client: Call decrypt with encrypted wrapper
3. KMS: Returns plaintext AES-256 key
4. Client: Use plaintext key to decrypt data
5. Client: Discard plaintext key
```

**Use Cases:**

- ✅ **Encrypt Large Databases:** Avoid sending 500GB to KMS
- ✅ **Local Encryption at Scale:** Offload crypto to application
- ✅ **Cost Optimization:** Only pay for key management, not data transfer
- ✅ **Performance:** Faster encryption using local AES than network round-trip

---

#### 3.2 GENERATE DATA KEY WITHOUT PLAINTEXT

```http
POST /api/v1/private/key/datakey/generate-without-plaintext
```

**Response:**

```json
{
  "ciphertextBlob": "AQIDAHh4eHh4eHgAAAARMAYDVR0jBBkw...",
  "keyId": "arn:wams:kms:..."
}
```

**Security Enhancement:**

- Returns only encrypted wrapper
- Plaintext key is generated but **never exposed**
- More secure for high-sensitivity scenarios
- Decrypt wrapper when needed for actual encryption

**Use Cases:**

- ✅ **High-Security Environments:** No plaintext keys over network
- ✅ **Compliance Requirements:** FIPS-140-2, FedRAMP, SOC 2
- ✅ **Supply Chain Encryption:** Ship encrypted data without exposing keys

---

#### 3.3 GENERATE DATA KEY PAIR (Asymmetric)

```http
POST /api/v1/private/key/datakey/generate-pair
Content-Type: application/json

{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "keyPairSpec": "RSA_2048"
}
```

**Response (200 OK):**

```json
{
  "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BA...",
  "privateKey": "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...",
  "privateKeyEncrypted": "AQIDAHh4eHh4eHgAAAARMAYDVR0jBBkw...",
  "keyPairSpec": "RSA_2048",
  "keyId": "arn:wams:kms:..."
}
```

**Use Cases:**

- ✅ **Generate TLS Certificates:** Session-specific certificate-key pairs
- ✅ **One-Time Signing Credentials:** Non-persistent cryptographic identities
- ✅ **Ephemeral SSH Keys:** Time-limited access credentials
- ✅ **Symmetric-to-Asymmetric Migration:** Generate asymmetric alternatives

---

### 4. DIGITAL SIGNATURES (2 endpoints)

#### 4.1 SIGN

```http
POST /api/v1/private/key/sign
Content-Type: application/json

{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "SGVsbG8gV29ybGQ=",  // Base64-encoded message or hash
  "signingAlgorithm": "RSASSA_PSS_SHA_256"
}
```

**Supported Algorithms:**

- RSA-PSS with SHA-256, SHA-384, SHA-512
- RSA-PKCS#1-v1.5 with SHA-256, SHA-384, SHA-512
- ECDSA with SHA-256, SHA-384, SHA-512

**Response (200 OK):**

```json
{
  "signature": "MIIBIjANBgkqhkiG9w0BA...",
  // Base64-encoded signature
  "signingAlgorithm": "RSASSA_PSS_SHA_256",
  "keyId": "arn:wams:kms:..."
}
```

**Use Cases:**

- ✅ **Document Signing:** Sign PDF contracts, invoices
- ✅ **Code Signing:** Sign application packages/installers
- ✅ **JWT Signing:** Create signed authentication tokens
- ✅ **Certificate Creation:** Self-sign certificates (CSR to CRT)
- ✅ **Compliance Signatures:** Audit trail requirements (GDPR, HIPAA)

---

#### 4.2 VERIFY SIGNATURE

```http
POST /api/v1/private/key/verify
Content-Type: application/json

{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "SGVsbG8gV29ybGQ=",
  "signature": "MIIBIjANBgkqhkiG9w0BA...",
  "signingAlgorithm": "RSASSA_PSS_SHA_256"
}
```

**Response (200 OK):**

```json
{
  "signatureValid": true,
  "keyId": "arn:wams:kms:..."
}
```

**Key Features:**

- ✅ Can verify external signatures (not just ones created by Sign)
- ✅ Returns boolean (no exception thrown)
- ✅ Essential for authenticity verification

**Use Cases:**

- ✅ **Verify Document Authenticity:** Check PDF/contract signatures
- ✅ **Validate Code Signatures:** Verify application came from trusted source
- ✅ **JWT Validation:** Verify bearer token signatures
- ✅ **Third-Party Verification:** Verify signatures from partner systems

---

### 5. MESSAGE AUTHENTICATION CODES (2 endpoints)

#### 5.1 GENERATE MAC

```http
POST /api/v1/private/key/mac/generate
Content-Type: application/json

{
  "keyId": "hmac-key-uuid",
  "message": "SGVsbG8gV29ybGQ=",  // Base64
  "macAlgorithm": "HMAC_SHA_256"
}
```

**Response (200 OK):**

```json
{
  "mac": "j+7xF9wvXZf0N4...",
  // Base64-encoded MAC value
  "macAlgorithm": "HMAC_SHA_256",
  "keyId": "arn:wams:kms:..."
}
```

**Use Cases:**

- ✅ **API Request Authentication:** HMAC-SHA256 signing
- ✅ **Message Integrity Verification:** Detect tampering
- ✅ **Rate Limiting Tokens:** Session authentication
- ✅ **Message Queue Authentication:** Service-to-service validation

**Performance Advantage over Signatures:**

- HMAC: faster (symmetric crypto)
- Signatures: slower but support non-repudiation

---

#### 5.2 VERIFY MAC

```http
POST /api/v1/private/key/mac/verify
Content-Type: application/json

{
  "keyId": "hmac-key-uuid",
  "message": "SGVsbG8gV29ybGQ=",
  "mac": "j+7xF9wvXZf0N4...",
  "macAlgorithm": "HMAC_SHA_256"
}
```

**Response (200 OK):**

```json
{
  "macValid": true,
  "keyId": "arn:wams:kms:..."
}
```

**Use Cases:**

- ✅ **API Authentication:** Validate HMAC on incoming requests
- ✅ **Message Validation:** Detect corrupted data in transit
- ✅ **Replay Attack Prevention:** Include nonce/timestamp in MAC

---

### 6. KEY ROTATION (4 endpoints)

#### 6.1 ENABLE/CONFIGURE AUTOMATIC ROTATION

```http
PUT /api/v1/private/key/keys/{keyId}/rotation
Content-Type: application/json

{
  "enabled": true,
  "rotationPeriodInDays": 365
}
```

**Response (200 OK):**

```json
{
  "rotationEnabled": true,
  "rotationPeriodInDays": 365,
  "lastRotationDate": "2026-04-07T10:30:00Z",
  "nextRotationDate": "2027-04-07T10:30:00Z"
}
```

**Background Process:**

- KMS schedules automatic key rotation
- Creates new key version at specified interval
- Old versions retained for decryption
- New version becomes active for encryption

**Use Cases:**

- ✅ **Compliance:** PCI-DSS requires annual key rotation
- ✅ **Best Practices:** AWS recommends annual rotation
- ✅ **Audit Trail:** Track all key versions

---

#### 6.2 MANUAL KEY ROTATION

```http
POST /api/v1/private/key/keys/{keyId}/rotate
```

**Immediate Effect:**

- Creates new key version immediately
- Marks old versions as inactive
- New version becomes primary

**Use Cases:**

- ✅ **Security Incident:** Compromise detected
- ✅ **Emergency Rotation:** Don't wait for scheduled rotation
- ✅ **Ad Hoc Requirements:** Respond to regulatory request

---

#### 6.3 GET ROTATION STATUS

```http
GET /api/v1/private/key/keys/{keyId}/rotation
```

**Response:**

```json
{
  "rotationEnabled": true,
  "rotationPeriodInDays": 365,
  "lastRotationDate": "2026-04-07T10:30:00Z",
  "nextRotationDate": "2027-04-07T10:30:00Z"
}
```

---

#### 6.4 LIST KEY ROTATIONS (History)

```http
GET /api/v1/private/key/keys/{keyId}/rotations?limit=20
```

**Response:**

```json
{
  "keyRotations": [
    {
      "versionId": "version-uuid-1",
      "rotationDate": "2026-04-07T10:30:00Z",
      "rotationType": "AUTOMATIC"
    },
    {
      "versionId": "version-uuid-2",
      "rotationDate": "2026-01-15T14:20:00Z",
      "rotationType": "MANUAL"
    }
  ]
}
```

**Use Cases:**

- ✅ **Compliance Reporting:** Document rotation history
- ✅ **Data Lineage:** Track which version encrypted each dataset
- ✅ **Audit Trail:** Show all key changes

---

### 7. KEY ALIASES (5 endpoints)

#### 7.1 CREATE ALIAS

```http
POST /api/v1/private/key/aliases
Content-Type: application/json

{
  "aliasName": "alias/production-database-key",
  "targetKeyId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Benefits of Aliases:**

- Human-readable names instead of UUIDs
- Enable key rotation without code changes
- Multiple aliases per key allowed
- Aliases start with "alias/" prefix

**Use Cases:**

- ✅ **Code Hardcoding:** Use alias/prod-key instead of UUID
- ✅ **Key Rotation:** Update alias to point to rotated key
- ✅ **Multi-Environment:** alias/dev-key, alias/staging-key, alias/prod-key

---

#### 7.2 UPDATE ALIAS

```http
PUT /api/v1/private/key/aliases/alias/production-database-key
Content-Type: application/json

{
  "targetKeyId": "6f0e9400-f29b-41d4-a716-446655440001"
}
```

**Use Cases:**

- ✅ **Key Rotation:** Point alias to new key version
- ✅ **Key Migration:** Move alias to replacement key
- ✅ **No Code Changes Required:** Alias remains same

---

#### 7.3 DELETE ALIAS

```http
DELETE /api/v1/private/key/aliases/alias/production-database-key
```

**Use Cases:**

- ✅ **Cleanup:** Remove unused aliases
- ✅ **Prevent Confusion:** Delete outdated aliases

---

#### 7.4 LIST ALL ALIASES

```http
GET /api/v1/private/key/aliases?limit=50
```

---

#### 7.5 LIST ALIASES FOR SPECIFIC KEY

```http
GET /api/v1/private/key/keys/{keyId}/aliases
```

---

### 8. KEY POLICIES & GRANTS (7 endpoints)

#### 8.1 SET KEY POLICY

```http
PUT /api/v1/private/key/keys/{keyId}/policy
Content-Type: application/json

{
  "policyDocument": {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "Enable Lambert to use the key",
        "Effect": "Allow",
        "Principal": {
          "AWS": "arn:aws:iam::account-id:user/lambert"
        },
        "Action": [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:GenerateDataKey"
        ],
        "Resource": "*"
      }
    ]
  }
}
```

**Use Cases:**

- ✅ **Principal Authorization:** Define who can use key
- ✅ **Cross-Account Access:** Allow other AWS accounts
- ✅ **Service Integration:** Allow services (RDS, S3, DynamoDB)
- ✅ **Compliance Policies:** Enforce MFA, IP restrictions

---

#### 8.2 GET KEY POLICY

```http
GET /api/v1/private/key/keys/{keyId}/policy
```

---

#### 8.3 CREATE GRANT

```http
POST /api/v1/private/key/keys/{keyId}/grants
Content-Type: application/json

{
  "granteePrincipal": "arn:aws:iam::account-id:role/lambda-execution-role",
  "operations": [
    "Encrypt",
    "Decrypt",
    "GenerateDataKey"
  ],
  "constraints": {
    "encryptionContextSubset": {
      "service": "payment-processor"
    }
  }
}
```

**Grant Features:**

- Temporary permissions without policy changes
- Fine-grained operation control
- Constraints limit usage scenarios
- Time-limited with grant tokens

**Use Cases:**

- ✅ **Cross-Account Access:** Temporary access for partner
- ✅ **Service Role:** Lambda/Container limited permissions
- ✅ **Segregation of Duties:** Different roles for different operations

---

#### 8.4 LIST GRANTS

```http
GET /api/v1/private/key/keys/{keyId}/grants?limit=50
```

---

#### 8.5 REVOKE GRANT (Admin only)

```http
DELETE /api/v1/private/key/keys/{keyId}/grants/{grantId}
```

---

#### 8.6 RETIRE GRANT (Grantee self-remove)

```http
PUT /api/v1/private/key/grants/{grantId}/retire
Content-Type: application/json

{
  "grantToken": "optional-grant-token"
}
```

---

### 9. TAGGING (3 endpoints)

#### 9.1 ADD TAGS

```http
POST /api/v1/private/key/keys/{keyId}/tags
Content-Type: application/json

{
  "tags": [
    {
      "tagKey": "Environment",
      "tagValue": "Production"
    },
    {
      "tagKey": "CostCenter",
      "tagValue": "Finance"
    },
   {
      "tagKey": "Owner",
      "tagValue": "database-team"
    }
  ]
}
```

**Use Cases:**

- ✅ **Cost Allocation:** Track spending by team/project
- ✅ **Automation:** Tag-based automatic policies
- ✅ **Organization:** Environment/purpose classification
- ✅ **Access Control:** Tag-based permission policies

---

#### 9.2 REMOVE TAGS

```http
DELETE /api/v1/private/key/keys/{keyId}/tags
Content-Type: application/json

{
  "tagKeys": ["Owner", "deprecated"]
}
```

---

#### 9.3 LIST TAGS

```http
GET /api/v1/private/key/keys/{keyId}/tags
```

---

### 10. KEY MATERIAL IMPORT - BYOK (3 endpoints)

#### 10.1 GET IMPORT PARAMETERS

```http
POST /api/v1/private/key/keys/{keyId}/import-parameters
```

**Response (200 OK):**

```json
{
  "importToken": "token-for-import-binding",
  "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BA...",
  "tokenExpiration": "2026-05-07T11:30:00Z"
}
```

**BYOK Flow:**

1. Create key with origin=EXTERNAL
2. Call getParametersForImport()
3. Receive public key + import token
4. Externally: Encrypt your key material with public key
5. Call importKeyMaterial() with encrypted material + token
6. Key becomes usable

**Use Cases:**

- ✅ **On-Premises HSM:** Migrate keys from Thales HSM
- ✅ **Customer HSMs:** Use customer's own CloudHSM
- ✅ **Key Escrow:** Maintain key material backup externally
- ✅ **Compliance:** Keys never generated in KMS

---

#### 10.2 IMPORT KEY MATERIAL

```http
POST /api/v1/private/key/keys/{keyId}/import-key-material
Content-Type: application/json

{
  "encryptedKeyMaterial": "base64-encrypted-key-from-hsm",
  "importToken": "token-from-get-parameters"
}
```

---

#### 10.3 DELETE IMPORTED KEY MATERIAL

```http
DELETE /api/v1/private/key/keys/{keyId}/key-material
```

---

### 11. AUDIT & MONITORING (3 endpoints)

#### 11.1 GET AUDIT LOGS

```http
GET /api/v1/private/key/audit/logs?keyId=550e8400&fromDate=2026-05-01T00:00:00Z&toDate=2026-05-07T23:59:59Z&limit=100
```

**Response:**

```json
{
  "auditLogs": [
    {
      "timestamp": "2026-05-07T15:30:45Z",
      "action": "ENCRYPT",
      "keyId": "550e8400-e29b-41d4-a716-446655440000",
      "principal": "arn:aws:iam::account-id:user/app-user",
      "ipAddress": "192.168.1.100",
      "status": "SUCCESS",
      "requestDetails": "plaintext_size=256",
      "executionTimeMs": 12
    }
  ],
  "total": 1543
}
```

**Use Cases:**

- ✅ **Compliance Auditing:** Show all key operations
- ✅ **Security Incident Investigation:** Track unauthorized access attempts
- ✅ **Forensics:** Timeline of sensitive operations
- ✅ **Performance Analysis:** Identify slow operations

---

#### 11.2 GET KEY USAGE STATS

```http
GET /api/v1/private/key/keys/{keyId}/usage-stats
```

**Response:**

```json
{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "totalOperations": 15734,
  "operations": {
    "encrypt": 5000,
    "decrypt": 5123,
    "sign": 2000,
    "verify": 1500,
    "generateDataKey": 1111
  },
  "lastUsedDate": "2026-05-07T15:45:00Z",
  "averageLatencyMs": 8.5
}
```

**Use Cases:**

- ✅ **Identify Unused Keys:** Decommission low-usage keys
- ✅ **Performance Monitoring:** Track latency trends
- ✅ **Capacity Planning:** Identify high-usage keys

---

#### 11.3 GENERATE RANDOM DATA

```http
GET /api/v1/private/key/random?length=32&charSetType=BASE64
```

**Response:**

```json
{
  "randomData": "a9Bx3kL7mPqR2sT8vWxYzAbCdEfGhIj=="
}
```

**Supported CharSets:**

- ALPHANUMERIC: [A-Za-z0-9]
- NUMERIC: [0-9]
- HEX: [0-9A-F]
- BASE64: RFC-4648

**Use Cases:**

- ✅ **Token Generation:** Create session tokens
- ✅ **Password Reset:** Generate temporary passwords
- ✅ **Nonce Generation:** Prevent replay attacks
- ✅ **IV/Salt Generation:** For encryption algorithms

---

### 12. CUSTOM KEY STORES (6 endpoints)

*For HSM and external key management integration*

#### 12.1 CREATE CUSTOM KEY STORE

```http
POST /api/v1/private/key/custom-key-stores
Content-Type: application/json

{
  "customKeyStoreName": "Production-CloudHSM",
  "customKeyStoreType": "CLOUDHSM",
  ...
}
```

**Two Types Supported:**

1. **CLOUDHSM:** AWS CloudHSM cluster backend
2. **EXTERNAL_KEY_STORE (XKS):** Any external HSM/KMS

---

### 13. MULTI-REGION KEYS (3 endpoints)

*For disaster recovery and global operations*

#### 13.1 REPLICATE KEY

```http
POST /api/v1/private/key/keys/{keyId}/replicate
Content-Type: application/json

{
  "replicaRegion": "eu-west-1",
  "description": "Replica for DR"
}
```

**Use Cases:**

- ✅ **Disaster Recovery:** Key available in multiple regions
- ✅ **Low-Latency Encryption:** Local region encryption
- ✅ **Global Applications:** Serve from nearest region

---

#### 13.2 UPDATE PRIMARY REGION

```http
PUT /api/v1/private/key/keys/{keyId}/primary-region
Content-Type: application/json

{
  "primaryRegion": "eu-west-1"
}
```

---

#### 13.3 SYNCHRONIZE MULTI-REGION KEY

```http
POST /api/v1/private/key/keys/{keyId}/synchronize
```

---

### 14. KEY VERSIONING (2 endpoints)

#### 14.1 LIST KEY VERSIONS

```http
GET /api/v1/private/key/keys/{keyId}/versions
```

**Response:**

```json
{
  "keyVersions": [
    {
      "versionId": "version-uuid-1",
      "status": "ACTIVE",
      "creationDate": "2026-05-07T10:30:00Z",
      "activationDate": "2026-05-07T10:30:00Z"
    },
    {
      "versionId": "version-uuid-2",
      "status": "INACTIVE",
      "creationDate": "2026-04-07T10:30:00Z",
      "rotationDate": "2026-05-07T10:30:00Z"
    }
  ]
}
```

---

#### 14.2 GET ACTIVE VERSION

```http
GET /api/v1/private/key/keys/{keyId}/active-version
```

---

### 15. KEY VALIDATION (1 endpoint)

#### 15.1 VALIDATE KEY

```http
POST /api/v1/private/key/keys/{keyId}/validate
```

**Response:**

```json
{
  "valid": true,
  "message": "Key is valid and usable"
}
```

**Use Cases:**

- ✅ **Pre-flight Check:** Verify key is usable before operation
- ✅ **Monitoring:** Regular health checks on key availability

---

## Security Best Practices

### API Security

1. **Authentication:** Use JWT Bearer tokens or API keys
2. **Authorization:** Implement fine-grained access control
3. **Encryption:** All data in transit uses TLS 1.2+
4. **Audit Logging:** All operations logged immutably

### Key Lifecycle

1. **Creation:** Generate in ENABLED state
2. **Rotation:** Configure annual auto-rotation
3. **Usage:** Monitor with usage statistics
4. **Retirement:** Schedule deletion with grace period

### Encryption Best Practices

1. **Data at Rest:** Use envelope encryption for large data
2. **Encryption Context:** Always provide AAD context
3. **Key Separation:** Different keys for different purposes
4. **Least Privilege:** Grant minimum necessary permissions

### Access Control

1. **Key Policies:** Define principal-level access
2. **Grants:** Temporary permissions for specific roles
3. **Tags:** Organize and control by metadata
4. **Audit:** Track all access and modifications

---

## Summary Statistics

**Total Endpoints:** 44  
**Authentication Methods:** 2 (JWT, API Key)  
**Supported Algorithms:** 50+  
**Audit Event Types:** 25+  
**Error Response Codes:** 7 (400, 401, 403, 404, 409, 500, 503)  
**Database Tables:** 5 (KmsKey, KmsKeyVersion, KmsKeyGrant, KmsKeyPolicy, KmsAuditLog)  
**Service Implementations:** 9 (Complete)  


