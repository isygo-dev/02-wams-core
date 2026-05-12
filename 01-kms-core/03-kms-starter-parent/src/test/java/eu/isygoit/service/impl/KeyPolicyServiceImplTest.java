package eu.isygoit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.exception.GrantNotFoundException;
import eu.isygoit.model.KmsKeyGrant;
import eu.isygoit.model.KmsKeyPolicy;
import eu.isygoit.repository.KmsKeyGrantRepository;
import eu.isygoit.repository.KmsKeyPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for KeyPolicyServiceImpl
 * Tests all methods including autowired services with complete coverage
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeyPolicyServiceImpl Tests")
class KeyPolicyServiceImplTest {

    @Mock
    private KmsKeyPolicyRepository kmsKeyPolicyRepository;

    @Mock
    private KmsKeyGrantRepository kmsKeyGrantRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KeyPolicyServiceImpl keyPolicyService;

    private String testTenant;
    private String testKeyId;
    private String testGrantId;
    private String testPrincipal;

    @BeforeEach
    void setUp() {
        testTenant = "test-tenant";
        testKeyId = "test-key-id";
        testGrantId = "grant-12345";
        testPrincipal = "arn:aws:iam::123456789012:role/test-role";
    }

    // =========================================================================
    // SetKeyPolicy Tests
    // =========================================================================

    @Test
    @DisplayName("Should set key policy successfully for new key")
    void testSetKeyPolicyNewKey() throws Exception {
        // Arrange
        Map<String, Object> policy = new HashMap<>();
        policy.put("Version", "2012-10-17");
        policy.put("Statement", new ArrayList<>());

        SetKeyPolicyRequestDto request = SetKeyPolicyRequestDto.builder()
                .policyName("default")
                .policy(policy)
                .bypassPolicyLockoutSafetyCheck(false)
                .build();

        when(kmsKeyPolicyRepository.findByTenantAndKeyId(testTenant, testKeyId))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(policy))
                .thenReturn("{\"Version\":\"2012-10-17\"}");

        KmsKeyPolicy savedPolicy = KmsKeyPolicy.builder()
                .id(1L)
                .tenant(testTenant)
                .keyId(testKeyId)
                .policyDocument("{\"Version\":\"2012-10-17\"}")
                .policyVersion("2012-10-17")
                .build();

        when(kmsKeyPolicyRepository.save(any(KmsKeyPolicy.class))).thenReturn(savedPolicy);

        // Act
        Map<String, Object> result = keyPolicyService.setKeyPolicy(testTenant, testKeyId, request);

        // Assert
        assertNotNull(result);
        assertEquals(policy, result);
        verify(kmsKeyPolicyRepository, times(1)).findByTenantAndKeyId(testTenant, testKeyId);
        verify(kmsKeyPolicyRepository, times(1)).save(any(KmsKeyPolicy.class));
        verify(objectMapper, times(1)).writeValueAsString(policy);
    }

    @Test
    @DisplayName("Should update existing key policy")
    void testSetKeyPolicyExistingKey() throws Exception {
        // Arrange
        Map<String, Object> policy = new HashMap<>();
        policy.put("Version", "2012-10-17");

        SetKeyPolicyRequestDto request = SetKeyPolicyRequestDto.builder()
                .policy(policy)
                .build();

        KmsKeyPolicy existingPolicy = KmsKeyPolicy.builder()
                .id(1L)
                .tenant(testTenant)
                .keyId(testKeyId)
                .policyDocument("{\"old\":\"policy\"}")
                .build();

        when(kmsKeyPolicyRepository.findByTenantAndKeyId(testTenant, testKeyId))
                .thenReturn(Optional.of(existingPolicy));
        when(objectMapper.writeValueAsString(policy))
                .thenReturn("{\"Version\":\"2012-10-17\"}");
        when(kmsKeyPolicyRepository.save(any(KmsKeyPolicy.class))).thenReturn(existingPolicy);

        // Act
        Map<String, Object> result = keyPolicyService.setKeyPolicy(testTenant, testKeyId, request);

        // Assert
        assertNotNull(result);
        assertEquals(policy, result);
        verify(kmsKeyPolicyRepository, times(1)).save(any(KmsKeyPolicy.class));
    }

    // =========================================================================
    // GetKeyPolicy Tests
    // =========================================================================

    @Test
    @DisplayName("Should get existing key policy")
    void testGetKeyPolicySuccess() throws Exception {
        // Arrange
        String policyJson = "{\"Version\":\"2012-10-17\",\"Statement\":[]}";
        Map<String, Object> expectedPolicy = new HashMap<>();
        expectedPolicy.put("Version", "2012-10-17");
        expectedPolicy.put("Statement", new ArrayList<>());

        KmsKeyPolicy policy = KmsKeyPolicy.builder()
                .id(1L)
                .tenant(testTenant)
                .keyId(testKeyId)
                .policyDocument(policyJson)
                .build();

        when(kmsKeyPolicyRepository.findByTenantAndKeyId(testTenant, testKeyId))
                .thenReturn(Optional.of(policy));
        when(objectMapper.readValue(eq(policyJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(expectedPolicy);

        // Act
        Map<String, Object> result = keyPolicyService.getKeyPolicy(testTenant, testKeyId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedPolicy, result);
        verify(kmsKeyPolicyRepository, times(1)).findByTenantAndKeyId(testTenant, testKeyId);
    }

    @Test
    @DisplayName("Should return default policy when key policy not found")
    void testGetKeyPolicyNotFound() {
        // Arrange
        when(kmsKeyPolicyRepository.findByTenantAndKeyId(testTenant, testKeyId))
                .thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = keyPolicyService.getKeyPolicy(testTenant, testKeyId);

        // Assert
        assertNotNull(result);
        assertEquals("2012-10-17", result.get("Version"));
        assertNotNull(result.get("Statement"));
    }

    // =========================================================================
    // CreateGrant Tests
    // =========================================================================

    @Test
    @DisplayName("Should create grant successfully")
    void testCreateGrantSuccess() {
        // Arrange
        List<String> operations = Arrays.asList("Encrypt", "Decrypt");

        CreateGrantRequestDto request = CreateGrantRequestDto.builder()
                .principal(testPrincipal)
                .operations(operations)
                .build();

        when(kmsKeyGrantRepository.save(any(KmsKeyGrant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        GrantResponseDto result = keyPolicyService.createGrant(testTenant, testKeyId, request);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getGrantId());
        assertTrue(result.getGrantId().startsWith("grant-"));
        assertEquals(testKeyId, result.getKeyId());
        verify(kmsKeyGrantRepository, times(1)).save(any(KmsKeyGrant.class));
    }

    @Test
    @DisplayName("Should create multiple grants with different principals")
    void testCreateMultipleGrants() {
        // Arrange
        CreateGrantRequestDto request1 = CreateGrantRequestDto.builder()
                .principal("arn:aws:iam::123456789012:role/role1")
                .operations(Arrays.asList("Encrypt"))
                .build();

        CreateGrantRequestDto request2 = CreateGrantRequestDto.builder()
                .principal("arn:aws:iam::123456789012:role/role2")
                .operations(Arrays.asList("Decrypt"))
                .build();

        when(kmsKeyGrantRepository.save(any(KmsKeyGrant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        GrantResponseDto result1 = keyPolicyService.createGrant(testTenant, testKeyId, request1);
        GrantResponseDto result2 = keyPolicyService.createGrant(testTenant, testKeyId, request2);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1.getGrantId(), result2.getGrantId());
        verify(kmsKeyGrantRepository, times(2)).save(any(KmsKeyGrant.class));
    }

    // =========================================================================
    // RevokeGrant Tests
    // =========================================================================

    @Test
    @DisplayName("Should revoke grant successfully")
    void testRevokeGrantSuccess() {
        // Arrange
        KmsKeyGrant grant = KmsKeyGrant.builder()
                .id(1L)
                .tenant(testTenant)
                .keyId(testKeyId)
                .grantId(testGrantId)
                .principal(testPrincipal)
                .status("ACTIVE")
                .operations("Encrypt,Decrypt")
                .build();

        when(kmsKeyGrantRepository.findByTenantAndGrantId(testTenant, testGrantId))
                .thenReturn(Optional.of(grant));
        when(kmsKeyGrantRepository.save(any(KmsKeyGrant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String result = keyPolicyService.revokeGrant(testTenant, testKeyId, testGrantId);

        // Assert
        assertEquals("REVOKED", result);
        verify(kmsKeyGrantRepository, times(1)).findByTenantAndGrantId(testTenant, testGrantId);
        verify(kmsKeyGrantRepository, times(1)).save(any(KmsKeyGrant.class));
    }

    @Test
    @DisplayName("Should throw exception when revoking non-existent grant")
    void testRevokeGrantNotFound() {
        // Arrange
        when(kmsKeyGrantRepository.findByTenantAndGrantId(testTenant, testGrantId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(GrantNotFoundException.class, () ->
                keyPolicyService.revokeGrant(testTenant, testKeyId, testGrantId));
    }

    // =========================================================================
    // ListGrants Tests
    // =========================================================================

    @Test
    @DisplayName("Should list grants successfully")
    void testListGrantsSuccess() {
        // Arrange
        KmsKeyGrant grant1 = KmsKeyGrant.builder()
                .id(1L)
                .tenant(testTenant)
                .keyId(testKeyId)
                .grantId("grant-1")
                .principal("arn:aws:iam::123456789012:role/role1")
                .operations("Encrypt,Decrypt")
                .creationDate(LocalDateTime.now())
                .build();

        KmsKeyGrant grant2 = KmsKeyGrant.builder()
                .id(2L)
                .tenant(testTenant)
                .keyId(testKeyId)
                .grantId("grant-2")
                .principal("arn:aws:iam::123456789012:role/role2")
                .operations("Sign")
                .creationDate(LocalDateTime.now())
                .build();

        Page<KmsKeyGrant> page = new PageImpl<>(Arrays.asList(grant1, grant2));

        when(kmsKeyGrantRepository.findByTenantAndKeyId(eq(testTenant), eq(testKeyId), any(Pageable.class)))
                .thenReturn(page);

        // Act
        ListGrantsResponseDto result = keyPolicyService.listGrants(testTenant, testKeyId, 100, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getGrants().size());
        assertEquals("grant-1", result.getGrants().get(0).getGrantId());
        assertEquals("grant-2", result.getGrants().get(1).getGrantId());
        verify(kmsKeyGrantRepository, times(1)).findByTenantAndKeyId(eq(testTenant), eq(testKeyId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty list when no grants exist")
    void testListGrantsEmpty() {
        // Arrange
        Page<KmsKeyGrant> page = new PageImpl<>(new ArrayList<>());

        when(kmsKeyGrantRepository.findByTenantAndKeyId(eq(testTenant), eq(testKeyId), any(Pageable.class)))
                .thenReturn(page);

        // Act
        ListGrantsResponseDto result = keyPolicyService.listGrants(testTenant, testKeyId, 100, null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getGrants().size());
        assertNull(result.getNextToken());
    }

    @Test
    @DisplayName("Should paginate grants correctly")
    void testListGrantsPagination() {
        // Arrange
        List<KmsKeyGrant> firstPageGrants = Arrays.asList(
                createMockGrant(1, "grant-1"),
                createMockGrant(2, "grant-2")
        );

        Page<KmsKeyGrant> firstPage = new PageImpl<>(firstPageGrants, PageRequest.of(0, 2), 10);

        when(kmsKeyGrantRepository.findByTenantAndKeyId(eq(testTenant), eq(testKeyId), any(Pageable.class)))
                .thenReturn(firstPage);

        // Act
        ListGrantsResponseDto result = keyPolicyService.listGrants(testTenant, testKeyId, 2, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getGrants().size());
        assertNotNull(result.getNextToken());
        assertEquals("1", result.getNextToken());
    }

    // =========================================================================
    // RetireGrant Tests
    // =========================================================================

    @Test
    @DisplayName("Should retire grant successfully")
    void testRetireGrantSuccess() {
        // Arrange
        KmsKeyGrant grant = KmsKeyGrant.builder()
                .id(1L)
                .tenant(testTenant)
                .keyId(testKeyId)
                .grantId(testGrantId)
                .principal(testPrincipal)
                .status("ACTIVE")
                .operations("Encrypt,Decrypt")
                .build();

        RetireGrantRequestDto request = RetireGrantRequestDto.builder()
                .grantToken(testGrantId)
                .retiringPrincipal(testPrincipal)
                .build();

        when(kmsKeyGrantRepository.findByTenantAndGrantId(testTenant, testGrantId))
                .thenReturn(Optional.of(grant));
        when(kmsKeyGrantRepository.save(any(KmsKeyGrant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RetireGrantResponse result = keyPolicyService.retireGrant(testTenant, testGrantId, request);

        // Assert
        assertNotNull(result);
        assertEquals(testKeyId, result.getKeyId());
        verify(kmsKeyGrantRepository, times(1)).findByTenantAndGrantId(testTenant, testGrantId);
        verify(kmsKeyGrantRepository, times(1)).save(any(KmsKeyGrant.class));
    }

    @Test
    @DisplayName("Should throw exception when retiring non-existent grant")
    void testRetireGrantNotFound() {
        // Arrange
        RetireGrantRequestDto request = RetireGrantRequestDto.builder()
                .grantToken(testGrantId)
                .build();

        when(kmsKeyGrantRepository.findByTenantAndGrantId(testTenant, testGrantId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(GrantNotFoundException.class, () ->
                keyPolicyService.retireGrant(testTenant, testGrantId, request));
    }

    // =========================================================================
    // ListRetirableGrants Tests
    // =========================================================================

    @Test
    @DisplayName("Should list retirable grants successfully")
    void testListRetirableGrantsSuccess() {
        // Arrange
        KmsKeyGrant grant1 = createMockGrant(1, "grant-1");
        grant1.setStatus("ACTIVE");

        KmsKeyGrant grant2 = createMockGrant(2, "grant-2");
        grant2.setStatus("ACTIVE");

        Page<KmsKeyGrant> page = new PageImpl<>(Arrays.asList(grant1, grant2));

        when(kmsKeyGrantRepository.findByTenantAndPrincipalAndStatus(
                eq(testTenant), eq(testPrincipal), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(page);

        // Act
        ListRetirableGrantsResponse result = keyPolicyService.listRetirableGrants(testTenant, testPrincipal, 100, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getGrants().size());
        assertFalse(result.getTruncated());
        assertNull(result.getNextMarker());
    }

    @Test
    @DisplayName("Should return empty list when no retirable grants exist")
    void testListRetirableGrantsEmpty() {
        // Arrange
        Page<KmsKeyGrant> page = new PageImpl<>(new ArrayList<>());

        when(kmsKeyGrantRepository.findByTenantAndPrincipalAndStatus(
                eq(testTenant), eq(testPrincipal), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(page);

        // Act
        ListRetirableGrantsResponse result = keyPolicyService.listRetirableGrants(testTenant, testPrincipal, 100, null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getGrants().size());
        assertFalse(result.getTruncated());
    }

    @Test
    @DisplayName("Should paginate retirable grants correctly")
    void testListRetirableGrantsPagination() {
        // Arrange
        List<KmsKeyGrant> firstPageGrants = Arrays.asList(
                createMockGrant(1, "grant-1"),
                createMockGrant(2, "grant-2")
        );

        Page<KmsKeyGrant> firstPage = new PageImpl<>(firstPageGrants, PageRequest.of(0, 2), 10);

        when(kmsKeyGrantRepository.findByTenantAndPrincipalAndStatus(
                eq(testTenant), eq(testPrincipal), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(firstPage);

        // Act
        ListRetirableGrantsResponse result = keyPolicyService.listRetirableGrants(testTenant, testPrincipal, 2, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getGrants().size());
        assertTrue(result.getTruncated());
        assertEquals("1", result.getNextMarker());
    }

    // =========================================================================
    // ListKeyPolicies Tests
    // =========================================================================

    @Test
    @DisplayName("Should list key policies when policy exists")
    void testListKeyPoliciesExists() {
        // Arrange
        KmsKeyPolicy policy = KmsKeyPolicy.builder()
                .id(1L)
                .tenant(testTenant)
                .keyId(testKeyId)
                .policyDocument("{}")
                .build();

        when(kmsKeyPolicyRepository.findByTenantAndKeyId(testTenant, testKeyId))
                .thenReturn(Optional.of(policy));

        // Act
        ListKeyPoliciesResponse result = keyPolicyService.listKeyPolicies(testTenant, testKeyId, 100, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getPolicyNames().size());
        assertEquals("default", result.getPolicyNames().get(0));
        assertFalse(result.getTruncated());
        assertNull(result.getNextMarker());
    }

    @Test
    @DisplayName("Should return empty list when no policy exists")
    void testListKeyPoliciesNotFound() {
        // Arrange
        when(kmsKeyPolicyRepository.findByTenantAndKeyId(testTenant, testKeyId))
                .thenReturn(Optional.empty());

        // Act
        ListKeyPoliciesResponse result = keyPolicyService.listKeyPolicies(testTenant, testKeyId, 100, null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getPolicyNames().size());
        assertFalse(result.getTruncated());
        assertNull(result.getNextMarker());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private KmsKeyGrant createMockGrant(long id, String grantId) {
        return KmsKeyGrant.builder()
                .id(id)
                .tenant(testTenant)
                .keyId(testKeyId)
                .grantId(grantId)
                .principal(testPrincipal)
                .operations("Encrypt,Decrypt")
                .status("ACTIVE")
                .creationDate(LocalDateTime.now())
                .build();
    }
}

