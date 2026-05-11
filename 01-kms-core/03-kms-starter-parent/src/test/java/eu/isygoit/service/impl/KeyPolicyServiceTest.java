package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.KmsDtos.CreateGrantRequestDto;
import eu.isygoit.dto.KmsDtos.GrantResponseDto;
import eu.isygoit.dto.KmsDtos.ListGrantsResponseDto;
import eu.isygoit.dto.KmsDtos.RetireGrantRequestDto;
import eu.isygoit.dto.KmsDtos.SetKeyPolicyRequestDto;
import eu.isygoit.exception.GrantNotFoundException;
import eu.isygoit.model.KmsKeyGrant;
import eu.isygoit.model.KmsKeyPolicy;
import eu.isygoit.repository.KmsKeyGrantRepository;
import eu.isygoit.repository.KmsKeyPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeyPolicyServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String KEY_ID = "key-1";
    private static final String GRANT_ID = "grant-1";

    @Mock
    private KmsKeyPolicyRepository kmsKeyPolicyRepository;

    @Mock
    private KmsKeyGrantRepository kmsKeyGrantRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KeyPolicyServiceImpl service;

    private KmsKeyPolicy policy;
    private KmsKeyGrant grant;

    @BeforeEach
    void setUp() {

        policy = KmsKeyPolicy.builder()
                .tenant(TENANT)
                .keyId(KEY_ID)
                .policyDocument("{\"Version\":\"2012-10-17\"}")
                .policyVersion("2012-10-17")
                .build();

        grant = KmsKeyGrant.builder()
                .tenant(TENANT)
                .keyId(KEY_ID)
                .grantId(GRANT_ID)
                .principal("user-1")
                .operations("Encrypt,Decrypt")
                .status("ACTIVE")
                .creationDate(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldSetKeyPolicySuccessfully() throws Exception {

        Map<String, Object> map = new HashMap<>();
        map.put("Version", "2012-10-17");

        SetKeyPolicyRequestDto request =
                SetKeyPolicyRequestDto.builder()
                        .policy(map)
                        .build();

        when(objectMapper.writeValueAsString(map))
                .thenReturn("{\"Version\":\"2012-10-17\"}");

        when(kmsKeyPolicyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(policy));

        Map<String, Object> response =
                service.setKeyPolicy(TENANT, KEY_ID, request);

        assertNotNull(response);
        assertEquals("2012-10-17", response.get("Version"));

        verify(kmsKeyPolicyRepository).save(any(KmsKeyPolicy.class));
    }

    @Test
    void shouldCreateNewPolicyWhenNotExists() throws Exception {

        Map<String, Object> map = new HashMap<>();
        map.put("Version", "2012-10-17");

        SetKeyPolicyRequestDto request =
                SetKeyPolicyRequestDto.builder()
                        .policy(map)
                        .build();

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"Version\":\"2012-10-17\"}");

        when(kmsKeyPolicyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.empty());

        Map<String, Object> response =
                service.setKeyPolicy(TENANT, KEY_ID, request);

        assertNotNull(response);

        verify(kmsKeyPolicyRepository).save(any(KmsKeyPolicy.class));
    }

    @Test
    void shouldThrowWhenPolicySerializationFails() throws Exception {

        SetKeyPolicyRequestDto request =
                SetKeyPolicyRequestDto.builder()
                        .policy(Map.of("k", "v"))
                        .build();

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialization error") {
                });

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.setKeyPolicy(TENANT, KEY_ID, request)
        );

        assertEquals("Failed to serialize policy", exception.getMessage());
    }

    @Test
    void shouldGetKeyPolicySuccessfully() throws Exception {

        Map<String, Object> expected = new HashMap<>();
        expected.put("Version", "2012-10-17");

        when(kmsKeyPolicyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(policy));

        when(objectMapper.readValue(
                eq(policy.getPolicyDocument()),
                any(TypeReference.class)))
                .thenReturn(expected);

        Map<String, Object> response =
                service.getKeyPolicy(TENANT, KEY_ID);

        assertNotNull(response);
        assertEquals("2012-10-17", response.get("Version"));
    }

    @Test
    void shouldReturnEmptyPolicyWhenPolicyNotFound() {

        when(kmsKeyPolicyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.empty());

        Map<String, Object> response =
                service.getKeyPolicy(TENANT, KEY_ID);

        assertNotNull(response);
        assertEquals("2012-10-17", response.get("Version"));
        assertTrue(response.containsKey("Statement"));
    }

    @Test
    void shouldReturnEmptyMapWhenDeserializationFails() throws Exception {

        when(kmsKeyPolicyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(policy));

        when(objectMapper.readValue(
                anyString(),
                any(TypeReference.class)))
                .thenThrow(new JsonProcessingException("error") {
                });

        Map<String, Object> response =
                service.getKeyPolicy(TENANT, KEY_ID);

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldCreateGrantSuccessfully() {

        CreateGrantRequestDto request =
                CreateGrantRequestDto.builder()
                        .principal("user-1")
                        .operations(List.of("Encrypt", "Decrypt"))
                        .build();

        GrantResponseDto response =
                service.createGrant(TENANT, KEY_ID, request);

        assertNotNull(response);
        assertNotNull(response.getGrantId());
        assertEquals(KEY_ID, response.getKeyId());

        verify(kmsKeyGrantRepository).save(any(KmsKeyGrant.class));
    }

    @Test
    void shouldRevokeGrantSuccessfully() {

        when(kmsKeyGrantRepository.findByTenantAndGrantId(TENANT, GRANT_ID))
                .thenReturn(Optional.of(grant));

        String response =
                service.revokeGrant(TENANT, KEY_ID, GRANT_ID);

        assertEquals("REVOKED", response);
        assertEquals("REVOKED", grant.getStatus());
        assertNotNull(grant.getRevocationDate());

        verify(kmsKeyGrantRepository).save(grant);
    }

    @Test
    void shouldThrowWhenRevokeGrantNotFound() {

        when(kmsKeyGrantRepository.findByTenantAndGrantId(TENANT, GRANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                GrantNotFoundException.class,
                () -> service.revokeGrant(TENANT, KEY_ID, GRANT_ID)
        );
    }

    @Test
    void shouldListGrantsSuccessfully() {

        Page<KmsKeyGrant> page =
                new PageImpl<>(List.of(grant));

        when(kmsKeyGrantRepository.findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                any(PageRequest.class)))
                .thenReturn(page);

        ListGrantsResponseDto response =
                service.listGrants(TENANT, KEY_ID, 10, null);

        assertNotNull(response);
        assertEquals(1, response.getGrants().size());

        ListGrantsResponseDto.GrantDto dto =
                response.getGrants().get(0);

        assertEquals(GRANT_ID, dto.getGrantId());
        assertEquals("user-1", dto.getGranteePrincipal());
        assertEquals(2, dto.getOperations().size());
    }

    @Test
    void shouldReturnNextTokenWhenMorePagesExist() {

        Page<KmsKeyGrant> page =
                new PageImpl<>(
                        List.of(grant),
                        PageRequest.of(0, 1),
                        2
                );

        when(kmsKeyGrantRepository.findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                any(PageRequest.class)))
                .thenReturn(page);

        ListGrantsResponseDto response =
                service.listGrants(TENANT, KEY_ID, 1, null);

        assertEquals("1", response.getNextToken());
    }

    @Test
    void shouldRetireGrantSuccessfully() {

        RetireGrantRequestDto request =
                RetireGrantRequestDto.builder()
                        .build();

        when(kmsKeyGrantRepository.findByTenantAndGrantId(TENANT, GRANT_ID))
                .thenReturn(Optional.of(grant));

        Object response =
                service.retireGrant(TENANT, GRANT_ID, request);

        assertEquals("RETIRED", response);
        assertEquals("RETIRED", grant.getStatus());
        assertNotNull(grant.getRevocationDate());

        verify(kmsKeyGrantRepository).save(grant);
    }

    @Test
    void shouldThrowWhenRetireGrantNotFound() {

        RetireGrantRequestDto request =
                RetireGrantRequestDto.builder()
                        .build();

        when(kmsKeyGrantRepository.findByTenantAndGrantId(TENANT, GRANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                GrantNotFoundException.class,
                () -> service.retireGrant(TENANT, GRANT_ID, request)
        );
    }
}