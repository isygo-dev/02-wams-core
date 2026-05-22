package eu.isygoit.security.config;

import eu.isygoit.filter.jwt.JwtKmsClientAuthFilter;
import eu.isygoit.jwt.IJwtService;
import eu.isygoit.service.RequestContextService;
import eu.isygoit.service.TokenServiceApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The type Jwt auth filter.
 */
@Slf4j
@Component
public class JwtAuthFilter extends JwtKmsClientAuthFilter {

    public JwtAuthFilter(IJwtService jwtService, RequestContextService requestContextService, TokenServiceApi tokenService) {
        super(jwtService, requestContextService, tokenService);
    }
}
