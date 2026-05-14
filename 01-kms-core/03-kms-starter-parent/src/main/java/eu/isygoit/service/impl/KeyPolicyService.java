package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.exception.GrantNotFoundException;
import eu.isygoit.model.KmsKeyGrant;
import eu.isygoit.model.KmsKeyPolicy;
import eu.isygoit.repository.KmsKeyGrantRepository;
import eu.isygoit.repository.KmsKeyPolicyRepository;
import eu.isygoit.repository.RepoHelper;
import eu.isygoit.service.IKeyPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The type Key policy service.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class KeyPolicyService implements IKeyPolicyService {

    private final KmsKeyPolicyRepository kmsKeyPolicyRepository;
    private final KmsKeyGrantRepository kmsKeyGrantRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> setKeyPolicy(String tenant, String keyId, SetKeyPolicyRequestDto request) {
        log.info("Setting key policy for tenant: {} keyId: {}", tenant, keyId);

        try {
            String policyJson = objectMapper.writeValueAsString(request.getPolicy());
            KmsKeyPolicy policy = kmsKeyPolicyRepository.findByTenantAndKeyIdAndPolicyName(tenant, keyId, request.getPolicyName())
                    .orElse(KmsKeyPolicy.builder()
                            .tenant(tenant)
                            .keyId(keyId)
                            .build());

            policy.setPolicyDocument(policyJson);
            policy.setPolicyVersion("2012-10-17"); // Default WAMS policy version
            kmsKeyPolicyRepository.save(policy);

            return request.getPolicy();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize policy", e);
            throw new RuntimeException("Failed to serialize policy", e);
        }
    }

    @Override
    public Map<String, Object> getKeyPolicy(String tenant, String keyId, String policyName) {
        log.info("Getting key policy for tenant: {} keyId: {}", tenant, keyId);

        return kmsKeyPolicyRepository.findByTenantAndKeyIdAndPolicyName(tenant, keyId, policyName)
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
    public GrantResponseDto createGrant(String tenant, String keyId, CreateGrantRequestDto request) {
        log.info("Creating grant for tenant: {} keyId: {} principal: {}", tenant, keyId, request.getPrincipal());

        String grantId = "grant-" + UUID.randomUUID();
        KmsKeyGrant grant = KmsKeyGrant.builder()
                .tenant(tenant)
                .keyId(keyId)
                .grantId(grantId)
                .principal(request.getPrincipal())
                .operations(String.join(",", request.getOperations()))
                .status("ACTIVE")
                .creationDate(LocalDateTime.now())
                .build();

        kmsKeyGrantRepository.save(grant);

        return GrantResponseDto.builder()
                .grantId(grantId)
                .keyId(keyId)
                .build();
    }

    @Override
    public String revokeGrant(String tenant, String keyId, String grantId) {
        log.info("Revoking grant: {} for tenant: {} keyId: {}", grantId, tenant, keyId);

        KmsKeyGrant grant = kmsKeyGrantRepository.findByTenantAndGrantId(tenant, grantId)
                .orElseThrow(() -> new GrantNotFoundException("Grant not found with id: " + grantId));

        grant.setStatus("REVOKED");
        grant.setRevocationDate(LocalDateTime.now());
        kmsKeyGrantRepository.save(grant);

        return "REVOKED";
    }

    @Override
    public ListGrantsResponseDto listGrants(String tenant, String keyId, Integer limit, String nextToken) {
        log.info("Listing grants for tenant: {} keyId: {}", tenant, keyId);

        Pageable pageable = RepoHelper.resolvePageable(limit, nextToken, "creationDate");

        Page<KmsKeyGrant> grantPage = kmsKeyGrantRepository.findByTenantAndKeyId(tenant, keyId, pageable);

        return ListGrantsResponseDto.builder()
                .grants(grantPage.getContent().stream()
                        .map(g -> ListGrantsResponseDto.GrantDto.builder()
                                .grantId(g.getGrantId())
                                .granteePrincipal(g.getPrincipal())
                                // retiringPrincipal field in DTO might not map directly to entity
                                .operations(Arrays.asList(g.getOperations().split(",")))
                                .constraints(g.getConstraints())
                                .createdAt(g.getCreationDate())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(grantPage.hasNext() ? String.valueOf(pageable.getPageNumber() + 1) : null)
                .build();
    }

    @Override
    public RetireGrantResponse retireGrant(String tenant, String grantId, RetireGrantRequestDto request) {
        log.info("Retiring grant: {} for tenant: {}", grantId, tenant);

        KmsKeyGrant grant = kmsKeyGrantRepository.findByTenantAndGrantId(tenant, grantId)
                .orElseThrow(() -> new GrantNotFoundException("Grant not found with id: " + grantId));

        grant.setStatus("RETIRED");
        grant.setRevocationDate(LocalDateTime.now());
        kmsKeyGrantRepository.save(grant);

        return new RetireGrantResponse(grant.getKeyId());
    }

    @Override
    public ListRetirableGrantsResponse listRetirableGrants(String tenant, String retiringPrincipal, Integer limit, String nextToken) {
        log.info("Listing retirable grants for tenant: {} retiringPrincipal: {}", tenant, retiringPrincipal);

        Pageable pageable = RepoHelper.resolvePageable(limit, nextToken, "creationDate");

        // Find active grants that the principal can retire
        Page<KmsKeyGrant> grantPage = kmsKeyGrantRepository.findByTenantAndPrincipalAndStatus(
                tenant, retiringPrincipal, "ACTIVE", pageable);

        List<ListGrantsResponse.Grant> grants = grantPage.getContent().stream()
                .map(g -> eu.isygoit.dto.KmsDtos.ListGrantsResponse.Grant.builder()
                        .grantId(g.getGrantId())
                        .granteePrincipal(g.getPrincipal())
                        .retiringPrincipal(retiringPrincipal)
                        .operations(Arrays.asList(g.getOperations().split(",")))
                        .constraints(null) // TODO: parse constraints if stored as JSON
                        .creationDate(g.getCreationDate().toString())
                        .keyId(g.getKeyId())
                        .name(g.getName())
                        .build())
                .collect(Collectors.toList());

        return ListRetirableGrantsResponse.builder()
                .grants(grants)
                .nextToken(grantPage.hasNext() ? String.valueOf(pageable.getPageNumber() + 1) : null)
                .truncated(grantPage.hasNext())
                .build();
    }

    @Override
    public ListKeyPoliciesResponse listKeyPolicies(
            String tenant,
            String keyId,
            Integer limit,
            String nextToken) {

        log.info("Listing key policies for tenant={} keyId={}", tenant, keyId);

        final Pageable pageable =
                RepoHelper.resolvePageable(limit, nextToken, "creationDate");

        final List<String> policyNames = kmsKeyPolicyRepository
                .findByTenantAndKeyId(tenant, keyId, pageable)
                .stream()
                .map(KmsKeyPolicy::getPolicyName)
                .toList();

        return ListKeyPoliciesResponse.builder()
                .policyNames(
                        policyNames.isEmpty()
                                ? List.of("default")
                                : policyNames)
                .nextToken(nextToken) // replace when real pagination token is implemented
                .truncated(false)
                .build();
    }
}

