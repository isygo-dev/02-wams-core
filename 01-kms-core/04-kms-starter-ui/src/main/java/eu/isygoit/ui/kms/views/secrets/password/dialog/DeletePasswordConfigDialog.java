package eu.isygoit.ui.kms.views.secrets.password.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public class DeletePasswordConfigDialog extends PinBaseActionDialog {

    private final PasswordConfigService configService;
    private final Long configId;

    public DeletePasswordConfigDialog(PasswordConfigService configService, Long configId, String code, Runnable onSuccess) {
        super("Delete Configuration",
                "Delete configuration '" + code + "'? This action is irreversible.",
                onSuccess,
                true);
        this.configService = configService;
        this.configId = configId;
        setOkButtonText("Delete permanently");
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append("Invalid confirmation code");
            return false;
        }

        try {
            configService.delete(configId);
            append("Configuration deleted successfully");
            return true;
        } catch (Exception e) {
            append("Delete failed: " + e.getMessage());
            return false;
        }
    }
}