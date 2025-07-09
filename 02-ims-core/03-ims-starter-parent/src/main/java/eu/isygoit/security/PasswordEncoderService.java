package eu.isygoit.security;

import eu.isygoit.dto.request.MatchesRequestDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.remote.kms.KmsPasswordService;
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

    @Override
    public String encode(CharSequence rawPassword) {
        log.warn("Sorry, Encoding is delegated to KMS service");
        return rawPassword.toString(); //prepareTimingAttackProtection
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword /*userName@Domain*/) {
        String[] encodedPasswordArray = encodedPassword.split("@");
        if (encodedPasswordArray.length == 3) {
            return !Arrays.asList(IEnumPasswordStatus.Types.BAD, IEnumPasswordStatus.Types.BROKEN).contains(
                    kmsPasswordService.matches(//RequestContextDto.builder().build(),
                            MatchesRequestDto.builder()
                                    .tenant(encodedPasswordArray[1])
                                    .userName(encodedPasswordArray[0])
                                    .authType(IEnumAuth.Types.valueOf(encodedPasswordArray[2]))
                                    .password(rawPassword.toString())
                                    .build()).getBody()
            );
        }
        return false;
    }
}
