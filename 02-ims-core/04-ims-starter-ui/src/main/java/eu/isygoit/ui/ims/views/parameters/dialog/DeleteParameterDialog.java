package eu.isygoit.ui.ims.views.parameters.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;

public class DeleteParameterDialog extends DeleteActionDialog {

    public DeleteParameterDialog(ParameterManagementView parentView,
                                 AppParameterService parameterService,
                                 Long parameterId,
                                 Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.parameter.dialog.delete.title"),
                        I18n.t("ims.parameter.dialog.delete.message"),
                        I18n.t("ims.parameter.dialog.delete.button"),
                        I18n.t("ims.parameter.dialog.delete.invalid.code"),
                        I18n.t("ims.parameter.dialog.delete.success"),
                        detail -> I18n.t("ims.parameter.dialog.delete.error", detail)),
                () -> parameterService.delete(parameterId),
                onSuccess,
                parentView::showLoading,
                "ims-dialog");
    }
}
