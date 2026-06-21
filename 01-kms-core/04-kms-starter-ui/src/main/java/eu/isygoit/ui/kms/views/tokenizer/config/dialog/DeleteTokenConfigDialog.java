package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

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
                true);
        this.tokenConfigService = tokenConfigService;
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
            tokenConfigService.delete(configId);
            append("Configuration deleted successfully");
            return true;
        } catch (Exception e) {
            append("Delete failed: " + e.getMessage());
            return false;
        }
    }
}