package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.request.CreateGrantRequestDto;
import eu.isygoit.dto.request.RetireGrantRequestDto;
import eu.isygoit.dto.request.SetKeyPolicyRequestDto;
import eu.isygoit.dto.response.GrantResponseDto;
import eu.isygoit.dto.response.ListGrantsResponseDto;
import eu.isygoit.model.KmsKeyPolicy;
import eu.isygoit.repository.KmsKeyPolicyRepository;
import eu.isygoit.service.IKeyPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The type Key policy service.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class KeyPolicyServiceImpl implements IKeyPolicyService {

    private final KmsKeyPolicyRepository kmsKeyPolicyRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> setKeyPolicy(String tenant, Long keyId, SetKeyPolicyRequestDto request) {
        log.info("Setting key policy for tenant: {} keyId: {}", tenant, keyId);

        try {
            String policyJson = objectMapper.writeValueAsString(request.getPolicy());
            KmsKeyPolicy policy = kmsKeyPolicyRepository.findByTenantAndKeyId(tenant, keyId)
                    .orElse(KmsKeyPolicy.builder()
                            .tenant(tenant)
                            .keyId(keyId)
                            .build());

            policy.setPolicyDocument(policyJson);
            policy.setPolicyVersion("2012-10-17"); // Default AWS policy version
            kmsKeyPolicyRepository.save(policy);

            return request.getPolicy();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize policy", e);
            throw new RuntimeException("Failed to serialize policy", e);
        }
    }

    @Override
    public Map<String, Object> getKeyPolicy(String tenant, Long keyId) {
        log.info("Getting key policy for tenant: {} keyId: {}", tenant, keyId);

        return kmsKeyPolicyRepository.findByTenantAndKeyId(tenant, keyId)
                .map(p -> {
                    try {
                        return objectMapper.readValue(p.getPolicyDocument(), new TypeReference<Map<String, Object>>() {
                        });
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize policy", e);
                        return new HashMap<String, Object>();
                    }
                })
                .orElseGet(() -> {
                    Map<String, Object> policy = new HashMap<>();
                    policy.put("Version", "2012-10-17");
                    policy.put("Statement", new Object[]{});
                    return policy;
                });
    }

    @Override
    public GrantResponseDto createGrant(String tenant, Long keyId, CreateGrantRequestDto request) {
        log.info("Creating grant for tenant: {} keyId: {} principal: {}", tenant, keyId, request.getPrincipal());

        String grantId = "grant-" + UUID.randomUUID().toString();

        return GrantResponseDto.builder()
                .grantId(grantId)
                .keyId(keyId)
                .build();
    }

    @Override
    public String revokeGrant(String tenant, Long keyId, String grantId) {
        log.info("Revoking grant: {} for tenant: {} keyId: {}", grantId, tenant, keyId);

        return "REVOKED" ;
    }

    @Override
    public ListGrantsResponseDto listGrants(String tenant, Long keyId, Integer limit, String nextToken) {
        return null;
    }

    @Override
    public Object retireGrant(String tenant, String grantId, RetireGrantRequestDto request) {
        return null;
    }
}

