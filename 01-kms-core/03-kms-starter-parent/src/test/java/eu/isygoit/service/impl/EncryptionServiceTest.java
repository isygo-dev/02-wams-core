package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.DecryptRequest;
import eu.isygoit.dto.KmsDtos.DecryptResponse;
import eu.isygoit.dto.KmsDtos.EncryptRequest;
import eu.isygoit.dto.KmsDtos.EncryptResponse;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.exception.DecryptionException;
import eu.isygoit.exception.DisabledKeyException;
import eu.isygoit.exception.KeyNotAllowedForUsageException;
import eu.isygoit.exception.WrongAlgorithmException;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.KmsKeyVersionRepository;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.utils.CiphertextEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EncryptionService - Realistic User Stories")
class EncryptionServiceTest {

    private final String tenant = "acme-corp";
    private final String keyId = "123e4567-e89b-12d3-a456-426614174000";
    private final String versionId = "v-1a2b3c4d";
    private final String currentVersionId = "v-current";
    private final String plaintext = "MySecretData";
    private final String plaintextBase64 =
            Base64.getEncoder().encodeToString(plaintext.getBytes());
    private final byte[] ciphertextBytes = "encrypted".getBytes();
    private final byte[] wrappedCiphertext =
            CiphertextEnvelope.wrap(versionId, ciphertextBytes);
    @Mock
    private KmsKeyRepository kmsKeyRepository;
    @Mock
    private KmsKeyVersionRepository kmsKeyVersionRepository;
    @Mock
    private ICryptoService cryptoService;
    @InjectMocks
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        reset(cryptoService);
        reset(kmsKeyRepository);
        reset(kmsKeyVersionRepository);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private KmsKey createSymmetricKey(IEnumKeyStatus.Types status) {
        return KmsKey.builder()
                .tenant(tenant)
                .keyId(keyId)
                .keySpec(IEnumKeySpec.Types.SYMMETRIC_DEFAULT)
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(status)
                .currentVersionId(currentVersionId)
                .build();
    }

    private KmsKey createAsymmetricKey(IEnumKeyStatus.Types status) {
        return KmsKey.builder()
                .tenant(tenant)
                .keyId(keyId)
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(status)
                .currentVersionId(currentVersionId)
                .build();
    }

    private KmsKeyVersion createKeyVersion(
            String keyId,
            String versionId,
            IEnumKeyStatus.Types status,
            byte[] keyMaterial,
            byte[] publicKey
    ) {
        return KmsKeyVersion.builder()
                .tenant(tenant)
                .keyId(keyId)
                .versionId(versionId)
                .keyStatus(status)
                .keyMaterial(keyMaterial)
                .publicKey(publicKey)
                .build();
    }

    // =========================================================================
    // Story 1: Encrypt
    // =========================================================================

    @Nested
    @DisplayName("Story 1: Encrypt")
    class EncryptStory {

        @Test
        @DisplayName("Encrypt with symmetric key")
        void encryptWithSymmetricKey() {

            KmsKey key = createSymmetricKey(IEnumKeyStatus.Types.ENABLED);

            KmsKeyVersion version = createKeyVersion(
                    keyId,
                    currentVersionId,
                    IEnumKeyStatus.Types.ENABLED,
                    "symmetricKey".getBytes(),
                    null
            );

            EncryptRequest request = EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(plaintextBase64)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .encryptionContext(Map.of("purpose", "payment"))
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            currentVersionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.of(version));

            doReturn(ciphertextBytes)
                    .when(cryptoService)
                    .encryptData(
                            any(byte[].class),
                            any(byte[].class),
                            any(IEnumKeySpec.Types.class),
                            anyString(),
                            anyMap()
                    );

            EncryptResponse response =
                    encryptionService.encrypt(tenant, request);

            assertThat(response).isNotNull();
            assertThat(response.getCiphertextBlob()).isNotBlank();
            assertThat(response.getKeyId()).isEqualTo(keyId);
            assertThat(response.getKeyVersionId())
                    .isEqualTo(currentVersionId);

            verify(cryptoService).encryptData(
                    eq(plaintext.getBytes()),
                    eq("symmetricKey".getBytes()),
                    eq(key.getKeySpec()),
                    eq("AES/GCM/NoPadding"),
                    eq(request.getEncryptionContext())
            );
        }

        @Test
        @DisplayName("Encrypt with asymmetric key uses public key")
        void encryptWithAsymmetricKey() {

            KmsKey key = createAsymmetricKey(
                    IEnumKeyStatus.Types.ENABLED
            );

            byte[] publicKeyBytes = "publicKey".getBytes();

            KmsKeyVersion version = createKeyVersion(
                    keyId,
                    currentVersionId,
                    IEnumKeyStatus.Types.ENABLED,
                    "privateKey".getBytes(),
                    publicKeyBytes
            );

            EncryptRequest request = EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(plaintextBase64)
                    .encryptionAlgorithmSpec("RSAES_OAEP_SHA_256")
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            currentVersionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.of(version));

            doReturn(ciphertextBytes)
                    .when(cryptoService)
                    .encryptData(
                            any(byte[].class),
                            any(byte[].class),
                            any(IEnumKeySpec.Types.class),
                            anyString(),
                            nullable(Map.class)
                    );

            EncryptResponse response =
                    encryptionService.encrypt(tenant, request);

            assertThat(response).isNotNull();
            assertThat(response.getCiphertextBlob()).isNotBlank();

            verify(cryptoService).encryptData(
                    eq(plaintext.getBytes()),
                    eq(publicKeyBytes),
                    eq(key.getKeySpec()),
                    eq("RSAES_OAEP_SHA_256"),
                    nullable(Map.class)
            );
        }
    }

    // =========================================================================
    // Story 2: Encrypt errors
    // =========================================================================

    @Nested
    @DisplayName("Story 2: Encrypt errors")
    class EncryptErrorStory {

        @Test
        void encryptDisabledKey() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.DISABLED);

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            EncryptRequest request = EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(plaintextBase64)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            assertThatThrownBy(() ->
                    encryptionService.encrypt(tenant, request)
            ).isInstanceOf(DisabledKeyException.class);
        }

        @Test
        void encryptWrongUsage() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.ENABLED);

            key.setKeyUsage(IEnumKeyUsage.Types.SIGN_VERIFY);

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            EncryptRequest request = EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(plaintextBase64)
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            assertThatThrownBy(() ->
                    encryptionService.encrypt(tenant, request)
            ).isInstanceOf(KeyNotAllowedForUsageException.class);
        }

        @Test
        void encryptMissingAlgorithm() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.ENABLED);

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            EncryptRequest request = EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(plaintextBase64)
                    .build();

            assertThatThrownBy(() ->
                    encryptionService.encrypt(tenant, request)
            ).isInstanceOf(WrongAlgorithmException.class);
        }
    }

    // =========================================================================
    // Story 3: Decrypt
    // =========================================================================

    @Nested
    @DisplayName("Story 3: Decrypt")
    class DecryptStory {

        @Test
        void decryptWithExactVersion() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.ENABLED);

            KmsKeyVersion version = createKeyVersion(
                    keyId,
                    versionId,
                    IEnumKeyStatus.Types.ENABLED,
                    "symmetricKey".getBytes(),
                    null
            );

            DecryptRequest request = DecryptRequest.builder()
                    .keyId(keyId)
                    .ciphertextBlob(
                            Base64.getEncoder()
                                    .encodeToString(wrappedCiphertext)
                    )
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .encryptionContext(
                            Map.of("purpose", "payment")
                    )
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            versionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.of(version));

            doReturn(plaintext.getBytes())
                    .when(cryptoService)
                    .decryptData(
                            eq(tenant),
                            any(byte[].class),
                            eq("symmetricKey".getBytes()),
                            eq(key.getKeySpec()),
                            eq("AES/GCM/NoPadding"),
                            eq(request.getEncryptionContext())
                    );

            DecryptResponse response =
                    encryptionService.decrypt(tenant, request);

            assertThat(response.getPlaintext())
                    .isEqualTo(plaintextBase64);

            assertThat(response.getKeyVersionId())
                    .isEqualTo(versionId);
        }

        @Test
        void decryptWithAsymmetricKey() {

            KmsKey key =
                    createAsymmetricKey(IEnumKeyStatus.Types.ENABLED);

            byte[] privateKeyBytes = "privateKey".getBytes();

            KmsKeyVersion version = createKeyVersion(
                    keyId,
                    versionId,
                    IEnumKeyStatus.Types.ENABLED,
                    privateKeyBytes,
                    null
            );

            DecryptRequest request = DecryptRequest.builder()
                    .keyId(keyId)
                    .ciphertextBlob(
                            Base64.getEncoder()
                                    .encodeToString(wrappedCiphertext)
                    )
                    .encryptionAlgorithmSpec("RSAES_OAEP_SHA_256")
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            versionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.of(version));

            doReturn(plaintext.getBytes())
                    .when(cryptoService)
                    .decryptData(
                            eq(tenant),
                            any(byte[].class),
                            eq(privateKeyBytes),
                            eq(key.getKeySpec()),
                            eq("RSAES_OAEP_SHA_256"),
                            nullable(Map.class)
                    );

            DecryptResponse response =
                    encryptionService.decrypt(tenant, request);

            assertThat(response.getPlaintext())
                    .isEqualTo(plaintextBase64);

            verify(cryptoService).decryptData(
                    eq(tenant),
                    any(byte[].class),
                    eq(privateKeyBytes),
                    eq(key.getKeySpec()),
                    eq("RSAES_OAEP_SHA_256"),
                    nullable(Map.class)
            );
        }
    }

    // =========================================================================
    // Story 4: Decrypt fallback
    // =========================================================================

    @Nested
    @DisplayName("Story 4: Decrypt fallback")
    class DecryptFallbackStory {

        @Test
        void fallbackToCurrentVersion() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.ENABLED);

            KmsKeyVersion currentVersion = createKeyVersion(
                    keyId,
                    currentVersionId,
                    IEnumKeyStatus.Types.ENABLED,
                    "currentKey".getBytes(),
                    null
            );

            DecryptRequest request = DecryptRequest.builder()
                    .keyId(keyId)
                    .ciphertextBlob(
                            Base64.getEncoder()
                                    .encodeToString(wrappedCiphertext)
                    )
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            versionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.empty());

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            currentVersionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.of(currentVersion));

            doReturn(plaintext.getBytes())
                    .when(cryptoService)
                    .decryptData(
                            eq(tenant),
                            any(byte[].class),
                            eq("currentKey".getBytes()),
                            eq(key.getKeySpec()),
                            eq("AES/GCM/NoPadding"),
                            nullable(Map.class)
                    );

            DecryptResponse response =
                    encryptionService.decrypt(tenant, request);

            assertThat(response.getKeyVersionId())
                    .isEqualTo(currentVersionId);
        }
    }

    // =========================================================================
    // Story 5: Multiple versions
    // =========================================================================

    @Nested
    @DisplayName("Story 5: Multiple versions")
    class MultipleVersionsStory {

        @Test
        void tryAllEnabledVersions() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.ENABLED);

            KmsKeyVersion oldVersion1 = createKeyVersion(
                    keyId,
                    "v-old1",
                    IEnumKeyStatus.Types.ENABLED,
                    "oldKey1".getBytes(),
                    null
            );

            KmsKeyVersion oldVersion2 = createKeyVersion(
                    keyId,
                    "v-old2",
                    IEnumKeyStatus.Types.ENABLED,
                    "oldKey2".getBytes(),
                    null
            );

            DecryptRequest request = DecryptRequest.builder()
                    .keyId(keyId)
                    .ciphertextBlob(
                            Base64.getEncoder()
                                    .encodeToString(wrappedCiphertext)
                    )
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            versionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.empty());

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            currentVersionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.empty());

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndKeyStatusOrderByCreateDateDesc(
                            tenant,
                            keyId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(List.of(oldVersion1, oldVersion2));

            doThrow(new RuntimeException("decrypt failed"))
                    .doReturn(plaintext.getBytes())
                    .when(cryptoService)
                    .decryptData(
                            eq(tenant),
                            any(byte[].class),
                            any(byte[].class),
                            eq(key.getKeySpec()),
                            eq("AES/GCM/NoPadding"),
                            nullable(Map.class)
                    );

            DecryptResponse response =
                    encryptionService.decrypt(tenant, request);

            assertThat(response.getKeyVersionId())
                    .isEqualTo("v-old2");

            verify(cryptoService, times(2))
                    .decryptData(
                            eq(tenant),
                            any(byte[].class),
                            any(byte[].class),
                            eq(key.getKeySpec()),
                            eq("AES/GCM/NoPadding"),
                            nullable(Map.class)
                    );
        }

        @Test
        void noVersionCanDecrypt() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.ENABLED);

            KmsKeyVersion oldVersion = createKeyVersion(
                    keyId,
                    "v-old",
                    IEnumKeyStatus.Types.ENABLED,
                    "oldKey".getBytes(),
                    null
            );

            DecryptRequest request = DecryptRequest.builder()
                    .keyId(keyId)
                    .ciphertextBlob(
                            Base64.getEncoder()
                                    .encodeToString(wrappedCiphertext)
                    )
                    .encryptionAlgorithmSpec("AES/GCM/NoPadding")
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            versionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.empty());

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionIdAndKeyStatus(
                            tenant,
                            keyId,
                            currentVersionId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(Optional.empty());

            when(kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndKeyStatusOrderByCreateDateDesc(
                            tenant,
                            keyId,
                            IEnumKeyStatus.Types.ENABLED
                    ))
                    .thenReturn(List.of(oldVersion));

            doThrow(new RuntimeException("decrypt failed"))
                    .when(cryptoService)
                    .decryptData(
                            eq(tenant),
                            any(byte[].class),
                            any(byte[].class),
                            eq(key.getKeySpec()),
                            eq("AES/GCM/NoPadding"),
                            nullable(Map.class)
                    );

            assertThatThrownBy(() ->
                    encryptionService.decrypt(tenant, request)
            )
                    .isInstanceOf(DecryptionException.class)
                    .hasMessage("Decrypt failed");
        }
    }

    // =========================================================================
    // Story 6: Decrypt errors
    // =========================================================================

    @Nested
    @DisplayName("Story 6: Decrypt errors")
    class DecryptErrorStory {

        @Test
        void decryptDisabledKey() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.DISABLED);

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            DecryptRequest request = DecryptRequest.builder()
                    .keyId(keyId)
                    .ciphertextBlob("dummy")
                    .build();

            assertThatThrownBy(() ->
                    encryptionService.decrypt(tenant, request)
            ).isInstanceOf(DisabledKeyException.class);
        }

        @Test
        void decryptWrongUsage() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.ENABLED);

            key.setKeyUsage(IEnumKeyUsage.Types.SIGN_VERIFY);

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            DecryptRequest request = DecryptRequest.builder()
                    .keyId(keyId)
                    .ciphertextBlob("dummy")
                    .build();

            assertThatThrownBy(() ->
                    encryptionService.decrypt(tenant, request)
            ).isInstanceOf(KeyNotAllowedForUsageException.class);
        }

        @Test
        void decryptMissingAlgorithm() {

            KmsKey key =
                    createSymmetricKey(IEnumKeyStatus.Types.ENABLED);

            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(key));

            DecryptRequest request = DecryptRequest.builder()
                    .keyId(keyId)
                    .ciphertextBlob(
                            Base64.getEncoder()
                                    .encodeToString(wrappedCiphertext)
                    )
                    .build();

            assertThatThrownBy(() ->
                    encryptionService.decrypt(tenant, request)
            ).isInstanceOf(WrongAlgorithmException.class);
        }

        @Test
        void decryptMissingKeyId() {

            DecryptRequest request =
                    DecryptRequest.builder().build();

            assertThatThrownBy(() ->
                    encryptionService.decrypt(tenant, request)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("keyId required");
        }
    }
}