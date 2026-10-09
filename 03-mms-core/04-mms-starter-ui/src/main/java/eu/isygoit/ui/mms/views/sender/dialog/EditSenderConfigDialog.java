package eu.isygoit.ui.mms.views.sender.dialog;

import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link SenderConfigDto}. The form lives in
 * {@link AbstractSenderConfigFormDialog}; this class only updates the configuration.
 * Leaving the password empty keeps the current one.
 */
public class EditSenderConfigDialog extends AbstractSenderConfigFormDialog {

    private final SenderConfigDto config;

    public EditSenderConfigDialog(SenderConfigService senderConfigService,
                                  SenderConfigDto config,
                                  Runnable onSuccess) {
        super(I18n.t("mms.sender.dialog.edit.title"), "mms.sender.dialog.edit",
                null, // parentView not needed for edit
                senderConfigService, onSuccess);
        this.config = config;
        setOkButtonText(I18n.t("mms.sender.dialog.edit.button"));

        buildForm();
        prefillData();
    }

    @Override
    boolean tenantEditable() {
        return false;
    }

    @Override
    boolean passwordRequired() {
        return false;
    }

    @Override
    String existingPassword() {
        return config.getPassword();
    }

    @Override
    SenderConfigDto newDto() {
        return SenderConfigDto.builder()
                .id(config.getId())
                .tenant(config.getTenant())
                .code(config.getCode())
                .build();
    }

    @Override
    ResponseEntity<SenderConfigDto> persist(SenderConfigDto dto) {
        return senderConfigService.update(config.getId(), dto);
    }

    private void prefillData() {
        tenantField.setValue(config.getTenant() != null ? config.getTenant() : "");
        codeField.setValue(config.getCode() != null ? config.getCode() : "");
        nameField.setValue(config.getName() != null ? config.getName() : "");
        descriptionField.setValue(config.getDescription() != null ? config.getDescription() : "");
        hostField.setValue(config.getHost() != null ? config.getHost() : "");
        portField.setValue(config.getPort() != null ? config.getPort() : "");
        usernameField.setValue(config.getUsername() != null ? config.getUsername() : "");
        // Password field is left empty - an empty value keeps the current password
        transportProtocolField.setValue(config.getTransportProtocol() != null ?
                config.getTransportProtocol() : "smtp");
        smtpAuthField.setValue(config.getSmtpAuth() != null ? config.getSmtpAuth() : "true");
        smtpStarttlsEnableCheckbox.setValue(Boolean.TRUE.equals(config.getSmtpStarttlsEnable()));
        smtpStarttlsRequiredCheckbox.setValue(Boolean.TRUE.equals(config.getSmtpStarttlsRequired()));
        debugCheckbox.setValue(Boolean.TRUE.equals(config.getDebug()));
        defaultSenderField.setValue(config.getDefaultSender() != null ? config.getDefaultSender() : "");
    }
}
