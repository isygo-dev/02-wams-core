package eu.isygoit.ui.ims.views.application.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;

public class ToggleApplicationStatusDialog extends PinConfirmActionDialog {

    public ToggleApplicationStatusDialog(ApplicationManagementView parentView,
                                         ApplicationService applicationService,
                                         Long applicationId,
                                         IEnumEnabledBinaryStatus.Types currentStatus,
                                         Runnable onSuccess) {
        super(new Texts(
                        currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("ims.app.dialog.toggle.title.disable") : I18n.t("ims.app.dialog.toggle.title.enable"),
                        currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("ims.app.dialog.toggle.message.disable") : I18n.t("ims.app.dialog.toggle.message.enable"),
                        currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("ims.app.dialog.toggle.button.disable") : I18n.t("ims.app.dialog.toggle.button.enable"),
                        I18n.t("common.dialog.pin.invalid"),
                        I18n.t("ims.app.dialog.toggle.success." + (currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "disable" : "enable")),
                        detail -> detail != null && detail.startsWith("HTTP ")
                                ? I18n.t("ims.app.dialog.toggle.failed", detail.substring(5))
                                : I18n.t("ims.app.dialog.toggle.error", detail)),
                () -> applicationService.updateStatus(applicationId,
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
