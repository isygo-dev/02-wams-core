package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;

public class ToggleCustomerStatusDialog extends PinConfirmActionDialog {

    public ToggleCustomerStatusDialog(CustomerManagementView parentView,
                                      CustomerService customerService,
                                      Long customerId,
                                      IEnumEnabledBinaryStatus.Types currentStatus,
                                      Runnable onSuccess) {
        super(new Texts(
                        currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("ims.customer.dialog.toggle.title.disable") : I18n.t("ims.customer.dialog.toggle.title.enable"),
                        currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("ims.customer.dialog.toggle.message.disable") : I18n.t("ims.customer.dialog.toggle.message.enable"),
                        currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("ims.customer.dialog.toggle.button.disable") : I18n.t("ims.customer.dialog.toggle.button.enable"),
                        I18n.t("common.dialog.pin.invalid"),
                        I18n.t("ims.customer.dialog.toggle.success." + (currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "disable" : "enable")),
                        detail -> detail != null && detail.startsWith("HTTP ")
                                ? I18n.t("ims.customer.dialog.toggle.failed", detail.substring(5))
                                : I18n.t("ims.customer.dialog.toggle.error", detail)),
                () -> customerService.updateCustomerStatus(customerId,
                        currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED
                                ? IEnumEnabledBinaryStatus.Types.DISABLED
                                : IEnumEnabledBinaryStatus.Types.ENABLED),
                onSuccess,
                parentView::showLoading,
                "ims-dialog",
                false, // simple confirmation, no PIN
                ButtonVariant.LUMO_PRIMARY);
    }
}
