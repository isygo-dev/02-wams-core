package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.List;

public class UpdateTokenConfigDialog extends TokenConfigDialogBase {

    private final KmsTokenConfigService tokenConfigService;
    private final TokenConfigDto original;
    private TextField codeField;

    public UpdateTokenConfigDialog(KmsTokenConfigService tokenConfigService, KmsApiService kmsApiService, TokenConfigDto dto, Runnable onSuccess) {
        super(I18n.t("dialog.token.update.title"), onSuccess, kmsApiService);
        this.tokenConfigService = tokenConfigService;
        this.original = dto;
        setOkButtonText(I18n.t("dialog.token.save.button"));
        initUI();
        addCodeFieldToMetadataCard();
        bindData();
    }

    private void addCodeFieldToMetadataCard() {
        codeField = new TextField(I18n.t("dialog.token.code"));
        codeField.setReadOnly(true);
        codeField.setWidthFull();
        VerticalLayout metaForm = (VerticalLayout) metadataCard.getChildren().toArray()[1];
        metaForm.addComponentAsFirst(codeField);
    }

    @Override
    protected void bindData() {
        codeField.setValue(original.getCode());
        tokenTypeCombo.setValue(original.getTokenType());
        tokenTypeCombo.setReadOnly(true);
        issuerField.setValue(original.getIssuer() != null ? original.getIssuer() : "");
        if (original.getAudience() != null && !original.getAudience().isEmpty()) {
            setAudienceList(original.getAudience());
        } else {
            setAudienceList(List.of());
        }
        setLifeTimeFromMs(original.getLifeTimeInMs());

        // Handle key source selection
        if (StringUtils.hasText(original.getKmsKeyId())) {
            keySourceGroup.setValue(I18n.t("dialog.token.key.source.kms"));
            if (availableKeyOptions != null) {
                KeyOption selected = availableKeyOptions.stream()
                        .filter(opt -> opt.getKeyId().equals(original.getKmsKeyId()))
                        .findFirst()
                        .orElse(null);
                kmsKeyCombo.setValue(selected);
            }
            // No custom key fields to populate
        } else {
            keySourceGroup.setValue(I18n.t("dialog.token.key.source.custom"));
            signatureAlgorithmCombo.setValue(original.getSignatureAlgorithm());
            String storedKey = original.getSecretKey();
            if (HMAC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
                secretKeyField.setValue(storedKey != null ? storedKey : "");
                updateCryptographySection(original.getSignatureAlgorithm());
            } else if (ASYMMETRIC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
                privateKeyArea.setValue(storedKey != null ? storedKey : "");
                publicKeyArea.setValue(original.getPublicKey() != null ? original.getPublicKey() :
                        I18n.t("dialog.token.public.key.not.stored"));
                updateCryptographySection(original.getSignatureAlgorithm());
            }
        }
    }

    @Override
    protected boolean onOk() {
        IEnumToken.Types tokenType = tokenTypeCombo.getValue();
        if (tokenType == null) {
            append(I18n.t("dialog.token.type.required"));
            return false;
        }

        Integer lifeTime = getLifeTimeInMs();

        TokenConfigDto.TokenConfigDtoBuilder builder = TokenConfigDto.builder()
                .id(original.getId())
                .code(original.getCode())
                .tokenType(tokenType)
                .issuer(issuerField.getValue())
                .audience(getAudienceList())
                .lifeTimeInMs(lifeTime);

        boolean useKmsKey = I18n.t("dialog.token.key.source.kms").equals(keySourceGroup.getValue());
        if (useKmsKey) {
            KeyOption selected = kmsKeyCombo.getValue();
            if (selected == null) {
                append(I18n.t("dialog.token.kms.select"));
                return false;
            }
            builder.kmsKeyId(selected.getKeyId())
                    .secretKey(null)
                    .publicKey(null)
                    .signatureAlgorithm(null);
        } else {
            String signatureAlgorithm = signatureAlgorithmCombo.getValue();
            if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
                append(I18n.t("dialog.token.algorithm.required"));
                return false;
            }
            builder.signatureAlgorithm(signatureAlgorithm);
            String secretOrPrivateKey;
            if (HMAC_ALGORITHMS.contains(signatureAlgorithm)) {
                String secretKey = secretKeyField.getValue();
                if (secretKey == null || secretKey.isBlank()) {
                    append(I18n.t("dialog.token.secret.required", signatureAlgorithm));
                    return false;
                }
                if (!validateHmacKey(signatureAlgorithm, secretKey)) return false;
                secretOrPrivateKey = secretKey;
                builder.publicKey(null);
            } else if (ASYMMETRIC_ALGORITHMS.contains(signatureAlgorithm)) {
                String privateKey = privateKeyArea.getValue();
                if (privateKey == null || privateKey.isBlank()) {
                    append(I18n.t("dialog.token.private.required", signatureAlgorithm));
                    return false;
                }
                secretOrPrivateKey = privateKey;
                builder.publicKey(publicKeyArea.getValue());
            } else {
                append(I18n.t("dialog.token.unsupported.algorithm", signatureAlgorithm));
                return false;
            }
            builder.secretKey(secretOrPrivateKey)
                    .kmsKeyId(null);
        }

        TokenConfigDto updated = builder.build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.update(original.getId(), updated);
            if (response.getStatusCode().is2xxSuccessful()) {
                onSaveSuccess();
                return true;
            } else {
                append(I18n.t("dialog.token.update.failed", response.getStatusCode()));
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
        append(I18n.t("dialog.token.updated"));
    }
}