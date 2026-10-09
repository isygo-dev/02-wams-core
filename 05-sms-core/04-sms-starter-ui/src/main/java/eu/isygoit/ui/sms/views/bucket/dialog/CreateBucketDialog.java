package eu.isygoit.ui.sms.views.bucket.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.sms.views.bucket.BucketManagementView;
import eu.isygoit.ui.sms.views.common.AbstractCreateBucketDialog;

/**
 * Create-bucket dialog of the bucket screen. The form and validation live in
 * {@link AbstractCreateBucketDialog}; this class only supplies the message keys.
 */
public class CreateBucketDialog extends AbstractCreateBucketDialog {

    private static final Messages MESSAGES = new Messages(
            "sms.buckets.dialog.field.bucket.name",
            "sms.buckets.dialog.field.bucket.name.placeholder",
            "sms.buckets.dialog.field.bucket.name.helper",
            "sms.buckets.dialog.field.bucket.name.rules",
            "sms.buckets.dialog.field.bucket.name.required",
            "sms.buckets.dialog.field.bucket.name.length.error",
            "sms.buckets.dialog.field.bucket.name.format.error",
            "sms.buckets.dialog.create.bucket.success",
            "sms.buckets.dialog.create.bucket.failed",
            "sms.buckets.dialog.create.bucket.error");

    public CreateBucketDialog(BucketManagementView parentView, ObjectStorageService objectStorageService,
                              String tenant, Runnable onSuccess) {
        super(I18n.t("sms.buckets.dialog.create.bucket.title"),
                I18n.t("sms.buckets.dialog.create.bucket.button"),
                onSuccess, tenant, objectStorageService, parentView::showLoading, MESSAGES);
    }
}
