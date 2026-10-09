package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Update dialog for {@link TokenConfigDto}. The form lives in
 * {@link TokenConfigDialogBase}; this class only updates the configuration.
 * The loaded DTO is edited in place, so fields not edited in the form
 * (id, code, tenant, audit) are sent back unchanged.
 */
public class UpdateTokenConfigDialog extends TokenConfigDialogBase {

    private final KmsTokenConfigService tokenConfigService;
    private final TokenConfigDto original;

    public UpdateTokenConfigDialog(KmsTokenConfigService tokenConfigService, KmsApiService kmsApiService, TokenConfigDto dto, Runnable onSuccess) {
        super(I18n.t("kms.dialog.token.update.title"), onSuccess, kmsApiService);
        this.tokenConfigService = tokenConfigService;
        this.original = dto;
        setOkButtonText(I18n.t("kms.dialog.token.save.button"));
        initUI();
        bindData();
    }

    @Override
    protected void bindData() {
        bindIdentity(original);
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
            keySourceGroup.setValue(I18n.t("kms.dialog.token.key.source.kms"));
            if (availableKeyOptions != null) {
                KeyOption selected = availableKeyOptions.stream()
                        .filter(opt -> opt.getKeyId().equals(original.getKmsKeyId()))
                        .findFirst()
                        .orElse(null);
                kmsKeyCombo.setValue(selected);
            }
            // No custom key fields to populate
        } else {
            keySourceGroup.setValue(I18n.t("kms.dialog.token.key.source.custom"));
            signatureAlgorithmCombo.setValue(original.getSignatureAlgorithm());
            String storedKey = original.getSecretKey();
            if (HMAC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
                secretKeyField.setValue(storedKey != null ? storedKey : "");
                updateCryptographySection(original.getSignatureAlgorithm());
            } else if (ASYMMETRIC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
                updateCryptographySection(original.getSignatureAlgorithm());
                privateKeyArea.setValue(storedKey != null ? storedKey : "");
                if (StringUtils.hasText(original.getPublicKey())) {
                    publicKeyArea.setValue(original.getPublicKey());
                } else {
                    // Shown as a hint only: it must not be sent back as the public key.
                    publicKeyArea.setPlaceholder(I18n.t("kms.dialog.token.public.key.not.stored"));
                }
            }
        }
    }

    @Override
    protected boolean onOk() {
        if (!collectInto(original)) {
            return false;
        }
        return send(() -> tokenConfigService.update(original.getId(), original), "kms.dialog.token.update.failed");
    }

    @Override
    protected void onSaveSuccess() {
        append(I18n.t("kms.dialog.token.updated"));
    }
}
