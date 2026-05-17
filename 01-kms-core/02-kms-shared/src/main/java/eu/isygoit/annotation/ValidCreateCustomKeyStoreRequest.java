package eu.isygoit.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for CreateCustomKeyStoreRequest.
 * <p>
 * Ensures that type-specific required fields are populated based on the key store type:
 * - CLOUDHSM: cloudHsmClusterId, cloudHsmPassword
 * - EXTERNAL_KEY_STORE: xksProxyUriEndpoint, xksProxyAuthenticationCredential
 * <p>
 * Usage:
 *
 * @ValidCreateCustomKeyStoreRequest public class CreateCustomKeyStoreRequest { ... }
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CreateCustomKeyStoreRequestValidator.class)
public @interface ValidCreateCustomKeyStoreRequest {

    /**
     * Default violation message
     */
    String message() default "Invalid key store configuration for the specified type";

    /**
     * Validation groups (advanced feature)
     */
    Class<?>[] groups() default {};

    /**
     * Additional payload
     */
    Class<? extends Payload>[] payload() default {};
}