# KMS-Core Service Implementation Guide

**Document Version:** 1.0  
**Last Updated:** 2026-05-07  
**Author:** Development Team

---

## Service Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     REST Controller Layer                    │
│                    (KmsController.java)                      │
│                 40+ REST API Endpoints                       │
└──────────────────────┬──────────────────────────────────────┘
                       │ @Autowired
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer (9 Services)               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ 1. IKeyService                                       │   │
│  │    - generateRandomData(tenant, length, charset)    │   │
│  │    - Used for generating random tokens, IVs, keys   │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ 2. IKeyManagementService                             │   │
│  │    - createKey(tenant, request)                      │   │
│  │    - getKeyMetadata(tenant, keyId)                   │   │
│  │    - listKeys(tenant, limit, nextToken)             │   │
│  │    - enableKey(tenant, keyId)                        │   │
│  │    - disableKey(tenant, keyId)                       │   │
│  │    - scheduleKeyDeletion(tenant, keyId, days)       │   │
│  │    - cancelKeyDeletion(tenant, keyId)               │   │
│  │    - deleteKey(tenant, keyId)                        │   │
│  │    - rotateKey(tenant, keyId)                        │   │
│  │    - updateKeyMetadata(tenant, keyId, request)      │   │
│  │    - getPublicKey(tenant, keyId)                     │   │
│  │    - createAlias / updateAlias / deleteAlias         │   │
│  │    - listAliases / listAliasesForKey                 │   │
│  │    - getKeyUsageStats(tenant, keyId)                │   │
│  │    - validateKey(tenant, keyId)                      │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ 3. IEncryptionService                                │   │
│  │    - encrypt(tenant, request)       [Uses CMK]       │   │
│  │    - decrypt(tenant, request)       [Auto-detect]    │   │
│  │    - reencrypt(tenant, request) [CMK to CMK]         │   │
│  │                                                       │   │
│  │    Implementation: EncryptionServiceImpl              │   │
│  │    - Algorithm Selection: AES/RSA based on keySpec   │   │
│  │    - Error Handling: Custom crypto exceptions         │   │
│  │    - Logging: All operations logged for audit        │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ 4. ISigningService                                   │   │
│  │    - sign(tenant, request)          [RSA/ECDSA]      │   │
│  │    - verify(tenant, request)        [RSA/ECDSA]      │   │
│  │    - generateMac(tenant, request)   [HMAC]           │   │
│  │    - verifyMac(tenant, request)     [HMAC]           │   │
│  │                                                       │   │
│  │    Implementation: SigningServiceImpl                 │   │
│  │    - RSA-PSS with SHA-256/384/512                    │   │
│  │    - ECDSA with NIST curves                          │   │
│  │    - HMAC-SHA for message authentication             │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ 5. IDataKeyService                                   │   │
│  │    - generateDataKey(tenant, request)                │   │
│  │    - generateDataKeyWithoutPlaintext(...)            │   │
│  │    - generateDataKeyPair(tenant, request)            │   │
│  │    - generateDataKeyPairWithoutPlaintext(...)        │   │
│  │                                                       │   │
│  │    Implementation: DataKeyServiceImpl                 │   │
│  │    - Envelope Encryption: Generate unique data keys  │   │
│  │    - Key Wrapping: Encrypt data key under CMK        │   │
│  │    - Asymmetric Support: RSA/ECC key pairs           │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ 6. IKeyPolicyService                                 │   │
│  │    - setKeyPolicy(tenant, keyId, request)            │   │
│  │    - getKeyPolicy(tenant, keyId)                     │   │
│  │    - createGrant(tenant, keyId, request)             │   │
│  │    - listGrants(tenant, keyId, ...)                  │   │
│  │    - revokeGrant(tenant, keyId, grantId)             │   │
│  │    - retireGrant(tenant, grantId, request)           │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ 7. IKeyVersionService                                │   │
│  │    - listKeyVersions(tenant, keyId)                  │   │
│  │    - getActiveVersion(tenant, keyId)                 │   │
│  │    - updateKeyRotation(tenant, keyId, request)       │   │
│  │    - getKeyRotationStatus(tenant, keyId)             │   │
│  │    - listKeyRotations(tenant, keyId, ...)            │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ 8. IAuditService                                     │   │
│  │    - logAction(tenant, action, keyId, user, ip)      │   │
│  │    - getAuditLogs(tenant, keyId, from, to, limit)    │   │
│  │                                                       │   │
│  │    Implementation: AuditServiceImpl                   │   │
│  │    - Persists to KmsAuditLog entity                  │   │
│  │    - Tracks all operations                           │   │
│  │    - Retention: 90 days (configurable)               │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ 9. IMultiRegionService                               │   │
│  │    - updatePrimaryRegion(tenant, keyId, request)     │   │
│  │    - replicateKey(tenant, keyId, request)            │   │
│  │    - synchronizeMultiRegionKey(tenant, keyId)        │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer (5 Repositories)        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ KmsKeyRepository                                     │   │
│  │ - findByTenantAndKeyId(tenant, keyId)                │   │
│  │ - findByTenantAndKeyAlias(tenant, alias)             │   │
│  │ - findByTenantAndKeyArn(tenant, arn)                 │   │
│  │ - findByTenant(tenant, pageable)                     │   │
│  │ - findByTenantAndStatus(tenant, status, pageable)    │   │
│  │ - findActiveKeysByTenant(tenant)                     │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ KmsKeyVersionRepository                              │   │
│  │ - findByTenantAndKeyIdAndVersionId(...)              │   │
│  │ - findVersionsByKeyId(tenant, keyId)                 │   │
│  │ - findActiveVersionByKeyId(tenant, keyId)            │   │
│  │ - findByTenantAndKeyId(tenant, keyId, pageable)      │   │
│  │ - countByKeyId(keyId)                                │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ KmsKeyGrantRepository                                │   │
│  │ - Standard CRUD + tenant-filtered queries             │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ KmsKeyPolicyRepository                               │   │
│  │ - Standard CRUD + tenant-filtered queries             │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ KmsAuditLogRepository                                │   │
│  │ - findByTenantAndKeyId(tenant, keyId, pageable)      │   │
│  │ - findByTenantAndAction(tenant, action, pageable)    │   │
│  │ - findByDateRange(tenant, from, to, pageable)        │   │
│  │ - findByMultipleCriteria(tenant, keyId, action, ...) │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Database Layer (5 JPA Entities)                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ KmsKey (T_KMS_KEY)                                   │   │
│  │ - Master key metadata                                │   │
│  │ - Columns: keyId, keyArn, keySpec, keyPurpose,      │   │
│  │           status, currentVersionId, rotation config  │   │
│  │ - Indexes: (TENANT, KEY_ID), (TENANT, STATUS)       │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ KmsKeyVersion (T_KMS_KEY_VERSION)                    │   │
│  │ - Key version history for rotation                   │   │
│  │ - Columns: versionId, keyId (FK), keyMaterial,      │   │
│  │           status, creationDate, activationDate       │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ KmsKeyGrant (T_KMS_KEY_GRANT)                        │   │
│  │ - Fine-grained access control                        │   │
│  │ - Columns: grantId, keyId (FK), principal,           │   │
│  │           operations, constraints, status            │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ KmsKeyPolicy (T_KMS_KEY_POLICY)                      │   │
│  │ - IAM-style key access policies                      │   │
│  │ - Columns: policyDocument (JSON), policyVersion      │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ KmsAuditLog (T_KMS_AUDIT_LOG)                        │   │
│  │ - Compliance audit trail                             │   │
│  │ - Columns: keyId, action, principal, ipAddress,      │   │
│  │           timestamp, status, errorMessage, execTime  │   │
│  │ - Indexes: (KEY_ID), (TENANT, ACTION), (TIMESTAMP)   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Service Implementation Details

### 1. IKeyService Implementation

**File:** `KeyService.java`

**Methods:**

```java
public String generateRandomData(String tenant, Integer length, IEnumCharSet.Types charSetType)
```

**Implementation Details:**

- Uses SecureRandom for FIPS-compliant random generation
- Supports multiple charsets: ALPHANUMERIC, NUMERIC, HEX, BASE64
- Length validation: 1-1024 bytes
- Logs operation for audit trail
- Tenant isolation enforced

**Error Handling:**

- Throws `IllegalArgumentException` if length out of range
- Throws `CharacterEncodingException` if charset unsupported

---

### 2. IKeyManagementService Implementation

**File:** `KeyManagementServiceImpl.java`

**Key Methods:**

#### a) createKey()

```java
public CreateKeyResponseDto createKey(String tenant, CreateKeyRequestDto request)
```

**Flow:**

1. Validate keySpec against supported algorithms
2. Validate keyPurpose (ENCRYPT_DECRYPT or SIGN_VERIFY)
3. Generate new key material or prepare for import
4. Create ARN in AWS format: `arn:wams:kms:region:account:key/uuid`
5. Persist to KmsKey entity
6. Create initial version in KmsKeyVersion
7. Return CreateKeyResponseDto with metadata

**Database Operations:**

- INSERT into T_KMS_KEY
- INSERT into T_KMS_KEY_VERSION (initial active version)
- INSERT into T_KMS_AUDIT_LOG

**Supported Key Specs:**

- SYMMETRIC_DEFAULT (AES-256)
- RSA_2048, RSA_3072, RSA_4096
- ECC_NIST_P256, ECC_NIST_P384, ECC_NIST_P521

#### b) enableKey() / disableKey()

**Operations:**

- Update T_KMS_KEY.STATUS to ENABLED/DISABLED
- Validate current state (prevent double operations)
- Return updated metadata

#### c) scheduleKeyDeletion()

**Flow:**

1. Validate key exists and is not already pending
2. Set PENDING_DELETION status
3. Calculate deletion_date = now + pendingWindowInDays (7-30 days)
4. Update T_KMS_KEY with deletion fields
5. Key becomes unusable immediately but can be recovered during grace period

#### d) rotateKey() / updateKeyRotation()

**Manual Rotation (rotateKey):**

1. Generate new key material
2. Create new KmsKeyVersion entry with ACTIVE status
3. Mark old version as INACTIVE
4. Update currentVersionId in KmsKey
5. Return new version ID

**Automatic Rotation (updateKeyRotation):**

1. Enable/disable rotationEnabled flag
2. Set rotationPeriodDays (default: 365)
3. Calculate nextRotationDate
4. Background job handles automatic rotation

---

### 3. IEncryptionService Implementation

**File:** `EncryptionServiceImpl.java`

#### encrypt() Method

```java
public EncryptResponseDto encrypt(String tenant, EncryptRequestDto request)
```

**Flow:**

1. Lookup key by keyId (supports UUID or alias)
2. Validate key status (must be ENABLED)
3. Validate key purpose (must be ENCRYPT_DECRYPT)
4. Retrieve active key version from KmsKeyVersion
5. Select encryption algorithm based on keySpec:
    - SYMMETRIC_DEFAULT → AES-256-GCM
    - RSA_* → RSAES-OAEP with SHA-256
    - EC_* → Elliptic Curve (if applicable)
6. Generate/retrieve IV for AES
7. Encrypt plaintext using selected algorithm
8. Include encryption algorithm metadata in ciphertext
9. Log operation for audit
10. Return ciphertext + key metadata

**Encryption Context (AAD - Authenticated Additional Data):**

- Optional JSON object passed by caller
- Used during decryption for validation
- Prevents accidental use of ciphertext in different context

**Error Cases:**

- `KeyNotFoundException` - key doesn't exist
- `KeyDisabledException` - key is disabled
- `InvalidKeyPurposeException` - key cannot be used for encryption
- `EncryptionFailedException` - crypto operation failed
- `PlaintextTooLargeException` - > 4KB for direct encryption

#### decrypt() Method

```java
public DecryptResponseDto decrypt(String tenant, DecryptRequestDto request)
```

**Flow:**

1. Extract key ID from ciphertext header
2. Retrieve KmsKey and active version
3. Validate key status and purpose
4. Extract encryption algorithm from ciphertext
5. Decrypt using appropriate algorithm:
    - For AES: Extract IV and decrypt
    - For RSA: Decrypt using private key
6. Validate encryption context matches (if provided)
7. Extract plaintext
8. Log for audit
9. Return plaintext + key metadata

#### reencrypt() Method

```java
public ReEncryptResponseDto reencrypt(String tenant, ReEncryptRequestDto request)
```

**Advanced Feature:**

- Decrypt under source key + re-encrypt under destination key
- All done server-side (plaintext never exposed)
- Supports different encryption contexts for source/destination
- Use case: Key rotation, multi-tenant data migration

**Flow:**

1. Decrypt ciphertext under source key (if not auto-detected)
2. Validate source encryption context
3. Retrieve destination key and version
4. Re-encrypt plaintext under destination key
5. Validate destination encryption context if different
6. Return new ciphertext
7. Log for audit (source and destination key IDs)

---

### 4. ISigningService Implementation

**File:** `SigningServiceImpl.java`

#### sign() Method

```java
public SignResponseDto sign(String tenant, SignRequestDto request)
```

**Supported Algorithms:**

- RSASSA_PSS_SHA_256, RSASSA_PSS_SHA_384, RSASSA_PSS_SHA_512
- RSASSA_PKCS1_V1_5_SHA_256, RSASSA_PKCS1_V1_5_SHA_384, RSASSA_PKCS1_V1_5_SHA_512
- ECDSA_SHA_256, ECDSA_SHA_384, ECDSA_SHA_512

**Flow:**

1. Lookup asymmetric key by keyId
2. Validate key purpose (must be SIGN_VERIFY)
3. Retrieve active key version
4. Select signing algorithm
5. Sign message hash using private key
6. Return signature + algorithm + key version ID
7. Log for audit

**Message Processing:**

- Accepts raw message or digest
- If message: compute SHA hash (size depends on algorithm)
- Validates message size ≤ 4KB (or hash specified)

#### verify() Method

```java
public VerifyResponseDto verify(String tenant, VerifyRequestDto request)
```

**Flow:**

1. Lookup asymmetric key
2. Retrieve public key from KMS or generate from private key
3. Verify signature using public key
4. Return boolean result + key metadata
5. Log for audit

**Security Notes:**

- Does NOT validate key ID - allows external signature verification
- Can verify signatures from external sources
- Returns verified boolean without throwing exception

#### generateMac() / verifyMac() Methods

```java
public GenerateMacResponseDto generateMac(String tenant, GenerateMacRequestDto request)

public VerifyMacResponseDto verifyMac(String tenant, VerifyMacRequestDto request)
```

**Supported Algorithms:**

- HMAC_SHA_224, HMAC_SHA_256, HMAC_SHA_384, HMAC_SHA_512

**Flow (generateMac):**

1. Lookup symmetric key
2. Validate key can be used for MAC (usually dedicated key)
3. Compute HMAC over message using key material
4. Return MAC value + algorithm
5. Log for audit

**Flow (verifyMac):**

1. Generate MAC over message using same algorithm
2. Compare with provided MAC using constant-time comparison
3. Return boolean (valid/invalid)
4. Log for audit

---

### 5. IDataKeyService Implementation

**File:** `DataKeyServiceImpl.java`

#### generateDataKey() Method

```java
public DataKeyResponseDto generateDataKey(String tenant, GenerateDataKeyRequestDto request)
```

**Envelope Encryption Pattern:**

1. Generate random data key (size based on keySpec parameter)
2. Encrypt data key under CMK (customer master key)
3. Return both:
    - Plaintext data key (for immediate use to encrypt data)
    - Encrypted data key (for secure storage alongside encrypted data)

**Supported KeySpecs:**

- AES_128, AES_256
- Various HMAC key sizes

**Flow:**

1. Lookup CMK by keyId
2. Generate cryptographically secure random data of requested size
3. Call encryptionService.encrypt() to wrap data key
4. Return plaintext + encrypted versions
5. Log for audit

**Use Case:**

```
1. Client calls generateDataKey for 256GB database
2. Receives plaintext 256-bit AES key + encrypted wrapper
3. Uses plaintext key to encrypt database locally
4. Stores encrypted wrapper alongside encrypted data
5. Discards plaintext AES key (only stored in RAM during encryption)
6. To decrypt: decrypt wrapper to get AES key, then decrypt data
```

#### generateDataKeyWithoutPlaintext() Method

- Same as generateDataKey but only returns encrypted wrapper
- Plaintext key is generated but discarded server-side
- More secure for high-security scenarios
- Use when plaintext should never leave KMS

#### generateDataKeyPair() / generateDataKeyPairWithoutPlaintext()

**Asymmetric Envelope Encryption:**

1. Generate RSA or ECC key pair
2. Wrap private key under CMK
3. Return public key + plaintext private key (or only encrypted version)

**Algorithm Support:**

- RSA_2048, RSA_3072, RSA_4096
- ECC_NIST_P256, ECC_NIST_P384

**Use Case:**

- Session-based TLS/SSL keys
- Certificate generation
- One-time signing keys
- Ephemeral key pairs for specific operations

---

### 6. IKeyPolicyService Implementation

**File:** `KeyPolicyServiceImpl.java`

#### setKeyPolicy() / getKeyPolicy()

**Policy Format:**
IAM-like JSON document:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Enable Use of Key",
      "Effect": "Allow",
      "Principal": {
        "Service": "s3.amazonaws.com"
      },
      "Action": "kms:*",
      "Resource": "*"
    }
  ]
}
```

**Implementation:**

- Validate policy JSON format
- Persist to KmsKeyPolicy entity
- Update T_KMS_KEY_POLICY table
- Log for audit

#### createGrant() / listGrants() / revokeGrant()

**Grants:** Temporary permission without modifying key policy

**Create Grant Flow:**

1. Create KmsKeyGrant record
2. Store principal (who gets access)
3. Store allowed operations (encrypt, decrypt, sign, generate_data_key, etc.)
4. Store optional constraints (encryption context conditions)
5. Generate grant token (short-lived, used in crypto operations)
6. Return grant ID + token

**Grant Constraints:**

- Encryption context conditions
- Grant tokens (time-limited validations)
- Prevents unauthorized reuse

#### retireGrant()

- Allow grantee to self-remove permissions
- Different from revokeGrant (which requires admin)

---

### 7. IKeyVersionService Implementation

**File:** `KeyVersionServiceImpl.java`

**Key Version Tracking:**

- Each key has multiple versions (from rotations)
- One version marked as ACTIVE (used for new operations)
- Old versions retained for decrypting existing data

**Methods:**

- listKeyVersions() - all versions with metadata
- getActiveVersion() - current active version
- Update rotation config - enable/disable automatic rotation

---

### 8. IAuditService Implementation

**File:** `AuditServiceImpl.java`

**Audit Logging:**
Every operation logged to T_KMS_AUDIT_LOG with:

- Tenant ID
- Action type (CREATE_KEY, ENCRYPT, DECRYPT, SIGN, etc.)
- Key ID (if applicable)
- Principal (user/service performing action)
- Client IP address (extracted from HTTP context)
- Timestamp (UTC)
- Status (SUCCESS or FAILURE)
- Error message (if failed)
- Execution time in milliseconds

**Retention:**

- Default: 90 days
- Configurable per deployment
- Compliance requirement for HIPAA, PCI-DSS, SOC 2

**Security Considerations:**

- Never logs plaintext data or key material
- Only logs ciphertext size, not contents
- Use for security incident investigation

---

### 9. Custom Key Store & Multi-Region Services

*Implementations TBD based on deployment scenario*

---

## Error Handling Strategy

### Custom Exception Hierarchy

```
KmsServiceException (root)
├── KeyNotFoundException
├── KeyDisabledException
├── InvalidKeyStateException
├── InvalidKeyPurposeException
├── EncryptionException
├── DecryptionException
├── SigningException
├── VerificationException
├── KeyPolicyException
├── GrantException
├── AliasException
└── AuditException
```

### HTTP Status Mapping

| Scenario                | HTTP Code | Exception                  |
|-------------------------|-----------|----------------------------|
| Key not found           | 404       | KeyNotFoundException       |
| Key disabled            | 409       | KeyDisabledException       |
| Prohibited key purpose  | 400       | InvalidKeyPurposeException |
| Crypto operation failed | 500       | EncryptionException        |
| Access denied           | 403       | ForbiddenException         |
| Validation failed       | 400       | ValidationException        |

### KmsExceptionHandler

Maps exceptions to HTTP responses with:

- Appropriate status code
- Error message
- Request ID (for tracing)
- Timestamp

---

## Transaction Management

All service methods use:

```java
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
```

Ensures:

- Database consistency (all-or-nothing operations)
- Automatic rollback on exception
- Audit log created even if main operation fails

---

## Performance Considerations

### Caching Strategy

**Recommended caching layers:**

1. Key metadata cache (TTL: 1 hour)
    - Avoids repeated database lookups
    - Invalidated on key status change

2. Public key cache (TTL: 24 hours)
    - Public keys are immutable
    - Can be safely cached

3. Policy cache (TTL: 1 hour)
    - Accessed for authorization checks
    - Invalidated on policy update

### Query Optimization

- All queries use indexes on TENANT, STATUS, KEY_ID
- Pagination for list operations (default: 100 items/page)
- Audit log queries use date range indexes

### Batch Operations

- Future: Support batch encrypt/decrypt for performance
- Bulk key rotation scripts
- Scheduled maintenance operations

---

## Security Best Practices

### Implemented

✅ Multi-tenant isolation at all layers
✅ Audit logging for all operations
✅ Encryption context validation
✅ Key purpose enforcement
✅ Principal tracking in grants
✅ Time-limited grant tokens

### Recommended

- [ ] Rate limiting per principal
- [ ] Rotation of encryption keys for stored key material
- [ ] HSM backing for key material storage
- [ ] Threat detection for unusual access patterns
- [ ] Certificate pinning for external key stores

---

## Testing Strategy

### Unit Tests

- Mock repositories and crypto libraries
- Test business logic paths
- Validate error scenarios

### Integration Tests

- Use H2 in-memory database
- Test full service → repository → database flow
- Verify audit logging

### Performance Tests

- Benchmark crypto operations
- Measure database query latency
- Validate caching effectiveness

### Security Tests

- Multi-tenant isolation validation
- Authorization bypass attempts
- Encryption validation
- Key material protection verification


