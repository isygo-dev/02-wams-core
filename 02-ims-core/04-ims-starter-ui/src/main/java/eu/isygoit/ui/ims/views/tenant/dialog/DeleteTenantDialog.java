package eu.isygoit.ui.ims.views.tenant.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;

public class DeleteTenantDialog extends DeleteActionDialog {

    public DeleteTenantDialog(TenantManagementView parentView,
                              TenantService tenantService,
                              Long tenantId,
                              Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.tenant.dialog.delete.title"),
                        I18n.t("ims.tenant.dialog.delete.message"),
                        I18n.t("ims.tenant.dialog.delete.button"),
                        I18n.t("ims.tenant.dialog.delete.invalid.code"),
                        I18n.t("ims.tenant.dialog.delete.success"),
                        detail -> I18n.t("ims.tenant.dialog.delete.error", detail)),
                () -> tenantService.delete(tenantId),
                onSuccess,
                parentView::showLoading,
                "ims-dialog");
    }
}
