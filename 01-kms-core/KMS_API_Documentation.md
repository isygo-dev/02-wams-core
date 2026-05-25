# KMS Service API Documentation

A comprehensive REST API for managing cryptographic keys, performing encryption/decryption operations, and securing
sensitive data. This service supports symmetric keys (AES-256), asymmetric keys (RSA, ECC), key rotation, multi-region
configurations, grants, and Bring Your Own Key (BYOK) functionality.

---

## ⚠️ Important: Key Identifier Formats

**Throughout all KMS API operations, the `KeyId` parameter can be specified in three different formats:**

1. **Key ID (UUID)**: Direct key identifier
    - Format: `1234abcd-12ab-34cd-56ef-1234567890ab`
    - Example: `"KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab"`

2. **Alias**: Human-readable alias reference
    - Format: `alias:alias-name`
    - Example: `"KeyId": "alias:production-db-key"`
    - Benefits: Self-documenting, supports key rotation without code changes

3. **WRN (WAMS Resource Name)**: Fully-qualified resource identifier
    - Format: `wrn:wams:kms:region:account-id:key:key-id`
    - Example: `"KeyId": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab"`
    - Use Case: Cross-region references, audit logs, infrastructure-as-code

---

## Table of Contents

1. [Key Management](#key-management)
2. [Cryptographic Operations](#cryptographic-operations)
3. [Key Aliases](#key-aliases)
4. [Key Tagging](#key-tagging)
5. [Key Policies](#key-policies)
6. [Grants](#grants)
7. [BYOK (Import Key Material)](#byok-import-key-material)
8. [Custom Key Stores](#custom-key-stores)
9. [Annex: Abbreviations & Technical Terminology](#annex-abbreviations--technical-terminology)

---

## Key Management

### 1. Create Key

**Description**  
Creates a new customer-managed key (CMK) in the KMS service. Supports both symmetric (AES-256) and asymmetric (RSA, ECC)
keys. The key can be configured with a description, usage policy, key spec, origin (WAMS-managed or EXTERNAL for BYOK),
tags, and multi-region capabilities.

**HTTP Method & Endpoint**

```
POST /keys
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST http://localhost:8080/keys \
  -H "Content-Type: application/json" \
  -d '{
    "Description": "Production Database Encryption Key",
    "KeyUsage": "ENCRYPT_DECRYPT",
    "KeySpec": "SYMMETRIC_DEFAULT",
    "Origin": "WAMS_KMS",
    "MultiRegion": false,
    "Tags": {
      "Environment": "production",
      "Application": "database"
    }
  }'
```

**Response Example**

```json
{
  "KeyMetadata": {
    "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
    "Wrn": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab",
    "CreationDate": "2024-05-10T10:30:00Z",
    "Enabled": true,
    "Description": "Production Database Encryption Key",
    "KeySpec": "SYMMETRIC_DEFAULT",
    "KeyUsage": "ENCRYPT_DECRYPT",
    "KeyState": "Enabled",
    "Origin": "WAMS_KMS"
  }
}
```

**Real Use Case**  
A financial services company needs to encrypt customer credit card data stored in their RDS database. They create a CMK
to ensure PCI-DSS compliance. The key is tagged with environment and application metadata for audit trails, and only
applications with explicit IAM permissions can use the key for encryption.

**Internal Behavior**

- Validates the request parameters (KeySpec, KeyUsage, Origin combinations)
- Generates cryptographically secure key material using FIPS 140-2 validated HSM
- Stores key metadata in the KMS database with status ENABLED
- Creates initial key version (version ID = key ID for symmetric keys)
- If MultiRegion: true, marks key as primary and prepares for replication
- If Origin: EXTERNAL, creates key in PENDING_IMPORT state awaiting BYOK material
- Returns key metadata including auto-generated KeyId and WRN
- Logs CreateKey event to CloudTrail for audit

---

### 2. Describe Key

**Description**  
Retrieves detailed metadata about a KMS key, including its WRN, state (Enabled/Disabled/PendingDeletion), creation date,
rotation status, key material origin, and multi-region configuration.

**Note:** The `keyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
GET /keys/{keyId}
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab" \
  -H "Accept: application/json"

# Or using an alias:
curl -X GET "http://localhost:8080/keys/alias:production-db-key" \
  -H "Accept: application/json"

# Or using a WRN:
curl -X GET "http://localhost:8080/keys/wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "KeyMetadata": {
    "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
    "Wrn": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab",
    "CreationDate": "2024-05-10T10:30:00Z",
    "Enabled": true,
    "RotationEnabled": true,
    "RotationPeriodInDays": 365,
    "LastRotationDate": "2024-05-10T10:30:00Z",
    "KeySpec": "SYMMETRIC_DEFAULT",
    "KeyUsage": "ENCRYPT_DECRYPT",
    "KeyState": "Enabled",
    "Origin": "WAMS_KMS",
    "MultiRegion": false,
    "CurrentVersion": "1234abcd-12ab-34cd-56ef-1234567890ab"
  }
}
```

**Real Use Case**  
A DevOps team audits all KMS keys quarterly to verify rotation status. They use this API to retrieve metadata for every
key and identify keys that haven't been rotated in over 90 days, triggering a manual review and rotation process for
compliance.

**Internal Behavior**

- Resolves keyId using the resolver (supports KeyId, WRN, or alias)
- Checks IAM permissions for kms:DescribeKey action
- Retrieves key metadata from the KMS database
- If key is a multi-region replica, includes replication metadata
- Returns cached metadata (no cryptographic operations performed)
- Does NOT expose key material or plaintext in response

---

### 3. List Keys

**Description**  
Returns a paginated list of all KMS keys in the account. Each entry includes the key ID and WRN, supporting pagination
with limit and nextToken parameters.

**HTTP Method & Endpoint**

```
GET /keys?limit={limit}&nextToken={nextToken}
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/keys?limit=50&nextToken=abc123" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "Keys": [
    {
      "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
      "Wrn": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab"
    },
    {
      "KeyId": "5678efgh-56ef-78gh-90ij-0987654321cd",
      "Wrn": "wrn:wams:kms:us-east-1:123456789012:key:5678efgh-56ef-78gh-90ij-0987654321cd"
    }
  ],
  "NextToken": "xyz789",
  "Truncated": true
}
```

**Real Use Case**  
An organization's security team uses this API to build an automated inventory of all encryption keys across the account.
They export the list to their CMDB (Configuration Management Database) daily and generate reports on key ownership,
rotation status, and usage patterns.

**Internal Behavior**

- Enforces kms:ListKeys permission
- Queries KMS database for keys belonging to the account
- Applies pagination using cursor-based nextToken (not offset/limit)
- Returns minimal metadata (KeyId + WRN) for performance
- Does NOT return key state or other metadata (use DescribeKey for details)
- Results are ordered by creation date (newest first)

---

### 4. Schedule Key Deletion

**Description**  
Schedules a KMS key for deletion with a configurable waiting period (7–30 days). During the waiting period, the key
cannot be used and its state becomes 'PendingDeletion'. This provides a safety mechanism to prevent accidental key
deletion.

**Note:** The `keyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
DELETE /keys/{keyId}/schedule-deletion?pendingWindowInDays={days}
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/schedule-deletion?pendingWindowInDays=30" \
  -H "Accept: application/json"

# Or using an alias:
curl -X DELETE "http://localhost:8080/keys/alias:production-db-key/schedule-deletion?pendingWindowInDays=30" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
  "KeyState": "PendingDeletion",
  "DeletionDate": "2024-06-09T10:30:00Z",
  "PendingWindowInDays": 30
}
```

**Real Use Case**  
When decommissioning a legacy application, the DevOps team schedules its encryption key for deletion with a 30-day
waiting period. This allows time to verify that no running processes still depend on the key. After 30 days with no
issues, they proceed with permanent deletion.

**Internal Behavior**

- Validates pendingWindowInDays (7-30 days, default 30)
- Requires kms:ScheduleKeyDeletion permission
- Changes key state to PENDING_DELETION
- Marks key as disabled for all cryptographic operations immediately
- Stores DeletionDate = current time + window in database
- Schedules background job to permanently delete key after DeletionDate
- Key cannot be used during waiting period, even with grants
- Can be cancelled using Cancel Key Deletion API

---

### 5. Cancel Key Deletion

**Description**  
Cancels a previously scheduled key deletion, restoring the key to its previous state (Enabled or Disabled). This is
useful if the scheduled deletion was initiated by mistake or if new dependencies on the key are discovered.

**Note:** The `keyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/cancel-deletion
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/cancel-deletion" \
  -H "Content-Type: application/json"

# Or using an alias:
curl -X POST "http://localhost:8080/keys/alias:production-db-key/cancel-deletion" \
  -H "Content-Type: application/json"
```

**Response Example**

```json
{
  "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
  "KeyState": "Enabled"
}
```

**Real Use Case**  
A database team accidentally schedules deletion of a production database encryption key. Before the waiting period
expires, they discover the mistake and call this API to cancel the deletion and restore the key, preventing a
catastrophic outage.

**Internal Behavior**

- Requires kms:CancelKeyDeletion permission
- Verifies key is in PENDING_DELETION state
- Removes the scheduled deletion record
- Restores key to previous state (ENABLED or DISABLED)
- Cancels any pending background deletion job
- Key becomes usable again immediately after cancellation
- Logs cancellation event to CloudTrail

---

### 6. Delete Key (Permanent)

**Description**  
Permanently deletes a KMS key. This operation is irreversible and should only be used after the key has been scheduled
for deletion and the waiting period has expired. Once deleted, any data encrypted with this key cannot be decrypted.

**Note:** The `keyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
DELETE /keys/{keyId}
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab" \
  -H "Accept: application/json"

# Or using an alias:
curl -X DELETE "http://localhost:8080/keys/alias:legacy-key" \
  -H "Accept: application/json"
```

**⚠️ Warning: This is a destructive operation that cannot be undone.**

**Response Example**

```json
{
  "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
  "KeyState": "Deleted"
}
```

**Real Use Case**  
After the 30-day waiting period expires and all systems confirm the key is no longer needed, an automated compliance
tool calls this API to permanently delete the key, freeing up key quotas and ensuring no accidental reuse.

**Internal Behavior**

- Requires kms:DeleteKey permission (additional to schedule-deletion)
- Verifies key is in PENDING_DELETION state AND deletion date has passed
- Permanently removes key metadata from KMS database
- Securely zeroizes key material from HSM/backing storage
- Removes all aliases, grants, and policy associations
- Key cannot be recovered after this operation
- Any data encrypted with this key becomes permanently inaccessible

---

### 7. Update Key Rotation

**Description**  
Enables or disables automatic key rotation for a CMK. When enabled, WAMS KMS automatically rotates the key material
annually. Key rotation strengthens security by limiting the amount of data encrypted with a single key material version.

**Note:** The `keyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
PATCH /keys/{keyId}/rotation
Content-Type: application/json
```

**Usage Call**

```bash
curl -X PATCH "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/rotation" \
  -H "Content-Type: application/json" \
  -d '{
    "EnableRotation": true,
    "RotationPeriodInDays": 365,
    "Reason": "Compliance requirement for PCI-DSS"
  }'

# Or using an alias:
curl -X PATCH "http://localhost:8080/keys/alias:production-db-key/rotation" \
  -H "Content-Type: application/json" \
  -d '{
    "EnableRotation": true,
    "RotationPeriodInDays": 365
  }'
```

**Response Example**

```json
{
  "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
  "RotationEnabled": true,
  "RotationPeriodInDays": 365,
  "NextRotationDate": "2025-05-10T10:30:00Z"
}
```

**Real Use Case**  
A healthcare organization adopts a policy requiring all encryption keys to rotate annually for HIPAA compliance. They
use this API to enable rotation on all production encryption keys, and KMS automatically rotates each key every 365 days
without manual intervention.

**Internal Behavior**

- Validates rotation is only supported for symmetric keys
- Checks kms:UpdateKeyRotation permission
- If enabling, schedules a cron job for next rotation date
- If disabling, cancels any scheduled rotation tasks
- Updates RotationEnabled flag and RotationPeriodInDays in metadata
- Rotation does NOT affect existing encrypted data (old versions remain decryptable)
- New rotation creates a new key version; old versions retained for decryption

---

## Cryptographic Operations

### 8. Encrypt

**Description**  
Encrypts plaintext data using a KMS key. The plaintext must be up to 4 KB in size. Returns an encrypted ciphertext blob
that can only be decrypted using the same KMS key or a replica in a multi-region setup.

**Note:** The `KeyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
POST /encrypt
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/encrypt" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
    "Plaintext": "Y3JlZGl0Q2FyZE51bWJlcjogNDExMS0xMTExLTExMTEtMTExMQ==",
    "EncryptionAlgorithmSpec": "SYMMETRIC_DEFAULT",
    "EncryptionContext": {
      "Department": "Finance",
      "CustomerId": "12345"
    }
  }'

# Or using an alias:
curl -X POST "http://localhost:8080/encrypt" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "alias:production-db-key",
    "Plaintext": "Y3JlZGl0Q2FyZE51bWJlcjogNDExMS0xMTExLTExMTEtMTExMQ==",
    "EncryptionContext": {
      "Department": "Finance"
    }
  }'
```

**Response Example**

```json
{
  "CiphertextBlob": "AQIDAHh...7u6k8r9==",
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab",
  "KeyVersionId": "1234abcd-12ab-34cd-56ef-1234567890ab",
  "EncryptionAlgorithmSpec": "SYMMETRIC_DEFAULT"
}
```

**Real Use Case**  
An e-commerce platform encrypts customer payment information before storing it in the database. Each credit card number
is encrypted with the same key, and the ciphertext is stored in the payment records table. Only authorized services with
KMS decrypt permissions can access the original card data.

**Internal Behavior**

- Resolves KeyId to key metadata and current active key version using the resolver
- Validates key is ENABLED and supports ENCRYPT_DECRYPT usage
- Checks IAM permissions or grants for kms:Encrypt action
- For symmetric keys: generates IV/nonce, encrypts using AES-GCM or AES-CBC
- For asymmetric keys: encrypts using RSA-OAEP
- Embeds key version ID and encryption context in ciphertext (authenticated)
- Returns base64-encoded ciphertext blob
- Logs Encrypt event with encryption context (values redacted)
- Plaintext never persisted to disk; held in memory only during operation

---

### 9. Decrypt

**Description**  
Decrypts a ciphertext blob encrypted by the Encrypt operation. Returns the plaintext. The user must have permission to
use the key (via IAM policy or grant) to decrypt data.

**HTTP Method & Endpoint**

```
POST /decrypt
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/decrypt" \
  -H "Content-Type: application/json" \
  -d '{
    "CiphertextBlob": "AQIDAHh...7u6k8r9==",
    "EncryptionContext": {
      "Department": "Finance",
      "CustomerId": "12345"
    }
  }'
```

**Response Example**

```json
{
  "Plaintext": "Y3JlZGl0Q2FyZE51bWJlcjogNDExMS0xMTExLTExMTEtMTExMQ==",
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab",
  "KeyVersionId": "1234abcd-12ab-34cd-56ef-1234567890ab",
  "EncryptionAlgorithmSpec": "SYMMETRIC_DEFAULT"
}
```

**Real Use Case**  
When a customer requests a refund, the payment processing system retrieves the encrypted credit card number from the
database and calls this API to decrypt it. The plaintext is temporarily held in memory for the refund operation, then
immediately discarded.

**Internal Behavior**

- Parses ciphertext blob to extract key ID, version ID, and encrypted data
- Resolves key and version from embedded metadata
- Validates key is ENABLED (or was ENABLED at encryption time for rotated keys)
- Checks permissions for kms:Decrypt
- Validates encryption context matches (if provided) - all key-value pairs must match
- Performs authenticated decryption (GCM tag validation)
- Returns base64-encoded plaintext
- Plaintext never persisted; cleared from memory after response
- Logs Decrypt event (no plaintext logged)

---

### 10. Generate Data Key

**Description**  
Generates a unique symmetric data key and returns both a plaintext and encrypted copy. The plaintext key is used for
local encryption, while the encrypted key is stored alongside the encrypted data. This enables envelope encryption
pattern, protecting the data key with the KMS key.

**Note:** The `KeyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
POST /datakey/generate
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/datakey/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
    "KeySpec": "AES_256",
    "EncryptionContext": {
      "FileId": "user-123/document-456"
    }
  }'

# Or using an alias:
curl -X POST "http://localhost:8080/datakey/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "alias:file-encryption-key",
    "KeySpec": "AES_256",
    "EncryptionContext": {
      "FileId": "user-123/document-456"
    }
  }'
```

**Response Example**

```json
{
  "Plaintext": "eFgHiJkLmNoPqRsTuVwXyZ0123456789ABCDEFGHIJKLMN==",
  "CiphertextBlob": "AQIDAHh...7u6k8r9==",
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab",
  "EncryptionAlgorithmSpec": "SYMMETRIC_DEFAULT"
}
```

**Real Use Case**  
A cloud storage service stores millions of user files. Instead of encrypting each file directly with the master KMS
key (which would be slow and costly), they use this API to generate a unique data key for each file. The file is
encrypted locally with the plaintext key, then the encrypted data key is stored with the file. When decryption is
needed, they decrypt the data key first, then decrypt the file.

**Internal Behavior**

- Generates cryptographically secure random bytes for data key (AES_128, AES_256, etc.)
- Encrypts the data key using the specified KMS key (same as Encrypt operation)
- Returns both plaintext (unencrypted) and ciphertext (encrypted) versions
- KMS never stores or logs the plaintext data key
- Plaintext key should be used locally and zeroized after use
- Ciphertext key can be stored alongside encrypted data
- Supports encryption context for additional authenticated data

---

### 11. Generate Data Key Pair

**Description**  
Generates an asymmetric data key pair (RSA or ECC) for envelope encryption with asymmetric keys. Returns the public
key (plaintext) and the encrypted private key. The private key never leaves KMS in plaintext form.

**Note:** The `KeyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
POST /datakey/generate-pair
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/datakey/generate-pair" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
    "KeyPairSpec": "RSA_2048",
    "EncryptionContext": {
      "SessionId": "session-abc-123"
    }
  }'

# Or using a WRN:
curl -X POST "http://localhost:8080/datakey/generate-pair" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab",
    "KeyPairSpec": "RSA_2048"
  }'
```

**Response Example**

```json
{
  "PublicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
  "PrivateKeyCiphertextBlob": "AQIDAHh...7u6k8r9==",
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab",
  "KeyPairSpec": "RSA_2048",
  "EncryptionAlgorithmSpec": "RSAES_OAEP_SHA_256"
}
```

**Real Use Case**  
A messaging application needs to establish end-to-end encrypted sessions. For each user session, the server generates an
ephemeral RSA key pair. The public key is sent to the client, while the encrypted private key is stored in the session
database. Only the KMS can decrypt the private key when needed for message decryption.

**Internal Behavior**

- Generates cryptographic key pair using secure RNG
- Public key returned in plaintext (safe for transmission)
- Private key encrypted immediately using the KMS key
- Private key plaintext never returned to caller or persisted
- Only the encrypted private key (ciphertext blob) is returned
- Decrypt the ciphertext blob using Decrypt API to obtain private key when needed
- Supports both RSA and elliptic curve (ECC) key types

---

### 12. Sign

**Description**  
Creates a digital signature for data using an asymmetric KMS key (RSA or ECC). The signature proves that specific data
was signed by the key owner and provides non-repudiation.

**Note:** The `KeyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
POST /sign
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/sign" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
    "Message": "Y29udHJhY3REb2N1bWVudDEyMw==",
    "MessageType": "RAW",
    "SigningAlgorithm": "RSASSA_PSS_SHA_256"
  }'

# Or using an alias:
curl -X POST "http://localhost:8080/sign" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "alias:contract-signing-key",
    "Message": "Y29udHJhY3REb2N1bWVudDEyMw==",
    "MessageType": "RAW",
    "SigningAlgorithm": "RSASSA_PSS_SHA_256"
  }'
```

**Response Example**

```json
{
  "Signature": "j7g3Hk...9qW2sA==",
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab",
  "KeyVersionId": "5678efgh-56ef-78gh-90ij-0987654321cd",
  "SigningAlgorithm": "RSASSA_PSS_SHA_256"
}
```

**Real Use Case**  
A legal tech company uses this API to digitally sign electronic contracts. Each contract is hashed and signed with an
asymmetric key. The signature is stored with the contract. Later, any party can verify the signature using the public
key to prove that the contract was signed by the company and has not been altered.

**Internal Behavior**

- Resolves asymmetric key (must support SIGN_VERIFY usage)
- Checks kms:Sign permission
- If MessageType: DIGEST, uses message as precomputed hash
- If MessageType: RAW, hashes message using algorithm's hash function
- Signs hash using private key component (never exposed)
- Returns base64-encoded signature
- Signature can be verified using Verify operation or public key

---

### 13. Verify

**Description**  
Verifies a digital signature created by the Sign operation. Confirms that the signature is valid and was created using
the specified asymmetric key.

**Note:** The `KeyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
POST /verify
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
    "Message": "Y29udHJhY3REb2N1bWVudDEyMw==",
    "Signature": "j7g3Hk...9qW2sA==",
    "SigningAlgorithm": "RSASSA_PSS_SHA_256"
  }'

# Or using an alias:
curl -X POST "http://localhost:8080/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyId": "alias:contract-signing-key",
    "Message": "Y29udHJhY3REb2N1bWVudDEyMw==",
    "Signature": "j7g3Hk...9qW2sA==",
    "SigningAlgorithm": "RSASSA_PSS_SHA_256"
  }'
```

**Response Example**

```json
{
  "SignatureValid": true,
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key:1234abcd-12ab-34cd-56ef-1234567890ab",
  "SigningAlgorithm": "RSASSA_PSS_SHA_256"
}
```

**Real Use Case**  
A SaaS platform receives digitally signed API requests from partners. Before processing each request, the platform calls
this API to verify the signature. If the signature is invalid, the request is rejected. This ensures that only
authorized partners can access certain API endpoints.

**Internal Behavior**

- Resolves asymmetric key (public key from key metadata)
- Checks kms:Verify permission
- Computes hash of message (or uses as-is for DIGEST type)
- Verifies signature against public key using specified algorithm
- Returns boolean SignatureValid (true/false)
- Does NOT require key to be ENABLED (can verify with disabled keys)
- Uses public key only; never accesses private key

---

## Key Policies

### 14. Get Key Policy

**Description**  
Retrieves the resource-based policy document attached to a KMS key. Policies define who can perform which operations on
the key.

**Note:** The `keyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
GET /keys/{keyId}/policy
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/policy" \
  -H "Accept: application/json"

# Or using an alias:
curl -X GET "http://localhost:8080/keys/alias:production-db-key/policy" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "Policy": {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "Enable IAM policies",
        "Effect": "Allow",
        "Principal": {
          "WAMS": "wrn:wams:iam::123456789012:root"
        },
        "Action": "kms:*",
        "Resource": "*"
      },
      {
        "Sid": "Allow encrypt and decrypt",
        "Effect": "Allow",
        "Principal": {
          "WAMS": "wrn:wams:iam::123456789012:role/ApplicationRole"
        },
        "Action": [
          "kms:Encrypt",
          "kms:Decrypt"
        ],
        "Resource": "*"
      }
    ]
  }
}
```

**Real Use Case**  
A security auditor retrieves the policy of a key to verify that only authorized roles and services have
encryption/decryption permissions. They check that the policy follows the principle of least privilege and doesn't grant
overly broad permissions.

**Internal Behavior**

- Requires kms:GetKeyPolicy permission
- Retrieves policy document stored in JSON format
- Policy is evaluated at request time for all operations
- Default policy name is "default" (other names supported)
- Policy controls both IAM users/roles and cross-account access
- Returns policy as JSON string

---

### 15. Put Key Policy

**Description**  
Replaces the resource-based policy document for a KMS key. This completely replaces the existing policy with the new
one.

**Note:** The `keyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
PUT /keys/{keyId}/policy
Content-Type: application/json
```

**Usage Call**

```bash
curl -X PUT "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/policy" \
  -H "Content-Type: application/json" \
  -d '{
    "PolicyName": "default",
    "Policy": {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Effect": "Allow",
          "Principal": {
            "WAMS": "wrn:wams:iam::123456789012:root"
          },
          "Action": "kms:*",
          "Resource": "*"
        }
      ]
    },
    "BypassPolicyLockoutSafetyCheck": false
  }'

# Or using an alias:
curl -X PUT "http://localhost:8080/keys/alias:production-db-key/policy" \
  -H "Content-Type: application/json" \
  -d '{
    "PolicyName": "default",
    "Policy": {
      "Version": "2012-10-17",
      "Statement": [...]
    }
  }'
```

**Response Example**

```json
{
  "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab"
}
```

**Real Use Case**  
When a new microservice is deployed, the infrastructure team updates the key policy to grant the microservice's IAM role
permission to encrypt and decrypt data. The policy update is reviewed and approved through the CI/CD pipeline before
being applied.

**Internal Behavior**

- Requires kms:PutKeyPolicy permission
- Validates policy syntax and size limits (max 32KB)
- Performs safety check to prevent lockout (unless bypassed)
- Completely replaces existing policy (not a merge)
- Changes take effect immediately for new requests
- Existing sessions may continue with old permissions (cached)
- Logs policy change to CloudTrail

---

## Grants

### 16. Create Grant

**Description**  
Creates a grant, which is a flexible permission mechanism for temporary or delegated access. Grants are useful for
giving applications, services, or cross-account principals limited permissions on a key without modifying the key
policy. Grants can be revoked instantly.

**Note:** The `KeyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/grants
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/grants" \
  -H "Content-Type: application/json" \
  -d '{
    "GranteePrincipal": "wrn:wams:iam::123456789012:role/TemporaryServiceRole",
    "Operations": ["Encrypt", "Decrypt", "GenerateDataKey"],
    "Constraints": {
      "EncryptionContextSubset": {
        "Department": "Finance"
      }
    }
  }'

# Or using an alias:
curl -X POST "http://localhost:8080/keys/alias:production-db-key/grants" \
  -H "Content-Type: application/json" \
  -d '{
    "GranteePrincipal": "wrn:wams:iam::123456789012:role/TemporaryServiceRole",
    "Operations": ["Encrypt", "Decrypt"]
  }'
```

**Response Example**

```json
{
  "GrantToken": "AQpAM2RhZjM1YTY2MTk0NDc3Y2I5NzY1MTI3NTQ5MTQyZT...",
  "GrantId": "0c237476b89f0cbc37edb331c91f26a9f75967e6aec1acfc946e24d47ed6b976"
}
```

**Real Use Case**  
A temporary Lambda function needs to decrypt data from S3 for a one-time batch processing job. Instead of modifying the
key policy, the infrastructure team creates a grant that expires in 2 hours. After the Lambda function completes and
terminates, the grant automatically expires, and the Lambda function can no longer use the key.

**Internal Behavior**

- Creates grant record linked to specified key
- Grant token can be used immediately (eventual consistency)
- Grant constraints limit operations (encryption context, etc.)
- Grantee principal can be IAM user, role, or WAMS account
- Grants are evaluated AFTER key policy (both must allow)
- Grants can be retired by grantee or revoked by key owner
- Operations list cannot include permission management actions

---

### 17. Revoke Grant

**Description**  
Revokes a grant by grant ID and key ID. Similar to retiring a grant but can only be done by the key owner (not by the
grantee).

**Note:** The `keyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
DELETE /keys/{keyId}/grants/{grantId}
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/grants/0c237476b89f0cbc37edb331c91f26a9f75967e6aec1acfc946e24d47ed6b976" \
  -H "Accept: application/json"

# Or using an alias:
curl -X DELETE "http://localhost:8080/keys/alias:production-db-key/grants/0c237476b89f0cbc37edb331c91f26a9f75967e6aec1acfc946e24d47ed6b976" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab"
}
```

**Real Use Case**  
A team's project is cancelled, and the grant created for that project needs to be revoked. The key owner immediately
calls this API to revoke the grant, preventing the team from continuing to use the key.

**Internal Behavior**

- Requires kms:RevokeGrant permission on the key
- Immediately invalidates the grant for all new requests
- Does NOT require grant token (only grant ID)
- Cannot be undone after revocation
- Grantee cannot revoke their own grant (use RetireGrant)
- Changes are visible within seconds (eventual consistency)
- Logs revocation to CloudTrail

---

## BYOK (Import Key Material)

### 18. Get Parameters for Import

**Description**  
Returns a public key and import token needed to import your own key material (BYOK). The public key is used to encrypt
your key material externally, and the token binds the encrypted material to the KMS key. This is a two-step process that
ensures key material never exists in plaintext in WAMS.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/import-parameters
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/import-parameters" \
  -H "Content-Type: application/json" \
  -d '{
    "WrappingAlgorithm": "RSAES_OAEP_SHA_256",
    "WrappingKeySpec": "RSA_2048"
  }'

# Or using an alias:
curl -X POST "http://localhost:8080/keys/alias:byok-key/import-parameters" \
  -H "Content-Type: application/json" \
  -d '{
    "WrappingAlgorithm": "RSAES_OAEP_SHA_256",
    "WrappingKeySpec": "RSA_2048"
  }'
```

**Response Example**

```json
{
  "PublicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
  "ImportToken": "AQADAGdx...",
  "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
  "ValidTo": "2024-05-11T10:30:00Z"
}
```

**Real Use Case**  
A financial institution with strict key management requirements wants to generate encryption keys on their own hardware
security module (HSM) and import them into WAMS KMS. The security team calls this API, uses the returned public key to
encrypt their HSM-generated key material, and then imports it in the next step.

**Internal Behavior**

- Key must be created with Origin: EXTERNAL
- Generates ephemeral RSA key pair for wrapping (24-hour validity)
- Creates import token bound to this specific key ID
- Wrapping public key returned to caller for encryption
- Parameters expire after validity period (usually 24 hours)
- Import token must be used with the matching encrypted material
- Multiple import parameter sets cannot be active simultaneously

---

### 19. Import Key Material

**Description**  
Imports your own key material (encrypted with the public key from Get Parameters for Import) into a KMS key created with
origin = EXTERNAL. Once imported, the key can be used immediately for encryption/decryption operations.

**Note:** The `keyId` parameter accepts a key ID, alias, or WRN (see resolver above).

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/import
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/import" \
  -H "Content-Type: application/json" \
  -d '{
    "EncryptedKeyMaterial": "AQADAGdx...",
    "ImportToken": "AQADAGdx...",
    "ExpirationModel": "KEY_MATERIAL_DOES_NOT_EXPIRE"
  }'

# Or using an alias:
curl -X POST "http://localhost:8080/keys/alias:byok-key/import" \
  -H "Content-Type: application/json" \
  -d '{
    "EncryptedKeyMaterial": "AQADAGdx...",
    "ImportToken": "AQADAGdx...",
    "ExpirationModel": "KEY_MATERIAL_DOES_NOT_EXPIRE"
  }'
```

**Response Example**

```json
{
  "KeyId": "1234abcd-12ab-34cd-56ef-1234567890ab"
}
```

**Real Use Case**  
Continuing the HSM scenario: after the security team encrypts their key material with the public key, they call this API
with the encrypted material and import token. The key material is never exposed in plaintext to WAMS systems, satisfying
their compliance requirements. The key is now ready for use.

**Internal Behavior**

- Validates import token matches key ID and hasn't expired
- Decrypts encrypted key material using private wrapping key
- Validates decrypted material matches expected key spec
- Securely stores key material in HSM
- Changes key state from PENDING_IMPORT to ENABLED
- Import token and wrapping key are destroyed after use
- Supports optional expiration for imported material

---

## Custom Key Stores

### 20. Create Custom Key Store

**Description**  
Creates a custom key store backed by a CloudHSM cluster or external proxy (XKS - External Key Store). Custom key stores
allow you to store and use key material in your own hardware while leveraging WAMS KMS APIs.

**HTTP Method & Endpoint**

```
POST /custom-key-stores
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/custom-key-stores" \
  -H "Content-Type: application/json" \
  -d '{
    "KeyStoreName": "production-hsm-store",
    "Type": "WAMS_CLOUDHSM",
    "CloudHsmClusterId": "cluster-2bghtf7cbt2w",
    "KeyStorePassword": "secure-password",
    "TrustAnchorCertificate": "-----BEGIN CERTIFICATE-----\nMIIC..."
  }'
```

**Response Example**

```json
{
  "CustomKeyStoreId": 1234
}
```

**Real Use Case**  
A regulated financial institution requires key material to be stored on their own CloudHSM cluster for regulatory
compliance. They create a custom key store pointing to their CloudHSM cluster, allowing them to use WAMS KMS APIs while
maintaining full control of key material storage.

**Internal Behavior**

- Validates connection to CloudHSM cluster or XKS proxy
- Establishes secure TLS connection for all crypto operations
- Stores configuration encrypted in KMS database
- Keys created in custom store never leave customer HSM
- All crypto operations forwarded to HSM for execution
- Supports failover and load balancing for high availability
- Custom key store can be disconnected for maintenance

---

## Annex: Abbreviations & Technical Terminology

### Core Cryptographic Terms

| Term | Definition                             | Technical Context                                                                                             |
|------|----------------------------------------|---------------------------------------------------------------------------------------------------------------|
| AES  | Advanced Encryption Standard           | Symmetric block cipher, NIST standard. Key sizes: 128, 192, 256 bits. Used for data encryption.               |
| RSA  | Rivest-Shamir-Adleman                  | Asymmetric cryptosystem for encryption and digital signatures. Security based on integer factorization.       |
| ECC  | Elliptic Curve Cryptography            | Asymmetric cryptography using elliptic curve mathematics. Smaller key sizes than RSA for equivalent security. |
| HMAC | Hash-based Message Authentication Code | Message authentication using cryptographic hash function + secret key. Provides integrity and authenticity.   |
| DEK  | Data Encryption Key                    | Symmetric key used to encrypt actual data (payload). Typically ephemeral and per-item.                        |
| KEK  | Key Encryption Key                     | Key used to encrypt other keys (DEKs). Core component of envelope encryption.                                 |
| CMK  | Customer Master Key                    | Primary key in KMS used to protect other keys or small amounts of data.                                       |
| BYOK | Bring Your Own Key                     | Ability to import externally generated key material into KMS.                                                 |
| HSM  | Hardware Security Module               | Physical device for secure key generation, storage, and cryptographic operations.                             |
| XKS  | External Key Store                     | KMS feature using key material stored in external systems via proxy.                                          |

### Protocol & Algorithm Terms

| Term  | Definition                            | Usage                                                                                           |
|-------|---------------------------------------|-------------------------------------------------------------------------------------------------|
| OAEP  | Optimal Asymmetric Encryption Padding | Padding scheme for RSA encryption. Provides semantic security against chosen plaintext attacks. |
| PSS   | Probabilistic Signature Scheme        | Padding scheme for RSA signatures. Provides provable security.                                  |
| GCM   | Galois/Counter Mode                   | Authenticated encryption mode for block ciphers. Provides confidentiality + integrity.          |
| CBC   | Cipher Block Chaining                 | Block cipher mode where each plaintext block XORed with previous ciphertext block.              |
| IV    | Initialization Vector                 | Random/nonce value used to ensure same plaintext produces different ciphertext.                 |
| Nonce | Number Used Once                      | Cryptographic value used only once per cryptographic operation. Prevents replay attacks.        |
| Salt  | N/A                                   | Random data added to hashing input to prevent rainbow table attacks.                            |

### KMS Architecture Terms

| Term               | Definition                                                                                                    |
|--------------------|---------------------------------------------------------------------------------------------------------------|
| WRN                | WAMS Resource Name - Unique identifier for WAMS resources. Format: `wrn:wams:service:region:account:resource` |
| Key Material       | Actual cryptographic key bits. Never exposed from KMS for CMKs.                                               |
| Key Rotation       | Replacing key material with new version while retaining old versions for decryption.                          |
| Key Version        | Specific generation of key material. Multiple versions can coexist during rotation.                           |
| Key State          | Current lifecycle status: Enabled, Disabled, PendingDeletion, PendingImport.                                  |
| Grant              | Temporary permission mechanism allowing delegated access without modifying key policy.                        |
| Grant Token        | Opaque token returned during grant creation, usable immediately.                                              |
| Encryption Context | Key-value pairs bound to ciphertext. Must match for decryption. Provides authenticated data.                  |

### Security & Compliance Terms

| Term                | Definition                                                                            |
|---------------------|---------------------------------------------------------------------------------------|
| FIPS 140-2/140-3    | US government standard for cryptographic module security levels (Level 1-4).          |
| PCI-DSS             | Payment Card Industry Data Security Standard. Requires encryption of cardholder data. |
| HIPAA               | Health Insurance Portability and Accountability Act. Protects health information.     |
| GDPR                | General Data Protection Regulation. EU data protection law.                           |
| SOC 2               | Service Organization Control 2. Audits security, availability, processing integrity.  |
| Non-repudiation     | Ability to prove a specific action was performed by a specific entity.                |
| Least Privilege     | Security principle granting only necessary permissions for task completion.           |
| Envelope Encryption | Pattern where data encrypted with DEK, DEK encrypted with KEK.                        |

### Infrastructure & Network Terms

| Term      | Definition                                                                           |
|-----------|--------------------------------------------------------------------------------------|
| CloudHSM  | WAMS-managed HSM service providing single-tenant HSM instances.                      |
| VPC       | Virtual Private Cloud - Isolated network segment within WAMS cloud.                  |
| TLS       | Transport Layer Security - Protocol for secure network communications.               |
| XKS Proxy | External Key Store proxy - Interface between KMS and external key management system. |
| CMDB      | Configuration Management Database - Repository of IT infrastructure assets.          |

### API & Data Format Terms

| Term       | Definition                                                                                                |
|------------|-----------------------------------------------------------------------------------------------------------|
| REST       | Representational State Transfer - Architectural style for web APIs.                                       |
| JSON       | JavaScript Object Notation - Lightweight data interchange format.                                         |
| Base64     | Binary-to-text encoding scheme. Used for transmitting binary data in JSON.                                |
| UUID       | Universally Unique Identifier - 128-bit identifier, standard format: 12345678-1234-1234-1234-123456789abc |
| Pagination | Technique for splitting large result sets into pages. Uses limit and nextToken.                           |

### Key Specifications (KeySpec)

| KeySpec Value     | Type       | Key Size    | Primary Use           |
|-------------------|------------|-------------|-----------------------|
| SYMMETRIC_DEFAULT | Symmetric  | 256-bit AES | Encryption/Decryption |
| RSA_2048          | Asymmetric | 2048-bit    | Encryption or Signing |
| RSA_3072          | Asymmetric | 3072-bit    | Encryption or Signing |
| RSA_4096          | Asymmetric | 4096-bit    | Encryption or Signing |
| ECC_NIST_P256     | Asymmetric | 256-bit ECC | Signing               |
| ECC_NIST_P384     | Asymmetric | 384-bit ECC | Signing               |
| ECC_NIST_P521     | Asymmetric | 521-bit ECC | Signing               |
| ECC_SECG_P256K1   | Asymmetric | 256-bit ECC | Signing (Bitcoin)     |
| HMAC_256          | Symmetric  | 256-bit     | Generate/Verify MAC   |

### Key Usage Types

| KeyUsage Value      | Supported Operations              | Key Types                |
|---------------------|-----------------------------------|--------------------------|
| ENCRYPT_DECRYPT     | Encrypt, Decrypt, GenerateDataKey | SYMMETRIC_DEFAULT, RSA_* |
| SIGN_VERIFY         | Sign, Verify, GetPublicKey        | RSA_, ECC_               |
| GENERATE_VERIFY_MAC | GenerateMac, VerifyMac            | HMAC_*                   |

### Error Codes

| HTTP Status | Error Code                 | Description                                         |
|-------------|----------------------------|-----------------------------------------------------|
| 400         | ValidationException        | Invalid parameter (e.g., key spec + usage mismatch) |
| 400         | InvalidCiphertextException | Ciphertext corrupted or malformed                   |
| 400         | IncorrectKeyException      | Ciphertext was encrypted with different key         |
| 403         | AccessDeniedException      | Missing IAM permissions                             |
| 404         | NotFoundException          | Key ID, alias, or grant not found                   |
| 409         | ConflictException          | Alias already exists or key in invalid state        |
| 500         | DependencyTimeoutException | HSM or external dependency timeout                  |
| 500         | KMSInternalException       | Internal KMS service error                          |

---

## Best Practices Summary

- **Key Rotation**: Enable automatic rotation for all production symmetric keys (365 days default).

- **Least Privilege**: Use key policies and grants to limit actions to required operations only.

- **Envelope Encryption**: Use GenerateDataKey for large data (files > 4KB) to improve performance.

- **Encryption Context**: Always use encryption context for authenticated encryption.

- **Aliases**: Reference keys by alias (e.g., alias:production-db-key) for easier rotation.

- **Key Identifier Flexibility**: Use key IDs for scripts, aliases for application code, and WRNs for audit logs and
  infrastructure code.

- **Grants**: Use for temporary access (Lambda functions, cross-account) instead of policy modifications.

- **Audit Logging**: Enable CloudTrail logging for all KMS API calls.

- **BYOK Security**: Generate key material in FIPS-validated HSM, encrypt before transmission.

- **Custom Key Stores**: Use for regulated workloads requiring full control of key material storage.

---

## Summary

This KMS Service API provides a comprehensive suite of cryptographic key management and operations. From basic key
creation and deletion to advanced features like envelope encryption, digital signatures, grants, and Bring Your Own
Key (BYOK), it supports a wide range of security and compliance requirements.

**Key Identifier Flexibility**: The API's support for multiple key identifier formats (Key ID, Alias, and WRN) through
the resolver ensures that:

- **Applications** can use human-readable aliases that support transparent key rotation
- **Infrastructure code** can reference keys using fully-qualified WRNs for auditability
- **Automation** can work with direct key IDs for performance-critical operations

By following best practices and leveraging the API's features appropriately, organizations can build secure, compliant
applications that protect sensitive data throughout their lifecycle.
