package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.config.JwtProperties;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.enums.IEnumAppToken;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.TokenConfig;
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
public class TokenConfigService extends CodeAssignableService<Long, TokenConfig, TokenConfigRepository> implements ITokenConfigService {

    private final JwtProperties jwtProperties;

    @Autowired
    private TokenConfigRepository tokenConfigRepository;

    /**
     * Instantiates a new Token config service.
     *
     * @param jwtProperties the jwt properties
     */
    public TokenConfigService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }


    @Override
    public TokenConfig buildTokenConfig(String domain, IEnumAppToken.Types tokenType) {
        //Serach for token config configured for the domein by type
        Optional<TokenConfig> optional = tokenConfigRepository.findByDomainIgnoreCaseAndTokenType(domain, tokenType);
        if (!optional.isPresent()) {
            //Serach for token config configured for default by type
            optional = tokenConfigRepository.findByDomainIgnoreCaseAndTokenType(DomainConstants.DEFAULT_DOMAIN_NAME, tokenType);
        }

        if (optional.isPresent()) {
            return optional.get();
        }

        //Build token config secified by system properties
        return TokenConfig.builder()
                .issuer(domain)
                .audience(domain)
                .signatureAlgorithm(jwtProperties.getSignatureAlgorithm().name())
                .secretKey(jwtProperties.getSecretKey())
                .lifeTimeInMs(jwtProperties.getLifeTimeInMs())
                .build();
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(TokenConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("TKN")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }
}
