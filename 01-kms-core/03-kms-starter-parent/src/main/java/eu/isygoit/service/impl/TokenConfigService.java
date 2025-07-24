package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.config.JwtProperties;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumToken;
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
@InjectCodeGen(value = NextCodeService.class)
@InjectRepository(value = TokenConfigRepository.class)
public class TokenConfigService extends CodeAssignableTenantService<Long, TokenConfig, TokenConfigRepository> implements ITokenConfigService {

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
    public TokenConfig buildTokenConfig(String tenant, IEnumToken.Types tokenType) {
        //Serach for token config configured for the domein by type
        Optional<TokenConfig> optional = tokenConfigRepository.findByTenantIgnoreCaseAndTokenType(tenant, tokenType);
        if (!optional.isPresent()) {
            //Serach for token config configured for default by type
            optional = tokenConfigRepository.findByTenantIgnoreCaseAndTokenType(TenantConstants.DEFAULT_TENANT_NAME, tokenType);
        }

        if (optional.isPresent()) {
            return optional.get();
        }

        //Build token config secified by system properties
        return TokenConfig.builder()
                .issuer(tenant)
                .audience(tenant)
                .signatureAlgorithm(jwtProperties.getSignatureAlgorithm().name())
                .secretKey(jwtProperties.getSecretKey())
                .lifeTimeInMs(jwtProperties.getLifeTimeInMs())
                .build();
    }

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(TokenConfig.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("TKN")
                .valueLength(6L)
                .codeValue(1L)
                .increment(1)
                .build();
    }
}
