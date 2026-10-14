package eu.isygoit.dto.request;

import eu.isygoit.enums.IEnumAuth;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Request to change an account's authentication type.
 * The tenant is never taken from the body: it is always the caller's tenant.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ChangeAuthTypeRequestDto {

    /**
     * Target account. Optional: defaults to the caller. Only a tenant admin may target another account.
     */
    private String userName;

    @NotNull
    private IEnumAuth.Types authType;

    /**
     * Re-authentication: the caller's current credential (password, OTP or QR code, depending on the caller's auth type).
     */
    @NotEmpty
    @ToString.Exclude
    private String currentCredential;
}
