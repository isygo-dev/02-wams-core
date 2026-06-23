package eu.isygoit.security.config;

import eu.isygoit.jwt.IJwtService;
import eu.isygoit.jwt.filter.JwtKmsAuthFilter;
import eu.isygoit.service.ITokenValidationService;
import eu.isygoit.service.RequestContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The type Jwt auth filter.
 */
@Slf4j
@Component
public class JwtAuthFilter extends JwtKmsAuthFilter {

    public JwtAuthFilter(IJwtService jwtService, RequestContextService requestContextService, ITokenValidationService tokenService) {
        super(jwtService, requestContextService, tokenService);
    }
}
