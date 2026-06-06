package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.config.JwtProperties;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.exception.KmsKeyNotFoundException;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.TokenConfig;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.TokenConfigRepository;
import eu.isygoit.service.ITokenConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Optional;
import java.util.Set;

/**
 * The type Token config service.
 */

@Slf4j
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectRepository(value = TokenConfigRepository.class)
public class TokenConfigService extends CodeAssignableTenantService<Long, TokenConfig, TokenConfigRepository> implements ITokenConfigService {

    private final JwtProperties jwtProperties;

    @Autowired
    private TokenConfigRepository tokenConfigRepository;

    @Autowired
    private KmsKeyRepository kmsKeyRepository;

    /**
     * Instantiates a new Token config service.
     *
     * @param jwtProperties the jwt properties
     */
    public TokenConfigService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }


    @Override
    public TokenConfig prepareTokenConfig(String tenant, IEnumToken.Types tokenType) {
        //Serach for token config configured for the domein by type
        Optional<TokenConfig> optional = tokenConfigRepository.findByTenantIgnoreCaseAndTokenType(tenant, tokenType);
        if (!optional.isPresent()) {
            //Serach for token config configured for default by type
            optional = tokenConfigRepository.findByTenantIgnoreCaseAndTokenType(TenantConstants.DEFAULT_TENANT_NAME, tokenType);
        }

        if (optional.isPresent()) {
            TokenConfig tokenConfig = optional.get();
            if(!StringUtils.isEmpty(tokenConfig.getKmsKeyId())) {
                tokenConfig = this.fillSecretsWithSelectedKmsKey(tenant, tokenConfig);
            }
            return tokenConfig;
        }

        //Build token config secified by system properties
        return TokenConfig.builder()
                .issuer(tenant)
                .audience(Set.of("*"))
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

    @Override
    public TokenConfig beforeCreate(String tenant, TokenConfig tokenConfig) {
        if (tokenConfig.getKmsKeyId() != null) {
            fillSecretsWithSelectedKmsKey(tenant, tokenConfig);
        }
        return super.beforeCreate(tenant, tokenConfig);
    }

    @Override
    public TokenConfig beforeUpdate(String tenant, TokenConfig tokenConfig) {
        if (tokenConfig.getKmsKeyId() != null) {
            fillSecretsWithSelectedKmsKey(tenant, tokenConfig);
        }
        return super.beforeUpdate(tenant, tokenConfig);
    }

    public TokenConfig fillSecretsWithSelectedKmsKey(String tenant, TokenConfig tokenConfig) {
        if(!StringUtils.isEmpty(tokenConfig.getKmsKeyId())) {
            KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, tokenConfig.getKmsKeyId())
                    .orElseThrow(() -> new KmsKeyNotFoundException("KMS Key with ID " + tokenConfig.getKmsKeyId() + " not found for tenant " + tenant));

            // Derive signature algorithm from the key spec
            String algorithm = mapKeySpecToJwtAlgorithm(kmsKey.getKeySpec());
            tokenConfig.setSignatureAlgorithm(algorithm);

            // Determine key type and set appropriate fields
            if (!kmsKey.getKeySpec().isAsymmetric()) {
                // Symmetric key: store as Base64
                String secretKeyBase64 = Base64.getEncoder().encodeToString(kmsKey.getKeyMaterial());
                tokenConfig.setSecretKey(secretKeyBase64);
                tokenConfig.setPublicKey(null);   // not used for symmetric
            } else {
                // Asymmetric key: store private key as Base64, public key as Base64
                String privateKeyBase64 = Base64.getEncoder().encodeToString(kmsKey.getKeyMaterial());
                String publicKeyBase64 = Base64.getEncoder().encodeToString(kmsKey.getPublicKey());
                tokenConfig.setSecretKey(privateKeyBase64);
                tokenConfig.setPublicKey(publicKeyBase64);
            }
        } else {
            log.warn("TokenConfig with ID {} does not have a KMS Key ID specified. Skipping key material population.", tokenConfig.getId());
        }

        return tokenConfig;
    }

    private String mapKeySpecToJwtAlgorithm(IEnumKeySpec.Types keySpec) {
        switch (keySpec) {
            // Symmetric (AES) – JJWT does not support AES for signing; only HMAC.
            // If a symmetric key is used for signing, it must be an HMAC key spec (HMAC_xxx).
            // SYMMETRIC_DEFAULT is for encryption only, so we throw an exception.
            case SYMMETRIC_DEFAULT:
                throw new IllegalArgumentException("SYMMETRIC_DEFAULT is not a valid key spec for JWT signing. Use HMAC_256, HMAC_384, or HMAC_512.");

            // RSA – map based on key size
            case RSA_2048:
                return "RS256";
            case RSA_3072:
                return "RS384";
            case RSA_4096:
                return "RS512";

            // HMAC – only HMAC_256, 384, 512 are supported by JJWT
            case HMAC_224:
                throw new IllegalArgumentException("HMAC_224 is not supported by JJWT. Use HS256, HS384, or HS512.");
            case HMAC_256:
                return "HS256";
            case HMAC_384:
                return "HS384";
            case HMAC_512:
                return "HS512";

            // ECC (NIST curves) – map to ES algorithms
            case ECC_NIST_P256:
                return "ES256";
            case ECC_NIST_P384:
                return "ES384";
            case ECC_NIST_P521:
                return "ES512";

            // ECC (SECG curve secp256k1) – supported as ES256K in JJWT
            case ECC_SECG_P256K1:
                return "ES256K";

            // SM2 – not natively supported; throw exception
            case SM2:
                throw new IllegalArgumentException("SM2 is not supported by JJWT. Use RSA or EC (NIST) instead.");

            default:
                throw new IllegalArgumentException("Unsupported key spec for JWT signing: " + keySpec);
        }
    }
}
