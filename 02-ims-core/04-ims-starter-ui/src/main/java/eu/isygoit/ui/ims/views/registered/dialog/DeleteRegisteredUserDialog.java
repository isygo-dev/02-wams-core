package eu.isygoit.ui.ims.views.registered.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;

public class DeleteRegisteredUserDialog extends DeleteActionDialog {

    public DeleteRegisteredUserDialog(RegisteredManagementView parentView,
                                      RegisteredUserService registeredUserService,
                                      Long registeredUserId,
                                      Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.registered.dialog.delete.title"),
                        I18n.t("ims.registered.dialog.delete.message"),
                        I18n.t("ims.registered.dialog.delete.button"),
                        I18n.t("ims.registered.dialog.delete.invalid.code"),
                        I18n.t("ims.registered.dialog.delete.success"),
                        detail -> I18n.t("ims.registered.dialog.delete.error", detail)),
                () -> registeredUserService.delete(registeredUserId),
                onSuccess,
                parentView::showLoading,
                "ims-dialog");
    }
}
