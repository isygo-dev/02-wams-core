package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.mms.views.msgtemplate.MsgTemplateManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class CreateMsgTemplateDialog extends BaseMsgTemplateDialog {

    private TextField codeField;
    private TextField tenantField;
    private ComboBox<IEnumEmailTemplate.Types> nameCombo;
    private TextArea descriptionField;
    private ComboBox<IEnumLanguage.Types> languageCombo;

    public CreateMsgTemplateDialog(MsgTemplateManagementView parentView,
                                   MsgTemplateService templateService,
                                   MsgTemplateFileService templateFileService,
                                   Runnable onSuccess) {
        super(I18n.t("mms.msgtemplate.dialog.create.title"), parentView, templateService, templateFileService, onSuccess);
        setOkButtonText(I18n.t("mms.msgtemplate.dialog.create.button"));
        buildContent();
    }

    protected void buildContent() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        codeField = new TextField(I18n.t("mms.msgtemplate.dialog.create.field.code"));
        codeField.setPlaceholder(I18n.t("mms.msgtemplate.dialog.create.field.code.placeholder"));
        codeField.setRequiredIndicatorVisible(true);
        codeField.setWidthFull();

        tenantField = new TextField(I18n.t("mms.msgtemplate.dialog.create.field.tenant"));
        tenantField.setPlaceholder(I18n.t("mms.msgtemplate.dialog.create.field.tenant.placeholder"));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setWidthFull();

        nameCombo = new ComboBox<>(I18n.t("mms.msgtemplate.dialog.create.field.name"));
        nameCombo.setItems(IEnumEmailTemplate.Types.values());
        nameCombo.setRequiredIndicatorVisible(true);
        nameCombo.setWidthFull();

        descriptionField = new TextArea(I18n.t("mms.msgtemplate.dialog.create.field.description"));
        descriptionField.setPlaceholder(I18n.t("mms.msgtemplate.dialog.create.field.description.placeholder"));
        descriptionField.setWidthFull();
        descriptionField.setHeight("80px");

        languageCombo = new ComboBox<>(I18n.t("mms.msgtemplate.dialog.create.field.language"));
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setValue(IEnumLanguage.Types.EN);
        languageCombo.setWidthFull();

        // File upload section
        setupFileUpload(null);

        form.add(codeField, tenantField, nameCombo, descriptionField, languageCombo);
        form.setColspan(codeField, 2);
        form.setColspan(tenantField, 2);
        form.setColspan(nameCombo, 2);
        form.setColspan(descriptionField, 2);

        contentArea.add(form);

        // Add file upload section
        Div uploadSection = new Div();
        uploadSection.addClassName("wams-dialog-upload-section");
        uploadSection.add(fileUpload, fileInfoArea);
        contentArea.add(uploadSection);
    }

    @Override
    protected boolean onOk() {
        clearErrors();

        if (codeField.getValue() == null || codeField.getValue().isBlank()) {
            append(I18n.t("mms.msgtemplate.dialog.create.error.code.required"));
            return false;
        }
        if (tenantField.getValue() == null || tenantField.getValue().isBlank()) {
            append(I18n.t("mms.msgtemplate.dialog.create.error.tenant.required"));
            return false;
        }
        if (nameCombo.getValue() == null) {
            append(I18n.t("mms.msgtemplate.dialog.create.error.name.required"));
            return false;
        }

        if (!hasFileUploaded()) {
            append(I18n.t("mms.msgtemplate.dialog.create.error.file.required"));
            return false;
        }

        MultipartFile uploadedFile = getUploadedFile();
        if (uploadedFile == null || uploadedFile.isEmpty()) {
            append(I18n.t("mms.msgtemplate.dialog.create.error.file.invalid"));
            return false;
        }

        if (parentView != null) {
            parentView.showLoading(true);
        }
        try {
            MsgTemplateDto template = MsgTemplateDto.builder()
                    .code(codeField.getValue().trim().toUpperCase())
                    .tenant(tenantField.getValue().trim())
                    .name(nameCombo.getValue().name())
                    .description(descriptionField.getValue() != null ?
                            descriptionField.getValue().trim() : null)
                    .language(languageCombo.getValue())
                    .build();

            // Use MsgTemplateFileService for create with file
            // The API expects: @RequestPart(name = "file") MultipartFile file, @RequestPart(name = "dto") D dto
            ResponseEntity<MsgTemplateDto> response = templateFileService.createWithFile(
                    uploadedFile, template);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("mms.msgtemplate.dialog.create.failed",
                        response.getBody() != null ? response.getBody().toString() : I18n.t("mms.common.error.unknown")));
                return false;
            }

            showSuccess(I18n.t("mms.msgtemplate.dialog.create.success"));
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ?
                    ex.contentUTF8() : ex.getMessage();
            append(I18n.t("mms.msgtemplate.dialog.create.error", errorMsg));
            log.error("Failed to create template", ex);
        } catch (Exception e) {
            append(I18n.t("mms.msgtemplate.dialog.create.error", e.getMessage()));
            log.error("Failed to create template", e);
        } finally {
            if (parentView != null) {
                parentView.showLoading(false);
            }
        }
        return false;
    }
}