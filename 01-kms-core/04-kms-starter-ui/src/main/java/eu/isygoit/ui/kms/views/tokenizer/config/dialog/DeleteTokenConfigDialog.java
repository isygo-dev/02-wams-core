package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public class DeleteTokenConfigDialog extends PinBaseActionDialog {

    private final KmsTokenConfigService tokenConfigService;
    private final Long configId;

    public DeleteTokenConfigDialog(KmsTokenConfigService tokenConfigService,
                                   Long configId,
                                   String code,
                                   Runnable onSuccess) {
        super(I18n.t("dialog.delete.title"),
                I18n.t("token.config.delete.confirmation", code),
                onSuccess,
                true);
        this.tokenConfigService = tokenConfigService;
        this.configId = configId;
        setOkButtonText(I18n.t("common.button.delete"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("dialog.action.required"));
            return false;
        }

        try {
            tokenConfigService.delete(configId);
            append(I18n.t("token.config.deleted"));
            return true;
        } catch (Exception e) {
            append(I18n.t("notification.error") + ": " + e.getMessage());
            return false;
        }
    }
}