package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.mms.views.msgtemplate.MsgTemplateManagementView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Create dialog for {@link MsgTemplateDto}. The form lives in
 * {@link AbstractMsgTemplateFormDialog}; this class only creates the template
 * together with its uploaded file.
 */
public class CreateMsgTemplateDialog extends AbstractMsgTemplateFormDialog {

    public CreateMsgTemplateDialog(MsgTemplateManagementView parentView,
                                   MsgTemplateService templateService,
                                   MsgTemplateFileService templateFileService,
                                   SenderConfigService senderConfigService,
                                   Runnable onSuccess) {
        super(I18n.t("mms.msgtemplate.dialog.create.title"), "mms.msgtemplate.dialog.create",
                parentView, templateService, templateFileService, senderConfigService, onSuccess);
        setOkButtonText(I18n.t("mms.msgtemplate.dialog.create.button"));

        buildForm();
        languageCombo.setValue(IEnumLanguage.Types.EN);
    }

    @Override
    String currentFileName() {
        return null;
    }

    @Override
    boolean fileRequired() {
        return true;
    }

    @Override
    MsgTemplateDto newDto() {
        return MsgTemplateDto.builder()
                .tenant(tenantField.getValue().trim())
                .build();
    }

    @Override
    ResponseEntity<MsgTemplateDto> persist(MsgTemplateDto dto, MultipartFile file) {
        return templateFileService.createWithFile(file, dto);
    }
}
