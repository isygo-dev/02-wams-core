package eu.isygoit.ui.views.kms.tokenizer.config.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.views.common.dialog.PinBaseActionDialog;

public class DeleteTokenConfigDialog extends PinBaseActionDialog {

    private final KmsTokenConfigService tokenConfigService;
    private final Long configId;

    public DeleteTokenConfigDialog(KmsTokenConfigService tokenConfigService,
                                   Long configId,
                                   String code,
                                   Runnable onSuccess) {
        super("Delete Configuration",
                "Delete configuration '" + code + "'? This action is irreversible.",
                onSuccess,
                true); // require PIN
        this.tokenConfigService = tokenConfigService;
        this.configId = configId;
        setOkButtonText("Delete permanently");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        try {
            tokenConfigService.delete(configId);
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