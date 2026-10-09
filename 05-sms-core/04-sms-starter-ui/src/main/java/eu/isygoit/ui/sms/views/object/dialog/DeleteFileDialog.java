package eu.isygoit.ui.sms.views.object.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;

/**
 * PIN-confirmed permanent deletion of a single stored object.
 */
public class DeleteFileDialog extends DeleteActionDialog {

    public DeleteFileDialog(ObjectStorageManagementView parentView,
                            ObjectStorageService objectStorageService,
                            String tenant,
                            String bucketName,
                            ObjectStorageManagementView.FileItem file,
                            Runnable onSuccess) {
        super(new Texts(
                        I18n.t("sms.objects.dialog.delete.title"),
                        I18n.t("sms.objects.dialog.delete.message", file.getFileName()),
                        I18n.t("sms.objects.dialog.delete.button"),
                        I18n.t("sms.objects.dialog.delete.invalid.code"),
                        I18n.t("sms.objects.dialog.delete.success", file.getFileName()),
                        detail -> I18n.t("sms.objects.dialog.delete.error", detail)),
                () -> objectStorageService.delete(tenant, bucketName, file.getPath(), file.getFileName()),
                onSuccess,
                parentView::showLoading,
                "sms-dialog");
    }
}
