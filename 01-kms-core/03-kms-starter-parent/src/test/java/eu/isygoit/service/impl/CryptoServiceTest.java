package eu.isygoit.service.impl;

import eu.isygoit.dto.data.KeyPairMaterial;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumSignatureAlgorithm;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.model.KmsAuditLog;
import eu.isygoit.repository.DigesterConfigRepository;
import eu.isygoit.repository.KmsAuditLogRepository;
import eu.isygoit.repository.PEBConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CryptoService - Real Implementation User Stories")
class CryptoServiceTest {

    @Mock
    private PEBConfigRepository pebConfigRepository;

    @Mock
    private DigesterConfigRepository digesterConfigRepository;

    @Mock
    private KmsAuditLogRepository auditLogRepository;

    @InjectMocks
    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        // Use lenient stubbing to avoid unnecessary stubbing exceptions
        lenient().when(pebConfigRepository.findFirstByTenantIgnoreCase(any()))
                .thenReturn(Optional.empty());
        lenient().when(digesterConfigRepository.findFirstByTenantIgnoreCase(any()))
                .thenReturn(Optional.empty());
    }

    // ========================================================================
    // User Story 1: Symmetric encryption (AES-256)
    // ========================================================================

    @Test
    @DisplayName("Validate key integrity returns true for valid symmetric key")
    void validateKeyIntegrity() {
        IEnumKeySpec.Types spec = IEnumKeySpec.Types.SYMMETRIC_DEFAULT;
        KeyPairMaterial material = cryptoService.generateKeyMaterial(spec);
        byte[] validKey = material.privateKey();
        assertThat(cryptoService.validateKeyIntegrity(validKey, spec)).isTrue();

        assertThat(cryptoService.validateKeyIntegrity(null, spec)).isFalse();
        assertThat(cryptoService.validateKeyIntegrity(new byte[0], spec)).isFalse();
    }

    // ========================================================================
    // User Story 2: Asymmetric signing (RSA-2048)
    // ========================================================================

    @Nested
    @DisplayName("Story 1: Symmetric encryption (AES-256)")
    class SymmetricEncryptionStory {

        @Test
        @DisplayName("Generate AES-256 key, encrypt and decrypt a secret")
        void encryptAndDecryptWithSymmetricKey() {
            IEnumKeySpec.Types keySpec = IEnumKeySpec.Types.SYMMETRIC_DEFAULT;
            KeyPairMaterial keyMaterial = cryptoService.generateKeyMaterial(keySpec);
            byte[] aesKey = keyMaterial.privateKey(); // symmetric key stored in privateKey field

            String plaintext = "Secret message: pay 1000 EUR";
            byte[] plainBytes = plaintext.getBytes();
            Map<String, String> context = Map.of("purpose", "payment");

            byte[] ciphertext = cryptoService.encryptData(
                    plainBytes, aesKey, keySpec, "AES/GCM/NoPadding", context);

            assertThat(ciphertext).isNotEmpty();
            assertThat(ciphertext).isNotEqualTo(plainBytes);

            byte[] decrypted = cryptoService.decryptData(
                    "tenant1", ciphertext, aesKey, keySpec, "AES/GCM/NoPadding", context);

            assertThat(new String(decrypted)).isEqualTo(plaintext);
        }
    }

    // ========================================================================
    // User Story 3: Envelope encryption – generate and wrap a data key
    // ========================================================================

    @Nested
    @DisplayName("Story 2: Asymmetric signing with RSA-2048")
    class AsymmetricSigningStory {

        @Test
        @DisplayName("Sign a document with private key and verify with public key")
        void signAndVerifyWithRsa() {
            IEnumKeySpec.Types keySpec = IEnumKeySpec.Types.RSA_2048;
            KeyPairMaterial keyPair = cryptoService.generateKeyMaterial(keySpec);
            byte[] privateKey = keyPair.privateKey();
            byte[] publicKey = keyPair.publicKey();

            String document = "Contract: transfer 5000 EUR to account BE12 3456";
            byte[] message = document.getBytes();
            IEnumSignatureAlgorithm algo = IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_256;

            byte[] signature = cryptoService.signData(message, privateKey, algo);

            assertThat(cryptoService.verifySignature(message, signature, publicKey, algo)).isTrue();

            byte[] tampered = "Contract: transfer 50000 EUR".getBytes();
            assertThat(cryptoService.verifySignature(tampered, signature, publicKey, algo)).isFalse();
        }
    }

    // ========================================================================
    // User Story 4: Audit trail – usage counters and last used date
    // ========================================================================

    @Nested
    @DisplayName("Story 3: Envelope encryption – data key generation")
    class EnvelopeEncryptionStory {

        @Test
        @DisplayName("Generate a data key wrapped under a master AES key")
        void generateAndUseDataKey() {
            IEnumKeySpec.Types masterSpec = IEnumKeySpec.Types.SYMMETRIC_DEFAULT;
            KeyPairMaterial masterMaterial = cryptoService.generateKeyMaterial(masterSpec);
            byte[] masterKey = masterMaterial.privateKey();

            Map<String, byte[]> dataKey = cryptoService.generateDataKey(masterKey, 256);
            byte[] plaintextDataKey = dataKey.get("plaintextKey");
            byte[] encryptedDataKey = dataKey.get("encryptedKey");

            assertThat(plaintextDataKey).hasSize(32);
            assertThat(encryptedDataKey).isNotEmpty();

            // Decrypt with the same context (null) used during encryption in generateDataKey
            byte[] decryptedDataKey = cryptoService.decryptData(
                    "tenant1", encryptedDataKey, masterKey, masterSpec,
                    "AES/GCM/NoPadding", null);

            assertThat(decryptedDataKey).isEqualTo(plaintextDataKey);
        }
    }

    // ========================================================================
    // Additional validation: key integrity check
    // ========================================================================

    @Nested
    @DisplayName("Story 4: Audit trail for key usage")
    class AuditTrailStory {

        @Test
        @DisplayName("Retrieve encryption count, decryption count, and last used timestamp")
        void keyUsageMetrics() {
            String tenant = "acme-corp";
            String keyId = "key-456";

            when(auditLogRepository.countByTenantAndActionAndKeyId(tenant, IKmsActionType.Types.ENCRYPT, keyId))
                    .thenReturn(42L);
            when(auditLogRepository.countByTenantAndActionAndKeyId(tenant, IKmsActionType.Types.DECRYPT, keyId))
                    .thenReturn(7L);
            LocalDateTime lastUsed = LocalDateTime.of(2025, 3, 15, 10, 30);
            KmsAuditLog lastLog = KmsAuditLog.builder().timestamp(lastUsed).build();
            when(auditLogRepository.findFirstByTenantAndKeyIdOrderByTimestampDesc(tenant, keyId))
                    .thenReturn(Optional.of(lastLog));

            long encryptCount = cryptoService.getEncryptCount(tenant, keyId);
            long decryptCount = cryptoService.getDecryptCount(tenant, keyId);
            LocalDateTime lastUsedDate = cryptoService.getLastUsedDate(tenant, keyId);

            assertThat(encryptCount).isEqualTo(42L);
            assertThat(decryptCount).isEqualTo(7L);
            assertThat(lastUsedDate).isEqualTo(lastUsed);
        }
    }
}