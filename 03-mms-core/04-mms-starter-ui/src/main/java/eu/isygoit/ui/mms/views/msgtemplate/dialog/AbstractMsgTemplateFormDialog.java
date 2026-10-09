package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.mms.views.common.MmsDialogSupport;
import eu.isygoit.ui.mms.views.common.MmsEnumTag;
import eu.isygoit.ui.mms.views.msgtemplate.MsgTemplateManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Shared form of the message-template create/update dialogs: every editable
 * {@link MsgTemplateDto} field is declared here once. {@code id} is never shown
 * and {@code code} is always read-only (assigned by the server). The file
 * related fields ({@code path}, {@code fileName}, {@code originalFileName},
 * {@code file}) are managed through the upload widget. Subclasses only decide
 * how the DTO is built and persisted.
 *
 * <p>Message keys are derived from {@code messagePrefix}
 * ({@code mms.msgtemplate.dialog.create} or {@code mms.msgtemplate.dialog.edit}).
 * Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
@Slf4j
abstract class AbstractMsgTemplateFormDialog extends BaseMsgTemplateDialog {

    private final String messagePrefix;

    TextField codeField;
    TextField tenantField;
    ComboBox<IEnumEmailTemplate.Types> nameCombo;
    TextArea descriptionField;
    ComboBox<IEnumLanguage.Types> languageCombo;
    EmailField defaultSenderField;

    AbstractMsgTemplateFormDialog(String title,
                                  String messagePrefix,
                                  MsgTemplateManagementView parentView,
                                  MsgTemplateService templateService,
                                  MsgTemplateFileService templateFileService,
                                  SenderConfigService senderConfigService,
                                  Runnable onSuccess) {
        super(title, parentView, templateService, templateFileService, senderConfigService, onSuccess);
        this.messagePrefix = messagePrefix;
    }

    /** Name of the file currently attached to the template, or null. */
    abstract String currentFileName();

    /** Optional hint displayed above the upload widget, or null. */
    String uploadHint() {
        return null;
    }

    /** Whether a template file must be uploaded before saving. */
    abstract boolean fileRequired();

    /** A DTO carrying the identity of the template ({@code id}, {@code code}, {@code tenant}). */
    abstract MsgTemplateDto newDto();

    /** Persists the DTO, with the uploaded file when there is one. */
    abstract ResponseEntity<MsgTemplateDto> persist(MsgTemplateDto dto, MultipartFile file);

    private String key(String suffix) {
        return messagePrefix + suffix;
    }

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        setupFileUpload(currentFileName());

        VerticalLayout root = DialogLayout.stack();
        root.add(buildIdentitySection(), buildConfigurationSection(), buildFileSection());
        addContent(root);
    }

    private VerticalLayout buildIdentitySection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("mms.msgtemplate.dialog.view.section.identity"), VaadinIcon.FILE_TEXT);
        FormLayout form = DialogLayout.responsiveForm();

        codeField = new TextField(I18n.t(key(".field.code")));
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        tenantField = new TextField(I18n.t(key(".field.tenant")));
        tenantField.setPlaceholder(I18n.t(key(".field.tenant.placeholder")));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setWidthFull();

        nameCombo = new ComboBox<>(I18n.t(key(".field.name")));
        nameCombo.setItems(IEnumEmailTemplate.Types.values());
        MmsEnumTag.useTagRenderer(nameCombo, "mms.msgtemplate.enum.name");
        nameCombo.setRequiredIndicatorVisible(true);
        nameCombo.setWidthFull();

        languageCombo = new ComboBox<>(I18n.t(key(".field.language")));
        languageCombo.setItems(IEnumLanguage.Types.values());
        MmsEnumTag.useTagRenderer(languageCombo, "mms.msgtemplate.view.language");
        languageCombo.setWidthFull();

        descriptionField = DialogLayout.tall(new TextArea(I18n.t(key(".field.description"))));
        descriptionField.setPlaceholder(I18n.t(key(".field.description.placeholder")));

        form.add(codeField, tenantField, nameCombo, languageCombo, descriptionField);
        form.setColspan(descriptionField, 2);
        section.add(form);
        return section;
    }

    private VerticalLayout buildConfigurationSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("mms.msgtemplate.dialog.view.section.configuration"), VaadinIcon.COG);
        FormLayout form = DialogLayout.responsiveForm();

        defaultSenderField = new EmailField(I18n.t(key(".field.defaultSender")));
        defaultSenderField.setPlaceholder(I18n.t(key(".field.defaultSender.placeholder")));
        defaultSenderField.setHelperText(I18n.t(key(".field.defaultSender.helper")));
        defaultSenderField.setWidthFull();

        senderConfigCombo = createSenderConfigCombo();
        senderConfigCombo.setHelperText(I18n.t(key(".field.senderConfig.helper")));

        form.add(defaultSenderField, senderConfigCombo);
        section.add(form);
        return section;
    }

    private VerticalLayout buildFileSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("mms.msgtemplate.dialog.view.section.file"), VaadinIcon.FILE);

        Div uploadSection = new Div();
        uploadSection.addClassName("wams-dialog-upload-section");
        if (uploadHint() != null) {
            uploadSection.add(DialogLayout.help(uploadHint()));
        }
        uploadSection.add(fileUpload, fileInfoArea);
        section.add(uploadSection);
        return section;
    }

    /** Copies the form values into the DTO ({@code id}, {@code code} and the file fields are left untouched). */
    private void applyTo(MsgTemplateDto dto) {
        String defaultSender = defaultSenderField.getValue();
        dto.setName(nameCombo.getValue().name());
        dto.setDescription(descriptionField.getValue() != null ? descriptionField.getValue().trim() : null);
        dto.setLanguage(languageCombo.getValue());
        dto.setDefaultSender(defaultSender != null && !defaultSender.isBlank() ? defaultSender.trim() : null);
        dto.setSenderConfigId(getSelectedSenderConfigId());
    }

    private boolean isValid() {
        if (!tenantField.isReadOnly()
                && (tenantField.getValue() == null || tenantField.getValue().isBlank())) {
            append(I18n.t(key(".error.tenant.required")));
            return false;
        }
        if (nameCombo.getValue() == null) {
            append(I18n.t(key(".error.name.required")));
            return false;
        }
        String defaultSender = defaultSenderField.getValue();
        if (defaultSender != null && !defaultSender.isBlank() && !MmsDialogSupport.isValidEmail(defaultSender)) {
            append(I18n.t(key(".error.defaultSender.invalid")));
            return false;
        }
        return true;
    }

    @Override
    protected final boolean onOk() {
        clearError();

        if (!isValid()) {
            return false;
        }

        MultipartFile uploadedFile = null;
        if (hasFileUploaded()) {
            uploadedFile = getUploadedFile();
            if (uploadedFile == null || uploadedFile.isEmpty()) {
                append(I18n.t(key(".error.file.invalid")));
                return false;
            }
        } else if (fileRequired()) {
            append(I18n.t(key(".error.file.required")));
            return false;
        }

        if (parentView != null) {
            parentView.showLoading(true);
        }
        try {
            MsgTemplateDto dto = newDto();
            applyTo(dto);

            ResponseEntity<MsgTemplateDto> response = persist(dto, uploadedFile);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t(key(".failed"),
                        response.getBody() != null ? response.getBody().toString() : I18n.t("mms.common.error.unknown")));
                return false;
            }

            append(I18n.t(key(".success")));
            return true;
        } catch (FeignException ex) {
            append(I18n.t(key(".error"), MmsDialogSupport.errorMessage(ex)));
            log.error("Failed to save template", ex);
        } catch (Exception e) {
            append(I18n.t(key(".error"), e.getMessage()));
            log.error("Failed to save template", e);
        } finally {
            if (parentView != null) {
                parentView.showLoading(false);
            }
        }
        return false;
    }
}
