package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.remote.mms.SenderConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Update dialog for {@link MsgTemplateDto}. The form lives in
 * {@link AbstractMsgTemplateFormDialog}; this class only updates the template,
 * replacing its file when a new one is uploaded.
 */
public class EditMsgTemplateDialog extends AbstractMsgTemplateFormDialog {

    private final MsgTemplateDto template;

    public EditMsgTemplateDialog(MsgTemplateService templateService,
                                 MsgTemplateFileService templateFileService,
                                 SenderConfigService senderConfigService,
                                 MsgTemplateDto template,
                                 Runnable onSuccess) {
        super(I18n.t("mms.msgtemplate.dialog.edit.title"), "mms.msgtemplate.dialog.edit",
                null, templateService, templateFileService, senderConfigService, onSuccess);
        this.template = template;
        setOkButtonText(I18n.t("mms.msgtemplate.dialog.edit.button"));

        buildForm();
        tenantField.setReadOnly(true); // Tenant is immutable
        prefillData();
    }

    @Override
    String currentFileName() {
        return template.getOriginalFileName();
    }

    @Override
    String uploadHint() {
        return I18n.t("mms.msgtemplate.dialog.edit.upload.new.file");
    }

    @Override
    boolean fileRequired() {
        return false;
    }

    @Override
    MsgTemplateDto newDto() {
        return MsgTemplateDto.builder()
                .id(template.getId())
                .code(template.getCode())
                .tenant(template.getTenant())
                .build();
    }

    @Override
    ResponseEntity<MsgTemplateDto> persist(MsgTemplateDto dto, MultipartFile file) {
        if (file != null) {
            // Update with the newly uploaded file
            return templateFileService.updateWithFile(template.getId(), file, dto);
        }
        // Update without file change
        return templateService.update(template.getId(), dto);
    }

    private void prefillData() {
        codeField.setValue(template.getCode() != null ? template.getCode() : "");
        tenantField.setValue(template.getTenant() != null ? template.getTenant() : "");
        nameCombo.setValue(template.getName() != null ?
                IEnumEmailTemplate.Types.valueOf(template.getName()) : null);
        descriptionField.setValue(template.getDescription() != null ? template.getDescription() : "");
        languageCombo.setValue(template.getLanguage() != null ? template.getLanguage() : IEnumLanguage.Types.EN);
        defaultSenderField.setValue(template.getDefaultSender() != null ? template.getDefaultSender() : "");
        setSelectedSenderConfig(template.getSenderConfigId());
    }
}
