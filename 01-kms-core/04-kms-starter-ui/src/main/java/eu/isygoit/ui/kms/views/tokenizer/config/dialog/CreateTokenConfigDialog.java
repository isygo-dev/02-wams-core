package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;

import java.util.List;
import java.util.UUID;

/**
 * Create dialog for {@link TokenConfigDto}. The form lives in
 * {@link TokenConfigDialogBase}; this class only creates the configuration.
 */
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
        TokenConfigDto tokenConfig = new TokenConfigDto();
        // The code field stays empty in the form (read-only); the code sent is still generated here, as before.
        tokenConfig.setCode("TC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));

        if (!collectInto(tokenConfig)) {
            return false;
        }
        return send(() -> tokenConfigService.create(tokenConfig), "kms.dialog.token.create.failed");
    }

    @Override
    protected void onSaveSuccess() {
        append(I18n.t("kms.dialog.token.created"));
    }
}
