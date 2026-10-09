package eu.isygoit.ui.ims.views.customer.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;

public class DeleteCustomerDialog extends DeleteActionDialog {

    public DeleteCustomerDialog(CustomerManagementView parentView,
                                CustomerService customerService,
                                Long customerId,
                                Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.customer.dialog.delete.title"),
                        I18n.t("ims.customer.dialog.delete.message"),
                        I18n.t("ims.customer.dialog.delete.button"),
                        I18n.t("ims.customer.dialog.delete.invalid.code"),
                        I18n.t("ims.customer.dialog.delete.success"),
                        detail -> I18n.t("ims.customer.dialog.delete.error", detail)),
                () -> customerService.delete(customerId),
                onSuccess,
                parentView::showLoading,
                "ims-dialog");
    }
}
