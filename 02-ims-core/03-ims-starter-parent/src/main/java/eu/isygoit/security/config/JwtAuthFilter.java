package eu.isygoit.security.config;

import eu.isygoit.filter.JwtKmsClientAuthFilter;
import eu.isygoit.jwt.IJwtService;
import eu.isygoit.service.token.TokenServiceApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The type Jwt auth filter.
 */
@Slf4j
@Component
public class JwtAuthFilter extends JwtKmsClientAuthFilter {

    private final TokenServiceApi tokenService;
    private final IJwtService jwtService;

    @Autowired
    public JwtAuthFilter(TokenServiceApi tokenService, IJwtService jwtService) {
        this.tokenService = tokenService;
        this.jwtService = jwtService;
    }

    @Override
    protected TokenServiceApi getTokenServiceInstance() {
        return tokenService;
    }

    @Override
    protected IJwtService getJwtServiceInstance() {
        return jwtService;
    }
}
