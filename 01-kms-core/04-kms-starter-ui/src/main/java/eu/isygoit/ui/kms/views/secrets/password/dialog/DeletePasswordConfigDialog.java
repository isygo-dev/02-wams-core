package eu.isygoit.ui.kms.views.secrets.password.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public class DeletePasswordConfigDialog extends PinBaseActionDialog {

    private final PasswordConfigService configService;
    private final Long configId;

    public DeletePasswordConfigDialog(PasswordConfigService configService, Long configId, String code, Runnable onSuccess) {
        super(I18n.t("password.dialog.delete.title"),
                I18n.t("password.dialog.delete.confirmation", code),
                onSuccess,
                true);
        this.configService = configService;
        this.configId = configId;
        setOkButtonText(I18n.t("password.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("password.dialog.delete.invalid.code"));
            return false;
        }

        try {
            configService.delete(configId);
            append(I18n.t("password.dialog.delete.success"));
            return true;
        } catch (Exception e) {
            append(I18n.t("password.dialog.delete.failed", e.getMessage()));
            return false;
        }
    }
}