package eu.isygoit.security.config;

import eu.isygoit.filter.jwt.JwtKmsAuthFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The type Jwt auth filter.
 */
@Slf4j
@Component
public class JwtAuthFilter extends JwtKmsAuthFilter {

}
