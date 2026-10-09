package eu.isygoit.ui.mms.views.sender.dialog;

import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.mms.views.sender.SenderConfigManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link SenderConfigDto}. The form lives in
 * {@link AbstractSenderConfigFormDialog}; this class only creates the configuration.
 */
public class CreateSenderConfigDialog extends AbstractSenderConfigFormDialog {

    public CreateSenderConfigDialog(SenderConfigManagementView parentView,
                                    SenderConfigService senderConfigService,
                                    Runnable onSuccess) {
        super(I18n.t("mms.sender.dialog.create.title"), "mms.sender.dialog.create",
                parentView, senderConfigService, onSuccess);
        setOkButtonText(I18n.t("mms.sender.dialog.create.button"));

        buildForm();
        transportProtocolField.setValue("smtp");
        smtpAuthField.setValue("true");
    }

    @Override
    boolean tenantEditable() {
        return true;
    }

    @Override
    boolean passwordRequired() {
        return true;
    }

    @Override
    SenderConfigDto newDto() {
        return SenderConfigDto.builder()
                .tenant(tenantField.getValue().trim())
                .build();
    }

    @Override
    ResponseEntity<SenderConfigDto> persist(SenderConfigDto dto) {
        return senderConfigService.create(dto);
    }
}
