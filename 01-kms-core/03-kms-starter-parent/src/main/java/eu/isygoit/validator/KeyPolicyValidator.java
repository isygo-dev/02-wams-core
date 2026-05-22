package eu.isygoit.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KeyPolicyValidator {

    private final ObjectMapper objectMapper;

    public KeyPolicyValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validates that the given policy document does not lock out the key administrator.
     * Throws an exception if the safety check fails and bypass flag is false.
     *
     * @param policyJson        The policy document as a JSON string (or Map serialized to string).
     * @param bypassSafetyCheck Whether to bypass the lockout safety check.
     * @param tenantAccountId   The account/tenant identifier (e.g., "123456789012" or tenant name).
     * @throws IllegalArgumentException if the policy would lock out the administrator and bypass is false.
     */
    public void validatePolicyLockout(String policyJson, Boolean bypassSafetyCheck, String tenantAccountId) {
        if (Boolean.TRUE.equals(bypassSafetyCheck)) {
            log.debug("Bypassing policy lockout safety check as requested");
            return;
        }
        if (policyJson == null || policyJson.isBlank()) {
            // No policy provided – default policy will grant admin access (safe)
            return;
        }

        try {
            JsonNode policy = objectMapper.readTree(policyJson);
            if (!hasAdminAccess(policy, tenantAccountId)) {
                throw new IllegalArgumentException(
                        "Policy would lock out the key administrator because no statement grants " +
                                "kms:PutKeyPolicy or administrative actions (kms:*) to the account root or an admin principal. " +
                                "Set bypassPolicyLockoutSafetyCheck=true to apply this policy anyway."
                );
            }
        } catch (Exception e) {
            log.error("Failed to parse policy JSON for lockout validation", e);
            throw new IllegalArgumentException("Invalid policy JSON: " + e.getMessage(), e);
        }
    }

    private boolean hasAdminAccess(JsonNode policy, String tenantAccountId) {
        // Default root principal WRN (adjust to your tenant format)
        String rootPrincipal = "wrn:kms:iam::" + tenantAccountId + ":root";
        // Also allow any principal in the same account if you want (simplified)
        // For a more accurate check, you'd parse the account ID from the principal.

        JsonNode statements = policy.get("Statement");
        if (statements == null) return false;

        // Handle both single Statement object or an array
        if (statements.isObject()) {
            return statementAllowsAdmin(statements, rootPrincipal);
        } else if (statements.isArray()) {
            for (JsonNode stmt : statements) {
                if (statementAllowsAdmin(stmt, rootPrincipal)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean statementAllowsAdmin(JsonNode stmt, String rootPrincipal) {
        // Effect must be "Allow"
        JsonNode effectNode = stmt.get("Effect");
        if (effectNode == null || !"Allow".equals(effectNode.asText())) {
            return false;
        }

        // Action must include "kms:PutKeyPolicy" or "kms:*" (or an array containing them)
        JsonNode actionNode = stmt.get("Action");
        if (actionNode == null) return false;

        boolean hasAdminAction = false;
        if (actionNode.isTextual()) {
            String action = actionNode.asText();
            hasAdminAction = action.equals("kms:*") || action.equals("kms:PutKeyPolicy");
        } else if (actionNode.isArray()) {
            for (JsonNode act : actionNode) {
                String action = act.asText();
                if (action.equals("kms:*") || action.equals("kms:PutKeyPolicy")) {
                    hasAdminAction = true;
                    break;
                }
            }
        }
        if (!hasAdminAction) return false;

        // Principal must include the root principal or a principal that can administer the key.
        // For simplicity, we check if the root principal is present.
        JsonNode principalNode = stmt.get("Principal");
        if (principalNode == null) return false;

        // KMS supports multiple principal formats: { "KMS": "wrn:..." } or { "KMS": ["wrn1", "wrn2"] }
        JsonNode kmsPrincipal = principalNode.get("KMS");
        if (kmsPrincipal == null) return false;

        if (kmsPrincipal.isTextual()) {
            return kmsPrincipal.asText().equals(rootPrincipal) ||
                    kmsPrincipal.asText().equals("*"); // Wildcard also grants admin (but risky)
        } else if (kmsPrincipal.isArray()) {
            for (JsonNode p : kmsPrincipal) {
                if (p.asText().equals(rootPrincipal) || p.asText().equals("*")) {
                    return true;
                }
            }
        }
        return false;
    }
}