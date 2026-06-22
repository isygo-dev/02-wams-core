package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;

public class DeleteCustomerDialog extends PinBaseActionDialog {

    private final CustomerManagementView parentView;
    private final CustomerService customerService;
    private final Long customerId;

    public DeleteCustomerDialog(CustomerManagementView parentView,
                                CustomerService customerService,
                                Long customerId,
                                Runnable onSuccess) {
        super("Delete Customer",
                "This action is irreversible. The customer and all associated data will be permanently removed.",
                onSuccess);
        this.parentView = parentView;
        this.customerService = customerService;
        this.customerId = customerId;

        setOkButtonText("Delete permanently");
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append("Invalid confirmation code");
            return false;
        }

        parentView.showLoading(true);
        try {
            customerService.delete(customerId);
            append("Customer deleted successfully");
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
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