package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.impl.CodifiableService;
import eu.isygoit.config.JwtProperties;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.enums.IEnumAppToken;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.TokenConfig;
import eu.isygoit.model.extendable.NextCodeModel;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.repository.TokenConfigRepository;
import eu.isygoit.service.ITokenConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Token config service.
 */
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@SrvRepo(value = TokenConfigRepository.class)
public class TokenConfigService extends CodifiableService<Long, TokenConfig, TokenConfigRepository> implements ITokenConfigService {

    private final JwtProperties jwtProperties;

    private final TokenConfigRepository tokenConfigRepository;

    /**
     * Instantiates a new Token config service.
     *
     * @param jwtProperties         the jwt properties
     * @param tokenConfigRepository the token config repository
     */
    @Autowired
    public TokenConfigService(JwtProperties jwtProperties, TokenConfigRepository tokenConfigRepository) {
        this.jwtProperties = jwtProperties;
        this.tokenConfigRepository = tokenConfigRepository;
    }


    @Override
    public Optional<TokenConfig> buildTokenConfig(String domain, IEnumAppToken.Types tokenType) {
        // Attempt to find the token config for the given domain first
        return Optional.ofNullable(tokenConfigRepository.findByDomainIgnoreCaseAndTokenType(domain, tokenType)
                // If not found, fallback to default domain search
                .or(() -> tokenConfigRepository.findByDomainIgnoreCaseAndTokenType(DomainConstants.DEFAULT_DOMAIN_NAME, tokenType))
                // If still not found, build the default token config
                .orElseGet(() -> buildDefaultTokenConfig(domain)));
    }

    private TokenConfig buildDefaultTokenConfig(String domain) {
        return TokenConfig.builder()
                .issuer(domain)
                .audience(domain)
                .signatureAlgorithm(jwtProperties.getSignatureAlgorithm().name())
                .secretKey(jwtProperties.getSecretKey())
                .lifeTimeInMs(jwtProperties.getLifeTimeInMs())
                .build();
    }

    @Override
    public Optional<NextCodeModel> initCodeGenerator() {
        return Optional.ofNullable(AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(TokenConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("TKN")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build());
    }
}
