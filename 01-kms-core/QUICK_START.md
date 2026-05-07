# KMS API Quick Start Guide

## 🚀 Getting Started

### Prerequisites

- Java 11+
- Spring Boot 2.7+
- Maven 3.6+
- Postman or cURL

### Base URL

```
http://localhost:8080/api/v1/private/key
```

---

## 📚 Common API Examples

### 1. Create a Key

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/private/key/keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "keySpec": "AES_256",
    "purpose": "ENCRYPT_DECRYPT",
    "alias": "app-db-key",
    "description": "Key for database field encryption"
  }'
```

**Response:**

```json
{
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "arn": "arn:kms:tenant-1:key/key-123e4567-e89b-12d3-a456-426614174000",
  "status": "ENABLED",
  "createdAt": "2024-05-06T10:30:00"
}
```

**✅ Copy the `keyId` for use in other operations**

---

### 2. Get Key Metadata

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "status": "ENABLED",
  "keySpec": "AES_256",
  "purpose": "ENCRYPT_DECRYPT",
  "currentVersion": "v-1",
  "createdAt": "2024-05-06T10:30:00",
  "alias": "app-db-key",
  "description": "Key for database field encryption"
}
```

---

### 3. List All Keys

**Request:**

```bash
curl -X GET "http://localhost:8080/api/v1/private/key/keys?limit=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "keys": [
    {
      "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
      "alias": "app-db-key",
      "status": "ENABLED"
    },
    {
      "keyId": "key-987f6543-e89b-12d3-a456-426614174111",
      "alias": "app-api-key",
      "status": "ENABLED"
    }
  ],
  "nextToken": "token-for-pagination"
}
```

---

### 4. Encrypt Data

**Request:**

```bash
# First, encode your data in base64
# Example: "Hello World" = "SGVsbG8gV29ybGQ="

curl -X POST http://localhost:8080/api/v1/private/key/encrypt \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
    "plaintext": "SGVsbG8gV29ybGQ=",
    "encryptionContext": {
      "department": "finance",
      "dataType": "user-ssn"
    }
  }'
```

**Response:**

```json
{
  "ciphertext": "AQIDBAUG...encrypted-data...XYZDQ==",
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "keyVersion": "v-1"
}
```

**✅ Store the `ciphertext` securely**

---

### 5. Decrypt Data

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/private/key/decrypt \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "ciphertext": "AQIDBAUG...encrypted-data...XYZDQ=="
  }'
```

**Response:**

```json
{
  "plaintext": "SGVsbG8gV29ybGQ=",
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "keyVersion": "v-1"
}
```

**✅ Decode from base64: "SGVsbG8gV29ybGQ=" = "Hello World"**

---

### 6. Re-encrypt Data

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/private/key/reencrypt \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "ciphertext": "AQIDBAUG...encrypted-data...XYZDQ==",
    "destinationKeyId": "key-987f6543-e89b-12d3-a456-426614174111"
  }'
```

**Response:**

```json
{
  "ciphertext": "XYZCBA...new-encrypted-data...ZYXWVU==",
  "sourceKeyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "destinationKeyId": "key-987f6543-e89b-12d3-a456-426614174111"
}
```

---

### 7. Sign a Message

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/private/key/sign \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "keyId": "key-rsa-public-key-id",
    "message": "RmlsZSBjb250ZW50IHRvIHNpZ24=",
    "algorithm": "RSASSA_PSS_SHA256"
  }'
```

**Response:**

```json
{
  "signature": "SIGNATURE_BASE64_ENCODED...",
  "keyId": "key-rsa-public-key-id"
}
```

**✅ Share the signature with file/document**

---

### 8. Verify Signature

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/private/key/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "keyId": "key-rsa-public-key-id",
    "message": "RmlsZSBjb250ZW50IHRvIHNpZ24=",
    "signature": "SIGNATURE_BASE64_ENCODED..."
  }'
```

**Response:**

```json
{
  "valid": true
}
```

---

### 9. Enable Key

**Request:**

```bash
curl -X PATCH http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000/enable \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "status": "ENABLED"
}
```

---

### 10. Disable Key

**Request:**

```bash
curl -X PATCH http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000/disable \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "status": "DISABLED"
}
```

---

### 11. Rotate Key

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000/rotate \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "newVersion": "v-2",
  "rotationDate": "2024-05-06T10:35:00"
}
```

---

### 12. Schedule Key Deletion

**Request:**

```bash
curl -X DELETE "http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000?pendingWindowInDays=7" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "status": "PENDING_DELETION",
  "deletionDate": "2024-05-13T10:30:00"
}
```

---

### 13. Generate Data Key

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/private/key/datakey/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
    "keySize": 256
  }'
```

**Response:**

```json
{
  "plaintextKey": "base64-encoded-256-bit-key",
  "encryptedKey": "base64-encoded-encrypted-key",
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000"
}
```

**✅ Use `plaintextKey` for encryption, store `encryptedKey`**

---

### 14. Create Grant

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000/grants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "principal": "arn:aws:iam::123456789012:user/alice",
    "operations": ["encrypt", "decrypt"]
  }'
```

**Response:**

```json
{
  "grantId": "grant-87654321-dcba-98765-fedcba-987654321000",
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000"
}
```

---

### 15. Revoke Grant

**Request:**

```bash
curl -X DELETE http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000/grants/grant-87654321-dcba-98765-fedcba-987654321000 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "status": "REVOKED"
}
```

---

### 16. List Key Versions

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000/versions \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

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
      "createdAt": "2024-05-06T10:35:00",
      "status": "INACTIVE"
    }
  ]
}
```

---

### 17. Get Active Version

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000/active-version \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "versionId": "v-2"
}
```

---

### 18. Set Key Policy

**Request:**

```bash
curl -X PUT http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000/policy \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "policy": {
      "Version": "2023-01-01",
      "Statement": [
        {
          "Effect": "Allow",
          "Principal": {
            "AWS": "arn:aws:iam::123456789012:user/alice"
          },
          "Action": ["kms:Decrypt", "kms:Encrypt"],
          "Resource": "*"
        }
      ]
    }
  }'
```

**Response:**

```json
{
  "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
  "status": "UPDATED"
}
```

---

### 19. Get Key Policy

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/private/key/keys/key-123e4567-e89b-12d3-a456-426614174000/policy \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "Version": "2023-01-01",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::123456789012:user/alice"
      },
      "Action": ["kms:Decrypt", "kms:Encrypt"],
      "Resource": "*"
    }
  ]
}
```

---

### 20. Get Audit Logs

**Request:**

```bash
curl -X GET "http://localhost:8080/api/v1/private/key/audit/logs?keyId=key-123e4567-e89b-12d3-a456-426614174000&limit=50&fromDate=2024-05-01T00:00:00&toDate=2024-05-06T23:59:59" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**

```json
{
  "logs": [
    {
      "action": "CREATE_KEY",
      "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
      "timestamp": "2024-05-06T10:30:00",
      "principal": "admin@example.com",
      "ip": "192.168.1.100"
    },
    {
      "action": "ENCRYPT",
      "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
      "timestamp": "2024-05-06T10:31:00",
      "principal": "app-service@example.com",
      "ip": "192.168.1.50"
    },
    {
      "action": "ENCRYPT",
      "keyId": "key-123e4567-e89b-12d3-a456-426614174000",
      "timestamp": "2024-05-06T10:32:00",
      "principal": "app-service@example.com",
      "ip": "192.168.1.50"
    }
  ]
}
```

---

## 🧪 Testing with Postman

### 1. Import Collection

- Create new Collection "KMS API"
- Create folder "Key Management"
- Add requests for each endpoint above

### 2. Set Variables

```
{{baseUrl}} = http://localhost:8080/api/v1/private/key
{{token}} = YOUR_BEARER_TOKEN
{{keyId}} = key-123e4567-e89b-12d3-a456-426614174000
```

### 3. Use in Requests

```
URL: {{baseUrl}}/keys/{{keyId}}
Header: Authorization: Bearer {{token}}
```

---

## 🔧 Base64 Encoding Examples

### Encode String to Base64

```bash
# Linux/Mac
echo -n "Hello World" | base64
# Output: SGVsbG8gV29ybGQ=

# PowerShell
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("Hello World"))
# Output: SGVsbG8gV29ybGQ=
```

### Decode Base64 to String

```bash
# Linux/Mac
echo "SGVsbG8gV29ybGQ=" | base64 -d
# Output: Hello World

# PowerShell
[System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String("SGVsbG8gV29ybGQ="))
# Output: Hello World
```

---

## 📊 API Testing Workflow

### Complete Encryption Workflow

```
1. Create Key
   ↓
2. Get Key Metadata
   ↓
3. Encrypt Data
   ↓
4. Decrypt Data
   ↓
5. Rotate Key
   ↓
6. Re-encrypt with new key
   ↓
7. View Audit Logs
```

### Complete Signing Workflow

```
1. Create RSA Key (purpose: SIGN_VERIFY)
   ↓
2. Sign Message
   ↓
3. Verify Signature
   ↓
4. View Audit Logs
```

### Access Control Workflow

```
1. Create Key
   ↓
2. Create Grant (principal)
   ↓
3. Set Key Policy
   ↓
4. Get Key Policy
   ↓
5. Revoke Grant
```

---

## ✅ Checklist for Testing

- [ ] Create a key successfully
- [ ] List created keys
- [ ] Get key metadata
- [ ] Encrypt and decrypt data
- [ ] Verify round-trip encryption
- [ ] Create and revoke grants
- [ ] Set and get policies
- [ ] Sign and verify messages
- [ ] Generate data keys
- [ ] Rotate keys
- [ ] Enable/disable keys
- [ ] Check audit logs
- [ ] Schedule key deletion
- [ ] Test error scenarios (invalid key, missing params, etc.)

---

## 🐛 Troubleshooting

### Error: 404 Key Not Found

```
Cause: KeyId doesn't exist
Solution: Create the key first or use correct keyId
```

### Error: 400 Bad Request

```
Cause: Invalid request format or missing required fields
Solution: Check request body matches specification
```

### Error: 401 Unauthorized

```
Cause: Missing or invalid authentication token
Solution: Add Authorization header with valid token
```

### Error: 500 Internal Server Error

```
Cause: Server error (usually in mock implementation or database)
Solution: Check server logs and implementation
```

---

## 📞 Support Resources

- API Documentation: `KMS_API_DOCUMENTATION.md`
- Implementation Guide: `IMPLEMENTATION_GUIDE.md`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Code Comments: Check Java source files

---

**Ready to test the APIs? Start with Example #1 (Create a Key)!** 🎉

