package eu.isygoit.ui.sms.views.object.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.sms.views.common.SmsActionDialog;
import eu.isygoit.ui.sms.views.common.SmsDialogSupport;
import eu.isygoit.ui.sms.views.common.SmsUploadPanel;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Uploads one object to the selected bucket: the file (see {@link SmsUploadPanel}),
 * its target path, file name and tags, as expected by {@code ObjectStorageService.upload}.
 */
@Slf4j
public class UploadFileDialog extends SmsActionDialog {

    private final ObjectStorageManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final String bucketName;

    private SmsUploadPanel uploadPanel;
    private TextField pathField;
    private TextArea tagsField;
    private TextField fileNameField;

    public UploadFileDialog(ObjectStorageManagementView parentView, ObjectStorageService objectStorageService,
                            String tenant, String bucketName, Runnable onSuccess) {
        super(I18n.t("sms.objects.dialog.upload.file.title"), onSuccess);
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;
        this.bucketName = bucketName;

        setOkButtonText(I18n.t("sms.objects.dialog.upload.file.button"));
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        buildForm();
        enableOkButton(false);
    }

    private void buildForm() {
        pathField = new TextField(I18n.t("sms.objects.dialog.field.path"));
        pathField.setPlaceholder(I18n.t("sms.objects.dialog.field.path.placeholder"));
        pathField.setHelperText(I18n.t("sms.objects.dialog.field.path.helper"));
        pathField.setWidthFull();

        fileNameField = new TextField(I18n.t("sms.objects.dialog.field.file.name"));
        fileNameField.setPlaceholder(I18n.t("sms.objects.dialog.field.file.name.placeholder"));
        fileNameField.setHelperText(I18n.t("sms.objects.dialog.field.file.name.helper"));
        fileNameField.setWidthFull();

        tagsField = DialogLayout.tall(new TextArea(I18n.t("sms.objects.dialog.field.tags")));
        tagsField.setPlaceholder(I18n.t("sms.objects.dialog.field.tags.placeholder"));

        uploadPanel = new SmsUploadPanel(ready -> {
            if (ready && (fileNameField.getValue() == null || fileNameField.getValue().isBlank())) {
                fileNameField.setValue(uploadPanel.getUploadedFileName());
            }
            enableOkButton(ready);
        });

        FormLayout form = DialogLayout.responsiveForm();
        form.add(pathField, fileNameField, tagsField);
        form.setColspan(tagsField, 2);

        VerticalLayout stack = DialogLayout.stack();
        stack.add(uploadPanel, form);
        addContent(stack);
    }

    @Override
    protected boolean onOk() {
        String uploadedFileName = uploadPanel.getUploadedFileName();
        if (uploadPanel.getUploadedFile() == null) {
            append(I18n.t("sms.objects.dialog.upload.no.file"));
            return false;
        }
        parentView.showLoading(true);
        try {
            String path = pathField.getValue();
            if (path == null || path.isBlank()) path = "";
            else path = path.trim().replace("/", "#");

            String fileName = fileNameField.getValue();
            if (fileName == null || fileName.isBlank()) fileName = uploadedFileName;
            else {
                fileName = fileName.trim();
                if (!fileName.contains(".") && uploadedFileName.contains(".")) {
                    fileName += uploadedFileName.substring(uploadedFileName.lastIndexOf("."));
                }
            }

            List<String> tags = new ArrayList<>();
            if (tagsField.getValue() != null && !tagsField.getValue().isBlank()) {
                for (String t : tagsField.getValue().split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) tags.add(trimmed);
                }
            }

            ResponseEntity<Object> response = objectStorageService.upload(
                    tenant, bucketName, path, fileName, tags, uploadPanel.getUploadedFile());
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("sms.objects.dialog.upload.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("sms.objects.dialog.upload.success"));
            return true;
        } catch (FeignException ex) {
            append(SmsDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("sms.objects.dialog.upload.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
