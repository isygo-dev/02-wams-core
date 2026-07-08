package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.mms.views.msgtemplate.MsgTemplateManagementView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
public abstract class BaseMsgTemplateDialog extends Dialog {

    protected final MsgTemplateManagementView parentView;
    protected final MsgTemplateService templateService;
    protected final MsgTemplateFileService templateFileService;
    protected final Runnable onSuccess;

    protected final Div contentArea = new Div();
    protected final Div errorArea = new Div();
    protected Button okButton;
    protected Button cancelButton;

    // File upload components
    protected MemoryBuffer memoryBuffer;
    protected Upload fileUpload;
    protected Div fileInfoArea;
    protected String uploadedFileName;
    protected byte[] uploadedFileData;

    public BaseMsgTemplateDialog(String title,
                                 MsgTemplateManagementView parentView,
                                 MsgTemplateService templateService,
                                 MsgTemplateFileService templateFileService,
                                 Runnable onSuccess) {
        this.parentView = parentView;
        this.templateService = templateService;
        this.templateFileService = templateFileService;
        this.onSuccess = onSuccess;
        this.uploadedFileName = null;
        this.uploadedFileData = null;

        setHeaderTitle(title);
        setWidth("700px");
        setMaxWidth("95vw");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        buildLayout();
        addDialogCloseActionListener(e -> close());
    }

    private void buildLayout() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(true);
        mainLayout.setSpacing(true);
        mainLayout.setWidthFull();

        errorArea.setVisible(false);
        errorArea.addClassName("wams-dialog-error-area");
        mainLayout.add(errorArea);

        contentArea.addClassName("wams-dialog-content-area");
        mainLayout.add(contentArea);

        HorizontalLayout footer = buildFooter();
        mainLayout.add(footer);

        add(mainLayout);
    }

    private HorizontalLayout buildFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setSpacing(true);

        cancelButton = new Button(I18n.t("common.dialog.cancel"), e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        okButton = new Button(getOkButtonText(), e -> {
            clearErrors();
            if (onOk()) {
                close();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        footer.add(cancelButton, okButton);
        return footer;
    }

    protected String getOkButtonText() {
        return I18n.t("common.dialog.ok");
    }

    protected void setOkButtonText(String text) {
        okButton.setText(text);
    }

    protected void addThemeVariantsOkButton(ButtonVariant... variants) {
        okButton.addThemeVariants(variants);
    }

    protected void append(String errorMessage) {
        errorArea.removeAll();
        errorArea.add(new Span(errorMessage));
        errorArea.setVisible(true);
    }

    protected void clearErrors() {
        errorArea.removeAll();
        errorArea.setVisible(false);
    }

    protected void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    protected void showError(String message) {
        Notification.show(message, 5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    protected void setupFileUpload(String currentFileName) {
        memoryBuffer = new MemoryBuffer();
        fileUpload = new Upload(memoryBuffer);
        fileUpload.setMaxFileSize(10 * 1024 * 1024); // 10MB
        fileUpload.setAcceptedFileTypes(".html", ".htm", ".txt", ".ftl", ".vm", ".xml", ".json", ".properties");
        fileUpload.setDropAllowed(true);
        fileUpload.setUploadButton(new Button(I18n.t("mms.msgtemplate.dialog.upload.button")));
        fileUpload.setDropLabel(new Span(I18n.t("mms.msgtemplate.dialog.upload.drop")));

        fileInfoArea = new Div();
        fileInfoArea.addClassName("wams-dialog-file-info-area");

        if (currentFileName != null && !currentFileName.isEmpty()) {
            fileInfoArea.setText(I18n.t("mms.msgtemplate.dialog.current.file", currentFileName));
        } else {
            fileInfoArea.setText(I18n.t("mms.msgtemplate.dialog.no.file"));
        }

        fileUpload.addSucceededListener(event -> {
            uploadedFileName = event.getFileName();
            fileInfoArea.setText(I18n.t("mms.msgtemplate.dialog.file.uploaded", uploadedFileName));
            fileInfoArea.removeClassName("wams-dialog-file-info-area--error");
            fileInfoArea.addClassName("wams-dialog-file-info-area--success");
        });

        fileUpload.addFailedListener(event -> {
            String errorMsg = event.getReason() != null ? event.getReason().getMessage() : I18n.t("mms.common.error.unknown");
            fileInfoArea.setText(I18n.t("mms.msgtemplate.dialog.upload.failed", errorMsg));
            fileInfoArea.removeClassName("wams-dialog-file-info-area--success");
            fileInfoArea.addClassName("wams-dialog-file-info-area--error");
        });

        fileUpload.addFileRejectedListener(event -> {
            fileInfoArea.setText(I18n.t("mms.msgtemplate.dialog.upload.rejected", event.getErrorMessage()));
            fileInfoArea.removeClassName("wams-dialog-file-info-area--success");
            fileInfoArea.addClassName("wams-dialog-file-info-area--error");
        });
    }

    protected MultipartFile getUploadedFile() {
        if (uploadedFileName == null || uploadedFileName.isEmpty()) {
            return null;
        }

        try {
            InputStream inputStream = memoryBuffer.getInputStream();
            if (inputStream != null) {
                // Read the file data
                byte[] data = inputStream.readAllBytes();
                uploadedFileData = data;

                final String fileName = uploadedFileName;
                final byte[] fileData = data;

                return new MultipartFile() {
                    @Override
                    public String getName() {
                        return fileName;
                    }

                    @Override
                    public String getOriginalFilename() {
                        return fileName;
                    }

                    @Override
                    public String getContentType() {
                        // Try to determine content type from file extension
                        String contentType = "application/octet-stream";
                        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                            contentType = "text/html";
                        } else if (fileName.endsWith(".txt")) {
                            contentType = "text/plain";
                        } else if (fileName.endsWith(".xml")) {
                            contentType = "application/xml";
                        } else if (fileName.endsWith(".json")) {
                            contentType = "application/json";
                        } else if (fileName.endsWith(".ftl")) {
                            contentType = "text/plain";
                        } else if (fileName.endsWith(".vm")) {
                            contentType = "text/plain";
                        } else if (fileName.endsWith(".properties")) {
                            contentType = "text/plain";
                        }
                        return contentType;
                    }

                    @Override
                    public boolean isEmpty() {
                        return fileData == null || fileData.length == 0;
                    }

                    @Override
                    public long getSize() {
                        return fileData != null ? fileData.length : 0;
                    }

                    @Override
                    public byte[] getBytes() {
                        return fileData;
                    }

                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(fileData);
                    }

                    @Override
                    public void transferTo(java.io.File dest) throws IllegalStateException {
                        // Not implemented - use getBytes() or getInputStream() instead
                        throw new UnsupportedOperationException("transferTo not supported");
                    }
                };
            }
        } catch (Exception e) {
            log.error("Failed to get uploaded file", e);
        }
        return null;
    }

    protected boolean hasFileUploaded() {
        return uploadedFileName != null && !uploadedFileName.isEmpty();
    }

    protected abstract boolean onOk();

    protected boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return true; // Empty is allowed (optional field)
        }
        // Simple email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}