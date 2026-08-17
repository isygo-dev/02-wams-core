package eu.isygoit.ui.sms.views.object.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

/**
 * PIN-confirmed permanent deletion of a single stored object, matching the
 * same destructive-action pattern used across every other module
 * (e.g. DeleteLinkedFileDialog, DeleteRegisteredUserDialog) instead of a
 * plain confirm/cancel dialog.
 */
@Slf4j
public class DeleteFileDialog extends PinBaseActionDialog {

    private final ObjectStorageManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final String bucketName;
    private final ObjectStorageManagementView.FileItem file;

    public DeleteFileDialog(ObjectStorageManagementView parentView,
                            ObjectStorageService objectStorageService,
                            String tenant,
                            String bucketName,
                            ObjectStorageManagementView.FileItem file,
                            Runnable onSuccess) {
        super(I18n.t("sms.objects.dialog.delete.title"),
                I18n.t("sms.objects.dialog.delete.message", file.getFileName()),
                onSuccess);
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;
        this.bucketName = bucketName;
        this.file = file;

        setOkButtonText(I18n.t("sms.objects.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("sms.objects.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<Object> response = objectStorageService.delete(
                    tenant, bucketName, file.getPath(), file.getFileName());
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("sms.objects.dialog.delete.failed"));
                return false;
            }
            append(I18n.t("sms.objects.dialog.delete.success", file.getFileName()));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
            log.error("Delete failed for {}", file.getName(), ex);
        } catch (Exception e) {
            append(I18n.t("sms.objects.dialog.delete.error", e.getMessage()));
            log.error("Delete failed for {}", file.getName(), e);
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
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }
}
