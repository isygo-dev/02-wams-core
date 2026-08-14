package eu.isygoit.ui.sms.views.object.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;
import eu.isygoit.util.ByteArrayMultipartFile;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UploadFileDialog extends BaseActionDialog {

    private final ObjectStorageManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final String bucketName;
    private final Runnable onSuccess;

    private MemoryBuffer memoryBuffer;
    private Upload upload;
    private TextField pathField;
    private TextArea tagsField;
    private TextField fileNameField;
    private Span fileNameDisplay;
    private Span fileSizeDisplay;
    private Span uploadStatus;
    private Icon uploadIcon;
    private Span allowedFileTypes;

    private String uploadedFileName;
    private MultipartFile uploadedFile;

    public UploadFileDialog(ObjectStorageManagementView parentView, ObjectStorageService objectStorageService,
                            String tenant, String bucketName, Runnable onSuccess) {
        super(I18n.t("sms.objects.dialog.upload.file.title"), onSuccess);
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;
        this.bucketName = bucketName;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("sms.objects.dialog.upload.file.button"));
        setWidth("650px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
        enableOkButton(false);
    }

    private void buildForm() {
        memoryBuffer = new MemoryBuffer();
        upload = new Upload(memoryBuffer);
        upload.setDropAllowed(true);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(100 * 1024 * 1024);
        upload.setAcceptedFileTypes("application/pdf", "image/*", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain", "application/zip", "application/json", "text/csv");
        upload.addClassName("wams-upload-component");
        upload.getStyle().set("width", "100%");

        upload.addSucceededListener(event -> {
            uploadedFileName = event.getFileName();
            try {
                byte[] bytes = memoryBuffer.getInputStream().readAllBytes();
                uploadedFile = new ByteArrayMultipartFile(bytes, uploadedFileName, memoryBuffer.getFileData().getMimeType());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (fileNameField.getValue() == null || fileNameField.getValue().isBlank()) {
                fileNameField.setValue(uploadedFileName);
            }
            fileNameDisplay.setText(I18n.t("sms.objects.dialog.upload.file.name", uploadedFileName));
            fileSizeDisplay.setText(I18n.t("sms.objects.dialog.upload.file.size", formatFileSize(uploadedFile.getSize())));
            uploadStatus.setText(I18n.t("sms.objects.dialog.upload.success"));
            uploadStatus.getStyle().set("color", "var(--lumo-success-color)");
            uploadIcon.getStyle().set("color", "var(--lumo-success-color)");
            enableOkButton(true);
        });

        upload.addFailedListener(event -> {
            uploadStatus.setText(I18n.t("sms.objects.dialog.upload.failed", event.getReason().getMessage()));
            uploadStatus.getStyle().set("color", "var(--lumo-error-color)");
            uploadIcon.getStyle().set("color", "var(--lumo-error-color)");
            enableOkButton(false);
        });

        upload.addFileRejectedListener(event -> {
            uploadStatus.setText(I18n.t("sms.objects.dialog.upload.rejected", event.getErrorMessage()));
            uploadStatus.getStyle().set("color", "var(--lumo-error-color)");
            uploadIcon.getStyle().set("color", "var(--lumo-error-color)");
            enableOkButton(false);
        });

        pathField = new TextField(I18n.t("sms.objects.dialog.field.path"));
        pathField.setPlaceholder(I18n.t("sms.objects.dialog.field.path.placeholder"));
        pathField.setWidthFull();
        pathField.setHelperText(I18n.t("sms.objects.dialog.field.path.helper"));

        fileNameField = new TextField(I18n.t("sms.objects.dialog.field.file.name"));
        fileNameField.setPlaceholder(I18n.t("sms.objects.dialog.field.file.name.placeholder"));
        fileNameField.setWidthFull();
        fileNameField.setHelperText(I18n.t("sms.objects.dialog.field.file.name.helper"));

        tagsField = new TextArea(I18n.t("sms.objects.dialog.field.tags"));
        tagsField.setPlaceholder(I18n.t("sms.objects.dialog.field.tags.placeholder"));
        tagsField.setWidthFull();
        tagsField.setHeight("80px");

        allowedFileTypes = new Span(I18n.t("sms.objects.dialog.upload.allowed.types"));
        allowedFileTypes.addClassName(LumoUtility.FontSize.XXSMALL);
        allowedFileTypes.getStyle().set("color", "var(--lumo-secondary-text-color)");

        fileNameDisplay = new Span(); fileSizeDisplay = new Span(); uploadStatus = new Span();
        uploadIcon = VaadinIcon.UPLOAD.create();
        uploadIcon.setSize("20px");
        uploadIcon.getStyle().set("color", "var(--lumo-primary-color)");
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        VerticalLayout uploadLayout = new VerticalLayout();
        uploadLayout.setSpacing(true);
        uploadLayout.setPadding(false);
        Span instruction = new Span(I18n.t("sms.objects.dialog.upload.instruction"));
        instruction.addClassName(LumoUtility.FontSize.SMALL);
        instruction.getStyle().set("color", "var(--lumo-secondary-text-color)");
        uploadLayout.add(instruction);
        uploadLayout.add(upload);
        uploadLayout.add(allowedFileTypes);

        HorizontalLayout fileInfoLayout = new HorizontalLayout();
        fileInfoLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        fileInfoLayout.setSpacing(true);
        uploadIcon = VaadinIcon.UPLOAD.create();
        uploadIcon.setSize("20px");
        uploadIcon.getStyle().set("color", "var(--lumo-primary-color)");
        fileNameDisplay = new Span(I18n.t("sms.objects.dialog.upload.no.file.selected"));
        fileNameDisplay.getStyle().set("color", "var(--lumo-secondary-text-color)");
        fileSizeDisplay = new Span("");
        fileInfoLayout.add(uploadIcon, fileNameDisplay, fileSizeDisplay);
        uploadLayout.add(fileInfoLayout);
        uploadLayout.add(uploadStatus);

        form.add(uploadLayout, pathField, fileNameField, tagsField);
        return form;
    }

    @Override
    protected boolean onOk() {
        if (uploadedFile == null) {
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

            objectStorageService.upload(tenant, bucketName, path, fileName, tags, uploadedFile);
            append(I18n.t("sms.objects.dialog.upload.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("sms.objects.dialog.upload.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String extractErrorMessage(FeignException ex) {
        try { if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) return ex.contentUTF8(); } catch (Exception ignored) {}
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }
}