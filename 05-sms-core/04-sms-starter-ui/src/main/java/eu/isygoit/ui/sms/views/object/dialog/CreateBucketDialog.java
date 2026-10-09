package eu.isygoit.ui.sms.views.object.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.sms.views.common.AbstractCreateBucketDialog;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;

/**
 * Creates a new bucket for the currently selected tenant, so that a tenant with
 * no bucket yet can get one from the object screen. Same form and validation as
 * the bucket screen's dialog: both share {@link AbstractCreateBucketDialog}.
 */
public class CreateBucketDialog extends AbstractCreateBucketDialog {

    private static final Messages MESSAGES = new Messages(
            "sms.objects.dialog.create.bucket.field.name",
            "sms.objects.dialog.create.bucket.field.name.placeholder",
            "sms.objects.dialog.create.bucket.field.name.helper",
            null,
            "sms.objects.dialog.create.bucket.field.name.required",
            "sms.buckets.dialog.field.bucket.name.length.error",
            "sms.objects.dialog.create.bucket.field.name.invalid",
            "sms.objects.dialog.create.bucket.success",
            "sms.objects.dialog.create.bucket.failed",
            "sms.objects.dialog.create.bucket.error");

    public CreateBucketDialog(ObjectStorageManagementView parentView,
                              ObjectStorageService objectStorageService,
                              String tenant,
                              Runnable onSuccess) {
        super(I18n.t("sms.objects.dialog.create.bucket.title"),
                I18n.t("sms.objects.dialog.create.bucket.button"),
                onSuccess, tenant, objectStorageService, parentView::showLoading, MESSAGES);
    }
}
