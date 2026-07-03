package eu.isygoit.ui.kms.views.cryptography.incremental.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public class DeleteNextCodeConfigDialog extends PinBaseActionDialog {

    private final KmsAppNextCodeService nextCodeService;
    private final Long configId;

    public DeleteNextCodeConfigDialog(KmsAppNextCodeService nextCodeService,
                                      Long configId,
                                      String entity,
                                      String attribute,
                                      Runnable onSuccess) {
        super(I18n.t("kms.delete.nextcode.dialog.title"),
                I18n.t("kms.delete.nextcode.dialog.message", entity, attribute),
                onSuccess,
                true); // require PIN
        this.nextCodeService = nextCodeService;
        this.configId = configId;
        setOkButtonText(I18n.t("kms.delete.nextcode.dialog.button"));
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("kms.delete.nextcode.dialog.invalid.code"));
            return false;
        }

        try {
            nextCodeService.delete(configId);
            append(I18n.t("kms.delete.nextcode.dialog.success"));
            return true;
        } catch (Exception e) {
            append(I18n.t("kms.delete.nextcode.dialog.failed", e.getMessage()));
            return false;
        }
    }
}