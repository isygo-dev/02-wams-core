package eu.isygoit.api;

import eu.isygoit.dto.request.ChangeAuthTypeRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Authenticated API to change an account's authentication type. Inherited by {@link ProfileServiceApi}, so the
 * endpoints live under the profile path.
 */
public interface AuthTypeServiceApi {

    @Operation(summary = "Change authentication type",
            description = "Changes the auth type of the caller (or, for a tenant admin, of another account in the "
                    + "same tenant). Requires authentication and re-authentication with the caller's current credential.")
    @PostMapping(path = "/auth-type")
    ResponseEntity<Boolean> changeAuthType(@Valid @RequestBody ChangeAuthTypeRequestDto request);

    @Operation(summary = "Send a re-authentication code",
            description = "For a caller whose auth type is OTP, sends a one-time code to use as the current "
                    + "credential when changing the auth type. Returns false when the caller does not use OTP.")
    @PostMapping(path = "/auth-type/code")
    ResponseEntity<Boolean> sendReauthenticationCode();
}
