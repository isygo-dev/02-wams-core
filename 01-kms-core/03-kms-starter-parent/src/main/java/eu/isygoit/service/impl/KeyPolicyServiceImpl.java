package eu.isygoit.service.impl;

import eu.isygoit.dto.request.CreateGrantRequestDto;
import eu.isygoit.dto.request.SetKeyPolicyRequestDto;
import eu.isygoit.dto.response.GrantResponseDto;
import eu.isygoit.service.IKeyPolicyService;
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
public class KeyPolicyServiceImpl implements IKeyPolicyService {

    @Override
    public Map<String, Object> setKeyPolicy(String tenant, Long keyId, SetKeyPolicyRequestDto request) {
        log.info("Setting key policy for tenant: {} keyId: {}", tenant, keyId);

        return request.getPolicy();
    }

    @Override
    public Map<String, Object> getKeyPolicy(String tenant, Long keyId) {
        log.info("Getting key policy for tenant: {} keyId: {}", tenant, keyId);

        Map<String, Object> policy = new HashMap<>();
        policy.put("Version", "2023-01-01");
        policy.put("Statement", new Object[]{});

        return policy;
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
}

