# KMS-Core Developer Quick Reference

**Quick Links:**

- 📚 [Full Implementation Status](./IMPLEMENTATION_STATUS_SUMMARY.md)
- 📖 [Service Implementation Details](./SERVICE_IMPLEMENTATION_GUIDE.md)
- 🔗 [Complete API Reference](./API_REFERENCE_WITH_USE_CASES.md)
- ✅ [Implementation Analysis](./COMPLETION_ANALYSIS.md)

---

## Project Structure

```
01-kms-core/
├── 01-kms-jpa/                    # Persistence Layer
│   └── src/main/java/eu/isygoit/
│       ├── model/                 # JPA Entities (5 entities)
│       │   ├── KmsKey.java
│       │   ├── KmsKeyVersion.java
│       │   ├── KmsKeyGrant.java
│       │   ├── KmsKeyPolicy.java
│       │   └── KmsAuditLog.java
│       └── repository/            # Spring Data Repositories (5 repos)
│           ├── KmsKeyRepository.java
│           ├── KmsKeyVersionRepository.java
│           ├── KmsKeyGrantRepository.java
│           ├── KmsKeyPolicyRepository.java
│           └── KmsAuditLogRepository.java
│
├── 02-kms-shared/                 # API & Shared Layer
│   └── src/main/java/eu/isygoit/
│       ├── api/
│       │   └── KmsServiceApi.java # Interface with 44 endpoints
│       ├── dto/
│       │   ├── request/           # 15+ Request DTOs
│       │   └── response/          # 15+ Response DTOs
│       ├── enums/                 # Key enums
│       ├── constants/             # KMS constants
│       └── exception/             # Custom exceptions
│
├── 03-kms-starter-parent/         # Application Layer
│   └── src/main/java/eu/isygoit/
│       ├── controller/
│       │   └── KmsController.java # 44 endpoints (1089 lines)
│       ├── service/               # 9 Services + 9 Implementations
│       │   ├── impl/
│       │   │   ├── KeyManagementServiceImpl.java
│       │   │   ├── EncryptionServiceImpl.java
│       │   │   ├── SigningServiceImpl.java
│       │   │   ├── DataKeyServiceImpl.java
│       │   │   ├── KeyPolicyServiceImpl.java
│       │   │   ├── KeyVersionServiceImpl.java
│       │   │   ├── AuditServiceImpl.java
│       │   │   ├── MultiRegionService.java
│       │   │   └── CustomKeyStoreService.java
│       │   └── interfaces/
│       ├── config/
│       │   └── OpenApiConfiguration.java # NEW: Global API doc
│       └── exception/
│           └── KmsExceptionHandler.java
│
├── IMPLEMENTATION_STATUS_SUMMARY.md    # ← START HERE
├── SERVICE_IMPLEMENTATION_GUIDE.md
├── API_REFERENCE_WITH_USE_CASES.md
├── COMPLETION_ANALYSIS.md
├── AWS_KMS_ALIGNMENT_REPORT.md
├── README.md
└── QUICK_START_GUIDE.md
```

---

## Core Classes at a Glance

### 1. KmsController (1089 lines)

**44 REST Endpoints across 10 categories:**

| Category     | Endpoints | File Location   |
|--------------|-----------|-----------------|
| Key Mgmt     | 10        | Lines 101-247   |
| Encryption   | 3         | Lines 323-369   |
| Envelope     | 4         | Lines 372-433   |
| Signing      | 2         | Lines 435-465   |
| MAC          | 2         | Lines 467-497   |
| Rotation     | 4         | Lines 252-317   |
| Aliases      | 5         | Lines 605-685   |
| Policies     | 7         | Lines 691-791   |
| Tagging      | 3         | Lines 797-845   |
| Import       | 3         | Lines 851-897   |
| Custom KS    | 5         | Lines 904-984   |
| Audit        | 4         | Lines 1022-1089 |
| Versioning   | 2         | Lines 519-549   |
| Multi-Region | 3         | Lines 551-599   |

**Key Patterns:**

```java

@Override
public ResponseEntity<ResponseDto> methodName(...) {
    log.info("Logging message");
    try {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        ResponseDto response = serviceImpl.methodName(tenant, request);
        auditService.logAction(tenant, IKmsActionType.Types.ACTION_NAME, keyId, user, ip);
        return ResponseFactory.responseOk(response);
    } catch (Throwable e) {
        log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
        return getBackExceptionResponse(e);
    }
}
```

---

### 2. KmsServiceApi (1578 lines - Interface)

**All methods include:**

- `@PostMapping`, `@GetMapping`, `@PutMapping`, `@DeleteMapping`
- `@Operation(summary="...", description="...", operationId="...")`
- `@ApiResponses(value={@ApiResponse(...), ...})`
- `@Parameter(description="...", required=true/false, example="...")`
- Request/response examples with Base64 encoding

**Example:**

```java

@PostMapping("/encrypt")
@Operation(summary = "Encrypt", description = "...", operationId = "encrypt")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Encryption successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Key disabled")
})
ResponseEntity<EncryptResponseDto> encrypt(@RequestBody EncryptRequestDto request);
```

---

### 3. Service Implementations (9 Services)

#### KeyManagementServiceImpl (Main lifecycle service)

```java
// Key CRUD
createKey(tenant, request) →

CreateKeyResponseDto
describeKey(tenant, keyId) →

KeyMetadataResponseDto
listKeys(tenant, limit, nextToken) →

ListKeysResponseDto
updateKeyMetadata(tenant, keyId, request) →

KeyMetadataResponseDto

// Key State
enableKey(tenant, keyId) →

KeyMetadataResponseDto
disableKey(tenant, keyId) →

KeyMetadataResponseDto
scheduleKeyDeletion(tenant, keyId, days) →

KeyMetadataResponseDto
cancelKeyDeletion(tenant, keyId) →

KeyMetadataResponseDto
deleteKey(tenant, keyId) →

void

// Aliases
createAlias(tenant, request) →

AliasResponseDto
updateAlias(tenant, aliasName, request) →

AliasResponseDto
deleteAlias(tenant, aliasName) →

void
listAliases(tenant, limit, nextToken) →

ListAliasesResponseDto
listAliasesForKey(tenant, keyId) →

ListAliasesResponseDto

// Rotation
rotateKey(tenant, keyId) →

RotateKeyResponseDto
updateKeyRotation(tenant, keyId, request) →

KeyRotationStatusDto
getKeyRotationStatus(tenant, keyId) →

KeyRotationStatusDto
listKeyRotations(tenant, keyId, limit, nextToken) →

ListKeyRotationsResponseDto

// Tagging
tagResource(tenant, keyId, request) →

void
untagResource(tenant, keyId, request) →

void
listResourceTags(tenant, keyId) →

ListTagsResponseDto

// BYOK
getParametersForImport(tenant, keyId) →

ImportParametersResponseDto
importKeyMaterial(tenant, keyId, request) →

KeyMetadataResponseDto
deleteImportedKeyMaterial(tenant, keyId) →

KeyMetadataResponseDto

// Utility
getPublicKey(tenant, keyId) →

PublicKeyResponseDto
getKeyUsageStats(tenant, keyId) →

KeyUsageStatsDto
validateKey(tenant, keyId) →void
```

#### EncryptionServiceImpl (Crypto operations)

```java
encrypt(tenant, request) →EncryptResponseDto
  -
Lookup CMK
by keyId
  -
Validate key
status and
purpose
  -

Select algorithm(AES/RSA)
  -
Encrypt plaintext
with encryption
context
  -
Return ciphertext +

metadata

decrypt(tenant, request) →DecryptResponseDto
  -
Extract key
metadata from
ciphertext
  -
Lookup CMK
and version
  -
Validate encryption
context
  -
Decrypt and return

plaintext

reencrypt(tenant, request) →ReEncryptResponseDto
  -
Decrypt under
source key
  -Re-
encrypt under
destination key
  -
Plaintext never
exposed in
response
```

#### SigningServiceImpl (Digital signatures & MAC)

```java
sign(tenant, request) →SignResponseDto
  -
Get asymmetric

key(RSA/ECC)
  -
Select signing
algorithm
  -
Sign message/digest
  -
Return signature

verify(tenant, request) →VerifyResponseDto
  -
Validate signature
  -Return
boolean result

generateMac(tenant, request) →GenerateMacResponseDto
  -
Generate HMAC
with symmetric

key

verifyMac(tenant, request) →VerifyMacResponseDto
  -
Verify HMAC
```

#### DataKeyServiceImpl (Envelope encryption)

```java
generateDataKey(tenant, request) →DataKeyResponseDto
  -
Generate random
AES key
  -
Encrypt key
under CMK
  -
Return plaintext +
encrypted wrapper

generateDataKeyWithoutPlaintext(tenant, request) →DataKeyResponseDto
  -
Generate random
AES key
  -
Encrypt key
under CMK
  -
Return only
encrypted wrapper

generateDataKeyPair(tenant, request) →

DataKeyPairResponseDto
generateDataKeyPairWithoutPlaintext(tenant, request) →DataKeyPairResponseDto
  -
Generate RSA/
ECC key
pair
  -Wrap private key
  -
Return with/
without plaintext
private key
```

#### KeyPolicyServiceImpl (Access control)

```java
setKeyPolicy(tenant, keyId, request) →

void
getKeyPolicy(tenant, keyId) →

Object(JSON)

createGrant(tenant, keyId, request) →

GrantResponseDto
listGrants(tenant, keyId, ...) →

ListGrantsResponseDto
revokeGrant(tenant, keyId, grantId) →

void
retireGrant(tenant, grantId, request) →void
```

#### KeyVersionServiceImpl (Rotation history)

```java
listKeyVersions(tenant, keyId) →

KeyVersionListResponseDto
getActiveVersion(tenant, keyId) →ActiveVersionResponseDto
```

#### AuditServiceImpl (Logging)

```java
logAction(tenant, action, keyId, user, ip) →void
  -
Insert into
KmsAuditLog
  -
        Capture timestamp, status

getAuditLogs(tenant, keyId, from, to, limit) →AuditLogResponseDto
  -
Query with
filters
  -
Return paginated
results
```

---

### 4. Database Entities

**KmsKey (Main key table)**

```sql
CREATE TABLE T_KMS_KEY
(
    ID                           BIGINT PRIMARY KEY,
    TENANT                       VARCHAR(100) NOT NULL,
    KEY_ID                       BIGINT       NOT NULL,
    KEY_ARN                      VARCHAR(255) NOT NULL,
    KEY_SPEC                     VARCHAR(50)  NOT NULL, -- AES_256, RSA_2048, EC_P256, etc.
    KEY_PURPOSE                  VARCHAR(50)  NOT NULL, -- ENCRYPT_DECRYPT, SIGN_VERIFY
    STATUS                       VARCHAR(50)  NOT NULL, -- ENABLED, DISABLED, PENDING_DELETION
    CURRENT_VERSION_ID           VARCHAR(255),
    ROTATION_ENABLED             BOOLEAN   DEFAULT FALSE,
    ROTATION_PERIOD_DAYS         INTEGER,
    LAST_ROTATION_DATE           TIMESTAMP,
    DELETION_DATE                TIMESTAMP,
    PENDING_DELETION_WINDOW_DAYS INTEGER,
    KEY_MATERIAL                 BLOB         NOT NULL, -- Encrypted
    KEY_MATERIAL_ENCRYPTED       BOOLEAN   DEFAULT TRUE,
    CREATION_DATE                TIMESTAMP    NOT NULL,
    UPDATED_AT                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Unique constraint
    UNIQUE (TENANT, KEY_ID),
    -- Indexes
    INDEX                        IDX_KMS_KEY_TENANT_STATUS (TENANT, STATUS),
    INDEX                        IDX_KMS_KEY_ALIAS (TENANT, ALIAS)
);
```

**KmsKeyVersion (Rotation history)**

```sql
CREATE TABLE T_KMS_KEY_VERSION
(
    ID                BIGINT PRIMARY KEY,
    TENANT            VARCHAR(100) NOT NULL,
    KEY_ID            BIGINT       NOT NULL, -- FK to T_KMS_KEY
    VERSION_ID        VARCHAR(255) NOT NULL,
    STATUS            VARCHAR(50)  NOT NULL, -- ACTIVE, INACTIVE
    KEY_MATERIAL      BLOB         NOT NULL,
    CREATION_DATE     TIMESTAMP    NOT NULL,
    ACTIVATION_DATE   TIMESTAMP,
    DEACTIVATION_DATE TIMESTAMP,
    ROTATION_DATE     TIMESTAMP,
    -- Foreign key
    FOREIGN KEY (KEY_ID) REFERENCES T_KMS_KEY (ID),
    -- Unique
    UNIQUE (TENANT, KEY_ID, VERSION_ID)
);
```

**KmsAuditLog (Compliance audit)**

```sql
CREATE TABLE T_KMS_AUDIT_LOG
(
    ID                BIGINT PRIMARY KEY,
    TENANT            VARCHAR(100) NOT NULL,
    KEY_ID            BIGINT,
    ACTION            VARCHAR(100) NOT NULL, -- CREATE_KEY, ENCRYPT, DECRYPT, etc.
    PRINCIPAL         VARCHAR(255) NOT NULL, -- User/service performing action
    IP_ADDRESS        VARCHAR(50),
    TIMESTAMP         TIMESTAMP    NOT NULL,
    STATUS            VARCHAR(50)  NOT NULL, -- SUCCESS, FAILURE
    ERROR_MESSAGE     VARCHAR(1000),
    REQUEST_DETAILS   VARCHAR(2000),
    RESPONSE_DETAILS  VARCHAR(2000),
    EXECUTION_TIME_MS BIGINT,
    -- Indexes for queries
    INDEX             IDX_KMS_AUDIT_KEY (KEY_ID),
    INDEX             IDX_KMS_AUDIT_ACTION (TENANT, ACTION),
    INDEX             IDX_KMS_AUDIT_TIMESTAMP (TIMESTAMP),
    INDEX             IDX_KMS_AUDIT_PRINCIPAL (PRINCIPAL)
);
```

---

## Common Operations

### Encrypt Data

```bash
POST /api/v1/private/key/encrypt
{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "plaintext": "U2VjcmV0RGF0YQ==",
  "encryptionContext": {"service": "payment-processor"}
}
```

### Decrypt Data

```bash
POST /api/v1/private/key/decrypt
{
  "ciphertextBlob": "AQIDAAH4eHh4eHg...",
  "encryptionContext": {"service": "payment-processor"}
}
```

### Create Signing Key

```bash
POST /api/v1/private/key/keys
{
  "description": "API signing key",
  "keySpec": "RSA_2048",
  "keyUsage": "SIGN_VERIFY"
}
```

### Sign Document

```bash
POST /api/v1/private/key/sign
{
  "keyId": "...",
  "message": "SGVsbG8gV29ybGQ=",
  "signingAlgorithm": "RSASSA_PSS_SHA_256"
}
```

### Generate Data Key for Envelope Encryption

```bash
POST /api/v1/private/key/datakey/generate
{
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "keySpec": "AES_256"
}
```

---

## Configuration Required

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kmsdb
    username: kms_user
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for migrations
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

kms:
  audit:
    retention-days: 90
  crypto:
    algorithm: AES_256_GCM
  multitenancy:
    enabled: true

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
  use-interface-api-doc: true
```

---

## Error Handling

### Custom Exceptions

```java
KeyNotFoundException -404
Not Found
KeyDisabledException -409Conflict
InvalidKeyPurposeException -400
Bad Request
InvalidKeyStateException -409Conflict
EncryptionException -500
Internal Server
Error
SigningException -500
Internal Server
Error
AuditException -500
Internal Server
Error
UnauthorizedException -403Forbidden
```

### Response Format

```json
{
  "error": {
    "code": "KEY_NOT_FOUND",
    "message": "Key with ID 123 not found",
    "requestId": "req-12345",
    "timestamp": "2026-05-07T10:30:00Z"
  }
}
```

---

## Testing

### Test Service Layer

```java

@SpringBootTest
public class KeyManagementServiceImplTest {
    @MockBean
    private KmsKeyRepository keyRepository;

    @InjectMocks
    private KeyManagementServiceImpl service;

    @Test
    public void testCreateKey() {
        CreateKeyRequestDto request = new CreateKeyRequestDto();
        CreateKeyResponseDto response = service.createKey("tenant", request);
        assertNotNull(response.getKeyId());
    }
}
```

### Integration Test

```java

@SpringBootTest
public class KmsControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testEncrypt() throws Exception {
        mockMvc.perform(post("/api/v1/private/key/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}
```

---

## Performance Optimization Tips

1. **Enable Query caching for key metadata**
   ```java
   @Cacheable(value = "keys", key = "#keyId")
   public Key getKey(String tenant, Long keyId) { ... }
   ```

2. **Batch encrypt/decrypt operations**
    - Implement batch endpoints
    - Reduce network round trips

3. **Connection pooling**
   ```yaml
   spring.datasource.hikari.maximum-pool-size: 20
   ```

4. **Async audit logging**
    - Use `@Async` for auditService.logAction()
    - Don't block crypto operations

---

## Debugging Tips

1. **Enable debug logging:**
   ```yaml
   logging.level.eu.isygoit: DEBUG
   logging.level.org.hibernate.SQL: DEBUG
   ```

2. **Check audit logs:**
   ```sql
   SELECT * FROM T_KMS_AUDIT_LOG 
   WHERE TENANT = 'tenant' 
   ORDER BY TIMESTAMP DESC LIMIT 100;
   ```

3. **Validate key state:**
   ```bash
   GET /api/v1/private/key/keys/{keyId}
   ```

4. **Check grant permissions:**
   ```bash
   GET /api/v1/private/key/keys/{keyId}/grants
   ```

---

## Quick Command Reference

```bash
# Build
mvn clean package

# Run
java -jar target/kms-service.jar

# Test
mvn test

# Coverage
mvn jacoco:report

# Docker build (from 09-docker/)
docker build -f core-kms.Dockerfile -t kms-service:1.0 .

# Run with Docker
docker run -p 8080:8080 -e DB_URL=jdbc:mysql://db:3306/kms kms-service:1.0
```

---

## Swagger/OpenAPI Access

**After starting the service:**

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

---

## Documentation References

| Document                         | Purpose                        | Audience       |
|----------------------------------|--------------------------------|----------------|
| IMPLEMENTATION_STATUS_SUMMARY.md | Complete status overview       | Everyone       |
| SERVICE_IMPLEMENTATION_GUIDE.md  | Service implementation details | Developers     |
| API_REFERENCE_WITH_USE_CASES.md  | API usage examples             | API consumers  |
| AWS_KMS_ALIGNMENT_REPORT.md      | AWS KMS compatibility          | Architects     |
| QUICK_START_GUIDE.md             | 5-minute quick start           | New developers |
| README.md                        | Module overview                | All            |

---

**Last Updated:** 2026-05-07  
**Status:** ✅ Production Ready (95% Complete)  
**Version:** 1.0.260408-T1636


