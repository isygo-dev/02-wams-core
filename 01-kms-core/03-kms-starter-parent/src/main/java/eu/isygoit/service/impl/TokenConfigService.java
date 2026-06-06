package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.config.JwtProperties;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.exception.KmsKeyNotFoundException;
import eu.isygoit.exception.NoActiveVersionException;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.model.TokenConfig;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.KmsKeyVersionRepository;
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

    @Autowired
    private KmsKeyVersionRepository kmsKeyVersionRepository;  // new

    public TokenConfigService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Maps a KMS key specification to the corresponding JJWT algorithm name.
     */
    public static String mapKeySpecToJwtAlgorithm(IEnumKeySpec.Types keySpec) {
        switch (keySpec) {
            case SYMMETRIC_DEFAULT:
                throw new IllegalArgumentException("SYMMETRIC_DEFAULT is not a valid key spec for JWT signing. Use HMAC_256, HMAC_384, or HMAC_512.");
            case RSA_2048:
                return "RS256";
            case RSA_3072:
                return "RS384";
            case RSA_4096:
                return "RS512";
            case HMAC_224:
                throw new IllegalArgumentException("HMAC_224 is not supported by JJWT. Use HS256, HS384, or HS512.");
            case HMAC_256:
                return "HS256";
            case HMAC_384:
                return "HS384";
            case HMAC_512:
                return "HS512";
            case ECC_NIST_P256:
                return "ES256";
            case ECC_NIST_P384:
                return "ES384";
            case ECC_NIST_P521:
                return "ES512";
            case ECC_SECG_P256K1:
                return "ES256K";
            case SM2:
                throw new IllegalArgumentException("SM2 is not supported by JJWT. Use RSA or EC (NIST) instead.");
            default:
                throw new IllegalArgumentException("Unsupported key spec for JWT signing: " + keySpec);
        }
    }

    @Override
    public TokenConfig prepareTokenConfig(String tenant, IEnumToken.Types tokenType, String kmsKeyVersionId) {
        // Search for token config configured for the domain by type
        Optional<TokenConfig> optional = tokenConfigRepository.findByTenantIgnoreCaseAndTokenType(tenant, tokenType);
        if (!optional.isPresent()) {
            // Search for token config configured for default by type
            optional = tokenConfigRepository.findByTenantIgnoreCaseAndTokenType(TenantConstants.DEFAULT_TENANT_NAME, tokenType);
        }

        if (optional.isPresent()) {
            TokenConfig tokenConfig = optional.get();
            // If a KMS key is linked, refresh its secrets from the current active version
            if (StringUtils.hasText(tokenConfig.getKmsKeyId())) {
                return fillSecretsWithCurrentKmsKeyVersion(tenant, tokenConfig, kmsKeyVersionId);
            }
            return tokenConfig;
        }

        // Fallback: token config from system properties
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
            // At creation time, store a snapshot using the current active version
            fillSecretsWithCurrentKmsKeyVersion(tenant, tokenConfig, null);
        }
        return super.beforeCreate(tenant, tokenConfig);
    }

    @Override
    public TokenConfig beforeUpdate(String tenant, TokenConfig tokenConfig) {
        if (tokenConfig.getKmsKeyId() != null) {
            // When updating, also refresh to the current active version
            fillSecretsWithCurrentKmsKeyVersion(tenant, tokenConfig, null);
        }
        return super.beforeUpdate(tenant, tokenConfig);
    }

    /**
     * Fills the token configuration with the current active key material of the linked KMS key.
     * This method is called every time the configuration is used (prepareTokenConfig)
     * and also during create/update to persist a snapshot (optional).
     */
    public TokenConfig fillSecretsWithCurrentKmsKeyVersion(String tenant, TokenConfig tokenConfig, String kmsKeyVersionId) {
        if (!StringUtils.hasText(tokenConfig.getKmsKeyId())) {
            log.warn("TokenConfig with ID {} does not have a KMS Key ID specified. Skipping key material population.", tokenConfig.getId());
            return tokenConfig;
        }

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, tokenConfig.getKmsKeyId())
                .orElseThrow(() -> new KmsKeyNotFoundException("KMS Key with ID " + tokenConfig.getKmsKeyId() + " not found for tenant " + tenant));

        // Get the current active version of the KMS key
        if (!StringUtils.hasText(kmsKeyVersionId)) {
            kmsKeyVersionId = kmsKey.getCurrentVersionId();
        }
        KmsKeyVersion activeVersion = kmsKeyVersionRepository
                .findByTenantAndKeyIdAndVersionIdAndKeyStatus(tenant, kmsKey.getKeyId(), kmsKeyVersionId, IEnumKeyStatus.Types.ENABLED)
                .orElseThrow(() -> new NoActiveVersionException("No active version found for KMS key " + kmsKey.getKeyId()));

        // Derive signature algorithm from the key spec
        String algorithm = mapKeySpecToJwtAlgorithm(kmsKey.getKeySpec());
        tokenConfig.setSignatureAlgorithm(algorithm);
        tokenConfig.setKmsKeyVersion(activeVersion.getVersionId());

        // Set the key material from the active version
        if (!kmsKey.getKeySpec().isAsymmetric()) {
            // Symmetric key (HMAC)
            String secretKeyBase64 = Base64.getEncoder().encodeToString(activeVersion.getKeyMaterial());
            tokenConfig.setSecretKey(secretKeyBase64);
            tokenConfig.setPublicKey(null);
        } else {
            // Asymmetric key (RSA/EC)
            String privateKeyBase64 = Base64.getEncoder().encodeToString(activeVersion.getKeyMaterial());
            String publicKeyBase64 = Base64.getEncoder().encodeToString(activeVersion.getPublicKey());
            tokenConfig.setSecretKey(privateKeyBase64);
            tokenConfig.setPublicKey(publicKeyBase64);
        }

        return tokenConfig;
    }
}