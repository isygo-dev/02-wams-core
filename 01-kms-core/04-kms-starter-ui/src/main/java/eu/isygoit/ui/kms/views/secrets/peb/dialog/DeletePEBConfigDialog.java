package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.remote.kms.PEBConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import feign.FeignException;

public class DeletePEBConfigDialog extends PinBaseActionDialog {

    private final PEBConfigService configService;
    private final Long configId;

    public DeletePEBConfigDialog(PEBConfigService configService, Long configId, String code, Runnable onSuccess) {
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
        } catch (FeignException ex) {
            handleFeignException(ex);
            return false;
        } catch (Exception e) {
            this.append("Delete failed: " + e.getMessage());
            return false;
        }
    }

    private void handleFeignException(FeignException ex) {
        String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
        this.append(errorMsg);
    }
}