package eu.isygoit.ui.ims.views.customer.dialog;

import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
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
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("customer.dialog.toggle.title.disable") : I18n.t("customer.dialog.toggle.title.enable"),
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED
                        ? I18n.t("customer.dialog.toggle.message.disable")
                        : I18n.t("customer.dialog.toggle.message.enable"),
                onSuccess,
                false // simple confirmation, no PIN
        );
        this.parentView = parentView;
        this.customerService = customerService;
        this.customerId = customerId;
        this.currentStatus = currentStatus;

        setOkButtonText(currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("customer.dialog.toggle.button.disable") : I18n.t("customer.dialog.toggle.button.enable"));
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
                append(I18n.t("customer.dialog.toggle.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("customer.dialog.toggle.success." + (newStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "enable" : "disable")));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("customer.dialog.toggle.error", e.getMessage()));
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