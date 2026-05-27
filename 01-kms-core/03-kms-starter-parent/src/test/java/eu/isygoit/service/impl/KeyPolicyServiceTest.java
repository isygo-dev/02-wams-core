package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.exception.GrantNotFoundException;
import eu.isygoit.exception.KeyPolicyException;
import eu.isygoit.model.KmsKeyGrant;
import eu.isygoit.model.KmsKeyPolicy;
import eu.isygoit.repository.KmsKeyGrantRepository;
import eu.isygoit.repository.KmsKeyPolicyRepository;
import eu.isygoit.validator.KeyPolicyValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeyPolicyService - Realistic User Stories")
class KeyPolicyServiceTest {

    private final String tenant = "acme-corp";
    private final String keyId = "123e4567-e89b-12d3-a456-426614174000";
    @Mock
    private KeyPolicyValidator keyPolicyValidator;
    @Mock
    private KmsKeyPolicyRepository kmsKeyPolicyRepository;
    @Mock
    private KmsKeyGrantRepository kmsKeyGrantRepository;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private KeyPolicyService keyPolicyService;

    // =========================================================================
    // User Story 1: Set a key policy (IAM-style policy) with validation
    // =========================================================================

    @Nested
    @DisplayName("Story 1: Set a key policy with lockout safety check")
    class SetKeyPolicyStory {

        @Test
        @DisplayName("Admin sets a valid policy for a key")
        void setValidPolicy() throws Exception {
            Map<String, Object> policyMap = Map.of(
                    "Version", "2012-10-17",
                    "Statement", List.of(Map.of("Effect", "Allow", "Principal", "*", "Action", "kms:*", "Resource", "*"))
            );
            PutKeyPolicyRequest request = PutKeyPolicyRequest.builder()
                    .policy(policyMap)
                    .bypassPolicyLockoutSafetyCheck(false)
                    .build();

            String policyJson = "{\"Version\":\"2012-10-17\"}";
            when(objectMapper.writeValueAsString(policyMap)).thenReturn(policyJson);
            doNothing().when(keyPolicyValidator).validatePolicyLockout(policyJson, false, tenant);

            KmsKeyPolicy existingPolicy = KmsKeyPolicy.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .policyDocument("old policy")
                    .build();
            when(kmsKeyPolicyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(existingPolicy));
            when(kmsKeyPolicyRepository.save(any(KmsKeyPolicy.class))).thenReturn(existingPolicy);

            Map<String, Object> result = keyPolicyService.setKeyPolicy(tenant, keyId, request);

            assertThat(result).isEqualTo(policyMap);
            verify(keyPolicyValidator).validatePolicyLockout(policyJson, false, tenant);
            verify(kmsKeyPolicyRepository).save(existingPolicy);
            assertThat(existingPolicy.getPolicyDocument()).isEqualTo(policyJson);
        }

        @Test
        @DisplayName("Admin bypasses lockout safety check to set a potentially locking policy")
        void setPolicyWithBypass() throws Exception {
            Map<String, Object> policyMap = Map.of("Version", "2012-10-17", "Statement", List.of());
            PutKeyPolicyRequest request = PutKeyPolicyRequest.builder()
                    .policy(policyMap)
                    .bypassPolicyLockoutSafetyCheck(true)
                    .build();

            when(objectMapper.writeValueAsString(policyMap)).thenReturn("{}");
            doNothing().when(keyPolicyValidator).validatePolicyLockout(anyString(), eq(true), eq(tenant));

            when(kmsKeyPolicyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.empty());
            when(kmsKeyPolicyRepository.save(any(KmsKeyPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

            keyPolicyService.setKeyPolicy(tenant, keyId, request);

            verify(keyPolicyValidator).validatePolicyLockout(anyString(), eq(true), eq(tenant));
            verify(kmsKeyPolicyRepository).save(any(KmsKeyPolicy.class));
        }

        @Test
        @DisplayName("Policy validation fails – lockout would occur")
        void policyValidationFails() throws Exception {
            Map<String, Object> policyMap = Map.of("Version", "2012-10-17", "Statement", List.of());
            PutKeyPolicyRequest request = PutKeyPolicyRequest.builder()
                    .policy(policyMap)
                    .bypassPolicyLockoutSafetyCheck(false)
                    .build();

            when(objectMapper.writeValueAsString(policyMap)).thenReturn("{}");
            doThrow(new KeyPolicyException("Policy would lock out admin"))
                    .when(keyPolicyValidator).validatePolicyLockout(anyString(), eq(false), eq(tenant));

            assertThatThrownBy(() -> keyPolicyService.setKeyPolicy(tenant, keyId, request))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Policy would lock out admin");

            verify(kmsKeyPolicyRepository, never()).save(any());
        }
    }

    // =========================================================================
    // User Story 2: Get the current policy of a key
    // =========================================================================

    @Nested
    @DisplayName("Story 2: Retrieve key policy")
    class GetKeyPolicyStory {

        @Test
        @DisplayName("Get existing policy")
        void getExistingPolicy() throws Exception {
            String policyJson = "{\"Version\":\"2012-10-17\",\"Statement\":[]}";
            Map<String, Object> expectedPolicy = Map.of("Version", "2012-10-17", "Statement", List.of());

            KmsKeyPolicy policy = KmsKeyPolicy.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .policyDocument(policyJson)
                    .build();
            when(kmsKeyPolicyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.of(policy));
            when(objectMapper.readValue(eq(policyJson), any(TypeReference.class)))
                    .thenReturn(expectedPolicy);

            Map<String, Object> result = keyPolicyService.getKeyPolicy(tenant, keyId);

            assertThat(result).isEqualTo(expectedPolicy);
        }

        @Test
        @DisplayName("Get default policy when none exists")
        void getDefaultPolicyWhenNoneExists() {
            when(kmsKeyPolicyRepository.findByTenantAndKeyId(tenant, keyId))
                    .thenReturn(Optional.empty());

            Map<String, Object> result = keyPolicyService.getKeyPolicy(tenant, keyId);

            assertThat(result).containsKey("Version");
            assertThat(result.get("Version")).isEqualTo("2012-10-17");
            assertThat(result).containsKey("Statement");
            assertThat(result.get("Statement")).isInstanceOf(Object[].class);
        }
    }

    // =========================================================================
    // User Story 3: Create a grant for a key (delegated access)
    // =========================================================================

    @Nested
    @DisplayName("Story 3: Create a grant for a key")
    class CreateGrantStory {

        @Test
        @DisplayName("Create a grant allowing a service account to encrypt/decrypt")
        void createGrant() throws Exception {
            CreateGrantRequest request = CreateGrantRequest.builder()
                    .granteePrincipal("wrn:wams:iam::123456789012:role/encryption-service")
                    .retiringPrincipal("wrn:wams:iam::123456789012:user/admin")
                    .operations(List.of("Encrypt", "Decrypt"))
                    .name("encryption-grant")
                    .build();

            String operationsJson = "[\"Encrypt\",\"Decrypt\"]";
            when(objectMapper.writeValueAsString(request.getOperations())).thenReturn(operationsJson);

            when(kmsKeyGrantRepository.save(any(KmsKeyGrant.class))).thenAnswer(inv -> inv.getArgument(0));

            GrantResponse response = keyPolicyService.createGrant(tenant, keyId, request);

            assertThat(response.getGrantId()).startsWith("grant-");
            assertThat(response.getKeyId()).isEqualTo(keyId);

            ArgumentCaptor<KmsKeyGrant> captor = ArgumentCaptor.forClass(KmsKeyGrant.class);
            verify(kmsKeyGrantRepository).save(captor.capture());
            KmsKeyGrant saved = captor.getValue();
            assertThat(saved.getGranteePrincipal()).isEqualTo(request.getGranteePrincipal());
            assertThat(saved.getRetiringPrincipal()).isEqualTo(request.getRetiringPrincipal());
            assertThat(saved.getOperations()).isEqualTo(operationsJson);
            assertThat(saved.getName()).isEqualTo("encryption-grant");
        }
    }

    // =========================================================================
    // User Story 4: Revoke a grant
    // =========================================================================

    @Nested
    @DisplayName("Story 4: Revoke a grant")
    class RevokeGrantStory {

        @Test
        @DisplayName("Revoke an active grant")
        void revokeActiveGrant() {
            String grantId = "grant-123";
            KmsKeyGrant grant = KmsKeyGrant.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .grantId(grantId)
                    .build();
            when(kmsKeyGrantRepository.findByTenantAndGrantId(tenant, grantId))
                    .thenReturn(Optional.of(grant));
            when(kmsKeyGrantRepository.save(any(KmsKeyGrant.class))).thenReturn(grant);

            String status = keyPolicyService.revokeGrant(tenant, keyId, grantId);

            assertThat(status).isEqualTo("REVOKED");
            assertThat(grant.getRevocationDate()).isNotNull();
            verify(kmsKeyGrantRepository).save(grant);
        }

        @Test
        @DisplayName("Revoke non-existent grant throws exception")
        void revokeNonExistentGrant() {
            String grantId = "grant-999";
            when(kmsKeyGrantRepository.findByTenantAndGrantId(tenant, grantId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> keyPolicyService.revokeGrant(tenant, keyId, grantId))
                    .isInstanceOf(GrantNotFoundException.class)
                    .hasMessageContaining("Grant not found with id: grant-999");
        }
    }

    // =========================================================================
    // User Story 5: Retire a grant (self-service by retiring principal)
    // =========================================================================

    @Nested
    @DisplayName("Story 5: Retire a grant")
    class RetireGrantStory {

        @Test
        @DisplayName("Retiring principal retires a grant")
        void retireGrant() {
            String grantId = "grant-456";
            RetireGrantRequest request = new RetireGrantRequest();
            KmsKeyGrant grant = KmsKeyGrant.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .grantId(grantId)
                    .build();
            when(kmsKeyGrantRepository.findByTenantAndGrantId(tenant, grantId))
                    .thenReturn(Optional.of(grant));
            when(kmsKeyGrantRepository.save(any(KmsKeyGrant.class))).thenReturn(grant);

            RetireGrantResponse response = keyPolicyService.retireGrant(tenant, grantId, request);

            assertThat(response.getKeyId()).isEqualTo(keyId);
            assertThat(grant.getRetirementDate()).isNotNull();
            verify(kmsKeyGrantRepository).save(grant);
        }
    }

    // =========================================================================
    // User Story 6: List all grants for a key
    // =========================================================================

    @Nested
    @DisplayName("Story 6: List grants for a key")
    class ListGrantsStory {

        @Test
        @DisplayName("List active and revoked grants with pagination")
        void listGrants() throws Exception {
            String opsJson = "[\"Encrypt\",\"Decrypt\"]";
            String constraintsJson = "{\"encryptionContextSubset\":{\"Purpose\":\"Payment\"}}";

            KmsKeyGrant grant1 = KmsKeyGrant.builder()
                    .grantId("grant-1")
                    .granteePrincipal("principal-1")
                    .retiringPrincipal("admin")
                    .operations(opsJson)
                    .constraints(constraintsJson)
                    .name("test-grant")
                    .createDate(LocalDateTime.now())
                    .build();
            Page<KmsKeyGrant> page = new PageImpl<>(List.of(grant1));
            when(kmsKeyGrantRepository.findByTenantAndKeyId(eq(tenant), eq(keyId), any(Pageable.class)))
                    .thenReturn(page);
            when(objectMapper.readValue(eq(opsJson), any(TypeReference.class)))
                    .thenReturn(List.of("Encrypt", "Decrypt"));
            when(objectMapper.readValue(eq(constraintsJson), eq(CreateGrantRequest.GrantConstraints.class)))
                    .thenReturn(CreateGrantRequest.GrantConstraints.builder()
                            .encryptionContextSubset(Map.of("Purpose", "Payment"))
                            .build());

            ListGrantsResponse response = keyPolicyService.listGrants(tenant, keyId, 10, null);

            assertThat(response.getGrants()).hasSize(1);
            assertThat(response.getGrants().get(0).getGrantId()).isEqualTo("grant-1");
            assertThat(response.getGrants().get(0).getOperations()).containsExactly("Encrypt", "Decrypt");
            assertThat(response.getGrants().get(0).getConstraints()).isNotNull();
        }
    }

    // =========================================================================
    // User Story 7: List retirable grants for a retiring principal
    // =========================================================================

    @Nested
    @DisplayName("Story 7: List retirable grants for a principal")
    class ListRetirableGrantsStory {

        @Test
        @DisplayName("List active grants that a retiring principal can retire")
        void listRetirableGrants() {
            String retiringPrincipal = "wrn:wams:iam::123456789012:user/admin";
            KmsKeyGrant grant = KmsKeyGrant.builder()
                    .grantId("grant-retire-1")
                    .granteePrincipal("service-role")
                    .retiringPrincipal(retiringPrincipal)
                    .operations("[\"Encrypt\"]")
                    .keyId(keyId)
                    .name("test")
                    .createDate(LocalDateTime.now())
                    .build();
            Page<KmsKeyGrant> page = new PageImpl<>(List.of(grant));
            when(kmsKeyGrantRepository.findByTenantAndRetiringPrincipalAndRevocationDateIsNullAndRetirementDateIsNull(
                    eq(tenant), eq(retiringPrincipal), any(Pageable.class)))
                    .thenReturn(page);

            ListRetirableGrantsResponse response = keyPolicyService.listRetirableGrants(tenant, retiringPrincipal, 10, null);

            assertThat(response.getGrants()).hasSize(1);
            assertThat(response.getGrants().get(0).getGrantId()).isEqualTo("grant-retire-1");
            assertThat(response.getGrants().get(0).getRetiringPrincipal()).isEqualTo(retiringPrincipal);
        }
    }
}