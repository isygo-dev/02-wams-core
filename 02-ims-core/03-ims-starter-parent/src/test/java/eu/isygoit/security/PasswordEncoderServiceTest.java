package eu.isygoit.security;

import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.remote.kms.KmsPublicPasswordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A8: only VALID counts as a password match; null and every other status fail closed.
 */
@ExtendWith(MockitoExtension.class)
class PasswordEncoderServiceTest {

    private static final String PRINCIPAL_PWD = "alice@acme@PWD";

    @Mock
    private KmsPasswordService kmsPasswordService;
    @Mock
    private KmsPublicPasswordService kmsPublicPasswordService;
    @InjectMocks
    private PasswordEncoderService encoder;

    @Test
    void validIsTheOnlyMatch() {
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok(IEnumPasswordStatus.Types.VALID));

        assertTrue(encoder.matches("secret", PRINCIPAL_PWD));
    }

    @ParameterizedTest
    @EnumSource(value = IEnumPasswordStatus.Types.class, names = "VALID", mode = EnumSource.Mode.EXCLUDE)
    void everyNonValidStatusIsRejected(IEnumPasswordStatus.Types status) {
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok(status));

        assertFalse(encoder.matches("secret", PRINCIPAL_PWD));
    }

    @Test
    void nullBodyIsRejected() {
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok().build());

        assertFalse(encoder.matches("secret", PRINCIPAL_PWD));
    }

    @Test
    void otpAndQrcPathsAlsoFailClosed() {
        when(kmsPublicPasswordService.matchesOtp(any())).thenReturn(ResponseEntity.ok().build());
        when(kmsPublicPasswordService.matchesQrc(any())).thenReturn(ResponseEntity.ok(IEnumPasswordStatus.Types.EXPIRED));

        assertFalse(encoder.matches("1234", "alice@acme@OTP"));
        assertFalse(encoder.matches("token", "alice@acme@QRC"));
    }

    @Test
    void malformedPrincipalIsRejected() {
        assertFalse(encoder.matches("secret", "alice@acme"));
        assertFalse(encoder.matches("secret", "alice@acme@PWD@extra"));
    }
}
