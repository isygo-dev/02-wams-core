package eu.isygoit.annotation;

import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreRequestDto;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Validator for CreateCustomKeyStoreRequestDto that enforces type-specific field requirements.
 * <p>
 * Validation Rules:
 * <p>
 * For CLOUDHSM type:
 * - cloudHsmClusterId: REQUIRED, must not be blank
 * - cloudHsmPassword: REQUIRED, must not be blank
 * <p>
 * For EXTERNAL_KEY_STORE type:
 * - xksProxyUriEndpoint: REQUIRED, must not be blank
 * - xksProxyAuthenticationCredential: REQUIRED, must not be blank
 * - xksProxyUriPath: OPTIONAL
 */
@Slf4j
public class CreateCustomKeyStoreRequestValidator
        implements ConstraintValidator<ValidCreateCustomKeyStoreRequest, CreateCustomKeyStoreRequestDto> {

    @Override
    public void initialize(ValidCreateCustomKeyStoreRequest constraintAnnotation) {
        // Initialization logic if needed
    }

    @Override
    public boolean isValid(CreateCustomKeyStoreRequestDto request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Null values handled by @NotNull
        }

        boolean isValid = true;

        if (request.getType() == IEnumCustomKeyStoreType.Types.CLOUDHSM) {
            isValid = validateCloudHsmRequest(request, context);
        } else if (request.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            isValid = validateExternalKeyStoreRequest(request, context);
        }

        return isValid;
    }

    /**
     * Validates CloudHSM-specific fields
     */
    private boolean validateCloudHsmRequest(CreateCustomKeyStoreRequestDto request,
                                            ConstraintValidatorContext context) {
        boolean isValid = true;

        // Validate cloudHsmClusterId
        if (request.getCloudHsmClusterId() == null || request.getCloudHsmClusterId().isBlank()) {
            addConstraintViolation(context,
                    "CloudHSM cluster ID is required for CLOUDHSM type",
                    "cloudHsmClusterId");
            isValid = false;
        }

        // Validate keyStorePassword
        if (request.getKeyStorePassword() == null || request.getKeyStorePassword().isBlank()) {
            addConstraintViolation(context,
                    "Key store password is required for CLOUDHSM type",
                    "keyStorePassword");
            isValid = false;
        } else if (request.getKeyStorePassword().length() < 8) {
            addConstraintViolation(context,
                    "Key store password must be at least 8 characters",
                    "keyStorePassword");
            isValid = false;
        }

        // Validate trustAnchorCertificate
        if (request.getTrustAnchorCertificate() == null || request.getTrustAnchorCertificate().isBlank()) {
            addConstraintViolation(context,
                    "Trust anchor certificate is required for CLOUDHSM type",
                    "trustAnchorCertificate");
            isValid = false;
        } else if (!isValidPemCertificate(request.getTrustAnchorCertificate())) {
            addConstraintViolation(context,
                    "Trust anchor certificate must be a valid PEM-encoded X.509 certificate",
                    "trustAnchorCertificate");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Validates External Key Store (XKS) specific fields
     */
    private boolean validateExternalKeyStoreRequest(CreateCustomKeyStoreRequestDto request,
                                                    ConstraintValidatorContext context) {
        boolean isValid = true;

        // Validate xksProxyUriEndpoint
        if (request.getXksProxyUriEndpoint() == null || request.getXksProxyUriEndpoint().isBlank()) {
            addConstraintViolation(context,
                    "XKS proxy URI endpoint is required for EXTERNAL_KEY_STORE type",
                    "xksProxyUriEndpoint");
            isValid = false;
        } else if (!isValidUri(request.getXksProxyUriEndpoint())) {
            addConstraintViolation(context,
                    "XKS proxy URI endpoint must be a valid URI (e.g., https://example.com)",
                    "xksProxyUriEndpoint");
            isValid = false;
        }

        // Validate xksProxyAuthenticationCredential
        if (request.getXksProxyAuthenticationCredential() == null ||
                request.getXksProxyAuthenticationCredential().isBlank()) {
            addConstraintViolation(context,
                    "XKS proxy authentication credential is required for EXTERNAL_KEY_STORE type",
                    "xksProxyAuthenticationCredential");
            isValid = false;
        }

        // xksProxyUriPath is optional - no validation needed
        // But we can warn if it's not set (it's likely needed for operations)

        return isValid;
    }

    /**
     * Simple URI validation - checks if it starts with http:// or https://
     */
    private boolean isValidUri(String uri) {
        return uri != null && (uri.startsWith("https://") || uri.startsWith("http://"));
    }

    /**
     * Validates if a string is a PEM-encoded certificate
     * PEM format should start with "-----BEGIN CERTIFICATE-----"
     */
    private boolean isValidPemCertificate(String certificate) {
        return certificate != null &&
                certificate.contains("-----BEGIN CERTIFICATE-----") &&
                certificate.contains("-----END CERTIFICATE-----");
    }

    /**
     * Helper method to add constraint violations
     */
    private void addConstraintViolation(ConstraintValidatorContext context,
                                        String message,
                                        String propertyName) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyName)
                .addConstraintViolation()
                .disableDefaultConstraintViolation();
    }
}