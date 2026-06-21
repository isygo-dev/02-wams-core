package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
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
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception e) {
            append("Delete failed: " + e.getMessage());
            return false;
        }
    }
}