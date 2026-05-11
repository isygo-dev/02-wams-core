package eu.isygoit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.*;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
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
        // Safe context setup
        when(requestContextService.getCurrentContext()).thenReturn(ContextRequestDto.builder()
                .senderTenant(TENANT)
                .senderUser(USER)
                .clientIp(IP)
                .build()
        );

        // === CRITICAL: Initialize MockMvc ===
        mockMvc = MockMvcBuilders.standaloneSetup(kmsController)
                .build();
    }

    // =========================================================================
    // KEY MANAGEMENT
    // =========================================================================

    @Test
    void createKey_Success() throws Exception {
        CreateKeyRequest request = new CreateKeyRequest(); // Use no-arg if builder missing
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
    }

    @Test
    void enableKey_Success() throws Exception {
        when(keyManagementService.enableKey(eq(TENANT), eq(KEY_ID)))
                .thenReturn(new EnableKeyResponse());

        mockMvc.perform(post("/api/v1/private/kms/keys/{keyId}/enable", KEY_ID))
                .andExpect(status().isOk());
    }

    // Add similar tests for other methods...

    @Test
    void encrypt_Success() throws Exception {
        EncryptRequest request = new EncryptRequest();
        request.setKeyId(KEY_ID);
        request.setPlaintext("SGVsbG8=");

        EncryptResponse response = EncryptResponse.builder()
                .ciphertextBlob("encrypted-data")
                .keyId(KEY_ID)
                .build();

        when(encryptionService.encrypt(eq(TENANT), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/private/kms/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ... (keep other test methods as before, but ensure they use simple constructors where possible)

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
                .andExpect(status().isInternalServerError()); // or NotFound depending on handler
    }
}