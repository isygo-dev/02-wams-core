package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.exception.GrantConstraintException;
import eu.isygoit.exception.GrantNotFoundException;
import eu.isygoit.exception.KeyGrantException;
import eu.isygoit.model.KmsKeyGrant;
import eu.isygoit.model.KmsKeyPolicy;
import eu.isygoit.repository.KmsKeyGrantRepository;
import eu.isygoit.repository.KmsKeyPolicyRepository;
import eu.isygoit.repository.RepoHelper;
import eu.isygoit.service.IKeyPolicyService;
import eu.isygoit.validator.KeyPolicyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final KeyPolicyValidator keyPolicyValidator;
    private final KmsKeyPolicyRepository kmsKeyPolicyRepository;
    private final KmsKeyGrantRepository kmsKeyGrantRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> setKeyPolicy(String tenant, String keyId, PutKeyPolicyRequest request) {
        log.info("Setting key policy for tenant: {} keyId: {}", tenant, keyId);

        try {
            String policyJson = objectMapper.writeValueAsString(request.getPolicy());
            keyPolicyValidator.validatePolicyLockout(policyJson, request.getBypassPolicyLockoutSafetyCheck(), tenant);
            KmsKeyPolicy policy = kmsKeyPolicyRepository.findByTenantAndKeyId(tenant, keyId)
                    .orElse(KmsKeyPolicy.builder()
                            .tenant(tenant)
                            .keyId(keyId)
                            .build());

            policy.setPolicyDocument(policyJson);
            kmsKeyPolicyRepository.save(policy);

            return request.getPolicy();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize policy", e);
            throw new RuntimeException("Failed to serialize policy", e);
        }
    }

    @Override
    public Map<String, Object> getKeyPolicy(String tenant, String keyId) {
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
    public GrantResponse createGrant(String tenant, String keyId, CreateGrantRequest request) {
        log.info("Creating grant for tenant: {} keyId: {} principal: {}", tenant, keyId, request.getGranteePrincipal());

        String grantId = "grant-" + UUID.randomUUID();

        // Serialize operations to JSON
        String operationsJson = null;
        if (request.getOperations() != null && !request.getOperations().isEmpty()) {
            try {
                operationsJson = objectMapper.writeValueAsString(request.getOperations());
            } catch (Exception e) {
                throw new KeyGrantException("Failed to serialize operations to JSON", e);
            }
        }

        // Serialize constraints to JSON if present
        String constraintsJson = null;
        if (request.getConstraints() != null) {
            try {
                constraintsJson = objectMapper.writeValueAsString(request.getConstraints());
            } catch (Exception e) {
                throw new KeyGrantException("Failed to serialize constraints to JSON", e);
            }
        }

        KmsKeyGrant grant = KmsKeyGrant.builder()
                .tenant(tenant)
                .keyId(keyId)
                .grantId(grantId)
                .granteePrincipal(request.getGranteePrincipal())
                .retiringPrincipal(request.getRetiringPrincipal())
                .operations(operationsJson)
                .constraints(constraintsJson)
                .name(request.getName())
                .build();

        kmsKeyGrantRepository.save(grant);

        return GrantResponse.builder()
                .grantId(grantId)
                .keyId(keyId)
                .build();
    }

    @Override
    public String revokeGrant(String tenant, String keyId, String grantId) {
        log.info("Revoking grant: {} for tenant: {} keyId: {}", grantId, tenant, keyId);

        KmsKeyGrant grant = kmsKeyGrantRepository.findByTenantAndGrantId(tenant, grantId)
                .orElseThrow(() -> new GrantNotFoundException("Grant not found with id: " + grantId));

        grant.setRevocationDate(LocalDateTime.now());
        kmsKeyGrantRepository.save(grant);

        return "REVOKED";
    }

    @Override
    public RetireGrantResponse retireGrant(String tenant, String grantId, RetireGrantRequest request) {
        log.info("Retiring grant: {} for tenant: {}", grantId, tenant);

        KmsKeyGrant grant = kmsKeyGrantRepository.findByTenantAndGrantId(tenant, grantId)
                .orElseThrow(() -> new GrantNotFoundException("Grant not found with id: " + grantId));

        grant.setRetirementDate(LocalDateTime.now());
        kmsKeyGrantRepository.save(grant);

        return new RetireGrantResponse(grant.getKeyId());
    }

    @Override
    public ListGrantsResponse listGrants(String tenant, String keyId, Integer limit, String nextToken) {
        log.info("Listing grants for tenant: {} keyId: {}", tenant, keyId);

        Pageable pageable = RepoHelper.resolvePageable(limit, nextToken, "createDate");
        Page<KmsKeyGrant> page = kmsKeyGrantRepository.findByTenantAndKeyId(tenant, keyId, pageable);

        return ListGrantsResponse.builder()
                .grants(page.getContent().stream()
                        .map(g -> {
                            try {
                                // Deserialize operations from JSON string to List<String>
                                List<String> operations = null;
                                if (g.getOperations() != null && !g.getOperations().isBlank()) {
                                    operations = objectMapper.readValue(g.getOperations(), new TypeReference<List<String>>() {});
                                }

                                // Deserialize constraints from JSON string to GrantConstraints
                                CreateGrantRequest.GrantConstraints constraints = null;
                                if (g.getConstraints() != null && !g.getConstraints().isBlank()) {
                                    constraints = objectMapper.readValue(g.getConstraints(), CreateGrantRequest.GrantConstraints.class);
                                }

                                return ListGrantsResponse.Grant.builder()
                                        .grantId(g.getGrantId())
                                        .granteePrincipal(g.getGranteePrincipal())
                                        .retiringPrincipal(g.getRetiringPrincipal())
                                        .operations(operations)
                                        .constraints(constraints)
                                        .name(g.getName())
                                        .createDate(g.getCreateDate())
                                        .status(g.getStatus() != null ? g.getStatus().name() : "ACTIVE")
                                        .build();
                            } catch (JsonProcessingException e) {
                                log.error("Failed to deserialize grant {}: {}", g.getGrantId(), e.getMessage());
                                throw new GrantConstraintException("Failed to deserialize grant", e);
                            }
                        })
                        .collect(Collectors.toList()))
                .nextToken(page.hasNext() ? String.valueOf(pageable.getPageNumber() + 1) : null)
                .numberOfElements(page.getNumberOfElements())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .truncated(page.hasNext())
                .build();
    }

    @Override
    public ListRetirableGrantsResponse listRetirableGrants(String tenant, String retiringPrincipal, Integer limit, String nextToken) {
        log.info("Listing retirable grants for tenant: {} retiringPrincipal: {}", tenant, retiringPrincipal);

        Pageable pageable = RepoHelper.resolvePageable(limit, nextToken, "createDate");

        // Find active grants that the principal can retire
        Page<KmsKeyGrant> page = kmsKeyGrantRepository.findByTenantAndRetiringPrincipalAndRevocationDateIsNullAndRetirementDateIsNull(
                tenant, retiringPrincipal, pageable);

        List<ListGrantsResponse.Grant> grants = page.getContent().stream()
                .map(g -> eu.isygoit.dto.KmsDtos.ListGrantsResponse.Grant.builder()
                        .grantId(g.getGrantId())
                        .granteePrincipal(g.getGranteePrincipal())
                        .retiringPrincipal(retiringPrincipal)
                        .operations(Arrays.asList(g.getOperations().split(",")))
                        .constraints(null) // TODO: parse constraints if stored as JSON
                        .createDate(g.getCreateDate())
                        .keyId(g.getKeyId())
                        .name(g.getName())
                        .build())
                .collect(Collectors.toList());

        return ListRetirableGrantsResponse.builder()
                .grants(grants)
                .nextToken(page.hasNext() ? String.valueOf(pageable.getPageNumber() + 1) : null)
                .numberOfElements(page.getNumberOfElements())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .truncated(page.hasNext())
                .build();
    }
}

