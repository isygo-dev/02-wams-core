package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

@Slf4j
public class EditSenderConfigDialog extends BaseSenderConfigDialog {

    private final SenderConfigDto config;
    private TextField tenantField;
    private TextField codeField;
    private TextField nameField;
    private TextArea descriptionField;
    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField transportProtocolField;
    private TextField smtpAuthField;
    private Checkbox smtpStarttlsEnableCheckbox;
    private Checkbox smtpStarttlsRequiredCheckbox;
    private Checkbox debugCheckbox;
    private EmailField defaultSenderField;

    public EditSenderConfigDialog(SenderConfigService senderConfigService,
                                  SenderConfigDto config,
                                  Runnable onSuccess) {
        super(I18n.t("mms.sender.dialog.edit.title"),
                null, // parentView not needed for edit
                senderConfigService,
                onSuccess);
        this.config = config;
        setOkButtonText(I18n.t("mms.sender.dialog.edit.button"));
        buildContent();
        prefillData();
    }

    @Override
    protected void buildContent() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Read-only tenant field
        tenantField = new TextField(I18n.t("mms.sender.dialog.edit.field.tenant"));
        tenantField.setValue(config.getTenant() != null ? config.getTenant() : "");
        tenantField.setReadOnly(true);
        tenantField.setWidthFull();

        // Read-only code field
        codeField = new TextField(I18n.t("mms.sender.dialog.edit.field.code"));
        codeField.setValue(config.getCode() != null ? config.getCode() : "");
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        nameField = new TextField(I18n.t("mms.sender.dialog.edit.field.name"));
        nameField.setPlaceholder(I18n.t("mms.sender.dialog.edit.field.name.placeholder"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        descriptionField = new TextArea(I18n.t("mms.sender.dialog.edit.field.description"));
        descriptionField.setPlaceholder(I18n.t("mms.sender.dialog.edit.field.description.placeholder"));
        descriptionField.setWidthFull();
        descriptionField.setHeight("80px");

        hostField = new TextField(I18n.t("mms.sender.dialog.edit.field.host"));
        hostField.setPlaceholder(I18n.t("mms.sender.dialog.edit.field.host.placeholder"));
        hostField.setRequiredIndicatorVisible(true);
        hostField.setWidthFull();

        portField = new TextField(I18n.t("mms.sender.dialog.edit.field.port"));
        portField.setPlaceholder(I18n.t("mms.sender.dialog.edit.field.port.placeholder"));
        portField.setRequiredIndicatorVisible(true);
        portField.setWidthFull();

        usernameField = new TextField(I18n.t("mms.sender.dialog.edit.field.username"));
        usernameField.setPlaceholder(I18n.t("mms.sender.dialog.edit.field.username.placeholder"));
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setWidthFull();

        passwordField = new PasswordField(I18n.t("mms.sender.dialog.edit.field.password"));
        passwordField.setPlaceholder(I18n.t("mms.sender.dialog.edit.field.password.placeholder"));
        passwordField.setHelperText(I18n.t("mms.sender.dialog.edit.field.password.helper"));
        passwordField.setWidthFull();

        transportProtocolField = new TextField(I18n.t("mms.sender.dialog.edit.field.protocol"));
        transportProtocolField.setPlaceholder(I18n.t("mms.sender.dialog.edit.field.protocol.placeholder"));
        transportProtocolField.setWidthFull();

        smtpAuthField = new TextField(I18n.t("mms.sender.dialog.edit.field.smtp.auth"));
        smtpAuthField.setPlaceholder(I18n.t("mms.sender.dialog.edit.field.smtp.auth.placeholder"));
        smtpAuthField.setWidthFull();

        smtpStarttlsEnableCheckbox = new Checkbox(I18n.t("mms.sender.dialog.edit.field.tls.enable"));
        smtpStarttlsRequiredCheckbox = new Checkbox(I18n.t("mms.sender.dialog.edit.field.tls.required"));
        debugCheckbox = new Checkbox(I18n.t("mms.sender.dialog.edit.field.debug"));

        defaultSenderField = new EmailField(I18n.t("mms.sender.dialog.edit.field.defaultSender"));
        defaultSenderField.setPlaceholder(I18n.t("mms.sender.dialog.edit.field.defaultSender.placeholder"));
        defaultSenderField.setWidthFull();
        defaultSenderField.setHelperText(I18n.t("mms.sender.dialog.edit.field.defaultSender.helper"));

        form.add(tenantField, codeField, nameField, descriptionField,
                hostField, portField, usernameField, passwordField,
                transportProtocolField, smtpAuthField,
                smtpStarttlsEnableCheckbox, smtpStarttlsRequiredCheckbox,
                debugCheckbox, defaultSenderField);

        // Set column spans
        form.setColspan(tenantField, 2);
        form.setColspan(codeField, 2);
        form.setColspan(nameField, 2);
        form.setColspan(descriptionField, 2);
        form.setColspan(hostField, 2);
        form.setColspan(usernameField, 2);
        form.setColspan(passwordField, 2);
        form.setColspan(defaultSenderField, 2);

        addContent(form);
    }

    private void prefillData() {
        tenantField.setValue(config.getTenant() != null ? config.getTenant() : "");
        codeField.setValue(config.getCode() != null ? config.getCode() : "");
        nameField.setValue(config.getName() != null ? config.getName() : "");
        descriptionField.setValue(config.getDescription() != null ? config.getDescription() : "");
        hostField.setValue(config.getHost() != null ? config.getHost() : "");
        portField.setValue(config.getPort() != null ? config.getPort() : "");
        usernameField.setValue(config.getUsername() != null ? config.getUsername() : "");
        // Password field is left empty - user must re-enter if they want to change it
        transportProtocolField.setValue(config.getTransportProtocol() != null ?
                config.getTransportProtocol() : "smtp");
        smtpAuthField.setValue(config.getSmtpAuth() != null ?
                config.getSmtpAuth() : "true");
        smtpStarttlsEnableCheckbox.setValue(config.getSmtpStarttlsEnable() != null &&
                config.getSmtpStarttlsEnable());
        smtpStarttlsRequiredCheckbox.setValue(config.getSmtpStarttlsRequired() != null &&
                config.getSmtpStarttlsRequired());
        debugCheckbox.setValue(config.getDebug() != null && config.getDebug());
        defaultSenderField.setValue(config.getDefaultSender() != null ? config.getDefaultSender() : "");
    }

    @Override
    protected boolean onOk() {
        clearError();

        // Validate required fields
        if (nameField.getValue() == null || nameField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.edit.error.name.required"));
            return false;
        }
        if (hostField.getValue() == null || hostField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.edit.error.host.required"));
            return false;
        }
        if (portField.getValue() == null || portField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.edit.error.port.required"));
            return false;
        }
        if (usernameField.getValue() == null || usernameField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.edit.error.username.required"));
            return false;
        }

        // Validate default sender if provided
        String defaultSender = defaultSenderField.getValue();
        if (defaultSender != null && !defaultSender.isBlank() && !isValidEmail(defaultSender)) {
            append(I18n.t("mms.sender.dialog.edit.error.defaultSender.invalid"));
            return false;
        }

        try {
            // Build updated config - keep existing values for fields not being updated
            SenderConfigDto updatedConfig = SenderConfigDto.builder()
                    .id(config.getId())
                    .tenant(config.getTenant())
                    .code(config.getCode())
                    .name(nameField.getValue().trim())
                    .description(descriptionField.getValue() != null ?
                            descriptionField.getValue().trim() : null)
                    .host(hostField.getValue().trim())
                    .port(portField.getValue().trim())
                    .username(usernameField.getValue().trim())
                    .password(passwordField.getValue() != null && !passwordField.getValue().isBlank() ?
                            passwordField.getValue() : config.getPassword())
                    .transportProtocol(transportProtocolField.getValue() != null ?
                            transportProtocolField.getValue().trim() : "smtp")
                    .smtpAuth(smtpAuthField.getValue() != null ?
                            smtpAuthField.getValue().trim() : "true")
                    .smtpStarttlsEnable(smtpStarttlsEnableCheckbox.getValue())
                    .smtpStarttlsRequired(smtpStarttlsRequiredCheckbox.getValue())
                    .debug(debugCheckbox.getValue())
                    .defaultSender(defaultSender != null && !defaultSender.isBlank() ?
                            defaultSender.trim() : null)
                    .build();

            ResponseEntity<SenderConfigDto> response = senderConfigService.update(config.getId(), updatedConfig);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("mms.sender.dialog.edit.failed",
                        response.getBody() != null ? response.getBody().toString() : I18n.t("mms.common.error.unknown")));
                return false;
            }

            append(I18n.t("mms.sender.dialog.edit.success"));
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ?
                    ex.contentUTF8() : ex.getMessage();
            append(I18n.t("mms.sender.dialog.edit.error", errorMsg));
            log.error("Failed to update sender config {}", config.getId(), ex);
        } catch (Exception e) {
            append(I18n.t("mms.sender.dialog.edit.error", e.getMessage()));
            log.error("Failed to update sender config {}", config.getId(), e);
        }
        return false;
    }
}