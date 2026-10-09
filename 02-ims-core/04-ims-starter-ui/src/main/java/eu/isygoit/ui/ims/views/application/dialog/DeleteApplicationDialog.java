package eu.isygoit.ui.ims.views.application.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;

public class DeleteApplicationDialog extends DeleteActionDialog {

    public DeleteApplicationDialog(ApplicationManagementView parentView,
                                   ApplicationService applicationService,
                                   Long applicationId,
                                   Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.app.dialog.delete.title"),
                        I18n.t("ims.app.dialog.delete.message"),
                        I18n.t("ims.app.dialog.delete.button"),
                        I18n.t("ims.app.dialog.delete.invalid.code"),
                        I18n.t("ims.app.dialog.delete.success"),
                        detail -> I18n.t("ims.app.dialog.delete.error", detail)),
                () -> applicationService.delete(applicationId),
                onSuccess,
                parentView::showLoading,
                "ims-dialog");
    }
}
