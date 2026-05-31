package eu.isygoit.ui.views.incremental.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.views.PinBaseActionDialog;

public class DeleteNextCodeDialog extends PinBaseActionDialog {

    private final KmsAppNextCodeService nextCodeService;
    private final Long configId;

    public DeleteNextCodeDialog(KmsAppNextCodeService nextCodeService,
                                Long configId,
                                String entity,
                                String attribute,
                                Runnable onSuccess) {
        super("Delete configuration",
                "Delete configuration for '" + entity + ":" + attribute + "'? This action is irreversible.",
                onSuccess,
                true); // require PIN
        this.nextCodeService = nextCodeService;
        this.configId = configId;
        setOkButtonText("Delete permanently");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        try {
            nextCodeService.delete(configId);
            Notification.show("Configuration deleted successfully", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (Exception e) {
            String errorMsg = "Delete failed: " + e.getMessage();
            showError(errorMsg);
            Notification.show(errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
    }
}