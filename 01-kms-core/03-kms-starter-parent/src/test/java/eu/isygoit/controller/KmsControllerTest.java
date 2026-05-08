package eu.isygoit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.*;
import eu.isygoit.enums.*;
import eu.isygoit.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class KmsControllerTest {

    private final String tenant = "test-tenant" ;
    private final String user = "test-user" ;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private IKeyService keyService;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(kmsController).build();

        // Deep mock for requestContextService.getCurrentContext().getSenderTenant() etc.
        when(requestContextService.getCurrentContext()).thenReturn(org.mockito.Mockito.mock(org.mockito.Answers.RETURNS_DEEP_STUBS));

        try {
            when(requestContextService.getCurrentContext().getSenderTenant()).thenReturn(tenant);
            when(requestContextService.getCurrentContext().getSenderUser()).thenReturn(user);
            when(requestContextService.getCurrentContext().getClientIp()).thenReturn("127.0.0.1");
        } catch (Throwable t) {
            // Fallback
        }
    }

    @Test
    void testCreateKey() throws Exception {
        CreateKeyRequestDto request = CreateKeyRequestDto.builder()
                .description("Test Key")
                .keySpec(IEnumKeySpec.Types.AES_256)
                .purpose(IEnumKeyPurpose.Types.ENCRYPT_DECRYPT)
                .build();
        CreateKeyResponseDto response = CreateKeyResponseDto.builder()
                .keyId(1L)
                .arn("arn:kms:test:1")
                .build();

        when(keyManagementService.createKey(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/keys")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keyId").value(1L));
    }

    @Test
    void testDescribeKey() throws Exception {
        KeyMetadataResponseDto response = KeyMetadataResponseDto.builder()
                .keyId(1L)
                .description("Test Key")
                .build();

        when(keyManagementService.getKeyMetadata(tenant, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/private/key/keys/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.keyId").value(1L));
    }

    @Test
    void testEncrypt() throws Exception {
        EncryptRequestDto request = EncryptRequestDto.builder()
                .keyId(1L)
                .plaintext("SGVsbG8=")
                .build();
        EncryptResponseDto response = EncryptResponseDto.builder()
                .ciphertext("Q2lwaGVy")
                .keyId(1L)
                .build();

        when(encryptionService.encrypt(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/encrypt")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ciphertext").value("Q2lwaGVy"));
    }

    @Test
    void testGenerateDataKey() throws Exception {
        GenerateDataKeyRequestDto request = GenerateDataKeyRequestDto.builder()
                .keyId(1L)
                .build();
        DataKeyResponseDto response = DataKeyResponseDto.builder()
                .keyId(1L)
                .plaintextKey("cGxhaW50ZXh0")
                .encryptedKey("Y2lwaGVydGV4dA==")
                .build();

        when(dataKeyService.generateDataKey(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/datakey/generate")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plaintextKey").value("cGxhaW50ZXh0"));
    }

    @Test
    void testCreateAlias() throws Exception {
        CreateAliasRequestDto request = CreateAliasRequestDto.builder()
                .aliasName("alias/test")
                .targetKeyId(1L)
                .build();
        AliasResponseDto response = AliasResponseDto.builder()
                .aliasName("alias/test")
                .targetKeyId(1L)
                .build();

        when(keyManagementService.createAlias(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/aliases")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aliasName").value("alias/test"));
    }

    @Test
    void testCreateGrant() throws Exception {
        CreateGrantRequestDto request = CreateGrantRequestDto.builder()
                .principal("user1")
                .operations(Arrays.asList("ENCRYPT"))
                .build();
        GrantResponseDto response = GrantResponseDto.builder()
                .grantId("grant-1")
                .build();

        when(keyPolicyService.createGrant(eq(tenant), eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/keys/1/grants")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantId").value("grant-1"));
    }

    @Test
    void testUpdatePrimaryRegion() throws Exception {
        UpdatePrimaryRegionRequestDto request = UpdatePrimaryRegionRequestDto.builder()
                .primaryRegion("us-east-1")
                .build();
        KeyMetadataResponseDto response = KeyMetadataResponseDto.builder()
                .keyId(1L)
                .description("Multi-region key")
                .build();

        when(multiRegionService.updatePrimaryRegion(eq(tenant), eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/private/key/keys/1/primary-region")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyId").value(1L));
    }

    @Test
    void testDecrypt() throws Exception {
        DecryptRequestDto request = DecryptRequestDto.builder()
                .ciphertext("Q2lwaGVy")
                .build();
        DecryptResponseDto response = DecryptResponseDto.builder()
                .plaintext("SGVsbG8=")
                .keyId(1L)
                .build();

        when(encryptionService.decrypt(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/decrypt")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plaintext").value("SGVsbG8="));
    }

    @Test
    void testSign() throws Exception {
        SignRequestDto request = SignRequestDto.builder()
                .keyId(1L)
                .message("SGVsbG8=")
                .algorithm(IEnumSigningAlgorithm.Types.RSASSA_PSS_SHA256)
                .build();
        SignResponseDto response = SignResponseDto.builder()
                .signature("U2lnbmF0dXJl")
                .keyId(1L)
                .build();

        when(signingService.sign(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/sign")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signature").value("U2lnbmF0dXJl"));
    }

    @Test
    void testVerify() throws Exception {
        VerifyRequestDto request = VerifyRequestDto.builder()
                .keyId(1L)
                .message("SGVsbG8=")
                .signature("U2lnbmF0dXJl")
                .algorithm(IEnumSigningAlgorithm.Types.RSASSA_PSS_SHA256)
                .build();
        VerifyResponseDto response = VerifyResponseDto.builder()
                .valid(true)
                .build();

        when(signingService.verify(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/verify")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void testListKeys() throws Exception {
        ListKeysResponseDto response = ListKeysResponseDto.builder()
                .keys(Arrays.asList(ListKeysResponseDto.KeySummaryDto.builder().keyId(1L).alias("alias/1").build()))
                .build();

        when(keyManagementService.listKeys(any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/private/key/keys")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].keyId").value(1L));
    }

    @Test
    void testEnableKey() throws Exception {
        KeyMetadataResponseDto response = KeyMetadataResponseDto.builder()
                .keyId(1L)
                .status(IEnumKeyStatus.Types.ENABLED)
                .build();

        when(keyManagementService.enableKey(tenant, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/private/key/keys/1/enable")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENABLED"));
    }

    @Test
    void testDisableKey() throws Exception {
        KeyMetadataResponseDto response = KeyMetadataResponseDto.builder()
                .keyId(1L)
                .status(IEnumKeyStatus.Types.DISABLED)
                .build();

        when(keyManagementService.disableKey(tenant, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/private/key/keys/1/disable")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void testScheduleKeyDeletion() throws Exception {
        KeyMetadataResponseDto response = KeyMetadataResponseDto.builder()
                .keyId(1L)
                .status(IEnumKeyStatus.Types.PENDING_DELETION)
                .build();

        when(keyManagementService.scheduleKeyDeletion(eq(tenant), eq(1L), anyInt())).thenReturn(response);

        mockMvc.perform(delete("/api/v1/private/key/keys/1")
                        .param("pendingWindowInDays", "7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyId").value(1L));
    }

    @Test
    void testCancelKeyDeletion() throws Exception {
        KeyMetadataResponseDto response = KeyMetadataResponseDto.builder()
                .keyId(1L)
                .status(IEnumKeyStatus.Types.DISABLED)
                .build();

        when(keyManagementService.cancelKeyDeletion(tenant, 1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/keys/1/cancel-deletion")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyId").value(1L));
    }

    @Test
    void testDeleteKey() throws Exception {
        mockMvc.perform(delete("/api/v1/private/key/keys/1/delete")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetKeyRotationStatus() throws Exception {
        KeyRotationStatusResponseDto response = KeyRotationStatusResponseDto.builder()
                .keyId(1L)
                .rotationEnabled(true)
                .build();

        when(keyManagementService.getKeyRotationStatus(tenant, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/private/key/keys/1/rotation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rotationEnabled").value(true));
    }

    @Test
    void testRotateKey() throws Exception {
        RotateKeyResponseDto response = RotateKeyResponseDto.builder()
                .keyId(1L)
                .newVersion("v2")
                .build();

        when(keyManagementService.rotateKey(tenant, 1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/keys/1/rotate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newVersion").value("v2"));
    }

    @Test
    void testListAliases() throws Exception {
        ListAliasesResponseDto response = ListAliasesResponseDto.builder()
                .aliases(Arrays.asList(AliasResponseDto.builder().aliasName("alias/test").build()))
                .build();

        when(keyManagementService.listAliases(any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/private/key/aliases")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aliases[0].aliasName").value("alias/test"));
    }

    @Test
    void testDeleteAlias() throws Exception {
        mockMvc.perform(delete("/api/v1/private/key/aliases/alias%2Ftest")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void testCreateCustomKeyStore() throws Exception {
        CreateCustomKeyStoreRequestDto request = CreateCustomKeyStoreRequestDto.builder()
                .keyStoreName("MyKeyStore")
                .type(IEnumCustomKeyStoreType.Types.CLOUDHSM)
                .cloudHsmClusterId("cluster-1")
                .keyStorePassword("password123")
                .trustAnchorCertificate("-----BEGIN CERTIFICATE-----\nMIIDXTCCAkWgAwIBAgIJAJC1/iNAZwqDMA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV\n-----END CERTIFICATE-----")
                .build();
        CustomKeyStoreResponseDto response = CustomKeyStoreResponseDto.builder()
                .keyStoreId(1L)
                .build();

        when(customKeyStoreService.createCustomKeyStore(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/custom-key-stores")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keyStoreId").value(1L));
    }

    @Test
    void testReEncrypt() throws Exception {
        ReEncryptRequestDto request = ReEncryptRequestDto.builder()
                .ciphertextBlob("old-cipher")
                .destinationKeyId(2L)
                .build();
        ReEncryptResponseDto response = ReEncryptResponseDto.builder()
                .ciphertext("new-cipher")
                .sourceKeyId(2L)
                .sourceKeyId(1L)
                .build();

        when(encryptionService.reEncrypt(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/reencrypt")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ciphertext").value("new-cipher"));
    }

    @Test
    void testGenerateDataKeyPair() throws Exception {
        GenerateDataKeyPairRequestDto request = GenerateDataKeyPairRequestDto.builder()
                .keyId(1L)
                .keySpec("RSA_2048")
                .build();
        DataKeyPairResponseDto response = DataKeyPairResponseDto.builder()
                .keyId(1L)
                .publicKey("public-key")
                .encryptedPrivateKey("encrypted-private-key")
                .privateKey("plaintext-private-key")
                .keySpec("RSA_2048")
                .build();

        when(dataKeyService.generateDataKeyPair(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/datakey/generate-pair")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("public-key"));
    }

    @Test
    void testGenerateMac() throws Exception {
        GenerateMacRequestDto request = GenerateMacRequestDto.builder()
                .keyId(1L)
                .message("message")
                .macAlgorithm("HMAC_SHA_256")
                .build();
        GenerateMacResponseDto response = GenerateMacResponseDto.builder()
                .keyId(1L)
                .mac("mac-value")
                .macAlgorithm("HMAC_SHA_256")
                .macLength(32)
                .isHighSecurity(true)
                .build();

        when(signingService.generateMac(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/mac/generate")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mac").value("mac-value"));
    }

    @Test
    void testVerifyMac() throws Exception {
        VerifyMacRequestDto request = VerifyMacRequestDto.builder()
                .keyId(1L)
                .message("message")
                .mac("mac-value")
                .macAlgorithm("HMAC_SHA_256")
                .build();
        VerifyMacResponseDto response = VerifyMacResponseDto.builder()
                .keyId(1L)
                .macValid(true)
                .build();

        when(signingService.verifyMac(eq(tenant), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/mac/verify")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.macValid").value(true));
    }

    @Test
    void testGetPublicKey() throws Exception {
        PublicKeyResponseDto response = PublicKeyResponseDto.builder()
                .keyId(1L)
                .publicKey("public-key-material")
                .build();

        when(keyManagementService.getPublicKey(tenant, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/private/key/keys/1/public-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("public-key-material"));
    }

    @Test
    void testListKeyVersions() throws Exception {
        KeyVersionListResponseDto response = KeyVersionListResponseDto.builder()
                .versions(Arrays.asList(KeyVersionListResponseDto.KeyVersionDto.builder().versionId("v1").build()))
                .build();

        when(keyVersionService.listKeyVersions(tenant, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/private/key/keys/1/versions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions[0].versionId").value("v1"));
    }

    @Test
    void testReplicateKey() throws Exception {
        ReplicateKeyRequestDto request = ReplicateKeyRequestDto.builder()
                .replicaRegion("us-west-2")
                .build();
        ReplicateKeyResponseDto response = ReplicateKeyResponseDto.builder()
                .replicaKeyId("repid")
                .build();

        when(multiRegionService.replicateKey(eq(tenant), eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/key/keys/1/replicate")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replicaKeyId").value("repid"));
    }

    @Test
    void testTagResource() throws Exception {
        TagResourceRequestDto request = TagResourceRequestDto.builder()
                .tags(Map.of("Env", "Prod"))
                .build();

        mockMvc.perform(post("/api/v1/private/key/keys/1/tags")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAuditLogs() throws Exception {
        AuditLogResponseDto response = AuditLogResponseDto.builder()
                .logs(Arrays.asList(
                        AuditLogResponseDto.AuditLogEntryDto.builder()
                                .action("ENCRYPT")
                                .build()
                ))
                .build();

        when(auditService.getAuditLogs(any(), eq("1"), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/private/key/audit/logs")
                        .param("keyId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())// ← Very useful for debugging
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.logs[0].action").value("ENCRYPT"))
                .andExpect(jsonPath("$.logs[0].action").exists());
    }

    @Test
    void testGenerateRandomData() throws Exception {
        when(keyService.generateRandomKey(any(), anyInt(), any())).thenReturn("random-string");

        mockMvc.perform(get("/api/v1/private/key/random")
                        .param("length", "32")
                        .param("charSetType", IEnumCharSet.Types.ALPHANUM.name())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("random-string"));
    }

    @Test
    void testValidateKey() throws Exception {
        mockMvc.perform(post("/api/v1/private/key/keys/1/validate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
