package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public class CreateTokenConfigDialog extends TokenConfigDialogBase {

    private final KmsTokenConfigService tokenConfigService;

    public CreateTokenConfigDialog(KmsTokenConfigService tokenConfigService, KmsApiService kmsApiService, Runnable onSuccess) {
        super(I18n.t("kms.dialog.token.create.title"), onSuccess, kmsApiService);
        this.tokenConfigService = tokenConfigService;
        setOkButtonText(I18n.t("kms.dialog.token.create.button"));
        initUI();
        bindData();
    }

    @Override
    protected void bindData() {
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        issuerField.clear();
        setAudienceList(List.of());
        signatureAlgorithmCombo.setValue("HS256");
        secretKeyField.clear();
        privateKeyArea.clear();
        publicKeyArea.clear();
        lifeTimeValueField.setValue(1);
        lifeTimeUnitCombo.setValue(I18n.t("kms.dialog.token.lifetime.unit.hours"));
        keySourceGroup.setValue(I18n.t("kms.dialog.token.key.source.custom"));
        kmsKeyCombo.clear();
    }

    @Override
    protected boolean onOk() {
        IEnumToken.Types tokenType = tokenTypeCombo.getValue();
        if (tokenType == null) {
            append(I18n.t("kms.dialog.token.type.required"));
            return false;
        }

        Integer lifeTime = getLifeTimeInMs();

        String generatedCode = "TC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        TokenConfigDto.TokenConfigDtoBuilder builder = TokenConfigDto.builder()
                .code(generatedCode)
                .tokenType(tokenType)
                .issuer(issuerField.getValue())
                .audience(getAudienceList())
                .lifeTimeInMs(lifeTime);

        boolean useKmsKey = I18n.t("kms.dialog.token.key.source.kms").equals(keySourceGroup.getValue());
        if (useKmsKey) {
            KeyOption selected = kmsKeyCombo.getValue();
            if (selected == null) {
                append(I18n.t("kms.dialog.token.kms.select"));
                return false;
            }
            builder.kmsKeyId(selected.getKeyId())
                    .secretKey(null)
                    .publicKey(null)
                    .signatureAlgorithm(null);
        } else {
            String signatureAlgorithm = signatureAlgorithmCombo.getValue();
            if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
                append(I18n.t("kms.dialog.token.algorithm.required"));
                return false;
            }
            builder.signatureAlgorithm(signatureAlgorithm);
            String secretOrPrivateKey;
            if (HMAC_ALGORITHMS.contains(signatureAlgorithm)) {
                String secretKey = secretKeyField.getValue();
                if (secretKey == null || secretKey.isBlank()) {
                    append(I18n.t("kms.dialog.token.secret.required", signatureAlgorithm));
                    return false;
                }
                if (!validateHmacKey(signatureAlgorithm, secretKey)) return false;
                secretOrPrivateKey = secretKey;
                builder.publicKey(null);
            } else if (ASYMMETRIC_ALGORITHMS.contains(signatureAlgorithm)) {
                String privateKey = privateKeyArea.getValue();
                if (privateKey == null || privateKey.isBlank()) {
                    append(I18n.t("kms.dialog.token.private.required", signatureAlgorithm));
                    return false;
                }
                secretOrPrivateKey = privateKey;
                builder.publicKey(publicKeyArea.getValue());
            } else {
                append(I18n.t("kms.dialog.token.unsupported.algorithm", signatureAlgorithm));
                return false;
            }
            builder.secretKey(secretOrPrivateKey)
                    .kmsKeyId(null);
        }

        TokenConfigDto tokenConfig = builder.build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.create(tokenConfig);
            if (response.getStatusCode().is2xxSuccessful()) {
                onSaveSuccess();
                return true;
            } else {
                append(I18n.t("kms.dialog.token.create.failed", response.getStatusCode()));
                return false;
            }
        } catch (FeignException ex) {
            handleFeignException(ex);
            return false;
        } catch (Exception e) {
            handleGenericException(e);
            return false;
        }
    }

    @Override
    protected void onSaveSuccess() {
        append(I18n.t("kms.dialog.token.created"));
    }
}