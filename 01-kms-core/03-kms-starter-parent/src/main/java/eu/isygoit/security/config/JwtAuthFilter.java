package eu.isygoit.security.config;

import eu.isygoit.filter.JwtKmsAuthFilter;
import eu.isygoit.jwt.IJwtService;
import eu.isygoit.service.token.ITokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The type Jwt auth filter.
 */
@Slf4j
@Component
public class JwtAuthFilter extends JwtKmsAuthFilter {

    private final ITokenService tokenService;

    private final IJwtService jwtService;

    @Autowired
    public JwtAuthFilter(ITokenService tokenService, IJwtService jwtService) {
        this.tokenService = tokenService;
        this.jwtService = jwtService;
    }

    @Override
    protected ITokenService getTokenServiceInstance() {
        return tokenService;
    }

    @Override
    protected IJwtService getJwtServiceInstance() {
        return jwtService;
    }
}
