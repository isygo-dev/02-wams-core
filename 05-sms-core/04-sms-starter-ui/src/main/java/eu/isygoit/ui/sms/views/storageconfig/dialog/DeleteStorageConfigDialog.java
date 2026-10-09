package eu.isygoit.ui.sms.views.storageconfig.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.sms.views.storageconfig.StorageConfigManagementView;

public class DeleteStorageConfigDialog extends DeleteActionDialog {

    public DeleteStorageConfigDialog(StorageConfigManagementView parentView,
                                     StorageConfigService storageConfigService,
                                     Long configId,
                                     Runnable onSuccess) {
        super(new Texts(
                        I18n.t("sms.storageconfig.dialog.delete.title"),
                        I18n.t("sms.storageconfig.dialog.delete.message"),
                        I18n.t("sms.storageconfig.dialog.delete.button"),
                        I18n.t("sms.storageconfig.dialog.delete.invalid.code"),
                        I18n.t("sms.storageconfig.dialog.delete.success"),
                        detail -> I18n.t("sms.storageconfig.dialog.delete.error", detail)),
                () -> storageConfigService.delete(configId),
                onSuccess,
                parentView::showLoading,
                "sms-dialog");
    }
}
