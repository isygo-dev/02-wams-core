package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;

public class DeleteTokenConfigDialog extends DeleteActionDialog {

    public DeleteTokenConfigDialog(KmsTokenConfigService tokenConfigService,
                                   Long configId,
                                   String code,
                                   Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.dialog.token.delete.title"),
                        I18n.t("kms.dialog.token.delete.confirmation", code),
                        I18n.t("common.button.delete"),
                        I18n.t("kms.dialog.token.action.required"),
                        I18n.t("kms.dialog.token.deleted"),
                        detail -> I18n.t("kms.dialog.token.delete.failed") + ": " + detail),
                () -> tokenConfigService.delete(configId),
                onSuccess,
                null,
                "kms-dialog");
    }
}
