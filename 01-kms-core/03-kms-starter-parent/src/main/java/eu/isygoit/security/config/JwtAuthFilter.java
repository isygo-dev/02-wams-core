package eu.isygoit.security.config;

import eu.isygoit.filter.jwt.JwtKmsAuthFilter;
import eu.isygoit.jwt.IJwtService;
import eu.isygoit.service.ITokenService;
import eu.isygoit.service.RequestContextService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The type Jwt auth filter.
 */
@Slf4j
@Component
public class JwtAuthFilter extends JwtKmsAuthFilter {

    public JwtAuthFilter(IJwtService jwtService, RequestContextService requestContextService, ITokenService tokenService) {
        super(jwtService, requestContextService, tokenService);
    }
}
