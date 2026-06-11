package eu.isygoit.ui.views.kms.secrets.password.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.views.common.dialog.PinBaseActionDialog;

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
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        try {
            configService.delete(configId);
            Notification.show("Configuration deleted successfully", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (Exception e) {
            String errorMsg = "Delete failed: " + e.getMessage();
            append(errorMsg);
            return false;
        }
    }
}