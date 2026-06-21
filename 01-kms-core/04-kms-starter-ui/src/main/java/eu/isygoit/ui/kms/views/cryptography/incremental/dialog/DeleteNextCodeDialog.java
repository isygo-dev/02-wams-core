package eu.isygoit.ui.kms.views.cryptography.incremental.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

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
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        try {
            nextCodeService.delete(configId);
            append("Configuration deleted successfully");
            return true;
        } catch (Exception e) {
            append("Delete failed: " + e.getMessage());
            return false;
        }
    }
}