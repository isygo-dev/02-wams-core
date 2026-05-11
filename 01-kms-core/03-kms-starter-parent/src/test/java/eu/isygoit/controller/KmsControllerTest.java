package eu.isygoit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class KmsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IKeyManagementService keyManagementService;
    @Mock
    private IEncryptionService encryptionService;
    @Mock
    private ISigningService signingService;
    @Mock
    private IKeyPolicyService keyPolicyService;
    @Mock
    private IKeyVersionService keyVersionService;
    @Mock
    private IDataKeyService dataKeyService;
    @Mock
    private IAuditService auditService;
    @Mock
    private RequestContextService requestContextService;
    @Mock
    private IMultiRegionService multiRegionService;
    @Mock
    private ICustomKeyStoreService customKeyStoreService;

    @InjectMocks
    private KmsController kmsController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TENANT = "test-tenant";
    private static final String USER = "test-user";
    private static final String IP = "127.0.0.1";
    private static final String KEY_ID = "1234abcd-12ab-34cd-56ef-1234567890ab";

    @BeforeEach
    void setUp() {
        when(requestContextService.getCurrentContext()).thenReturn(ContextRequestDto.builder()
                .senderTenant(TENANT)
                .senderUser(USER)
                .clientIp(IP)
                .build());

        mockMvc = MockMvcBuilders.standaloneSetup(kmsController)
                .build();
    }

    // =========================================================================
    // KEY MANAGEMENT TESTS
    // =========================================================================

    @Test
    void createKey_Success() throws Exception {
        CreateKeyRequest request = new CreateKeyRequest();
        request.setKeySpec(IEnumKeySpec.Types.AES_256);
        request.setKeyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);

        CreateKeyResponse response = CreateKeyResponse.builder()
                .keyMetadata(CreateKeyResponse.KeyMetadata.builder().keyId(KEY_ID).build())
                .build();

        when(keyManagementService.createKey(eq(TENANT), any(CreateKeyRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/private/kms/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.CREATE_KEY),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    @Test
    void describeKey_Success() throws Exception {
        DescribeKeyResponse response = DescribeKeyResponse.builder()
                .keyMetadata(CreateKeyResponse.KeyMetadata.builder().keyId(KEY_ID).build())
                .build();

        when(keyManagementService.describeKey(eq(TENANT), eq(KEY_ID), isNull()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/private/kms/keys/{keyId}", KEY_ID))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.DESCRIBE_KEY),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    @Test
    void listKeys_Success() throws Exception {
        ListKeysResponse response = ListKeysResponse.builder()
                .keys(Collections.singletonList(new ListKeysResponse.KeyEntry()))
                .build();

        when(keyManagementService.listKeys(eq(TENANT), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/private/kms/keys")
                        .param("limit", "10"))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.LIST_KEYS),
                isNull(), eq(USER), eq(IP));
    }

    @Test
    void updateKeyDescription_Success() throws Exception {
        UpdateKeyDescriptionRequest request = new UpdateKeyDescriptionRequest();
        request.setDescription("Updated description");

        UpdateKeyDescriptionResponse response = new UpdateKeyDescriptionResponse();

        when(keyManagementService.updateKeyDescription(eq(TENANT), eq(KEY_ID), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/private/kms/keys/{keyId}/description", KEY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.UPDATE_KEY_METADATA),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    @Test
    void enableKey_Success() throws Exception {
        when(keyManagementService.enableKey(eq(TENANT), eq(KEY_ID)))
                .thenReturn(new EnableKeyResponse());

        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/enable", KEY_ID))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.ENABLE_KEY),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    @Test
    void disableKey_Success() throws Exception {
        when(keyManagementService.disableKey(eq(TENANT), eq(KEY_ID)))
                .thenReturn(new DisableKeyResponse());

        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/disable", KEY_ID))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.DISABLE_KEY),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    @Test
    void scheduleKeyDeletion_Success() throws Exception {
        ScheduleKeyDeletionResponse response = new ScheduleKeyDeletionResponse();

        when(keyManagementService.scheduleKeyDeletion(eq(TENANT), eq(KEY_ID), anyInt()))
                .thenReturn(response);

        mockMvc.perform(delete("/api/v1/private/kms/keys/{keyId}/schedule-deletion", KEY_ID)
                        .param("pendingWindowInDays", "30"))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.SCHEDULE_KEY_DELETION),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    @Test
    void cancelKeyDeletion_Success() throws Exception {
        when(keyManagementService.cancelKeyDeletion(eq(TENANT), eq(KEY_ID)))
                .thenReturn(new CancelKeyDeletionResponse());

        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/cancel-deletion", KEY_ID))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.CANCEL_KEY_DELETION),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    @Test
    void deleteKey_Success() throws Exception {
        doNothing().when(keyManagementService).deleteKey(eq(TENANT), eq(KEY_ID));

        mockMvc.perform(delete("/api/v1/private/kms/keys/{keyId}", KEY_ID))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.DELETE_KEY),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    // =========================================================================
    // KEY ROTATION TESTS
    // =========================================================================

    @Test
    void rotateKey_Success() throws Exception {
        RotateKeyResponse response = new RotateKeyResponse();

        when(keyManagementService.rotateKey(eq(TENANT), eq(KEY_ID))).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/rotate", KEY_ID))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.ROTATE_KEY),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    @Test
    void getKeyRotationStatus_Success() throws Exception {
        GetKeyRotationStatusResponse response = new GetKeyRotationStatusResponse();

        when(keyManagementService.getKeyRotationStatus(eq(TENANT), eq(KEY_ID))).thenReturn(response);

        mockMvc.perform(get("/api/v1/private/kms/keys/{keyId}/rotation-status", KEY_ID))
                .andExpect(status().isOk());
    }

    @Test
    void enableKeyRotation_Success() throws Exception {
        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/rotate/enable", KEY_ID))
                .andExpect(status().isOk());
    }

    @Test
    void disableKeyRotation_Success() throws Exception {
        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/rotate/disable", KEY_ID))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // CRYPTOGRAPHIC OPERATIONS TESTS
    // =========================================================================

    @Test
    void encrypt_Success() throws Exception {
        EncryptRequest request = new EncryptRequest();
        request.setKeyId(KEY_ID);
        request.setPlaintext("SGVsbG8gV29ybGQ=");

        EncryptResponse response = EncryptResponse.builder()
                .ciphertextBlob("encrypted-data")
                .keyId(KEY_ID)
                .build();

        when(encryptionService.encrypt(eq(TENANT), any(EncryptRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/kms/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(auditService).logAction(eq(TENANT), eq(IKmsActionType.Types.ENCRYPT),
                eq(KEY_ID), eq(USER), eq(IP));
    }

    @Test
    void decrypt_Success() throws Exception {
        DecryptRequest request = new DecryptRequest();
        request.setCiphertextBlob("encrypted-data");
        request.setKeyId(KEY_ID);

        DecryptResponse response = DecryptResponse.builder()
                .plaintext("SGVsbG8gV29ybGQ=")
                .keyId(KEY_ID)
                .build();

        when(encryptionService.decrypt(eq(TENANT), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/kms/decrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void generateDataKey_Success() throws Exception {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setKeyId(KEY_ID);

        GenerateDataKeyResponse response = new GenerateDataKeyResponse();

        when(dataKeyService.generateDataKey(eq(TENANT), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/kms/datakey/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void sign_Success() throws Exception {
        SignRequest request = new SignRequest();
        request.setKeyId(KEY_ID);
        request.setMessage("test-message");

        SignResponse response = new SignResponse();

        when(signingService.sign(eq(TENANT), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/kms/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicKey_Success() throws Exception {
        GetPublicKeyResponse response = new GetPublicKeyResponse();

        when(keyManagementService.getPublicKey(eq(TENANT), eq(KEY_ID))).thenReturn(response);

        mockMvc.perform(get("/api/v1/private/kms/keys/{keyId}/public-key", KEY_ID))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // ALIASES, POLICIES, GRANTS, TAGS
    // =========================================================================

    @Test
    void createAlias_Success() throws Exception {
        CreateAliasRequest request = new CreateAliasRequest();
        request.setAliasName("alias/test-alias");
        request.setTargetKeyId(KEY_ID);

        mockMvc.perform(post("/api/v1/private/kms/aliases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void putKeyPolicy_Success() throws Exception {
        PutKeyPolicyRequest request = new PutKeyPolicyRequest();
        request.setPolicy(Map.of("Version", "2012-10-17"));

        mockMvc.perform(put("/api/v1/private/kms/keys/{keyId}/policy", KEY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void createGrant_Success() throws Exception {
        CreateGrantRequest request = new CreateGrantRequest();
        request.setGranteePrincipal("arn:aws:iam::123:role/test");
        request.setOperations(List.of("Encrypt", "Decrypt"));

        CreateGrantResponse response = new CreateGrantResponse();

        when(keyPolicyService.createGrant(eq(TENANT), eq(KEY_ID), any()))
                .thenReturn(new GrantResponseDto()); // adjust based on actual return

        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/grants", KEY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void tagResource_Success() throws Exception {
        TagResourceRequest request = new TagResourceRequest();
        request.setTags(List.of(new ListResourceTagsResponse.Tag()));

        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/tags", KEY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // CUSTOM KEY STORES
    // =========================================================================

    @Test
    void createCustomKeyStore_Success() throws Exception {
        CreateCustomKeyStoreRequest request = new CreateCustomKeyStoreRequest();
        request.setCustomKeyStoreName("test-store");

        when(customKeyStoreService.createCustomKeyStore(eq(TENANT), any()))
                .thenReturn(new CustomKeyStoreResponseDto());

        mockMvc.perform(post("/api/v1/private/kms/custom-key-stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // EXCEPTION HANDLING TESTS
    // =========================================================================

    @Test
    void createKey_Exception_ReturnsErrorResponse() throws Exception {
        CreateKeyRequest request = new CreateKeyRequest();

        when(keyManagementService.createKey(anyString(), any()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/v1/private/kms/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void describeKey_NotFound_Exception() throws Exception {
        when(keyManagementService.describeKey(eq(TENANT), eq("invalid-key"), any()))
                .thenThrow(new RuntimeException("Key not found"));

        mockMvc.perform(get("/api/v1/private/kms/keys/{keyId}", "invalid-key"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void encrypt_Exception_ReturnsError() throws Exception {
        EncryptRequest request = new EncryptRequest();
        request.setKeyId(KEY_ID);

        when(encryptionService.encrypt(eq(TENANT), any()))
                .thenThrow(new RuntimeException("Encryption failed"));

        mockMvc.perform(post("/api/v1/private/kms/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // =========================================================================
    // ADDITIONAL COVERAGE TESTS (Utility & Edge)
    // =========================================================================

    @Test
    void generateRandom_Success() throws Exception {
        GenerateRandomRequest request = new GenerateRandomRequest();
        request.setNumberOfBytes(32);

        mockMvc.perform(post("/api/v1/private/kms/random")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void validateKey_Success() throws Exception {
        when(keyManagementService.isValidKey(eq(TENANT), eq(KEY_ID))).thenReturn(true);

        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/validate", KEY_ID))
                .andExpect(status().isOk());
    }

    @Test
    void listAliases_Success() throws Exception {
        mockMvc.perform(get("/api/v1/private/kms/aliases"))
                .andExpect(status().isOk());
    }

    @Test
    void getAuditLogs_Success() throws Exception {
        mockMvc.perform(get("/api/v1/private/kms/keys/{keyId}/audit-logs", KEY_ID))
                .andExpect(status().isOk());
    }
}