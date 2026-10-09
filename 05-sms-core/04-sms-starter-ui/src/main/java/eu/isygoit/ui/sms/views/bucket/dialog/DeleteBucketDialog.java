package eu.isygoit.ui.sms.views.bucket.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.sms.views.bucket.BucketManagementView;

public class DeleteBucketDialog extends DeleteActionDialog {

    public DeleteBucketDialog(BucketManagementView parentView, ObjectStorageService objectStorageService,
                              String tenant, String bucketName, Runnable onSuccess) {
        super(new Texts(
                        I18n.t("sms.buckets.dialog.delete.bucket.title"),
                        I18n.t("sms.buckets.dialog.delete.bucket.message", bucketName),
                        I18n.t("sms.buckets.dialog.delete.bucket.button"),
                        I18n.t("sms.buckets.dialog.delete.invalid.code"),
                        I18n.t("sms.buckets.dialog.delete.bucket.success", bucketName),
                        detail -> I18n.t("sms.buckets.dialog.delete.bucket.error", detail)),
                () -> objectStorageService.deleteBucket(tenant, bucketName),
                onSuccess,
                parentView::showLoading,
                "sms-dialog");
    }
}
