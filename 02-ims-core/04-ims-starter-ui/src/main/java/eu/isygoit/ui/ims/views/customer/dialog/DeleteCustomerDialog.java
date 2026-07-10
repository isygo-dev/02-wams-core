package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;

public class DeleteCustomerDialog extends PinBaseActionDialog {

    private final CustomerManagementView parentView;
    private final CustomerService customerService;
    private final Long customerId;

    public DeleteCustomerDialog(CustomerManagementView parentView,
                                CustomerService customerService,
                                Long customerId,
                                Runnable onSuccess) {
        super(I18n.t("ims.customer.dialog.delete.title"),
                I18n.t("ims.customer.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.customerService = customerService;
        this.customerId = customerId;

        setOkButtonText(I18n.t("ims.customer.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("ims.customer.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            customerService.delete(customerId);
            append(I18n.t("ims.customer.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.customer.dialog.delete.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}