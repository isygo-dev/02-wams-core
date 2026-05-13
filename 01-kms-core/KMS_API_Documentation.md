# KMS Service API Documentation

A comprehensive REST API for managing cryptographic keys, performing encryption/decryption operations, and securing
sensitive data. This service supports symmetric keys (AES-256), asymmetric keys (RSA, ECC), key rotation, multi-region
configurations, grants, and Bring Your Own Key (BYOK) functionality.

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

---

## Key Management

### 1. Create Key

**Description**  
Creates a new customer-managed key (CMK-Customer Master Key) in the KMS service. Supports both symmetric (AES-256) and
asymmetric (RSA, ECC) keys. The key can be configured with a description, usage policy, key spec, origin (WAMS-managed
or EXTERNAL for BYOK-Bring Your Own Key), tags, and multi-region capabilities.

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

**Real Use Case**  
A financial services company needs to encrypt customer credit card data stored in their RDS database. They create a CMK
to ensure PCI-DSS compliance. The key is tagged with environment and application metadata for audit trails, and only
applications with explicit IAM permissions can use the key for encryption.

---

### 2. Describe Key

**Description**  
Retrieves detailed metadata about a KMS key, including its WRN-WAMS Resource Name, state (
Enabled/Disabled/PendingDeletion), creation date, rotation status, key material origin, and multi-region configuration.

**HTTP Method & Endpoint**

```
GET /keys/{keyId}
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab" \
  -H "Accept: application/json"
```

**Real Use Case**  
A DevOps team audits all KMS keys quarterly to verify rotation status. They use this API to retrieve metadata for every
key and identify keys that haven't been rotated in over 90 days, triggering a manual review and rotation process for
compliance.

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
      "Wrn": "wrn:wams:kms:us-east-1:123456789012:key/1234abcd-12ab-34cd-56ef-1234567890ab"
    },
    {
      "KeyId": "5678efgh-56ef-78gh-90ij-0987654321cd",
      "Wrn": "wrn:wams:kms:us-east-1:123456789012:key/5678efgh-56ef-78gh-90ij-0987654321cd"
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

---

### 4. Schedule Key Deletion

**Description**  
Schedules a KMS key for deletion with a configurable waiting period (7–30 days). During the waiting period, the key
cannot be used and its state becomes 'PendingDeletion'. This provides a safety mechanism to prevent accidental key
deletion.

**HTTP Method & Endpoint**

```
DELETE /keys/{keyId}/schedule-deletion?pendingWindowInDays={days}
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/schedule-deletion?pendingWindowInDays=30" \
  -H "Accept: application/json"
```

**Real Use Case**  
When decommissioning a legacy application, the DevOps team schedules its encryption key for deletion with a 30-day
waiting period. This allows time to verify that no running processes still depend on the key. After 30 days with no
issues, they proceed with permanent deletion.

---

### 5. Cancel Key Deletion

**Description**  
Cancels a previously scheduled key deletion, restoring the key to its previous state (Enabled or Disabled). This is
useful if the scheduled deletion was initiated by mistake or if new dependencies on the key are discovered.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/cancel-deletion
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/cancel-deletion" \
  -H "Content-Type: application/json"
```

**Real Use Case**  
A database team accidentally schedules deletion of a production database encryption key. Before the waiting period
expires, they discover the mistake and call this API to cancel the deletion and restore the key, preventing a
catastrophic outage.

---

### 6. Delete Key (Permanent)

**Description**  
Permanently deletes a KMS key. This operation is **irreversible** and should only be used after the key has been
scheduled for deletion and the waiting period has expired. Once deleted, any data encrypted with this key cannot be
decrypted.

**HTTP Method & Endpoint**

```
DELETE /keys/{keyId}
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab" \
  -H "Accept: application/json"
```

⚠️ **Wwrning**: This is a destructive operation that cannot be undone.

**Real Use Case**  
After the 30-day waiting period expires and all systems confirm the key is no longer needed, an automated compliance
tool calls this API to permanently delete the key, freeing up key quotas and ensuring no accidental reuse.

---

### 7. Update Key Rotation

**Description**  
Enables or disables automatic key rotation for a CMK. When enabled, WAMS KMS automatically rotates the key material
annually. Key rotation strengthens security by limiting the amount of data encrypted with a single key material version.

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
    "EnableKeyRotation": true
  }'
```

**Real Use Case**  
A healthcare organization adopts a policy requiring all encryption keys to rotate annually for HIPAA compliance. They
use this API to enable rotation on all production encryption keys, and KMS automatically rotates each key every 365 days
without manual intervention.

---

## Cryptographic Operations

### 8. Encrypt

**Description**  
Encrypts plaintext data using a KMS key. The plaintext must be up to 4 KB in size. Returns an encrypted ciphertext blob
that can only be decrypted using the same KMS key or a replica in a multi-region setup.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/encrypt
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/encrypt" \
  -H "Content-Type: application/json" \
  -d '{
    "Plaintext": "creditCardNumber: 4111-1111-1111-1111",
    "EncryptionAlgorithm": "SYMMETRIC_DEFAULT"
  }'
```

**Response Example**

```json
{
  "CiphertextBlob": "AQADAGn...(base64-encoded)",
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key/1234abcd-12ab-34cd-56ef-1234567890ab",
  "EncryptionAlgorithm": "SYMMETRIC_DEFAULT"
}
```

**Real Use Case**  
An e-commerce platform encrypts customer payment information before storing it in the database. Each credit card number
is encrypted with the same key, and the ciphertext is stored in the payment records table. Only authorized services with
KMS decrypt permissions can access the original card data.

---

### 9. Decrypt

**Description**  
Decrypts a ciphertext blob encrypted by the Encrypt operation. Returns the plaintext. The user must have permission to
use the key (via IAM policy or grant) to decrypt data.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/decrypt
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/decrypt" \
  -H "Content-Type: application/json" \
  -d '{
    "CiphertextBlob": "AQADAGn...(base64-encoded)"
  }'
```

**Response Example**

```json
{
  "Plaintext": "creditCardNumber: 4111-1111-1111-1111",
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key/1234abcd-12ab-34cd-56ef-1234567890ab",
  "EncryptionAlgorithm": "SYMMETRIC_DEFAULT"
}
```

**Real Use Case**  
When a customer requests a refund, the payment processing system retrieves the encrypted credit card number from the
database and calls this API to decrypt it. The plaintext is temporarily held in memory for the refund operation, then
immediately discarded.

---

### 10. Generate Data Key

**Description**  
Generates a unique symmetric data key and returns both a plaintext and encrypted copy. The plaintext key is used for
local encryption, while the encrypted key is stored alongside the encrypted data. This enables envelope encryption
pattern, protecting the data key with the KMS key.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/generate-data-key
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/generate-data-key" \
  -H "Content-Type: application/json" \
  -d '{
    "KeySpec": "AES_256",
    "EncryptionAlgorithm": "SYMMETRIC_DEFAULT"
  }'
```

**Response Example**

```json
{
  "Plaintext": "eFgHiJkLmNoPqRsT...(base64-encoded)",
  "CiphertextBlob": "AQADAGn...(base64-encoded)",
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key/1234abcd-12ab-34cd-56ef-1234567890ab",
  "EncryptionAlgorithm": "SYMMETRIC_DEFAULT"
}
```

**Real Use Case**  
A cloud storage service stores millions of user files. Instead of encrypting each file directly with the master KMS
key (which would be slow and costly), they use this API to generate a unique data key for each file. The file is
encrypted locally with the plaintext key, then the encrypted data key is stored with the file. When decryption is
needed, they decrypt the data key first, then decrypt the file.

---

### 11. Generate Data Key Without Plaintext

**Description**  
Generates a data key but returns only the encrypted version, not the plaintext. Useful when the plaintext key is not
needed immediately or should never be returned in the API response.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/generate-data-key-without-plaintext
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/generate-data-key-without-plaintext" \
  -H "Content-Type: application/json" \
  -d '{
    "KeySpec": "AES_256"
  }'
```

**Real Use Case**  
A serverless application uses WAMS Lambda to process sensitive documents. The Lambda function generates an encrypted
data key without the plaintext, stores it with the document metadata, and sends the document to a separate secure
service that has the ability to decrypt the key. This reduces the risk of plaintext key material being exposed in the
Lambda function memory.

---

### 12. Decrypt Data Key

**Description**  
Decrypts an encrypted data key, returning the plaintext. Used in envelope encryption scenarios to retrieve the data key
needed for local decryption operations.

**HTTP Method & Endpoint**

```
POST /decrypt-data-key
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/decrypt-data-key" \
  -H "Content-Type: application/json" \
  -d '{
    "CiphertextBlob": "AQADAGn...(base64-encoded)"
  }'
```

**Real Use Case**  
Continuing the cloud storage example: when a user requests to download a file, the service retrieves the encrypted data
key stored with the file and calls this API to decrypt it. The plaintext data key is then used to decrypt the file
content locally, and the plaintext key is immediately discarded after use.

---

### 13. Generate MAC (Message Authentication Code)

**Description**  
Generates a message authentication code (MAC) for data integrity verification. Uses an HMAC algorithm to create a
signature that proves data integrity and authenticity.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/generate-mac
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/generate-mac" \
  -H "Content-Type: application/json" \
  -d '{
    "Message": "importantTransactionData123",
    "MacAlgorithm": "HMAC_SHA_256"
  }'
```

**Real Use Case**  
A financial trading system generates MACs for all transaction records. When a record is later retrieved, the system
regenerates the MAC and compares it with the stored value. If they match, the transaction data has not been tampered
with. If they don't match, the transaction is flagged for investigation.

---

### 14. Verify MAC

**Description**  
Verifies that a provided MAC matches the expected value for given data. Returns a boolean indicating whether the
signature is valid.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/verify-mac
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/verify-mac" \
  -H "Content-Type: application/json" \
  -d '{
    "Message": "importantTransactionData123",
    "Mac": "3f8c9d2e...(base64-encoded)",
    "MacAlgorithm": "HMAC_SHA_256"
  }'
```

**Response Example**

```json
{
  "SignatureValid": true,
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key/1234abcd-12ab-34cd-56ef-1234567890ab",
  "MacAlgorithm": "HMAC_SHA_256"
}
```

**Real Use Case**  
A supply chain platform receives purchase orders from vendors. Each order includes a MAC signed by the vendor using a
key the vendor controls. The platform verifies the MAC to ensure the order came from the legitimate vendor and hasn't
been modified in transit.

---

### 15. Sign

**Description**  
Creates a digital signature for data using an asymmetric KMS key (RSA or ECC). The signature proves that specific data
was signed by the key owner and provides non-repudiation.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/sign
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/sign" \
  -H "Content-Type: application/json" \
  -d '{
    "Message": "contractDocument123",
    "SigningAlgorithm": "RSASSA_PSS_SHA_256"
  }'
```

**Real Use Case**  
A legal tech company uses this API to digitally sign electronic contracts. Each contract is hashed and signed with an
asymmetric key. The signature is stored with the contract. Later, any party can verify the signature using the public
key to prove that the contract was signed by the company and has not been altered.

---

### 16. Verify

**Description**  
Verifies a digital signature created by the Sign operation. Confirms that the signature is valid and was created using
the specified asymmetric key.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/verify
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "Message": "contractDocument123",
    "Signature": "signa7ure_data...(base64-encoded)",
    "SigningAlgorithm": "RSASSA_PSS_SHA_256"
  }'
```

**Real Use Case**  
A SaaS platform receives digitally signed API requests from partners. Before processing each request, the platform calls
this API to verify the signature. If the signature is invalid, the request is rejected. This ensures that only
authorized partners can access certain API endpoints.

---

## Key Aliases

### 17. Create Alias

**Description**  
Creates a friendly alias (human-readable name) for a KMS key. Aliases are easier to remember and manage than key IDs.
Multiple aliases can point to the same key, but each alias must be unique.

**HTTP Method & Endpoint**

```
POST /aliases
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/aliases" \
  -H "Content-Type: application/json" \
  -d '{
    "AliasName": "alias/my-prod-db-key",
    "TargetKeyId": "1234abcd-12ab-34cd-56ef-1234567890ab"
  }'
```

**Real Use Case**  
A DevOps team creates an alias `alias/ecommerce-payment-key` for the production payment key. Application code can
reference the alias instead of the key ID, making deployment configurations more readable and easier to audit.

---

### 18. Update Alias

**Description**  
Changes the target key of an alias, allowing you to point the alias to a different key. Useful for key rotation
scenarios where traffic needs to migrate from an old key to a new one.

**HTTP Method & Endpoint**

```
PATCH /aliases/{aliasName}
Content-Type: application/json
```

**Usage Call**

```bash
curl -X PATCH "http://localhost:8080/aliases/alias%2Fmy-prod-db-key" \
  -H "Content-Type: application/json" \
  -d '{
    "TargetKeyId": "5678efgh-56ef-78gh-90ij-0987654321cd"
  }'
```

**Real Use Case**  
During a planned key rotation, the security team creates a new key and updates the alias to point to it. All
applications that reference the alias automatically start using the new key without requiring any code changes or
redeployments.

---

### 19. Delete Alias

**Description**  
Deletes an alias. The underlying key is not affected; it simply no longer has that alias. The key can still be
referenced by its key ID.

**HTTP Method & Endpoint**

```
DELETE /aliases/{aliasName}
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/aliases/alias%2Fmy-prod-db-key"
```

**Real Use Case**  
When a legacy application is decommissioned, the security team deletes its associated alias to clean up the namespace
and avoid confusion about which keys are actively used.

---

### 20. List Aliases

**Description**  
Returns a paginated list of all key aliases in the account, including their target key IDs.

**HTTP Method & Endpoint**

```
GET /aliases?limit={limit}&nextToken={nextToken}
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/aliases?limit=100" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "Aliases": [
    {
      "AliasName": "alias/my-prod-db-key",
      "TargetKeyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
      "Wrn": "wrn:wams:kms:us-east-1:123456789012:alias/my-prod-db-key"
    },
    {
      "AliasName": "alias/ecommerce-payment-key",
      "TargetKeyId": "5678efgh-56ef-78gh-90ij-0987654321cd",
      "Wrn": "wrn:wams:kms:us-east-1:123456789012:alias/ecommerce-payment-key"
    }
  ],
  "NextToken": null
}
```

**Real Use Case**  
A compliance auditor uses this API to generate a report of all aliases in the organization and their associated keys.
They verify that each alias follows the naming convention and corresponds to an active application.

---

## Key Tagging

### 21. Tag Resource

**Description**  
Adds one or more tags (key-value pairs) to a KMS key. Tags are used for organization, cost allocation, access control,
and automation. Tags help with filtering, searching, and managing keys by business context.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/tags
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/tags" \
  -H "Content-Type: application/json" \
  -d '{
    "Tags": {
      "Environment": "production",
      "CostCenter": "engineering",
      "Owner": "security-team",
      "Compliance": "HIPAA"
    }
  }'
```

**Real Use Case**  
A multi-team organization tags all encryption keys with cost center information. Finance uses this data to allocate KMS
costs back to the appropriate departments. Additionally, security policies automatically deny access to keys tagged
with "Compliance: PCI-DSS" unless the requester has special authorization.

---

### 22. Untag Resource

**Description**  
Removes one or more tags from a KMS key.

**HTTP Method & Endpoint**

```
DELETE /keys/{keyId}/tags
Content-Type: application/json
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/tags" \
  -H "Content-Type: application/json" \
  -d '{
    "TagKeys": ["Environment", "CostCenter"]
  }'
```

**Real Use Case**  
When an application is decommissioned, the operations team removes the "Application" tag from the encryption key. This
signals that the key is being phased out and can be scheduled for deletion after the retention period expires.

---

### 23. List Resource Tags

**Description**  
Retrieves all tags associated with a KMS key.

**HTTP Method & Endpoint**

```
GET /keys/{keyId}/tags
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/tags" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "Tags": {
    "Environment": "production",
    "CostCenter": "engineering",
    "Owner": "security-team",
    "Compliance": "HIPAA"
  }
}
```

**Real Use Case**  
An automated governance tool scans all keys daily and checks their tags. If a key is tagged with "Environment: test" but
hasn't been used in 30 days, the tool sends a notification to the team suggesting the key can be deleted.

---

## Key Policies

### 24. Get Key Policy

**Description**  
Retrieves the resource-based policy document attached to a KMS key. Policies define who can perform which operations on
the key.

**HTTP Method & Endpoint**

```
GET /keys/{keyId}/policy
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/policy" \
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

---

### 25. Put Key Policy

**Description**  
Replaces the resource-based policy document for a KMS key. This completely replaces the existing policy with the new
one.

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
          "Sid": "Allow specific role to use key",
          "Effect": "Allow",
          "Principal": {
            "WAMS": "wrn:wams:iam::123456789012:role/NewApplicationRole"
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
  }'
```

**Real Use Case**  
When a new microservice is deployed, the infrastructure team updates the key policy to grant the microservice's IAM role
permission to encrypt and decrypt data. The policy update is reviewed and approved through the CI/CD pipeline before
being applied.

---

## Grants

### 26. Create Grant

**Description**  
Creates a grant, which is a flexible permission mechanism for temporary or delegated access. Grants are useful for
giving applications, services, or cross-account principals limited permissions on a key without modifying the key
policy. Grants can be revoked instantly.

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

---

### 27. Retire Grant

**Description**  
Retires (removes) a grant by grant token or grant ID + key ID. After retirement, the grantee can no longer use the key
for the operations specified in the grant.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/retire-grant
Content-Type: application/json
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/retire-grant" \
  -H "Content-Type: application/json" \
  -d '{
    "GrantToken": "AQpAM2RhZjM1YTY2MTk0NDc3Y2I5NzY1MTI3NTQ5MTQyZT..."
  }'
```

**Real Use Case**  
A contractor's temporary access period is coming to an end. The security team calls this API to retire the contractor's
grant before their employment ends. Immediately after the API call returns, the contractor can no longer decrypt
sensitive data.

---

### 28. Revoke Grant

**Description**  
Revokes a grant by grant ID and key ID. Similar to retiring a grant but can only be done by the key owner (not by the
grantee).

**HTTP Method & Endpoint**

```
DELETE /keys/{keyId}/grants/{grantId}
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/grants/0c237476b89f0cbc37edb331c91f26a9f75967e6aec1acfc946e24d47ed6b976" \
  -H "Accept: application/json"
```

**Real Use Case**  
A team's project is cancelled, and the grant created for that project needs to be revoked. The key owner immediately
calls this API to revoke the grant, preventing the team from continuing to use the key.

---

### 29. List Grants

**Description**  
Lists all grants for a specific KMS key, including grant IDs, grantee principals, operations, and constraints.

**HTTP Method & Endpoint**

```
GET /keys/{keyId}/grants?limit={limit}&nextToken={nextToken}
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/grants?limit=50" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "Grants": [
    {
      "GrantId": "0c237476b89f0cbc37edb331c91f26a9f75967e6aec1acfc946e24d47ed6b976",
      "GranteePrincipal": "wrn:wams:iam::123456789012:role/TemporaryServiceRole",
      "Operations": ["Encrypt", "Decrypt"],
      "Constraints": {
        "EncryptionContextSubset": {
          "Department": "Finance"
        }
      },
      "CreationDate": "2024-05-10T10:30:00Z"
    }
  ],
  "NextToken": null
}
```

**Real Use Case**  
An auditor runs a quarterly compliance check to list all active grants for all keys. They identify overly permissive
grants or grants for users who are no longer with the company and revoke them.

---

### 30. List Retirable Grants

**Description**  
Lists grants that can be retired by a specific principal (the retiring principal). This is useful for a grantee to see
which of their own grants can be retired.

**HTTP Method & Endpoint**

```
GET /retirable-grants?retiringPrincipal={principal}&limit={limit}
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/retirable-grants?retiringPrincipal=wrn:wams:iam::123456789012:role/TemporaryServiceRole&limit=50" \
  -H "Accept: application/json"
```

**Real Use Case**  
An application role wants to clean up its temporary grants after completing a task. It calls this API to list its own
retirable grants and then retires them in a batch operation.

---

## BYOK (Import Key Material)

### 31. Get Parameters for Import

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
```

**Response Example**

```json
{
  "PublicKey": "MIIBIjANBgkqhkiG9w0BA...(base64-encoded)",
  "ImportToken": "AQADAGdx...(base64-encoded)",
  "KeyModulus": "wF5uVn2q9nJ...",
  "KeyExponent": "AQAB"
}
```

**Real Use Case**  
A financial institution with strict key management requirements wants to generate encryption keys on their own hardware
security module (HSM) and import them into WAMS KMS. The security team calls this API, uses the returned public key to
encrypt their HSM-generated key material, and then imports it in the next step.

---

### 32. Import Key Material

**Description**  
Imports your own key material (encrypted with the public key from Get Parameters for Import) into a KMS key created with
origin = EXTERNAL. Once imported, the key can be used immediately for encryption/decryption operations.

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
    "EncryptedKeyMaterial": "AQADAGdx...(base64-encoded, encrypted with the public key)",
    "ImportToken": "AQADAGdx...(from previous step)",
    "ExpirationModel": "KEY_MATERIAL_DOES_NOT_EXPIRE"
  }'
```

**Real Use Case**  
Continuing the HSM scenario: after the security team encrypts their key material with the public key, they call this API
with the encrypted material and import token. The key material is never exposed in plaintext to WAMS systems, satisfying
their compliance requirements. The key is now ready for use.

---

### 33. Delete Imported Key Material

**Description**  
Deletes imported key material from a KMS key. After deletion, the key becomes unusable and enters the "PendingImport"
state. New key material must be imported for the key to become usable again.

**HTTP Method & Endpoint**

```
DELETE /keys/{keyId}/key-material
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/key-material" \
  -H "Accept: application/json"
```

**Real Use Case**  
A financial institution suspects their HSM-generated key material may have been compromised. They immediately call this
API to delete the imported key material, rendering the KMS key unusable. They then generate new key material on their
HSM and re-import it.

---

## Custom Key Stores

### 34. Create Custom Key Store

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
    "CustomKeyStoreName": "production-hsm-store",
    "KeyStoreType": "CLOUDHSM",
    "CloudHsmClusterId": "cluster-2bghtf7cbt2w",
    "ConnectionConfig": {
      "Hostname": "10.0.1.100",
      "Port": 5696,
      "CertificateContent": "-----BEGIN CERTIFICATE-----\nMIIC..."
    }
  }'
```

**Real Use Case**  
A regulated financial institution requires key material to be stored on their own CloudHSM cluster for regulatory
compliance. They create a custom key store pointing to their CloudHSM cluster, allowing them to use WAMS KMS APIs while
maintaining full control of key material storage.

---

### 35. Describe Custom Key Store

**Description**  
Retrieves metadata about a custom key store, including its name, type, connection status, associated CloudHSM cluster or
proxy, and any error messages.

**HTTP Method & Endpoint**

```
GET /custom-key-stores/{customKeyStoreId}
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/custom-key-stores/1234" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "CustomKeyStoreId": "1234",
  "CustomKeyStoreName": "production-hsm-store",
  "KeyStoreType": "CLOUDHSM",
  "ConnectionState": "CONNECTED",
  "CloudHsmClusterId": "cluster-2bghtf7cbt2w",
  "CreationDate": "2024-05-10T10:30:00Z",
  "LastModifiedDate": "2024-05-15T12:00:00Z"
}
```

**Real Use Case**  
A DevOps engineer monitors the health of custom key stores. They periodically call this API to check the
ConnectionState. If a key store shows "DISCONNECTED", they investigate the CloudHSM cluster or network connectivity
issues.

---

### 36. Update Custom Key Store

**Description**  
Updates configuration properties of a custom key store, such as its name, connection parameters, or proxy settings.

**HTTP Method & Endpoint**

```
PATCH /custom-key-stores/{customKeyStoreId}
Content-Type: application/json
```

**Usage Call**

```bash
curl -X PATCH "http://localhost:8080/custom-key-stores/1234" \
  -H "Content-Type: application/json" \
  -d '{
    "CustomKeyStoreName": "production-hsm-store-updated",
    "ConnectionConfig": {
      "Hostname": "10.0.1.101",
      "Port": 5696
    }
  }'
```

**Real Use Case**  
When the CloudHSM cluster is upgraded or network settings change, the operations team calls this API to update the
custom key store's connection parameters without affecting running applications.

---

### 37. Delete Custom Key Store

**Description**  
Deletes a custom key store. The custom key store must be disconnected and contain no keys before deletion.

**HTTP Method & Endpoint**

```
DELETE /custom-key-stores/{customKeyStoreId}
```

**Usage Call**

```bash
curl -X DELETE "http://localhost:8080/custom-key-stores/1234" \
  -H "Accept: application/json"
```

**Real Use Case**  
A company decides to decommission their CloudHSM cluster. Before deletion, they delete all keys stored in the custom key
store, disconnect it, and then call this API to remove the custom key store from KMS.

---

### 38. Connect Custom Key Store

**Description**  
Establishes a connection between a custom key store and its underlying hardware (CloudHSM cluster or external proxy).
After connection, keys can be used.

**HTTP Method & Endpoint**

```
POST /custom-key-stores/{customKeyStoreId}/connect
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/custom-key-stores/1234/connect" \
  -H "Content-Type: application/json"
```

**Real Use Case**  
After a planned maintenance window on the CloudHSM cluster, the operations team calls this API to reconnect the custom
key store. Applications can now resume encryption/decryption operations.

---

### 39. Disconnect Custom Key Store

**Description**  
Disconnects a custom key store from its underlying hardware. While disconnected, keys in the custom key store cannot be
used for cryptographic operations.

**HTTP Method & Endpoint**

```
POST /custom-key-stores/{customKeyStoreId}/disconnect
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/custom-key-stores/1234/disconnect" \
  -H "Content-Type: application/json"
```

**Real Use Case**  
Before applying critical security patches to the CloudHSM cluster, the operations team calls this API to disconnect the
custom key store. This ensures no new operations attempt to use keys during the maintenance window.

---

### 40. List Custom Key Stores

**Description**  
Lists all custom key stores in the account with optional filtering by custom key store ID or name. Results are
paginated.

**HTTP Method & Endpoint**

```
GET /custom-key-stores?limit={limit}&customKeyStoreId={id}&customKeyStoreName={name}
```

**Usage Call**

```bash
curl -X GET "http://localhost:8080/custom-key-stores?limit=50&customKeyStoreName=production" \
  -H "Accept: application/json"
```

**Response Example**

```json
{
  "CustomKeyStores": [
    {
      "CustomKeyStoreId": "1234",
      "CustomKeyStoreName": "production-hsm-store",
      "KeyStoreType": "CLOUDHSM",
      "ConnectionState": "CONNECTED",
      "CloudHsmClusterId": "cluster-2bghtf7cbt2w"
    }
  ],
  "NextToken": null
}
```

**Real Use Case**  
A compliance auditor generates a quarterly report of all custom key stores in the organization. They filter for "
production" key stores, verify their connection status, and document which teams own each one.

---

### 41. Validate Key

**Description**  
Validates the specified key by checking key material integrity, key state, and user permissions. This is useful for
diagnostics and verification before using a key.

**HTTP Method & Endpoint**

```
POST /keys/{keyId}/validate
```

**Usage Call**

```bash
curl -X POST "http://localhost:8080/keys/1234abcd-12ab-34cd-56ef-1234567890ab/validate" \
  -H "Content-Type: application/json"
```

**Response Example**

```json
{
  "KeyValid": true,
  "KeyId": "wrn:wams:kms:us-east-1:123456789012:key/1234abcd-12ab-34cd-56ef-1234567890ab",
  "KeyState": "Enabled",
  "KeyMaterialIntegrity": "VALID"
}
```

**Real Use Case**  
An application performs a health check during startup. It calls this API for all encryption keys it needs. If any key
returns KeyValid = false, the application enters degraded mode and logs an alert to the operations team.

---

## Best Practices

- **Least Privilege**: Grant only the minimum necessary permissions (Encrypt, Decrypt, etc.) when creating grants or
  policies.
- **Key Rotation**: Enable automatic key rotation for all production keys and monitor rotation status.
- **Tagging**: Use consistent tagging strategies for cost allocation, access control, and automation.
- **Audit Logging**: Enable CloudTrail logging for all KMS API calls to track key usage.
- **Envelope Encryption**: Use GenerateDataKey for large data encryption to improve performance and reduce costs.
- **Alias Management**: Use aliases for application configuration instead of key IDs for easier management.
- **Grant Constraints**: Leverage encryption context constraints on grants to limit operations to specific data
  contexts.
- **BYOK Security**: When using BYOK, ensure key material is encrypted on your infrastructure before transmission.
- **Custom Key Stores**: For sensitive regulated environments, use custom key stores to maintain control over key
  material.

---

## Summary

This KMS Service API provides a comprehensive suite of cryptographic key management and operations. From basic key
creation and deletion to advanced features like envelope encryption, digital signatures, grants, and Bring Your Own
Key (BYOK), it supports a wide range of security and compliance requirements. By following best practices and leveraging
the API's features appropriately, organizations can build secure, compliant applications that protect sensitive data
throughout their lifecycle.

---

# Annex A: Abbreviations & Terminology

| Abbreviation            | Full Term                                           | Meaning                                                                                                                           |
|-------------------------|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| **API**                 | Application Programming Interface                   | A set of protocols and tools for building application software that specifies how software components should interact.            |
| **ARN**                 | Amazon Resource Name                                | A unique identifier for AWS resources in the format: `arn:aws:service:region:account-id:resource-type/resource-id`                |
| **AES**                 | Advanced Encryption Standard                        | A symmetric encryption algorithm standardized by NIST, supporting key sizes of 128, 192, and 256 bits.                            |
| **BYOK**                | Bring Your Own Key                                  | A feature allowing customers to import and manage their own encryption key material into a cloud service (AWS KMS).               |
| **CMDB**                | Configuration Management Database                   | A centralized repository that stores information about IT infrastructure, assets, and their relationships.                        |
| **CMK**                 | Customer Master Key                                 | A customer-managed encryption key in AWS KMS (now called Customer Managed Key). Used to encrypt/decrypt data or other keys.       |
| **CloudHSM**            | AWS CloudHSM                                        | A cloud-based Hardware Security Module service that allows you to manage single-tenant HSM instances.                             |
| **DEK**                 | Data Encryption Key                                 | A symmetric key used to encrypt actual data. Typically encrypted with a KEK and stored alongside the encrypted data.              |
| **ECC**                 | Elliptic Curve Cryptography                         | An asymmetric encryption algorithm based on the mathematics of elliptic curves. Used for signing and key agreement.               |
| **HMAC**                | Hash-based Message Authentication Code              | A cryptographic method for verifying message authenticity and integrity using a secret key and hash function.                     |
| **HSM**                 | Hardware Security Module                            | A physical security device that generates, stores, and manages cryptographic keys and performs cryptographic operations.          |
| **IAM**                 | Identity and Access Management                      | AWS service for managing user identities and their permissions to access AWS resources.                                           |
| **KEK**                 | Key Encryption Key                                  | A master key used to encrypt other keys (DEKs). Part of the envelope encryption pattern.                                          |
| **KMS**                 | Key Management Service                              | AWS service for creating, managing, and using cryptographic keys for data protection.                                             |
| **MAC**                 | Message Authentication Code                         | A cryptographic output that proves the authenticity and integrity of a message.                                                   |
| **PCI-DSS**             | Payment Card Industry Data Security Standard        | A set of security standards for handling credit card data to prevent fraud and data breaches.                                     |
| **REST**                | Representational State Transfer                     | An architectural style for designing networked applications using HTTP requests to perform CRUD operations.                       |
| **RSA**                 | Rivest-Shamir-Adleman                               | An asymmetric encryption algorithm widely used for encryption and digital signatures.                                             |
| **RDS**                 | Relational Database Service                         | AWS managed relational database service supporting MySQL, PostgreSQL, Oracle, SQL Server, and MariaDB.                            |
| **HIPAA**               | Health Insurance Portability and Accountability Act | US law that sets privacy and security standards for protected health information (PHI).                                           |
| **S3**                  | Simple Storage Service                              | AWS object storage service for storing and retrieving large amounts of data.                                                      |
| **XKS**                 | External Key Store                                  | A feature allowing AWS KMS to use key material stored in external key management systems via an external proxy.                   |
| **JSON**                | JavaScript Object Notation                          | A lightweight, text-based data format widely used for APIs and data exchange.                                                     |
| **HTTP**                | HyperText Transfer Protocol                         | The underlying protocol used for RESTful web services and APIs.                                                                   |
| **HTTPS**               | HTTP Secure                                         | Encrypted version of HTTP using TLS/SSL for secure data transmission.                                                             |
| **CI/CD**               | Continuous Integration/Continuous Deployment        | Practices and tools for automating code testing, building, and deployment.                                                        |
| **JWT**                 | JSON Web Token                                      | A compact, URL-safe token format for securely transmitting claims between parties.                                                |
| **OAuth**               | Open Authorization                                  | An open standard for delegated access, commonly used for third-party authentication and authorization.                            |
| **SLA**                 | Service Level Agreement                             | A contract between service provider and customer defining performance guarantees and support levels.                              |
| **TLS**                 | Transport Layer Security                            | A cryptographic protocol for securing communications over networks (successor to SSL).                                            |
| **UTC**                 | Coordinated Universal Time                          | A standard time reference used across systems and APIs, also known as GMT.                                                        |
| **UUID**                | Universally Unique Identifier                       | A 128-bit identifier guaranteed to be unique across all systems and time.                                                         |
| **OAM**                 | Operations, Administration, and Maintenance         | Functions and tools for managing and monitoring systems in production.                                                            |
| **SOC 2**               | Service Organization Control 2                      | A compliance framework for evaluating security, availability, and confidentiality controls.                                       |
| **FIPS**                | Federal Information Processing Standards            | US government standards for cryptographic algorithms and security modules.                                                        |
| **3DES**                | Triple Data Encryption Standard                     | A symmetric encryption algorithm applying DES encryption three times, now considered less secure than AES.                        |
| **OAEP**                | Optimal Asymmetric Encryption Padding               | A padding scheme for RSA encryption that provides semantic security.                                                              |
| **PSS**                 | Probabilistic Signature Scheme                      | A padding scheme for RSA signatures that provides probabilistic security guarantees.                                              |
| **RGBA**                | Red, Green, Blue, Alpha                             | A color model with red, green, blue components plus an alpha (transparency) channel.                                              |
| **Ciphertext**          | N/A                                                 | The encrypted output resulting from encrypting plaintext with an encryption algorithm and key.                                    |
| **Plaintext**           | N/A                                                 | Unencrypted, readable data before encryption is applied.                                                                          |
| **Encryption Context**  | N/A                                                 | Optional key-value pairs associated with an encryption operation to provide additional security and constraints.                  |
| **Envelope Encryption** | N/A                                                 | A pattern of encrypting data with a DEK and then encrypting the DEK with a KEK for improved performance and security.             |
| **Grant**               | N/A                                                 | A flexible permission mechanism in AWS KMS allowing temporary or delegated access to keys without modifying key policies.         |
| **Grant Token**         | N/A                                                 | A unique credential that can be used immediately after creating a grant, before the grant is fully propagated.                    |
| **Retiring Principal**  | N/A                                                 | A principal (user, role, or service) who has the ability to retire (remove) a grant they have been granted.                       |
| **Key Rotation**        | N/A                                                 | The process of replacing an old encryption key with a new one, limiting the amount of data encrypted with any single key version. |
| **Key Policy**          | N/A                                                 | A resource-based policy attached to a KMS key defining who can perform which operations on the key.                               |
| **Alias**               | N/A                                                 | A friendly, human-readable name for a KMS key (e.g., `alias/my-prod-db-key`) as an alternative to the key ID.                     |
| **Key Material**        | N/A                                                 | The actual cryptographic key data (bits/bytes) that is used to encrypt and decrypt information.                                   |
| **Key State**           | N/A                                                 | The current status of a key: Enabled, Disabled, PendingDeletion, or PendingImport.                                                |
| **Custom Key Store**    | N/A                                                 | A KMS feature that stores key material in a customer-managed CloudHSM cluster or external proxy (XKS).                            |
| **Digital Signature**   | N/A                                                 | A cryptographic mechanism that proves a message was created by a specific entity and has not been altered.                        |
| **Asymmetric Key**      | N/A                                                 | A cryptographic key pair (public and private keys) where the private key encrypts/signs and the public key decrypts/verifies.     |
| **Symmetric Key**       | N/A                                                 | A single cryptographic key used for both encryption and decryption operations.                                                    |
| **Key Spec**            | N/A                                                 | The specification of a key describing its type, size, and algorithm (e.g., RSA_2048, ECC_NIST_P256, SYMMETRIC_DEFAULT).           |
| **Pending Deletion**    | N/A                                                 | A key state during the scheduled deletion waiting period where the key cannot be used for cryptographic operations.               |
| **Pending Import**      | N/A                                                 | A key state waiting for imported key material to be imported via the BYOK process.                                                |

---

## Glossary Notes

- **Encryption Context**: In many cryptographic operations, additional authenticated data that constrains when and how a
  key can be used. For example, requiring that data encrypted with a certain encryption context can only be decrypted
  with the same context.

- **Grant Constraints**: When creating a grant, you can specify constraints that limit the operations allowed. For
  example, an `EncryptionContextSubset` constraint requires that specified encryption context keys and values must be
  present in any operation using the grant.

- **Key Material**: The actual secret data that comprises a cryptographic key. In BYOK scenarios, you control the
  generation and import of key material. In AWS-managed scenarios, AWS generates and manages key material.

- **Multi-Region Keys**: A feature allowing a primary key in one region to be replicated to secondary regions, enabling
  encryption in one region and decryption in another.

- **Origin**: Indicates where key material comes from. `AWS_KMS` means AWS generates the material; `EXTERNAL` means you
  provide the material via BYOK.

---

## Related Standards & Regulations

| Standard             | Description                                               | Use Case                                                |
|----------------------|-----------------------------------------------------------|---------------------------------------------------------|
| **NIST SP 800-57**   | NIST guidelines for cryptographic key management          | Best practices for key lifecycle management             |
| **ISO 27001**        | Information security management systems standard          | General information security compliance                 |
| **SOC 2 Type II**    | Controls over security, availability, and confidentiality | Third-party security audits and attestations            |
| **PCI-DSS v3.2**     | Payment Card Industry Data Security Standard              | Handling credit card data securely                      |
| **HIPAA**            | Health Insurance Portability and Accountability Act       | Protected health information (PHI) security             |
| **GDPR**             | General Data Protection Regulation                        | Personal data protection in the EU                      |
| **FedRAMP**          | Federal Risk and Authorization Management Program         | Security authorization for US government cloud services |
| **FIPS 140-2/140-3** | Federal Information Processing Standards                  | Cryptographic module validation and approval            |

---

## Common Key Use Cases by Industry

| Industry               | Primary Use Cases                                                                 |
|------------------------|-----------------------------------------------------------------------------------|
| **Financial Services** | Payment processing, transaction signing, regulatory compliance (PCI-DSS, SOX)     |
| **Healthcare**         | Patient data encryption, PHI protection (HIPAA compliance), audit trails          |
| **E-Commerce**         | Payment card protection, customer data encryption, fraud prevention               |
| **SaaS**               | Multi-tenant data isolation, customer data protection, regulatory compliance      |
| **Government**         | Classified data protection, FIPS compliance, federal regulation adherence         |
| **Manufacturing**      | Supply chain data protection, intellectual property security, IoT data encryption |
| **Retail**             | POS transaction security, customer loyalty data, inventory data protection        |
