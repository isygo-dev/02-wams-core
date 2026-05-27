package eu.isygoit.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.exception.KeyPolicyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Key Policy Validator - Functional Tests")
class KeyPolicyValidatorTest {

    private static final String ROOT_PRINCIPAL = "wrn:wams:admin::123456789012:root";
    private static final String USER_PRINCIPAL = "wrn:wams:user::123456789012:alice";
    private static final String VALID_KEY_RESOURCE = "wrn:wams:kms::123456789012:key/abc-123";
    private KeyPolicyValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new KeyPolicyValidator(objectMapper);
    }

    // =========================================================================
    //  Valid policy builders (all produce valid JSON)
    // =========================================================================

    private String validMinimalPolicy() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": {
                    "Effect": "Allow",
                    "Principal": { "WAMS": "%s" },
                    "Action": "kms:DescribeKey",
                    "Resource": "*"
                  }
                }
                """.formatted(ROOT_PRINCIPAL);
    }

    private String validFullPolicy() {
        return """
                {
                  "Version": "2012-10-17",
                  "Id": "key-policy-1",
                  "Statement": [
                    {
                      "Sid": "AdminAccess",
                      "Effect": "Allow",
                      "Principal": { "WAMS": "%s" },
                      "Action": "kms:*",
                      "Resource": "*"
                    },
                    {
                      "Sid": "UserReadOnly",
                      "Effect": "Allow",
                      "Principal": { "AWS": "%s" },
                      "Action": [ "kms:DescribeKey", "kms:ListKeys" ],
                      "Resource": "%s",
                      "Condition": {
                        "StringEquals": {
                          "wams:RequestedRegion": "eu-west-1"
                        }
                      }
                    }
                  ]
                }
                """.formatted(ROOT_PRINCIPAL, USER_PRINCIPAL, VALID_KEY_RESOURCE);
    }

    private String policyWithDenyStatement() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Sid": "DenyNonHttps",
                      "Effect": "Deny",
                      "Principal": "*",
                      "Action": "kms:*",
                      "Resource": "*",
                      "Condition": {
                        "Bool": {
                          "wams:SecureTransport": "false"
                        }
                      }
                    },
                    {
                      "Sid": "AllowRoot",
                      "Effect": "Allow",
                      "Principal": { "WAMS": "%s" },
                      "Action": "kms:*",
                      "Resource": "*"
                    }
                  ]
                }
                """.formatted(ROOT_PRINCIPAL);
    }

    private String policyWithoutAdminAccess() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": {
                    "Effect": "Allow",
                    "Principal": { "WAMS": "%s" },
                    "Action": "kms:DescribeKey",
                    "Resource": "%s"
                  }
                }
                """.formatted(ROOT_PRINCIPAL, VALID_KEY_RESOURCE);
    }

    private String policyWithDuplicateSid() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    { "Sid": "Dup", "Effect": "Allow", "Principal": "*", "Action": "kms:ListKeys", "Resource": "*" },
                    { "Sid": "Dup", "Effect": "Allow", "Principal": "*", "Action": "kms:DescribeKey", "Resource": "*" }
                  ]
                }
                """;
    }

    // Invalid because action contains a dash (not allowed by regex ^kms:[A-Za-z]+$)
    private String policyWithInvalidAction() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": {
                    "Effect": "Allow",
                    "Principal": "*",
                    "Action": "kms:Invalid-Action",
                    "Resource": "*"
                  }
                }
                """;
    }

    private String policyWithInvalidPrincipalWrn() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": {
                    "Effect": "Allow",
                    "Principal": { "WAMS": "invalid-wrn" },
                    "Action": "kms:ListKeys",
                    "Resource": "*"
                  }
                }
                """;
    }

    private String policyWithWildcardPrincipalAndAction() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": {
                    "Effect": "Allow",
                    "Principal": "*",
                    "Action": "kms:*",
                    "Resource": "*"
                  }
                }
                """;
    }

    private String policyThatWouldLockOutAdmin() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": {
                    "Effect": "Deny",
                    "Principal": "*",
                    "Action": "kms:PutKeyPolicy",
                    "Resource": "*"
                  }
                }
                """;
    }

    private String policyWithMfaWarning() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": {
                    "Effect": "Allow",
                    "Principal": { "WAMS": "%s" },
                    "Action": "kms:ScheduleKeyDeletion",
                    "Resource": "*"
                  }
                }
                """.formatted(ROOT_PRINCIPAL);
    }

    // =========================================================================
    //  validateFullPolicy - positive cases
    // =========================================================================

    @Nested
    @DisplayName("Full Policy Validation - Success")
    class FullPolicySuccess {
        @Test
        @DisplayName("should accept minimal valid policy")
        void testMinimalValidPolicy() {
            validator.validateFullPolicy(validMinimalPolicy());
        }

        @Test
        @DisplayName("should accept full featured policy")
        void testFullFeaturedPolicy() {
            validator.validateFullPolicy(validFullPolicy());
        }

        @Test
        @DisplayName("should accept policy with Deny statement and conditions")
        void testPolicyWithDenyAndCondition() {
            validator.validateFullPolicy(policyWithDenyStatement());
        }

        @Test
        @DisplayName("should accept policy with Map representation")
        void testPolicyAsMap() throws Exception {
            Map<String, Object> policyMap = objectMapper.readValue(validMinimalPolicy(), Map.class);
            validator.validateFullPolicy(policyMap);
        }

        @Test
        @DisplayName("should accept policy as JsonNode")
        void testPolicyAsJsonNode() throws Exception {
            JsonNode node = objectMapper.readTree(validMinimalPolicy());
            validator.validateFullPolicy(node);
        }
    }

    // =========================================================================
    //  validateFullPolicy - negative cases
    // =========================================================================

    @Nested
    @DisplayName("Full Policy Validation - Failures")
    class FullPolicyFailure {

        @Test
        @DisplayName("should reject policy exceeding size limit (10KB)")
        void testPolicySizeExceedsLimit() {
            // Build a policy with a 10KB+ string value (properly escaped, no newlines)
            StringBuilder largeDescription = new StringBuilder();
            largeDescription.append("x".repeat(10240)); // 10KB of 'x'
            String largePolicy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": {
                        "Effect": "Allow",
                        "Principal": "*",
                        "Action": "kms:ListKeys",
                        "Resource": "*",
                        "Description": "%s"
                      }
                    }
                    """.formatted(largeDescription.toString());
            assertThatThrownBy(() -> validator.validateFullPolicy(largePolicy))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("exceeds maximum size");
        }

        @Test
        @DisplayName("should reject missing Version field")
        void testMissingVersion() {
            String policy = """
                    {
                      "Statement": { "Effect": "Allow", "Principal": "*", "Action": "kms:ListKeys", "Resource": "*" }
                    }
                    """;
            assertThatThrownBy(() -> validator.validateFullPolicy(policy))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Missing required field: 'Version'");
        }

        @Test
        @DisplayName("should reject missing Statement field")
        void testMissingStatement() {
            String policy = """
                    {
                      "Version": "2012-10-17"
                    }
                    """;
            assertThatThrownBy(() -> validator.validateFullPolicy(policy))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Missing required field: 'Statement'");
        }

        @Test
        @DisplayName("should reject empty Statement array")
        void testEmptyStatementArray() {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": []
                    }
                    """;
            assertThatThrownBy(() -> validator.validateFullPolicy(policy))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Policy must contain at least one statement");
        }

        @Test
        @DisplayName("should reject duplicate Sid")
        void testDuplicateSid() {
            assertThatThrownBy(() -> validator.validateFullPolicy(policyWithDuplicateSid()))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Duplicate Sid found: 'Dup'");
        }

        @Test
        @DisplayName("should reject invalid action (contains dash)")
        void testInvalidAction() {
            assertThatThrownBy(() -> validator.validateFullPolicy(policyWithInvalidAction()))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("invalid action 'kms:Invalid-Action'");
        }

        @Test
        @DisplayName("should reject invalid principal WRN")
        void testInvalidPrincipalWrn() {
            assertThatThrownBy(() -> validator.validateFullPolicy(policyWithInvalidPrincipalWrn()))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("invalid principal WRN: invalid-wrn");
        }

        @Test
        @DisplayName("should reject invalid Version string")
        void testInvalidVersion() {
            String policy = """
                    {
                      "Version": "bad-version",
                      "Statement": { "Effect": "Allow", "Principal": "*", "Action": "kms:ListKeys", "Resource": "*" }
                    }
                    """;
            assertThatThrownBy(() -> validator.validateFullPolicy(policy))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Version must be '2012-10-17' or '2008-10-17'");
        }

        @Test
        @DisplayName("should reject empty Id field")
        void testEmptyId() {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Id": "",
                      "Statement": { "Effect": "Allow", "Principal": "*", "Action": "kms:ListKeys", "Resource": "*" }
                    }
                    """;
            assertThatThrownBy(() -> validator.validateFullPolicy(policy))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Id, if present, must be a non-empty string");
        }
    }

    // =========================================================================
    //  Lockout Safety Check
    // =========================================================================

    @Nested
    @DisplayName("Lockout Safety Check")
    class LockoutSafety {

        @Test
        @DisplayName("should allow policy that grants admin access")
        void testAdminAccessAllowed() {
            validator.validatePolicyLockout(policyWithDenyStatement(), false, ROOT_PRINCIPAL);
        }

        @Test
        @DisplayName("should reject policy that would lock out admin")
        void testLockoutRejection() {
            assertThatThrownBy(() -> validator.validatePolicyLockout(policyThatWouldLockOutAdmin(), false, ROOT_PRINCIPAL))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Policy would lock out the key administrator");
        }

        @Test
        @DisplayName("should bypass lockout check when flag is true")
        void testBypassLockout() {
            validator.validatePolicyLockout(policyThatWouldLockOutAdmin(), true, ROOT_PRINCIPAL);
        }

        @Test
        @DisplayName("should not throw lockout exception for null policy")
        void testNullPolicy() {
            validator.validatePolicyLockout(null, false, ROOT_PRINCIPAL);
        }

        @Test
        @DisplayName("should accept policy that grants admin through kms:PutKeyPolicy specifically")
        void testSpecificAdminAction() {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": {
                        "Effect": "Allow",
                        "Principal": { "WAMS": "%s" },
                        "Action": "kms:PutKeyPolicy",
                        "Resource": "*"
                      }
                    }
                    """.formatted(ROOT_PRINCIPAL);
            validator.validatePolicyLockout(policy, false, ROOT_PRINCIPAL);
        }

        @Test
        @DisplayName("should recognize root principal even when presented in different case")
        void testPrincipalCaseInsensitive() {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": {
                        "Effect": "Allow",
                        "Principal": { "wams": "%s" },
                        "Action": "kms:*",
                        "Resource": "*"
                      }
                    }
                    """.formatted(ROOT_PRINCIPAL.toUpperCase());
            validator.validatePolicyLockout(policy, false, ROOT_PRINCIPAL);
        }
    }

    // =========================================================================
    //  User Stories (real-life scenarios)
    // =========================================================================

    @Nested
    @DisplayName("User Stories")
    class UserStories {

        @Test
        @DisplayName("Story 1: Security admin applies a compliant policy with admin safeguard")
        void story1_compliantPolicyWithAdminGuard() {
            String policy = validFullPolicy();
            validator.validateFullPolicy(policy);
            validator.validatePolicyLockout(policy, false, ROOT_PRINCIPAL);
        }

        @Test
        @DisplayName("Story 2: Admin tries to apply a policy that accidentally denies PutKeyPolicy to root")
        void story2_policyWouldLockOutAdmin() {
            String riskyPolicy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Deny",
                          "Principal": "*",
                          "Action": "kms:PutKeyPolicy",
                          "Resource": "*"
                        },
                        {
                          "Effect": "Allow",
                          "Principal": { "WAMS": "%s" },
                          "Action": "kms:DescribeKey",
                          "Resource": "*"
                        }
                      ]
                    }
                    """.formatted(ROOT_PRINCIPAL);
            validator.validateFullPolicy(riskyPolicy);
            assertThatThrownBy(() -> validator.validatePolicyLockout(riskyPolicy, false, ROOT_PRINCIPAL))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Policy would lock out the key administrator");
        }

        @Test
        @DisplayName("Story 3: Security team wants to enforce MFA for key deletion (warning logged)")
        void story3_warningForSensitiveActionWithoutCondition() {
            // No exception, only a warning log (we don't assert logs here)
            validator.validateFullPolicy(policyWithMfaWarning());
        }

        @Test
        @DisplayName("Story 4: Developer creates policy with extremely permissive wildcards, validator warns")
        void story4_permissiveWildcardWarning() {
            validator.validateFullPolicy(policyWithWildcardPrincipalAndAction());
        }

        @Test
        @DisplayName("Story 5: Multi-tenant key policy with conditions and admin access")
        void story5_multiTenantWithConditions() {
            String tenantPolicy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Principal": { "WAMS": "%s" },
                          "Action": "kms:*",
                          "Resource": "*"
                        },
                        {
                          "Effect": "Allow",
                          "Principal": { "AWS": "wrn:wams:iam::123456789012:role/tenant-reader" },
                          "Action": "kms:Decrypt",
                          "Resource": "%s",
                          "Condition": {
                            "StringEquals": {
                              "wams:TenantId": "tenant-42"
                            }
                          }
                        }
                      ]
                    }
                    """.formatted(ROOT_PRINCIPAL, VALID_KEY_RESOURCE);
            validator.validateFullPolicy(tenantPolicy);
            validator.validatePolicyLockout(tenantPolicy, false, ROOT_PRINCIPAL);
        }
    }

    // =========================================================================
    //  Edge Cases & Additional coverage
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should accept wildcard resource and principal with AWS key")
        void testWildcardWithAwsKey() {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": {
                        "Effect": "Allow",
                        "Principal": { "AWS": "*" },
                        "Action": "kms:ListKeys",
                        "Resource": "*"
                      }
                    }
                    """;
            validator.validateFullPolicy(policy);
        }

        @Test
        @DisplayName("should accept action array with single element")
        void testSingleActionArray() {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": {
                        "Effect": "Allow",
                        "Principal": "*",
                        "Action": ["kms:ListKeys"],
                        "Resource": "*"
                      }
                    }
                    """;
            validator.validateFullPolicy(policy);
        }

        @Test
        @DisplayName("should reject empty action array")
        void testEmptyActionArray() {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": {
                        "Effect": "Allow",
                        "Principal": "*",
                        "Action": [],
                        "Resource": "*"
                      }
                    }
                    """;
            assertThatThrownBy(() -> validator.validateFullPolicy(policy))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Action array cannot be empty");
        }

        @Test
        @DisplayName("should accept boolean condition values")
        void testBoolCondition() {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": {
                        "Effect": "Deny",
                        "Principal": "*",
                        "Action": "kms:*",
                        "Resource": "*",
                        "Condition": {
                          "Bool": {
                            "wams:MultiFactorAuthPresent": false
                          }
                        }
                      }
                    }
                    """;
            validator.validateFullPolicy(policy);
        }

        @Test
        @DisplayName("should reject non-object condition value")
        void testInvalidConditionStructure() {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": {
                        "Effect": "Allow",
                        "Principal": "*",
                        "Action": "kms:ListKeys",
                        "Resource": "*",
                        "Condition": "not-an-object"
                      }
                    }
                    """;
            assertThatThrownBy(() -> validator.validateFullPolicy(policy))
                    .isInstanceOf(KeyPolicyException.class)
                    .hasMessageContaining("Condition must be a JSON object");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "wrn:wams:kms::123456789012:key/abc-123",
                "wrn:wams:kms::123456789012:key/my-key-with-dash",
                "wrn:wams:kms::123456789012:key/another/valid/key"   // slashes are allowed
        })
        @DisplayName("should accept valid KMS key WRN formats")
        void testValidWrns(String wrn) {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": {
                        "Effect": "Allow",
                        "Principal": "*",
                        "Action": "kms:ListKeys",
                        "Resource": "%s"
                      }
                    }
                    """.formatted(wrn);
            validator.validateFullPolicy(policy);
        }
    }
}