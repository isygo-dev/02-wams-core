package eu.isygoit.ui.sms.views.bucket.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.sms.views.bucket.BucketManagementView;
import feign.FeignException;

public class DeleteBucketDialog extends PinBaseActionDialog {

    private final BucketManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final String bucketName;

    public DeleteBucketDialog(BucketManagementView parentView, ObjectStorageService objectStorageService,
                              String tenant, String bucketName, Runnable onSuccess) {
        super(I18n.t("sms.buckets.dialog.delete.bucket.title"),
                I18n.t("sms.buckets.dialog.delete.bucket.message", bucketName), onSuccess);
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;
        this.bucketName = bucketName;

        setOkButtonText(I18n.t("sms.buckets.dialog.delete.bucket.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("sms.buckets.dialog.delete.invalid.code"));
            return false;
        }
        parentView.showLoading(true);
        try {
            objectStorageService.deleteBucket(tenant, bucketName);
            append(I18n.t("sms.buckets.dialog.delete.bucket.success", bucketName));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("sms.buckets.dialog.delete.bucket.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try { if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) return ex.contentUTF8(); } catch (Exception ignored) {}
        return ex.getMessage();
    }
}