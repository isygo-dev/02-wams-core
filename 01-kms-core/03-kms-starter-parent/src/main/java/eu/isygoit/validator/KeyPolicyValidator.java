package eu.isygoit.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.exception.KeyPolicyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
public class KeyPolicyValidator {

    private static final int MAX_POLICY_SIZE_BYTES = 10240; // 10 KB
    private static final Pattern WRN_PATTERN = Pattern.compile("^wrn:wams:[a-z]+::[0-9]+:(?:[a-zA-Z0-9\\-\\/]+|\\*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTION_PATTERN = Pattern.compile("^kms:[A-Za-z]+$");
    private static final Set<String> SUPPORTED_CONDITION_OPERATORS = Set.of(
            "StringEquals", "StringNotEquals", "StringEqualsIgnoreCase", "StringNotEqualsIgnoreCase",
            "StringLike", "StringNotLike", "Bool", "BoolIfExists",
            "DateLessThan", "DateLessThanEquals", "DateGreaterThan", "DateGreaterThanEquals",
            "IpAddress", "NotIpAddress", "ArnEquals", "ArnLike", "ArnNotEquals", "ArnNotLike"
    );

    private final ObjectMapper objectMapper;

    public KeyPolicyValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ========================================================================
    //  Lockout safety check (WAMS compatible)
    // ========================================================================

    public void validatePolicyLockout(Object policy, Boolean bypassSafetyCheck, String rootPrincipal) {
        if (Boolean.TRUE.equals(bypassSafetyCheck)) {
            log.debug("Bypassing policy lockout safety check as requested");
            return;
        }
        if (policy == null) return;
        JsonNode policyNode = parsePolicy(policy);
        if (!hasAdminAccess(policyNode, rootPrincipal)) {
            throw new KeyPolicyException(
                    "Policy would lock out the key administrator because no statement grants " +
                            "kms:PutKeyPolicy (or kms:*) with Resource '*') to the root principal (" + rootPrincipal + "). " +
                            "Set bypassPolicyLockoutSafetyCheck=true to apply this policy anyway."
            );
        }
    }

    // ========================================================================
    //  Full policy validation
    // ========================================================================

    public void validateFullPolicy(Object policy) {
        JsonNode policyNode = parsePolicy(policy);
        validateSize(policyNode);
        validateTopLevelStructure(policyNode);
        JsonNode statements = policyNode.get("Statement");
        validateStatements(statements);
        validateVersion(policyNode);
        validateId(policyNode);
        addWarnings(policyNode);
    }

    // ========================================================================
    //  Helper methods for full validation
    // ========================================================================

    private JsonNode parsePolicy(Object policy) {
        try {
            if (policy instanceof String) {
                return objectMapper.readTree((String) policy);
            } else if (policy instanceof Map) {
                return objectMapper.valueToTree(policy);
            } else if (policy instanceof JsonNode) {
                return (JsonNode) policy;
            } else {
                throw new KeyPolicyException("Unsupported policy type: " + policy.getClass());
            }
        } catch (Exception e) {
            log.error("Failed to parse policy JSON", e);
            throw new KeyPolicyException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    private void validateSize(JsonNode policyNode) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(policyNode);
            if (bytes.length > MAX_POLICY_SIZE_BYTES) {
                throw new KeyPolicyException(
                        String.format("Policy exceeds maximum size of %d bytes (current: %d bytes)",
                                MAX_POLICY_SIZE_BYTES, bytes.length));
            }
        } catch (Exception e) {
            throw new KeyPolicyException("Failed to calculate policy size: " + e.getMessage(), e);
        }
    }

    private void validateTopLevelStructure(JsonNode policyNode) {
        if (!policyNode.has("Version")) {
            throw new KeyPolicyException("Missing required field: 'Version'");
        }
        if (!policyNode.has("Statement")) {
            throw new KeyPolicyException("Missing required field: 'Statement'");
        }
        JsonNode statement = policyNode.get("Statement");
        if (!statement.isArray() && !statement.isObject()) {
            throw new KeyPolicyException("'Statement' must be an array or a single object");
        }
    }

    private void validateStatements(JsonNode statements) {
        List<JsonNode> stmtList = new ArrayList<>();
        if (statements.isObject()) {
            stmtList.add(statements);
        } else {
            statements.forEach(stmtList::add);
        }
        if (stmtList.isEmpty()) {
            throw new KeyPolicyException("Policy must contain at least one statement");
        }
        Set<String> sids = new HashSet<>();
        for (int i = 0; i < stmtList.size(); i++) {
            JsonNode stmt = stmtList.get(i);
            validateStatement(stmt, i);
            if (stmt.has("Sid") && !stmt.get("Sid").asText().isBlank()) {
                String sid = stmt.get("Sid").asText();
                if (!sids.add(sid)) {
                    throw new KeyPolicyException("Duplicate Sid found: '" + sid + "'");
                }
            }
        }
    }

    private void validateStatement(JsonNode stmt, int index) {
        String location = "statement[" + index + "]";
        if (!stmt.has("Effect")) {
            throw new KeyPolicyException(location + ": missing 'Effect'");
        }
        String effect = stmt.get("Effect").asText();
        if (!"Allow".equals(effect) && !"Deny".equals(effect)) {
            throw new KeyPolicyException(location + ": Effect must be 'Allow' or 'Deny'");
        }
        if (!stmt.has("Principal")) {
            throw new KeyPolicyException(location + ": missing 'Principal'");
        }
        validatePrincipal(stmt.get("Principal"), location);
        if (!stmt.has("Action")) {
            throw new KeyPolicyException(location + ": missing 'Action'");
        }
        validateAction(stmt.get("Action"), location);
        if (!stmt.has("Resource")) {
            throw new KeyPolicyException(location + ": missing 'Resource'");
        }
        validateResource(stmt.get("Resource"), location);
        if (stmt.has("Condition")) {
            validateCondition(stmt.get("Condition"), location);
        }
    }

    // Case‑insensitive lookup helper for JSON object keys
    private JsonNode getIgnoreCase(JsonNode obj, String key) {
        if (!obj.isObject()) return null;
        for (Iterator<Map.Entry<String, JsonNode>> it = obj.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void validatePrincipal(JsonNode principalNode, String location) {
        if (principalNode.isTextual()) {
            String principal = principalNode.asText();
            if (!"*".equals(principal) && !isValidWrn(principal)) {
                throw new KeyPolicyException(location + ": invalid principal WRN: " + principal);
            }
        } else if (principalNode.isObject()) {
            boolean hasValidKey = false;
            // Use case‑insensitive lookup for "wams"
            JsonNode wamsNode = getIgnoreCase(principalNode, "wams");
            if (wamsNode != null) {
                hasValidKey = true;
                if (wamsNode.isTextual()) {
                    String v = wamsNode.asText();
                    if (!"*".equals(v) && !isValidWrn(v)) {
                        throw new KeyPolicyException(location + ": invalid principal WRN: " + v);
                    }
                } else if (wamsNode.isArray()) {
                    for (JsonNode arrItem : wamsNode) {
                        String v = arrItem.asText();
                        if (!"*".equals(v) && !isValidWrn(v)) {
                            throw new KeyPolicyException(location + ": invalid principal WRN in array: " + v);
                        }
                    }
                } else {
                    throw new KeyPolicyException(location + ": Principal value must be a string or array");
                }
            }
            // Also allow "AWS" as alias for "wams" (case‑insensitive)
            JsonNode awsNode = getIgnoreCase(principalNode, "AWS");
            if (awsNode != null) {
                hasValidKey = true;
                if (awsNode.isTextual()) {
                    String v = awsNode.asText();
                    if (!"*".equals(v) && !isValidWrn(v)) {
                        throw new KeyPolicyException(location + ": invalid principal WRN: " + v);
                    }
                } else if (awsNode.isArray()) {
                    for (JsonNode arrItem : awsNode) {
                        String v = arrItem.asText();
                        if (!"*".equals(v) && !isValidWrn(v)) {
                            throw new KeyPolicyException(location + ": invalid principal WRN in array: " + v);
                        }
                    }
                } else {
                    throw new KeyPolicyException(location + ": Principal value must be a string or array");
                }
            }
            if (!hasValidKey) {
                throw new KeyPolicyException(location + ": Principal object must contain a 'wams' or 'AWS' key");
            }
        } else {
            throw new KeyPolicyException(location + ": Principal must be a string or object");
        }
    }

    private void validateAction(JsonNode actionNode, String location) {
        if (actionNode.isTextual()) {
            validateSingleAction(actionNode.asText(), location);
        } else if (actionNode.isArray()) {
            if (actionNode.size() == 0) {
                throw new KeyPolicyException(location + ": Action array cannot be empty");
            }
            for (JsonNode act : actionNode) {
                if (!act.isTextual()) {
                    throw new KeyPolicyException(location + ": action array must contain only strings");
                }
                validateSingleAction(act.asText(), location);
            }
        } else {
            throw new KeyPolicyException(location + ": Action must be a string or array of strings");
        }
    }

    private void validateSingleAction(String action, String location) {
        if (action.equals("kms:*")) return;
        if (!ACTION_PATTERN.matcher(action).matches()) {
            boolean known = false;
            for (IKmsActionType.Types t : IKmsActionType.Types.values()) {
                if (("kms:" + t.meaning()).equalsIgnoreCase(action)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                throw new KeyPolicyException(location + ": invalid action '" + action + "'");
            }
        }
    }

    private void validateResource(JsonNode resourceNode, String location) {
        if (resourceNode.isTextual()) {
            validateSingleResource(resourceNode.asText(), location);
        } else if (resourceNode.isArray()) {
            if (resourceNode.size() == 0) {
                throw new KeyPolicyException(location + ": Resource array cannot be empty");
            }
            for (JsonNode res : resourceNode) {
                if (!res.isTextual()) {
                    throw new KeyPolicyException(location + ": Resource array must contain only strings");
                }
                validateSingleResource(res.asText(), location);
            }
        } else {
            throw new KeyPolicyException(location + ": Resource must be a string or array of strings");
        }
    }

    private void validateSingleResource(String resource, String location) {
        if ("*".equals(resource)) return;
        if (!isValidWrn(resource)) {
            throw new KeyPolicyException(location + ": invalid resource WRN: " + resource);
        }
    }

    private void validateCondition(JsonNode conditionNode, String location) {
        if (!conditionNode.isObject()) {
            throw new KeyPolicyException(location + ": Condition must be a JSON object");
        }
        for (Iterator<Map.Entry<String, JsonNode>> it = conditionNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String operator = entry.getKey();
            if (!SUPPORTED_CONDITION_OPERATORS.contains(operator)) {
                log.warn("Condition operator '{}' is not standard – ensure your backend supports it", operator);
            }
            JsonNode conditionValue = entry.getValue();
            if (!conditionValue.isObject()) {
                throw new KeyPolicyException(location + ": Condition value for '" + operator + "' must be an object");
            }
            for (Iterator<Map.Entry<String, JsonNode>> it2 = conditionValue.fields(); it2.hasNext(); ) {
                Map.Entry<String, JsonNode> condEntry = it2.next();
                String condKey = condEntry.getKey();
                JsonNode condVal = condEntry.getValue();
                if (!condKey.toLowerCase().startsWith("wams:") && !condKey.equals("aws:MultiFactorAuthPresent")) {
                    log.warn("Condition key '{}' is non-standard – prefer 'wams:' prefix", condKey);
                }
                if (!condVal.isTextual() && !condVal.isBoolean()) {
                    throw new KeyPolicyException(location + ": Condition value must be a string or boolean");
                }
            }
        }
    }

    private void validateVersion(JsonNode policyNode) {
        String version = policyNode.get("Version").asText();
        if (!"2012-10-17".equals(version) && !"2008-10-17".equals(version)) {
            throw new KeyPolicyException("Version must be '2012-10-17' or '2008-10-17'");
        }
    }

    private void validateId(JsonNode policyNode) {
        if (policyNode.has("Id")) {
            JsonNode idNode = policyNode.get("Id");
            if (!idNode.isTextual() || idNode.asText().isBlank()) {
                throw new KeyPolicyException("Id, if present, must be a non-empty string");
            }
        }
    }

    private void addWarnings(JsonNode policyNode) {
        JsonNode statements = policyNode.get("Statement");
        List<JsonNode> stmtList = new ArrayList<>();
        if (statements.isArray()) statements.forEach(stmtList::add);
        else if (statements.isObject()) stmtList.add(statements);
        for (JsonNode stmt : stmtList) {
            JsonNode principalNode = stmt.get("Principal");
            if (principalNode != null && isWildcardPrincipal(principalNode)) {
                JsonNode actionNode = stmt.get("Action");
                if (actionNode != null && isWildcardAction(actionNode)) {
                    log.warn("Statement with Principal '*' and Action 'kms:*' is extremely permissive. Consider restricting.");
                }
            }
            JsonNode actionNode = stmt.get("Action");
            if (actionNode != null && containsAction(actionNode, "kms:ScheduleKeyDeletion") &&
                    !stmt.has("Condition")) {
                log.warn("Statement allows kms:ScheduleKeyDeletion without any condition. Consider adding MFA or other constraints.");
            }
        }
    }

    private boolean isWildcardPrincipal(JsonNode principalNode) {
        if (principalNode.isTextual() && "*".equals(principalNode.asText())) return true;
        if (principalNode.isObject()) {
            JsonNode wams = getIgnoreCase(principalNode, "wams");
            if (wams != null && wams.isTextual() && "*".equals(wams.asText())) return true;
            JsonNode aws = getIgnoreCase(principalNode, "AWS");
            if (aws != null && aws.isTextual() && "*".equals(aws.asText())) return true;
        }
        return false;
    }

    private boolean isWildcardAction(JsonNode actionNode) {
        if (actionNode.isTextual()) return "kms:*".equals(actionNode.asText());
        if (actionNode.isArray()) {
            for (JsonNode act : actionNode) {
                if ("kms:*".equals(act.asText())) return true;
            }
        }
        return false;
    }

    private boolean containsAction(JsonNode actionNode, String targetAction) {
        if (actionNode.isTextual()) return targetAction.equals(actionNode.asText());
        if (actionNode.isArray()) {
            for (JsonNode act : actionNode) {
                if (targetAction.equals(act.asText())) return true;
            }
        }
        return false;
    }

    private boolean isValidWrn(String wrn) {
        return WRN_PATTERN.matcher(wrn).matches();
    }

    // ========================================================================
    //  Lockout check helper methods (case‑insensitive)
    // ========================================================================

    private boolean hasAdminAccess(JsonNode policy, String rootPrincipal) {
        JsonNode statements = policy.get("Statement");
        if (statements == null) return false;
        if (statements.isObject()) {
            return statementAllowsAdmin(statements, rootPrincipal);
        } else if (statements.isArray()) {
            for (JsonNode stmt : statements) {
                if (statementAllowsAdmin(stmt, rootPrincipal)) return true;
            }
        }
        return false;
    }

    private boolean statementAllowsAdmin(JsonNode stmt, String rootPrincipal) {
        JsonNode effectNode = stmt.get("Effect");
        if (effectNode == null || !"Allow".equals(effectNode.asText())) return false;
        JsonNode actionNode = stmt.get("Action");
        if (!hasAdminAction(actionNode)) return false;
        JsonNode resourceNode = stmt.get("Resource");
        if (!hasResourceAll(resourceNode)) return false;
        JsonNode principalNode = stmt.get("Principal");
        return matchesPrincipal(principalNode, rootPrincipal);
    }

    private boolean hasAdminAction(JsonNode actionNode) {
        if (actionNode == null) return false;
        if (actionNode.isTextual()) {
            String action = actionNode.asText();
            return "kms:*".equals(action) || ("kms:" + IKmsActionType.Types.PUT_KEY_POLICY.meaning()).equalsIgnoreCase(action);
        } else if (actionNode.isArray()) {
            for (JsonNode act : actionNode) {
                String action = act.asText();
                if ("kms:*".equals(action) || ("kms:" + IKmsActionType.Types.PUT_KEY_POLICY.meaning()).equalsIgnoreCase(action)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasResourceAll(JsonNode resourceNode) {
        if (resourceNode == null) return false;
        if (resourceNode.isTextual()) return "*".equals(resourceNode.asText());
        if (resourceNode.isArray()) {
            for (JsonNode res : resourceNode) {
                if ("*".equals(res.asText())) return true;
            }
        }
        return false;
    }

    private boolean matchesPrincipal(JsonNode principalNode, String rootPrincipal) {
        if (principalNode == null) return false;
        if (principalNode.isTextual()) {
            String principal = principalNode.asText();
            return "*".equals(principal) || rootPrincipal.equalsIgnoreCase(principal);
        }
        if (principalNode.isObject()) {
            JsonNode wamsPrincipal = getIgnoreCase(principalNode, "wams");
            if (wamsPrincipal != null) {
                if (wamsPrincipal.isTextual()) {
                    String p = wamsPrincipal.asText();
                    return "*".equals(p) || p.toLowerCase().contains(rootPrincipal.toLowerCase());
                } else if (wamsPrincipal.isArray()) {
                    for (JsonNode p : wamsPrincipal) {
                        if ("*".equals(p.asText()) || p.asText().toLowerCase().contains(rootPrincipal.toLowerCase()))
                            return true;
                    }
                }
            }
            JsonNode awsPrincipal = getIgnoreCase(principalNode, "AWS");
            if (awsPrincipal != null) {
                if (awsPrincipal.isTextual()) {
                    String p = awsPrincipal.asText();
                    return "*".equals(p) || p.toLowerCase().contains(rootPrincipal.toLowerCase());
                } else if (awsPrincipal.isArray()) {
                    for (JsonNode p : awsPrincipal) {
                        if ("*".equals(p.asText()) || p.asText().toLowerCase().contains(rootPrincipal.toLowerCase()))
                            return true;
                    }
                }
            }
        }
        return false;
    }
}