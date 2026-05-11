# KMS Implementation Guide

## What Has Been Implemented

This guide explains what's been implemented and what needs to be customized for your specific use case.

## 1. Structure Overview

```
02-wms-core/
├── 01-kms-core/
│   ├── 01-kms-jpa/          # JPA/Database layer (TO BE IMPLEMENTED)
│   ├── 02-kms-shared/       # Shared DTOs, Enums, Constants
│   │   ├── dto/request/     # ✅ Created: 9 Request DTOs
│   │   ├── dto/response/    # ✅ Created: 10 Response DTOs
│   │   ├── enums/           # ✅ Created: 4 Enums
│   │   ├── constants/       # ✅ Created: KmsConstants
│   │   └── api/             # ✅ Created: IKmsServiceApi
│   └── 03-kms-starter-parent/
│       ├── controller/      # ✅ Updated: KeyController (all 20 endpoints)
│       └── service/         # ✅ Created: 7 Service Interfaces + Implementations
```

## 2. What's Already Been Done ✅

### A. DTOs & Data Models

- ✅ Request DTOs: CreateKeyRequest, EncryptRequest, DecryptRequest, ReencryptRequest, SignRequest, VerifyRequest,
  SetKeyPolicyRequest, CreateGrantRequest, GenerateDataKeyRequest
- ✅ Response DTOs: KeyMetadataResponse, CreateKeyResponse, EncryptResponse, DecryptResponse, ListKeysResponse,
  RotateKeyResponse, SignResponse, VerifyResponse, GrantResponse, DataKeyResponse, KeyVersionListResponse,
  ActiveVersionResponse, AuditLogResponse
- ✅ Validation annotations on all DTOs

### B. Enums

- ✅ IEnumKeySpec: AES_256, RSA_2048, EC_P256
- ✅ IEnumKeyUsage: ENCRYPT_DECRYPT, SIGN_VERIFY
- ✅ IEnumKeyStatus: ENABLED, DISABLED, PENDING_DELETION
- ✅ IEnumSigningAlgorithm: RSASSA_PSS_SHA256, ECDSA_SHA256

### C. Service Layer (Mock Implementations)

- ✅ IKeyManagementService + KeyManagementServiceImpl
- ✅ IEncryptionService + EncryptionServiceImpl
- ✅ ISigningService + SigningServiceImpl
- ✅ IKeyPolicyService + KeyPolicyServiceImpl
- ✅ IKeyVersionService + KeyVersionServiceImpl
- ✅ IDataKeyService + DataKeyServiceImpl
- ✅ IAuditService + AuditServiceImpl

### D. REST Controller

- ✅ KeyController with all 20 endpoints
- ✅ Swagger/OpenAPI annotations
- ✅ Exception handling
- ✅ Request validation

### E. Documentation

- ✅ Complete API documentation
- ✅ KMS constants
- ✅ Code examples

## 3. What Needs to Be Implemented (TODO)

### A. Database Layer (01-kms-jpa)

You need to implement JPA entities for persistence:

```java
// Create in 01-kms-jpa/src/main/java/eu/isygoit/model/

// 1. Key Entity
@Entity
@Table(name = "kms_keys")
public class Key extends AuditableEntity<Long> implements ITenantAssignable {
    private String keyId;
    private String alias;
    private String description;
    private IEnumKeySpec.Types keySpec;
    private IEnumKeyUsage.Types purpose;
    private IEnumKeyStatus.Types status;
    private String currentVersion;
    private LocalDateTime scheduledDeletionDate;
    // ... getters, setters
}

// 2. KeyVersion Entity
@Entity
@Table(name = "kms_key_versions")
public class KeyVersion extends AuditableEntity<Long> {
    private String versionId;
    private String keyId;      // FK to Key
    private String keyMaterial; // Encrypted key material
    private String status;   // ACTIVE, INACTIVE
    // ... getters, setters
}

// 3. KeyGrant Entity
@Entity
@Table(name = "kms_key_grants")
public class KeyGrant extends AuditableEntity<Long> {
    private String grantId;
    private String keyId;      // FK to Key
    private String principal;
    private List<String> operations;
    // ... getters, setters
}

// 4. KeyPolicy Entity
@Entity
@Table(name = "kms_key_policies")
public class KeyPolicy extends AuditableEntity<Long> {
    private String keyId;      // FK to Key
    private String policy;   // JSON stored as string or JSONB
    // ... getters, setters
}

// 5. AuditLog Entity
@Entity
@Table(name = "kms_audit_logs")
public class AuditLog extends AuditableEntity<Long> {
    private String action;
    private String keyId;
    private String principal;
    private String ip;
    private LocalDateTime timestamp;
    // ... getters, setters
}
```

### B. Repository Layer (01-kms-jpa)

Create repositories for data access:

```java
// Create in 01-kms-jpa/src/main/java/eu/isygoit/repository/

public interface KeyRepository extends JpaRepository<Key, Long>, JpaSpecificationExecutor<Key> {
    Optional<Key> findByKeyId(String keyId);

    Optional<Key> findByAlias(String alias);

    List<Key> findByTenant(String tenant);
}

public interface KeyVersionRepository extends JpaRepository<KeyVersion, Long> {
    List<KeyVersion> findByKeyId(String keyId);

    Optional<KeyVersion> findByVersionId(String versionId);
}

public interface KeyGrantRepository extends JpaRepository<KeyGrant, Long> {
    List<KeyGrant> findByKeyId(String keyId);

    Optional<KeyGrant> findByGrantId(String grantId);
}

public interface KeyPolicyRepository extends JpaRepository<KeyPolicy, Long> {
    Optional<KeyPolicy> findByKeyId(String keyId);
}

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByKeyIdAndTimestampBetween(String keyId, LocalDateTime from, LocalDateTime to);

    List<AuditLog> findByTenantAndTimestampBetween(String tenant, LocalDateTime from, LocalDateTime to);
}
```

### C. Implement Actual Cryptographic Operations

Replace mock implementations in service layer:

```java
// In EncryptionServiceImpl.encrypt()
// TODO: Implement with actual encryption library (e.g., Bouncy Castle, TweetNaCl)
// Current implementation uses Base64 encoding for demo purposes

// In SigningServiceImpl.sign()
// TODO: Implement with actual signing algorithm (RSA-PSS, ECDSA)
// Current implementation generates mock signatures

// In DataKeyServiceImpl.generateDataKey()
// TODO: Generate cryptographically secure random keys
```

### D. Tenant Context Extraction

Update helper methods in KeyController:

```java
// In KeyController.java
private String getTenant() {
    // TODO: Extract from SecurityContext or RequestContext
    // Example:
    // return SecurityContextHolder.getContext()
    //     .getAuthentication()
    //     .getPrincipal().getTenant();
    return "default-tenant" ;  // Current mock implementation
}

private String getPrincipal() {
    // TODO: Extract from SecurityContext
    return "system" ;
}

private String getClientIp() {
    // TODO: Extract from HttpServletRequest
    return "127.0.0.1" ;
}
```

### E. Database Schema Creation

Create migration scripts or Liquibase changesets:

```sql
-- src/main/resources/db/changelog/kms-schema.sql

CREATE TABLE kms_keys
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_id                  VARCHAR(255) UNIQUE NOT NULL,
    alias                   VARCHAR(255),
    description             VARCHAR(1024),
    key_spec                VARCHAR(50)         NOT NULL,
    purpose                 VARCHAR(50)         NOT NULL,
    status                  VARCHAR(50)         NOT NULL,
    current_version         VARCHAR(50),
    scheduled_deletion_date TIMESTAMP,
    tenant                  VARCHAR(255)        NOT NULL,
    created_at              TIMESTAMP           NOT NULL,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255)
);

CREATE TABLE kms_key_versions
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id   VARCHAR(255) UNIQUE NOT NULL,
    key_id       BIGINT              NOT NULL REFERENCES kms_keys (id),
    key_material LONGTEXT            NOT NULL,
    status       VARCHAR(50)         NOT NULL,
    created_at   TIMESTAMP           NOT NULL
);

-- Other tables follow similar pattern...
```

### F. Configuration Properties

Add to `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kms_db
    username: kms_user
    password: kms_password

kms:
  encryption:
    algorithm: AES/GCM/NoPadding
    key-size: 256
  signing:
    algorithm: RSASSA_PSS_SHA256
  audit:
    enabled: true
    retention-days: 2555  # ~7 years
  features:
    allow-key-deletion: true
    auto-rotation: true
    rotation-period-days: 90
```

### G. Logging & Monitoring Integration

Connect audit logging to external systems:

```java

@Service
public class AuditServiceImpl implements IAuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private ExternalAuditService externalAuditService;  // e.g., Splunk, datadog

    @Override
    public void logAction(String tenant, String action, String keyId,
                          String principal, String ip) {
        // Save to database
        AuditLog auditLog = AuditLog.builder()
                .tenant(tenant)
                .action(action)
                .keyId(keyId)
                .principal(principal)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);

        // TODO: Stream to external audit systems if configured
        if (externalAuditService != null) {
            externalAuditService.log(auditLog);
        }
    }
}
```

## 4. Integration Checklist

- [ ] Create JPA entities in `01-kms-jpa/src/main/java/eu/isygoit/model/`
- [ ] Create repositories in `01-kms-jpa/src/main/java/eu/isygoit/repository/`
- [ ] Implement actual cryptographic algorithms
- [ ] Inject repositories into service implementations
- [ ] Update `@Autowired` services with real implementations
- [ ] Implement tenant context extraction from security context
- [ ] Create database migration scripts
- [ ] Add KMS configuration to `application.yml`
- [ ] Set up external audit logging
- [ ] Create unit tests for each service
- [ ] Create integration tests
- [ ] Set up API documentation in Swagger UI
- [ ] Configure security/authorization rules
- [ ] Performance testing and optimization
- [ ] Security review and penetration testing

## 5. Testing

### Unit Tests Example

```java

@SpringBootTest
public class EncryptionServiceTest {

    @Autowired
    private IEncryptionService encryptionService;

    @Test
    public void testEncryptDecryptRoundTrip() {
        // Given
        EncryptRequestDto encryptRequest = EncryptRequestDto.builder()
                .keyId("key-123")
                .plaintext(Base64.getEncoder().encodeToString("secret data".getBytes()))
                .build();

        // When
        EncryptResponseDto encryptResponse = encryptionService.encrypt("tenant-1", encryptRequest);

        DecryptRequestDto decryptRequest = DecryptRequestDto.builder()
                .ciphertext(encryptResponse.getCiphertext())
                .build();

        DecryptResponseDto decryptResponse = encryptionService.decrypt("tenant-1", decryptRequest);

        // Then
        String decrypted = new String(Base64.getDecoder().decode(decryptResponse.getPlaintext()));
        assertEquals("secret data", decrypted);
    }
}
```

## 6. Security Considerations

- **Key Storage:** Store key material encrypted at rest
- **Audit Logging:** All cryptographic operations must be logged
- **Access Control:** Use grants and policies for least privilege
- **Rotation:** Implement automatic key rotation
- **Deletion:** Implement grace period before actual deletion
- **Encryption Context:** Use it for additional security
- **FIPS Compliance:** Ensure algorithms are FIPS 140-2 compliant if required

## 7. Performance Optimization

- Implement caching for frequently accessed keys
- Use connection pooling for database
- Asynchronous audit logging
- Batch processing for key operations
- Rate limiting and throttling

## 8. External Integrations

- [ ] HSM (Hardware Security Module) integration
- [ ] External PKI integration
- [ ] Cloud provider KMS integration (WAMS KMS, Azure Key Vault, etc.)
- [ ] Certificate authority integration
- [ ] LDAP/Active Directory for principals

---

**Next Steps:**

1. Implement JPA entities and repositories
2. Replace mock cryptographic operations with real implementations
3. Configure database and security
4. Create comprehensive tests
5. Deploy and monitor

