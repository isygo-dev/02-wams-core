package eu.isygoit.ui.sms.views.object.dialog;

import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Creates a new bucket for the currently selected tenant. Without this,
 * a tenant with no buckets yet had no way to get one from the UI at all —
 * the bucket selector could only ever pick among buckets that already
 * existed.
 */
public class CreateBucketDialog extends BaseActionDialog {

    private final ObjectStorageManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;

    private TextField bucketNameField;

    public CreateBucketDialog(ObjectStorageManagementView parentView,
                              ObjectStorageService objectStorageService,
                              String tenant,
                              Runnable onSuccess) {
        super(I18n.t("sms.objects.dialog.create.bucket.title"), onSuccess);
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;

        setOkButtonText(I18n.t("sms.objects.dialog.create.bucket.button"));
        setWidth("450px");

        bucketNameField = new TextField(I18n.t("sms.objects.dialog.create.bucket.field.name"));
        bucketNameField.setPlaceholder(I18n.t("sms.objects.dialog.create.bucket.field.name.placeholder"));
        bucketNameField.setHelperText(I18n.t("sms.objects.dialog.create.bucket.field.name.helper"));
        bucketNameField.setRequiredIndicatorVisible(true);
        bucketNameField.setWidthFull();

        addContent(bucketNameField);
    }

    @Override
    protected boolean onOk() {
        String bucketName = bucketNameField.getValue();
        if (bucketName == null || bucketName.isBlank()) {
            append(I18n.t("sms.objects.dialog.create.bucket.field.name.required"));
            return false;
        }
        if (!bucketName.matches("^[a-z0-9.-]{3,63}$")) {
            append(I18n.t("sms.objects.dialog.create.bucket.field.name.invalid"));
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<Object> response = objectStorageService.saveBucket(tenant, bucketName.trim());
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("sms.objects.dialog.create.bucket.failed", response.getStatusCodeValue()));
                return false;
            }
            append(I18n.t("sms.objects.dialog.create.bucket.success", bucketName));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("sms.objects.dialog.create.bucket.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }
}
