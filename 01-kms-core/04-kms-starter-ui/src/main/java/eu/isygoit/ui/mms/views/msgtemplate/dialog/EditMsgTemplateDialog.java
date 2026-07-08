package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class EditMsgTemplateDialog extends BaseMsgTemplateDialog {

    private final MsgTemplateDto template;
    private TextField codeField;
    private TextField tenantField;
    private ComboBox<IEnumEmailTemplate.Types> nameCombo;
    private TextArea descriptionField;
    private ComboBox<IEnumLanguage.Types> languageCombo;
    private EmailField defaultSenderField;

    public EditMsgTemplateDialog(MsgTemplateService templateService,
                                 MsgTemplateFileService templateFileService,
                                 MsgTemplateDto template,
                                 Runnable onSuccess) {
        super(I18n.t("mms.msgtemplate.dialog.edit.title"),
                null,
                templateService,
                templateFileService,
                onSuccess);
        this.template = template;
        setOkButtonText(I18n.t("mms.msgtemplate.dialog.edit.button"));
        buildContent();
        prefillData();
    }

    protected void buildContent() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        codeField = new TextField(I18n.t("mms.msgtemplate.dialog.edit.field.code"));
        codeField.setPlaceholder(I18n.t("mms.msgtemplate.dialog.edit.field.code.placeholder"));
        codeField.setRequiredIndicatorVisible(true);
        codeField.setReadOnly(true); // Code is immutable
        codeField.setWidthFull();

        tenantField = new TextField(I18n.t("mms.msgtemplate.dialog.edit.field.tenant"));
        tenantField.setPlaceholder(I18n.t("mms.msgtemplate.dialog.edit.field.tenant.placeholder"));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setReadOnly(true); // Tenant is immutable
        tenantField.setWidthFull();

        nameCombo = new ComboBox<>(I18n.t("mms.msgtemplate.dialog.edit.field.name"));
        nameCombo.setItems(IEnumEmailTemplate.Types.values());
        nameCombo.setRequiredIndicatorVisible(true);
        nameCombo.setWidthFull();

        descriptionField = new TextArea(I18n.t("mms.msgtemplate.dialog.edit.field.description"));
        descriptionField.setPlaceholder(I18n.t("mms.msgtemplate.dialog.edit.field.description.placeholder"));
        descriptionField.setWidthFull();
        descriptionField.setHeight("80px");

        languageCombo = new ComboBox<>(I18n.t("mms.msgtemplate.dialog.edit.field.language"));
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setWidthFull();

        defaultSenderField = new EmailField(I18n.t("mms.msgtemplate.dialog.edit.field.defaultSender"));
        defaultSenderField.setPlaceholder(I18n.t("mms.msgtemplate.dialog.edit.field.defaultSender.placeholder"));
        defaultSenderField.setWidthFull();
        defaultSenderField.setHelperText(I18n.t("mms.msgtemplate.dialog.edit.field.defaultSender.helper"));

        // File upload section
        setupFileUpload(template.getOriginalFileName());

        form.add(codeField, tenantField, nameCombo, descriptionField, languageCombo, defaultSenderField);
        form.setColspan(codeField, 2);
        form.setColspan(tenantField, 2);
        form.setColspan(nameCombo, 2);
        form.setColspan(descriptionField, 2);
        form.setColspan(languageCombo, 1);
        form.setColspan(defaultSenderField, 1);

        contentArea.add(form);

        // Add file upload section
        Div uploadSection = new Div();
        uploadSection.addClassName("wams-dialog-upload-section");

        Span uploadLabel = new Span(I18n.t("mms.msgtemplate.dialog.edit.upload.new.file"));
        uploadLabel.addClassName("wams-dialog-upload-label");
        uploadSection.add(uploadLabel, fileUpload, fileInfoArea);
        contentArea.add(uploadSection);
    }

    private void prefillData() {
        codeField.setValue(template.getCode() != null ? template.getCode() : "");
        tenantField.setValue(template.getTenant() != null ? template.getTenant() : "");
        nameCombo.setValue(template.getName() != null ?
                IEnumEmailTemplate.Types.valueOf(template.getName()) : null);
        descriptionField.setValue(template.getDescription() != null ? template.getDescription() : "");
        languageCombo.setValue(template.getLanguage() != null ? template.getLanguage() : IEnumLanguage.Types.EN);
        defaultSenderField.setValue(template.getDefaultSender() != null ? template.getDefaultSender() : "");
    }

    @Override
    protected boolean onOk() {
        clearErrors();

        if (nameCombo.getValue() == null) {
            append(I18n.t("mms.msgtemplate.dialog.edit.error.name.required"));
            return false;
        }

        // Validate default sender if provided
        String defaultSender = defaultSenderField.getValue();
        if (defaultSender != null && !defaultSender.isBlank() && !isValidEmail(defaultSender)) {
            append(I18n.t("mms.msgtemplate.dialog.edit.error.defaultSender.invalid"));
            return false;
        }

        try {
            MsgTemplateDto updatedTemplate = MsgTemplateDto.builder()
                    .id(template.getId())
                    .code(template.getCode())
                    .tenant(template.getTenant())
                    .name(nameCombo.getValue().name())
                    .description(descriptionField.getValue() != null ?
                            descriptionField.getValue().trim() : null)
                    .language(languageCombo.getValue())
                    .defaultSender(defaultSender != null && !defaultSender.isBlank() ?
                            defaultSender.trim() : null)
                    .build();

            ResponseEntity<MsgTemplateDto> response;
            if (hasFileUploaded()) {
                MultipartFile uploadedFile = getUploadedFile();
                if (uploadedFile == null || uploadedFile.isEmpty()) {
                    append(I18n.t("mms.msgtemplate.dialog.edit.error.file.invalid"));
                    return false;
                }
                // Use MsgTemplateFileService for update with new file
                // The API expects: @RequestPart(name = "file") MultipartFile file, @RequestPart(name = "dto") D dto
                response = templateFileService.updateWithFile(
                        template.getId(), uploadedFile, updatedTemplate);
            } else {
                // Use MsgTemplateService for update without file change
                response = templateService.update(template.getId(), updatedTemplate);
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("mms.msgtemplate.dialog.edit.failed",
                        response.getBody() != null ? response.getBody().toString() : I18n.t("mms.common.error.unknown")));
                return false;
            }

            showSuccess(I18n.t("mms.msgtemplate.dialog.edit.success"));
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ?
                    ex.contentUTF8() : ex.getMessage();
            append(I18n.t("mms.msgtemplate.dialog.edit.error", errorMsg));
            log.error("Failed to update template {}", template.getId(), ex);
        } catch (Exception e) {
            append(I18n.t("mms.msgtemplate.dialog.edit.error", e.getMessage()));
            log.error("Failed to update template {}", template.getId(), e);
        }
        return false;
    }
}