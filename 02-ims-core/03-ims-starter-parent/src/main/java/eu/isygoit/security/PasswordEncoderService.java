package eu.isygoit.security;

import eu.isygoit.dto.request.MatchesRequestDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.remote.kms.KmsPublicPasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * The type Password encoder service.
 */
@Slf4j
@Service
@Transactional
public class PasswordEncoderService implements PasswordEncoder {

    @Autowired
    private KmsPasswordService kmsPasswordService;

    @Autowired
    private KmsPublicPasswordService kmsPublicPasswordService;

    @Override
    public String encode(CharSequence rawPassword) {
        log.warn("Sorry, Encoding is delegated to KMS service");
        return rawPassword.toString(); //prepareTimingAttackProtection
    }

    private IEnumPasswordStatus.Types getAuthType(IEnumAuth.Types authType, MatchesRequestDto matchesRequestDto) {
        switch (authType) {
            case PWD:
                return kmsPasswordService.matchesPassword(matchesRequestDto).getBody();
            case TOKEN:
                return kmsPasswordService.matchesToken(matchesRequestDto).getBody();
            case OTP:
                return kmsPublicPasswordService.matchesOtp(matchesRequestDto).getBody();
            case QRC:
                return kmsPublicPasswordService.matchesQrc(matchesRequestDto).getBody();
            default: {
                log.warn("BAD, Matching is delegated to KMS service for unknown auth type: " + authType);
                return IEnumPasswordStatus.Types.BAD;
            }
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword /*userName@Tenant*/) {
        String[] encodedPasswordArray = encodedPassword.split("@");
        if (encodedPasswordArray.length == 3) {
            IEnumAuth.Types authType = IEnumAuth.Types.valueOf(encodedPasswordArray[2]);
            MatchesRequestDto matchesRequestDto = MatchesRequestDto.builder()
                    .tenant(encodedPasswordArray[1])
                    .userName(encodedPasswordArray[0])
                    .authType(authType)
                    .password(rawPassword.toString())
                    .build();

            return !Arrays.asList(IEnumPasswordStatus.Types.BAD, IEnumPasswordStatus.Types.BROKEN).contains(
                    getAuthType(authType, matchesRequestDto)
            );
        }
        return false;
    }
}
