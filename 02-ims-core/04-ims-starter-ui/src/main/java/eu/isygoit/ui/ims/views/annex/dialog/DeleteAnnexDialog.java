package eu.isygoit.ui.ims.views.annex.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;

public class DeleteAnnexDialog extends DeleteActionDialog {

    public DeleteAnnexDialog(AnnexManagementView parentView,
                             AnnexService annexService,
                             Long annexId,
                             Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.annex.dialog.delete.title"),
                        I18n.t("ims.annex.dialog.delete.message"),
                        I18n.t("ims.annex.dialog.delete.button"),
                        I18n.t("ims.annex.dialog.delete.invalid.code"),
                        I18n.t("ims.annex.dialog.delete.success"),
                        detail -> I18n.t("ims.annex.dialog.delete.error", detail)),
                () -> annexService.delete(annexId),
                onSuccess,
                parentView::showLoading,
                "ims-dialog");
    }
}
