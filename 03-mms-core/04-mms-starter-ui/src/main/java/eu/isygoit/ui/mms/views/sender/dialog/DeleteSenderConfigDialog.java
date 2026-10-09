package eu.isygoit.ui.mms.views.sender.dialog;

import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.mms.views.sender.SenderConfigManagementView;

public class DeleteSenderConfigDialog extends DeleteActionDialog {

    public DeleteSenderConfigDialog(SenderConfigManagementView parentView,
                                    SenderConfigService senderConfigService,
                                    SenderConfigDto config,
                                    Runnable onSuccess) {
        super(new Texts(
                        I18n.t("mms.sender.dialog.delete.title"),
                        buildMessage(config),
                        I18n.t("mms.sender.dialog.delete.button"),
                        I18n.t("mms.sender.dialog.delete.invalid.code"),
                        I18n.t("mms.sender.dialog.delete.success"),
                        detail -> I18n.t("mms.sender.dialog.delete.error", detail)),
                () -> senderConfigService.delete(config.getId()),
                onSuccess,
                parentView != null ? parentView::showLoading : null,
                "mms-dialog");
    }

    private static String buildMessage(SenderConfigDto config) {
        String displayName = config.getName() != null ? config.getName() :
                (config.getCode() != null ? config.getCode() : "ID: " + config.getId());
        return I18n.t("mms.sender.dialog.delete.message", displayName);
    }
}
