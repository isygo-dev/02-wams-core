package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.mms.views.msgtemplate.MsgTemplateManagementView;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.mms.views.common.MmsDialogSupport;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * Has a real commit action (save the edited content), so it extends
 * {@link BaseActionDialog} — the error/success span and Save/Cancel buttons
 * come from the shared footer contract. Download stays a content-level
 * action (it doesn't commit or discard anything) and lives next to the file
 * info bar rather than in the footer.
 */
@Slf4j
public class EditTemplateContentDialog extends BaseActionDialog {

    private final MsgTemplateManagementView parentView;
    private final MsgTemplateFileService templateFileService;
    private final MsgTemplateDto template;
    private TextArea contentArea;
    private Div statusArea;
    private String originalContent;

    public EditTemplateContentDialog(MsgTemplateManagementView parentView,
                                     MsgTemplateService templateService,
                                     MsgTemplateFileService templateFileService,
                                     MsgTemplateDto template,
                                     Runnable onSuccess) {
        super(I18n.t("mms.msgtemplate.dialog.edit.content.title",
                template.getName() != null ? template.getName() : template.getId()), onSuccess);
        addClassName("mms-dialog");
        this.parentView = parentView;
        this.templateFileService = templateFileService;
        this.template = template;

        DialogLayout.size(this, DialogLayout.WIDTH_L);

        setOkButtonText(I18n.t("mms.msgtemplate.dialog.edit.content.save"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_PRIMARY);
        enableOkButton(false);

        buildContent();
        loadTemplateContent();
    }

    private void buildContent() {
        // File info bar + download action (not a commit/discard action, so it
        // stays in the content area rather than the footer).
        HorizontalLayout infoBar = new HorizontalLayout();
        infoBar.setWidthFull();
        infoBar.setAlignItems(FlexComponent.Alignment.CENTER);
        infoBar.setSpacing(true);
        infoBar.addClassName("wams-dialog-info-bar");

        Icon fileIcon = VaadinIcon.FILE.create();
        fileIcon.addClassName("mms-primary-icon");

        Span fileName = new Span(template.getOriginalFileName() != null ?
                template.getOriginalFileName() : template.getFileName());
        fileName.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span fileInfo = new Span(I18n.t("mms.msgtemplate.dialog.edit.content.file.info",
                template.getPath() != null ? template.getPath() : I18n.t("mms.common.value.notAvailable")));
        fileInfo.addClassName(LumoUtility.FontSize.XSMALL);
        fileInfo.addClassName(LumoUtility.TextColor.SECONDARY);

        Button downloadButton = new Button(I18n.t("mms.msgtemplate.dialog.edit.content.download"), new Icon(VaadinIcon.DOWNLOAD));
        downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
        downloadButton.addClickListener(e -> downloadTemplate());

        infoBar.add(fileIcon, fileName, fileInfo, downloadButton);
        infoBar.expand(fileInfo);

        // Transient loading/no-file status (informational, not a save error —
        // real save errors go through the inherited error-span/Notification via onOk()).
        statusArea = new Div();
        statusArea.setVisible(false);
        statusArea.addClassName("wams-dialog-status-area");
        statusArea.setWidthFull();

        contentArea = DialogLayout.tall(new TextArea());
        contentArea.setPlaceholder(I18n.t("mms.msgtemplate.dialog.edit.content.placeholder"));
        contentArea.addClassName("wams-dialog-content-editor");
        contentArea.addClassName(LumoUtility.Border.ALL);
        contentArea.addClassName(LumoUtility.BorderRadius.MEDIUM);

        addContent(infoBar, statusArea, contentArea);
    }

    private void loadTemplateContent() {
        if (template.getFileName() == null || template.getFileName().isEmpty()) {
            showStatus(I18n.t("mms.msgtemplate.dialog.edit.content.no.file"), "warning");
            contentArea.setValue(I18n.t("mms.msgtemplate.dialog.edit.content.no.file.message"));
            contentArea.setReadOnly(true);
            enableOkButton(false);
            return;
        }

        showStatus(I18n.t("mms.msgtemplate.dialog.edit.content.loading"), "info");
        contentArea.setReadOnly(true);
        enableOkButton(false);

        try {
            ResponseEntity<Resource> response = templateFileService.downloadFile(template.getId(), 0L);
            if (response.getBody() != null) {
                Resource resource = response.getBody();
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                originalContent = content;
                contentArea.setValue(content);
                contentArea.setReadOnly(false);
                enableOkButton(true);
                hideStatus();
            } else {
                showError(I18n.t("mms.msgtemplate.dialog.edit.content.load.failed"));
            }
        } catch (FeignException ex) {
            String errorMsg = MmsDialogSupport.errorMessage(ex);
            showError(I18n.t("mms.msgtemplate.dialog.edit.content.load.error", errorMsg));
            log.error("Failed to load template content for {}", template.getId(), ex);
        } catch (Exception e) {
            showError(I18n.t("mms.msgtemplate.dialog.edit.content.load.error", e.getMessage()));
            log.error("Failed to load template content for {}", template.getId(), e);
        }
    }

    @Override
    protected boolean onOk() {
        String newContent = contentArea.getValue();
        if (newContent == null || newContent.isEmpty()) {
            append(I18n.t("mms.msgtemplate.dialog.edit.content.empty.error"));
            return false;
        }

        if (newContent.equals(originalContent)) {
            append(I18n.t("mms.msgtemplate.dialog.edit.content.no.changes"));
            return false;
        }

        if (parentView != null) {
            parentView.showLoading(true);
        }

        try {
            final byte[] contentBytes = newContent.getBytes(StandardCharsets.UTF_8);
            final String fileName = template.getOriginalFileName() != null ?
                    template.getOriginalFileName() : template.getFileName();

            MultipartFile multipartFile = MmsDialogSupport.multipartOf("file", fileName, contentBytes, "text/plain");

            // Send the complete loaded DTO so that only the file content changes
            // (defaultSender, senderConfigId, ... are preserved).

            ResponseEntity<MsgTemplateDto> response = templateFileService.updateWithFile(
                    template.getId(), multipartFile, template);

            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("mms.msgtemplate.dialog.edit.content.save.failed",
                        response.getBody() != null ? response.getBody().toString() : I18n.t("mms.common.error.unknown")));
                return false;
            }

            originalContent = newContent;
            append(I18n.t("mms.msgtemplate.dialog.edit.content.save.success"));
            return true;
        } catch (FeignException ex) {
            String errorMsg = MmsDialogSupport.errorMessage(ex);
            append(I18n.t("mms.msgtemplate.dialog.edit.content.save.error", errorMsg));
            log.error("Failed to save template content for {}", template.getId(), ex);
            return false;
        } catch (Exception e) {
            append(I18n.t("mms.msgtemplate.dialog.edit.content.save.error", e.getMessage()));
            log.error("Failed to save template content for {}", template.getId(), e);
            return false;
        } finally {
            if (parentView != null) {
                parentView.showLoading(false);
            }
        }
    }

    private void downloadTemplate() {
        MmsDialogSupport.downloadTemplateFile(this, templateFileService, template);
    }

    private void showStatus(String message, String type) {
        statusArea.setVisible(true);
        statusArea.removeAll();
        statusArea.add(new Span(message));
        statusArea.removeClassName("wams-dialog-status-area--warning");
        statusArea.removeClassName("wams-dialog-status-area--info");
        statusArea.addClassName("warning".equals(type) ? "wams-dialog-status-area--warning" : "wams-dialog-status-area--info");
    }

    private void hideStatus() {
        statusArea.setVisible(false);
        statusArea.removeAll();
    }
}
