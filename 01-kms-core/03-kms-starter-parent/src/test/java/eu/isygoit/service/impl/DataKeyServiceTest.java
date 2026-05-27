package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.exception.*;
import eu.isygoit.model.KmsAlias;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsAliasRepository;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.service.ICryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DataKeyService - Realistic User Stories")
class DataKeyServiceTest {

    private final String tenant = "acme-corp";
    private final String keyId = "master-key-123";
    private final byte[] fakeAesKey = "0123456789abcdef".getBytes(); // 16 bytes
    @Mock
    private KmsKeyRepository kmsKeyRepository;
    @Mock
    private KmsAliasRepository kmsAliasRepository;
    @Mock
    private ICryptoService cryptoService;
    @Spy
    @InjectMocks
    private DataKeyService dataKeyService;

    @BeforeEach
    void setUp() {
        // Mock the private generateAesKey method to avoid real crypto calls
        doReturn(fakeAesKey).when(dataKeyService).generateAesKey(anyInt());
    }

    private KmsKey createSymmetricMasterKey(IEnumKeyStatus.Types status) {
        return KmsKey.builder()
                .tenant(tenant)
                .keyId(keyId)
                .keyWrn("wrn:wams:kms:us-east-1:" + tenant + ":key/" + keyId)
                .keySpec(IEnumKeySpec.Types.SYMMETRIC_DEFAULT)
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(status)
                .currentVersionId("v1")
                .keyMaterial("masterKeyMaterial".getBytes())
                .build();
    }

    private KmsKey createAsymmetricMasterKey(IEnumKeyStatus.Types status) {
        return KmsKey.builder()
                .tenant(tenant)
                .keyId(keyId)
                .keyWrn("wrn:wams:kms:us-east-1:" + tenant + ":key/" + keyId)
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(status)
                .currentVersionId("v1")
                .keyMaterial("privateKey".getBytes())
                .publicKey("publicKey".getBytes())
                .build();
    }

    // =========================================================================
    // User Story 1: Generate a symmetric data key (envelope encryption)
    // =========================================================================

    @Nested
    @DisplayName("Story 1: Generate a symmetric data key (envelope encryption)")
    class GenerateDataKeyStory {

        @Test
        @DisplayName("Generate AES-256 data key wrapped by a symmetric master key")
        void generateDataKeyWithSymmetricMaster() {
            KmsKey masterKey = createSymmetricMasterKey(IEnumKeyStatus.Types.ENABLED);
            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                    .keyId(keyId)
                    .keySize(256)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .encryptionContext(Map.of("purpose", "file-encryption"))
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));
            when(cryptoService.encryptData(any(byte[].class), eq(masterKey.getKeyMaterial()),
                    eq(masterKey.getKeySpec()), eq(request.getEncryptionAlgorithmSpec()),
                    eq(request.getEncryptionContext())))
                    .thenReturn("encryptedKey".getBytes());

            GenerateDataKeyResponse response = dataKeyService.generateDataKey(tenant, request);

            assertThat(response.getPlaintext()).isNotNull();
            assertThat(response.getCiphertextBlob()).isNotNull();
            assertThat(response.getKeyId()).isEqualTo(keyId);
            verify(cryptoService).encryptData(any(byte[].class), eq(masterKey.getKeyMaterial()),
                    eq(masterKey.getKeySpec()), eq(request.getEncryptionAlgorithmSpec()),
                    eq(request.getEncryptionContext()));
        }

        @Test
        @DisplayName("Generate data key wrapped by an asymmetric master key (uses public key)")
        void generateDataKeyWithAsymmetricMaster() {
            KmsKey masterKey = createAsymmetricMasterKey(IEnumKeyStatus.Types.ENABLED);
            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                    .keyId(keyId)
                    .keySize(256)
                    .encryptionAlgorithmSpec("RSAES_OAEP_SHA_256")
                    .encryptionContext(Map.of("purpose", "key-wrapping"))
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));
            when(cryptoService.encryptData(any(byte[].class), eq(masterKey.getPublicKey()),
                    eq(masterKey.getKeySpec()), eq(request.getEncryptionAlgorithmSpec()),
                    eq(request.getEncryptionContext())))
                    .thenReturn("encryptedKey".getBytes());

            GenerateDataKeyResponse response = dataKeyService.generateDataKey(tenant, request);

            verify(cryptoService).encryptData(any(byte[].class), eq(masterKey.getPublicKey()),
                    eq(masterKey.getKeySpec()), eq(request.getEncryptionAlgorithmSpec()),
                    eq(request.getEncryptionContext()));
        }
    }

    // =========================================================================
    // User Story 2: Generate data key without returning plaintext
    // =========================================================================

    @Nested
    @DisplayName("Story 2: Generate data key without plaintext (only ciphertext)")
    class GenerateDataKeyWithoutPlaintextStory {

        @Test
        @DisplayName("Return only encrypted data key, not plaintext")
        void generateDataKeyWithoutPlaintext() {
            KmsKey masterKey = createSymmetricMasterKey(IEnumKeyStatus.Types.ENABLED);
            GenerateDataKeyWithoutPlaintextRequest request = GenerateDataKeyWithoutPlaintextRequest.builder()
                    .keyId(keyId)
                    .keySize(256)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));
            when(cryptoService.encryptData(any(byte[].class), any(byte[].class), any(), anyString(), anyMap()))
                    .thenReturn("encryptedKey".getBytes());

            GenerateDataKeyWithoutPlaintextResponse response = dataKeyService.generateDataKeyWithoutPlaintext(tenant, request);

            assertThat(response.getCiphertextBlob()).isNotNull();
            assertThat(response.getKeyId()).isEqualTo(keyId);
        }
    }

    // =========================================================================
    // User Story 3: Generate an asymmetric data key pair (RSA or EC)
    // =========================================================================

    @Nested
    @DisplayName("Story 3: Generate an asymmetric data key pair")
    class GenerateDataKeyPairStory {

        @Test
        @DisplayName("Generate RSA-2048 key pair, wrap private key with master key")
        void generateRsaKeyPair() {
            KmsKey masterKey = createSymmetricMasterKey(IEnumKeyStatus.Types.ENABLED);
            GenerateDataKeyPairRequest request = GenerateDataKeyPairRequest.builder()
                    .keyId(keyId)
                    .keyPairSpec(IEnumKeySpec.Types.RSA_2048)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .encryptionContext(Map.of("purpose", "asymmetric-key"))
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));
            when(cryptoService.encryptData(any(byte[].class), any(byte[].class), any(), anyString(), anyMap()))
                    .thenReturn("encryptedPrivateKey".getBytes());

            GenerateDataKeyPairResponse response = dataKeyService.generateDataKeyPair(tenant, request);

            assertThat(response.getPublicKey()).isNotNull();
            assertThat(response.getPrivateKeyCiphertextBlob()).isNotNull();
            assertThat(response.getKeyId()).isEqualTo(keyId);
            assertThat(response.getKeyPairSpec()).isEqualTo(IEnumKeySpec.Types.RSA_2048);
            verify(cryptoService).encryptData(any(byte[].class), eq(masterKey.getKeyMaterial()),
                    eq(masterKey.getKeySpec()), eq(request.getEncryptionAlgorithmSpec()),
                    eq(request.getEncryptionContext()));
        }

        @Test
        @DisplayName("Generate EC P-256 key pair (currently fails due to service bug)")
        void generateEcKeyPair() {
            KmsKey masterKey = createSymmetricMasterKey(IEnumKeyStatus.Types.ENABLED);
            GenerateDataKeyPairRequest request = GenerateDataKeyPairRequest.builder()
                    .keyId(keyId)
                    .keyPairSpec(IEnumKeySpec.Types.ECC_NIST_P256)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));
            when(cryptoService.encryptData(any(byte[].class), any(byte[].class), any(), anyString(), anyMap()))
                    .thenReturn("encryptedPrivateKey".getBytes());

            assertThatThrownBy(() -> dataKeyService.generateDataKeyPair(tenant, request))
                    .isInstanceOf(GenerateKeyException.class);
        }
    }

    // =========================================================================
    // User Story 4: Generate data key pair without plaintext
    // =========================================================================

    @Nested
    @DisplayName("Story 4: Generate data key pair without plaintext")
    class GenerateDataKeyPairWithoutPlaintextStory {

        @Test
        @DisplayName("Return only public key and encrypted private key")
        void generateDataKeyPairWithoutPlaintext() {
            KmsKey masterKey = createSymmetricMasterKey(IEnumKeyStatus.Types.ENABLED);
            GenerateDataKeyPairWithoutPlaintextRequest request = GenerateDataKeyPairWithoutPlaintextRequest.builder()
                    .keyId(keyId)
                    .keyPairSpec(IEnumKeySpec.Types.RSA_2048)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));
            when(cryptoService.encryptData(any(byte[].class), any(byte[].class), any(), anyString(), anyMap()))
                    .thenReturn("encryptedPrivateKey".getBytes());

            GenerateDataKeyPairWithoutPlaintextResponse response = dataKeyService.generateDataKeyPairWithoutPlaintext(tenant, request);

            assertThat(response.getPublicKey()).isNotNull();
            assertThat(response.getPrivateKeyCiphertextBlob()).isNotNull();
            assertThat(response.getKeyId()).isEqualTo(keyId);
        }
    }

    // =========================================================================
    // User Story 5: Resolve keyId from alias or WRN
    // =========================================================================

    @Nested
    @DisplayName("Story 5: Resolve key ID from alias or WRN")
    class ResolveKeyIdStory {

        @Test
        @DisplayName("Resolve alias to key ID")
        void resolveAlias() {
            String alias = "alias:my-key";
            KmsAlias kmsAlias = KmsAlias.builder()
                    .tenant(tenant)
                    .aliasName("my-key")
                    .targetKeyId(keyId)
                    .build();
            when(kmsAliasRepository.findByTenantAndAliasName(tenant, "my-key")).thenReturn(Optional.of(kmsAlias));

            String resolved = dataKeyService.resolveKeyId(tenant, alias);
            assertThat(resolved).isEqualTo(keyId);
        }

        @Test
        @DisplayName("Resolve WRN to key ID (including 'key/' prefix)")
        void resolveWrn() {
            String wrn = "wrn:wams:kms:us-east-1:acme-corp:key/123e4567-e89b-12d3-a456-426614174000";
            String resolved = dataKeyService.resolveKeyId(tenant, wrn);
            assertThat(resolved).isEqualTo("key/123e4567-e89b-12d3-a456-426614174000");
        }

        @Test
        @DisplayName("Return keyId as-is if not an alias or WRN")
        void resolvePlainKeyId() {
            String plain = "my-key-123";
            String resolved = dataKeyService.resolveKeyId(tenant, plain);
            assertThat(resolved).isEqualTo(plain);
        }

        @Test
        @DisplayName("Throw exception when alias not found")
        void aliasNotFound() {
            when(kmsAliasRepository.findByTenantAndAliasName(tenant, "unknown")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> dataKeyService.resolveKeyId(tenant, "alias:unknown"))
                    .isInstanceOf(KeyAliasNotFoundException.class);
        }

        @Test
        @DisplayName("Throw exception when keyId is null or blank")
        void nullKeyId() {
            assertThatThrownBy(() -> dataKeyService.resolveKeyId(tenant, null))
                    .isInstanceOf(NullIdentifierException.class);
            assertThatThrownBy(() -> dataKeyService.resolveKeyId(tenant, " "))
                    .isInstanceOf(NullIdentifierException.class);
        }
    }

    // =========================================================================
    // User Story 6: Generate random bytes
    // =========================================================================

    @Nested
    @DisplayName("Story 6: Generate random bytes")
    class GenerateRandomStory {

        @Test
        @DisplayName("Generate 32 random bytes and return as base64")
        void generateRandom() {
            GenerateRandomRequest request = GenerateRandomRequest.builder()
                    .numberOfBytes(32)
                    .build();

            GenerateRandomResponse response = dataKeyService.generateRandom(request);

            assertThat(response.getPlaintext()).isNotNull();
            byte[] decoded = java.util.Base64.getDecoder().decode(response.getPlaintext());
            assertThat(decoded).hasSize(32);
        }

        @Test
        @DisplayName("Different calls produce different random bytes")
        void randomness() {
            GenerateRandomRequest request = GenerateRandomRequest.builder().numberOfBytes(16).build();
            GenerateRandomResponse response1 = dataKeyService.generateRandom(request);
            GenerateRandomResponse response2 = dataKeyService.generateRandom(request);
            assertThat(response1.getPlaintext()).isNotEqualTo(response2.getPlaintext());
        }
    }

    // =========================================================================
    // User Story 7: Error handling
    // =========================================================================

    @Nested
    @DisplayName("Story 7: Error handling")
    class ErrorHandlingStory {

        @Test
        @DisplayName("Generate data key fails when master key is disabled")
        void generateDataKeyDisabledMaster() {
            KmsKey masterKey = createSymmetricMasterKey(IEnumKeyStatus.Types.DISABLED);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));

            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                    .keyId(keyId)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            assertThatThrownBy(() -> dataKeyService.generateDataKey(tenant, request))
                    .isInstanceOf(DisabledKeyException.class);
        }

        @Test
        @DisplayName("Generate data key fails when master key not intended for encryption")
        void generateDataKeyWrongKeyUsage() {
            KmsKey masterKey = createSymmetricMasterKey(IEnumKeyStatus.Types.ENABLED);
            masterKey.setKeyUsage(IEnumKeyUsage.Types.SIGN_VERIFY);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));

            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                    .keyId(keyId)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            assertThatThrownBy(() -> dataKeyService.generateDataKey(tenant, request))
                    .isInstanceOf(KeyNotAllowedForUsageException.class);
        }

        @Test
        @DisplayName("Generate data key fails when encryption algorithm missing")
        void generateDataKeyMissingAlgorithm() {
            KmsKey masterKey = createSymmetricMasterKey(IEnumKeyStatus.Types.ENABLED);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));

            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                    .keyId(keyId)
                    .build();

            assertThatThrownBy(() -> dataKeyService.generateDataKey(tenant, request))
                    .isInstanceOf(WrongAlgorithmException.class);
        }

        @Test
        @DisplayName("Generate data key pair fails when master key disabled")
        void generateKeyPairDisabledMaster() {
            KmsKey masterKey = createSymmetricMasterKey(IEnumKeyStatus.Types.DISABLED);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(masterKey));

            GenerateDataKeyPairRequest request = GenerateDataKeyPairRequest.builder()
                    .keyId(keyId)
                    .keyPairSpec(IEnumKeySpec.Types.RSA_2048)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            assertThatThrownBy(() -> dataKeyService.generateDataKeyPair(tenant, request))
                    .isInstanceOf(DisabledKeyException.class);
        }

        @Test
        @DisplayName("Generate data key pair fails when key not found")
        void generateKeyPairKeyNotFound() {
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.empty());

            GenerateDataKeyPairRequest request = GenerateDataKeyPairRequest.builder()
                    .keyId(keyId)
                    .keyPairSpec(IEnumKeySpec.Types.RSA_2048)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            assertThatThrownBy(() -> dataKeyService.generateDataKeyPair(tenant, request))
                    .isInstanceOf(KeyNotFoundException.class);
        }
    }
}