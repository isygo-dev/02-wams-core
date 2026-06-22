package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import eu.isygoit.remote.kms.DigestConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;

public class DeleteDigestConfigDialog extends PinBaseActionDialog {

    private final DigestConfigService configService;
    private final Long configId;

    public DeleteDigestConfigDialog(DigestConfigService configService, Long configId, String code, Runnable onSuccess) {
        super("Delete Configuration",
                "Delete configuration '" + code + "'? This action is irreversible.",
                onSuccess,
                true);
        this.configService = configService;
        this.configId = configId;
        setOkButtonText("Delete permanently");
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
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
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception e) {
            append("Delete failed: " + e.getMessage());
            return false;
        }
    }
}