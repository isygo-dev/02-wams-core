package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.mms.views.msgtemplate.MsgTemplateManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class EditTemplateContentDialog extends Dialog {

    private final MsgTemplateManagementView parentView;
    private final MsgTemplateService templateService;
    private final MsgTemplateFileService templateFileService;
    private final MsgTemplateDto template;
    private final Runnable onSuccess;

    private TextArea contentArea;
    private Div statusArea;
    private Button saveButton;
    private Button cancelButton;
    private Button downloadButton;
    private String originalContent;
    private boolean contentLoaded = false;

    public EditTemplateContentDialog(MsgTemplateManagementView parentView,
                                     MsgTemplateService templateService,
                                     MsgTemplateFileService templateFileService,
                                     MsgTemplateDto template,
                                     Runnable onSuccess) {
        this.parentView = parentView;
        this.templateService = templateService;
        this.templateFileService = templateFileService;
        this.template = template;
        this.onSuccess = onSuccess;

        setHeaderTitle(I18n.t("template.dialog.edit.content.title",
                template.getName() != null ? template.getName() : template.getId()));
        setWidth("800px");
        setMaxWidth("95vw");
        setHeight("600px");
        setMaxHeight("90vh");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        buildContent();
        loadTemplateContent();
    }

    private void buildContent() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(true);
        mainLayout.setSpacing(true);
        mainLayout.setWidthFull();
        mainLayout.setHeightFull();

        // File info bar
        HorizontalLayout infoBar = new HorizontalLayout();
        infoBar.setWidthFull();
        infoBar.setAlignItems(FlexComponent.Alignment.CENTER);
        infoBar.setSpacing(true);
        infoBar.getStyle()
                .set("padding", "var(--lumo-space-s)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        Icon fileIcon = VaadinIcon.FILE.create();
        fileIcon.setColor("var(--lumo-primary-color)");

        Span fileName = new Span(template.getOriginalFileName() != null ?
                template.getOriginalFileName() : template.getFileName());
        fileName.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span fileInfo = new Span(I18n.t("template.dialog.edit.content.file.info",
                template.getPath() != null ? template.getPath() : "N/A"));
        fileInfo.addClassName(LumoUtility.FontSize.XSMALL);
        fileInfo.addClassName(LumoUtility.TextColor.SECONDARY);

        infoBar.add(fileIcon, fileName, fileInfo);
        infoBar.expand(fileInfo);
        mainLayout.add(infoBar);

        // Status area
        statusArea = new Div();
        statusArea.setVisible(false);
        statusArea.getStyle()
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-s)");
        statusArea.setWidthFull();
        mainLayout.add(statusArea);

        // Content editor
        contentArea = new TextArea();
        contentArea.setWidthFull();
        contentArea.setHeight("350px");
        contentArea.setPlaceholder(I18n.t("template.dialog.edit.content.placeholder"));
        contentArea.getStyle()
                .set("font-family", "monospace")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("tab-size", "2");
        contentArea.addClassName(LumoUtility.Border.ALL);
        contentArea.addClassName(LumoUtility.BorderRadius.MEDIUM);
        mainLayout.add(contentArea);

        // Actions
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "var(--lumo-space-m)");

        downloadButton = new Button(I18n.t("template.dialog.edit.content.download"), new Icon(VaadinIcon.DOWNLOAD));
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        downloadButton.addClickListener(e -> downloadTemplate());

        saveButton = new Button(I18n.t("template.dialog.edit.content.save"), new Icon(VaadinIcon.CHECK));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setEnabled(false);
        saveButton.addClickListener(e -> saveContent());

        cancelButton = new Button(I18n.t("dialog.cancel"), e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        actions.add(downloadButton, saveButton, cancelButton);
        mainLayout.add(actions);

        add(mainLayout);
    }

    private void loadTemplateContent() {
        if (template.getFileName() == null || template.getFileName().isEmpty()) {
            showStatus(I18n.t("template.dialog.edit.content.no.file"), "warning");
            contentArea.setValue(I18n.t("template.dialog.edit.content.no.file.message"));
            contentArea.setReadOnly(true);
            saveButton.setEnabled(false);
            return;
        }

        showStatus(I18n.t("template.dialog.edit.content.loading"), "info");
        contentArea.setReadOnly(true);
        saveButton.setEnabled(false);

        try {
            ResponseEntity<Resource> response = templateFileService.downloadFile(template.getId(), 0L);
            if (response.getBody() != null) {
                Resource resource = response.getBody();
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                originalContent = content;
                contentArea.setValue(content);
                contentArea.setReadOnly(false);
                saveButton.setEnabled(true);
                contentLoaded = true;
                hideStatus();
            } else {
                showStatus(I18n.t("template.dialog.edit.content.load.failed"), "error");
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ?
                    ex.contentUTF8() : ex.getMessage();
            showStatus(I18n.t("template.dialog.edit.content.load.error", errorMsg), "error");
            log.error("Failed to load template content for {}", template.getId(), ex);
        } catch (Exception e) {
            showStatus(I18n.t("template.dialog.edit.content.load.error", e.getMessage()), "error");
            log.error("Failed to load template content for {}", template.getId(), e);
        }
    }

    private void saveContent() {
        String newContent = contentArea.getValue();
        if (newContent == null || newContent.isEmpty()) {
            showStatus(I18n.t("template.dialog.edit.content.empty.error"), "error");
            return;
        }

        if (newContent.equals(originalContent)) {
            showStatus(I18n.t("template.dialog.edit.content.no.changes"), "info");
            return;
        }

        if (parentView != null) {
            parentView.showLoading(true);
        }
        saveButton.setEnabled(false);

        try {
            // Create MultipartFile from the content
            final byte[] contentBytes = newContent.getBytes(StandardCharsets.UTF_8);
            final String fileName = template.getOriginalFileName() != null ?
                    template.getOriginalFileName() : template.getFileName();

            MultipartFile multipartFile = new MultipartFile() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getOriginalFilename() {
                    return fileName;
                }

                @Override
                public String getContentType() {
                    String contentType = "text/plain";
                    if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                        contentType = "text/html";
                    } else if (fileName.endsWith(".xml")) {
                        contentType = "application/xml";
                    } else if (fileName.endsWith(".json")) {
                        contentType = "application/json";
                    } else if (fileName.endsWith(".ftl") || fileName.endsWith(".vm")) {
                        contentType = "text/plain";
                    } else if (fileName.endsWith(".properties")) {
                        contentType = "text/plain";
                    }
                    return contentType;
                }

                @Override
                public boolean isEmpty() {
                    return contentBytes.length == 0;
                }

                @Override
                public long getSize() {
                    return contentBytes.length;
                }

                @Override
                public byte[] getBytes() {
                    return contentBytes;
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(contentBytes);
                }

                @Override
                public void transferTo(java.io.File dest) throws IllegalStateException {
                    throw new UnsupportedOperationException("transferTo not supported");
                }
            };

            // Update template with new file content
            MsgTemplateDto updatedTemplate = MsgTemplateDto.builder()
                    .id(template.getId())
                    .code(template.getCode())
                    .tenant(template.getTenant())
                    .name(template.getName())
                    .description(template.getDescription())
                    .language(template.getLanguage())
                    .build();

            // Use MsgTemplateFileService for update with file
            ResponseEntity<MsgTemplateDto> response = templateFileService.updateWithFile(
                    template.getId(), multipartFile, updatedTemplate);

            if (!response.getStatusCode().is2xxSuccessful()) {
                showStatus(I18n.t("template.dialog.edit.content.save.failed",
                        response.getBody() != null ? response.getBody().toString() : "unknown error"), "error");
                saveButton.setEnabled(true);
                return;
            }

            originalContent = newContent;
            showStatus(I18n.t("template.dialog.edit.content.save.success"), "success");

            // Refresh the template in the card
            if (onSuccess != null) {
                onSuccess.run();
            }

            // Close after a short delay
            getUI().ifPresent(ui -> ui.access(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                close();
            }));

        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ?
                    ex.contentUTF8() : ex.getMessage();
            showStatus(I18n.t("template.dialog.edit.content.save.error", errorMsg), "error");
            log.error("Failed to save template content for {}", template.getId(), ex);
        } catch (Exception e) {
            showStatus(I18n.t("template.dialog.edit.content.save.error", e.getMessage()), "error");
            log.error("Failed to save template content for {}", template.getId(), e);
        } finally {
            if (parentView != null) {
                parentView.showLoading(false);
            }
            saveButton.setEnabled(true);
        }
    }

    private void downloadTemplate() {
        if (template.getFileName() == null || template.getFileName().isEmpty()) {
            return;
        }
        try {
            ResponseEntity<Resource> response = templateFileService.downloadFile(template.getId(), 0L);
            if (response.getBody() != null) {
                Resource resource = response.getBody();
                // Read the resource content as bytes
                byte[] content = resource.getInputStream().readAllBytes();
                // Encode to Base64
                String base64Content = java.util.Base64.getEncoder().encodeToString(content);
                String fileName = template.getOriginalFileName() != null ?
                        template.getOriginalFileName() : template.getFileName();

                getUI().ifPresent(ui -> ui.getPage().executeJs(
                        "const byteCharacters = atob($0);" +
                                "const byteNumbers = new Array(byteCharacters.length);" +
                                "for (let i = 0; i < byteCharacters.length; i++) {" +
                                "    byteNumbers[i] = byteCharacters.charCodeAt(i);" +
                                "}" +
                                "const byteArray = new Uint8Array(byteNumbers);" +
                                "const blob = new Blob([byteArray]);" +
                                "const url = URL.createObjectURL(blob);" +
                                "const a = document.createElement('a');" +
                                "a.href = url;" +
                                "a.download = $1;" +
                                "a.click();" +
                                "URL.revokeObjectURL(url);",
                        base64Content, fileName
                ));
            }
        } catch (Exception e) {
            log.error("Failed to download template file for {}", template.getId(), e);
            Notification.show(I18n.t("template.download.error", e.getMessage()), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showStatus(String message, String type) {
        statusArea.setVisible(true);
        statusArea.removeAll();
        statusArea.add(new Span(message));

        switch (type) {
            case "success":
                statusArea.getStyle()
                        .set("background-color", "var(--lumo-success-color-10pct)")
                        .set("color", "var(--lumo-success-text-color)");
                break;
            case "error":
                statusArea.getStyle()
                        .set("background-color", "var(--lumo-error-color-10pct)")
                        .set("color", "var(--lumo-error-text-color)");
                break;
            case "warning":
                statusArea.getStyle()
                        .set("background-color", "var(--lumo-warning-color-10pct)")
                        .set("color", "var(--lumo-warning-text-color)");
                break;
            case "info":
            default:
                statusArea.getStyle()
                        .set("background-color", "var(--lumo-primary-color-10pct)")
                        .set("color", "var(--lumo-primary-text-color)");
                break;
        }
    }

    private void hideStatus() {
        statusArea.setVisible(false);
        statusArea.removeAll();
    }
}