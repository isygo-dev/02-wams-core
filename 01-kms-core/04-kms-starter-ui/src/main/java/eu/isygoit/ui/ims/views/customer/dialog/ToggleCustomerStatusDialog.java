package eu.isygoit.ui.ims.views.customer.dialog;

import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import org.springframework.http.ResponseEntity;

public class ToggleCustomerStatusDialog extends PinBaseActionDialog {

    private final CustomerManagementView parentView;
    private final CustomerService customerService;
    private final Long customerId;
    private final IEnumEnabledBinaryStatus.Types currentStatus;

    public ToggleCustomerStatusDialog(CustomerManagementView parentView,
                                      CustomerService customerService,
                                      Long customerId,
                                      IEnumEnabledBinaryStatus.Types currentStatus,
                                      Runnable onSuccess) {
        super(
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "Disable Customer" : "Enable Customer",
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED
                        ? "This will deactivate the customer. Are you sure?"
                        : "This will reactivate the customer. Are you sure?",
                onSuccess,
                false // simple confirmation, no PIN
        );
        this.parentView = parentView;
        this.customerService = customerService;
        this.customerId = customerId;
        this.currentStatus = currentStatus;

        setOkButtonText(currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "Disable" : "Enable");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            IEnumEnabledBinaryStatus.Types newStatus = currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED
                    ? IEnumEnabledBinaryStatus.Types.DISABLED
                    : IEnumEnabledBinaryStatus.Types.ENABLED;

            ResponseEntity<CustomerDto> response = customerService.updateCustomerStatus(customerId, newStatus);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Failed to update customer status: HTTP " + response.getStatusCodeValue());
                return false;
            }

            append("Customer " + (newStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "enabled" : "disabled") + " successfully");
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Operation failed: " + e.getMessage());
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