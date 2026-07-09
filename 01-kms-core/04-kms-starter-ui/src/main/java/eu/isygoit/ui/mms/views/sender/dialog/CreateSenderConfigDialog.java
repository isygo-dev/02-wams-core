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
import eu.isygoit.ui.mms.views.sender.SenderConfigManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

@Slf4j
public class CreateSenderConfigDialog extends BaseSenderConfigDialog {

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

    public CreateSenderConfigDialog(SenderConfigManagementView parentView,
                                    SenderConfigService senderConfigService,
                                    Runnable onSuccess) {
        super(I18n.t("mms.sender.dialog.create.title"), parentView, senderConfigService, onSuccess);
        setOkButtonText(I18n.t("mms.sender.dialog.create.button"));
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

        tenantField = new TextField(I18n.t("mms.sender.dialog.create.field.tenant"));
        tenantField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.tenant.placeholder"));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setWidthFull();

        codeField = new TextField(I18n.t("mms.sender.dialog.create.field.code"));
        codeField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.code.placeholder"));
        codeField.setRequiredIndicatorVisible(true);
        codeField.setWidthFull();

        nameField = new TextField(I18n.t("mms.sender.dialog.create.field.name"));
        nameField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.name.placeholder"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        descriptionField = new TextArea(I18n.t("mms.sender.dialog.create.field.description"));
        descriptionField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.description.placeholder"));
        descriptionField.setWidthFull();
        descriptionField.setHeight("80px");

        hostField = new TextField(I18n.t("mms.sender.dialog.create.field.host"));
        hostField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.host.placeholder"));
        hostField.setRequiredIndicatorVisible(true);
        hostField.setWidthFull();

        portField = new TextField(I18n.t("mms.sender.dialog.create.field.port"));
        portField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.port.placeholder"));
        portField.setRequiredIndicatorVisible(true);
        portField.setWidthFull();

        usernameField = new TextField(I18n.t("mms.sender.dialog.create.field.username"));
        usernameField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.username.placeholder"));
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setWidthFull();

        passwordField = new PasswordField(I18n.t("mms.sender.dialog.create.field.password"));
        passwordField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.password.placeholder"));
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidthFull();

        transportProtocolField = new TextField(I18n.t("mms.sender.dialog.create.field.protocol"));
        transportProtocolField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.protocol.placeholder"));
        transportProtocolField.setValue("smtp");
        transportProtocolField.setWidthFull();

        smtpAuthField = new TextField(I18n.t("mms.sender.dialog.create.field.smtp.auth"));
        smtpAuthField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.smtp.auth.placeholder"));
        smtpAuthField.setValue("true");
        smtpAuthField.setWidthFull();

        smtpStarttlsEnableCheckbox = new Checkbox(I18n.t("mms.sender.dialog.create.field.tls.enable"));
        smtpStarttlsEnableCheckbox.setValue(false);

        smtpStarttlsRequiredCheckbox = new Checkbox(I18n.t("mms.sender.dialog.create.field.tls.required"));
        smtpStarttlsRequiredCheckbox.setValue(false);

        debugCheckbox = new Checkbox(I18n.t("mms.sender.dialog.create.field.debug"));
        debugCheckbox.setValue(false);

        defaultSenderField = new EmailField(I18n.t("mms.sender.dialog.create.field.defaultSender"));
        defaultSenderField.setPlaceholder(I18n.t("mms.sender.dialog.create.field.defaultSender.placeholder"));
        defaultSenderField.setWidthFull();
        defaultSenderField.setHelperText(I18n.t("mms.sender.dialog.create.field.defaultSender.helper"));

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
        // Default values can be set here if needed
    }

    @Override
    protected boolean onOk() {
        clearError();

        // Validate required fields
        if (tenantField.getValue() == null || tenantField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.create.error.tenant.required"));
            return false;
        }
        if (codeField.getValue() == null || codeField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.create.error.code.required"));
            return false;
        }
        if (nameField.getValue() == null || nameField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.create.error.name.required"));
            return false;
        }
        if (hostField.getValue() == null || hostField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.create.error.host.required"));
            return false;
        }
        if (portField.getValue() == null || portField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.create.error.port.required"));
            return false;
        }
        if (usernameField.getValue() == null || usernameField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.create.error.username.required"));
            return false;
        }
        if (passwordField.getValue() == null || passwordField.getValue().isBlank()) {
            append(I18n.t("mms.sender.dialog.create.error.password.required"));
            return false;
        }

        // Validate default sender if provided
        String defaultSender = defaultSenderField.getValue();
        if (defaultSender != null && !defaultSender.isBlank() && !isValidEmail(defaultSender)) {
            append(I18n.t("mms.sender.dialog.create.error.defaultSender.invalid"));
            return false;
        }

        if (parentView != null) {
            parentView.showLoading(true);
        }
        try {
            SenderConfigDto config = SenderConfigDto.builder()
                    .tenant(tenantField.getValue().trim())
                    .code(codeField.getValue().trim().toUpperCase())
                    .name(nameField.getValue().trim())
                    .description(descriptionField.getValue() != null ?
                            descriptionField.getValue().trim() : null)
                    .host(hostField.getValue().trim())
                    .port(portField.getValue().trim())
                    .username(usernameField.getValue().trim())
                    .password(passwordField.getValue())
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

            ResponseEntity<SenderConfigDto> response = senderConfigService.create(config);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("mms.sender.dialog.create.failed",
                        response.getBody() != null ? response.getBody().toString() : I18n.t("mms.common.error.unknown")));
                return false;
            }

            append(I18n.t("mms.sender.dialog.create.success"));
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ?
                    ex.contentUTF8() : ex.getMessage();
            append(I18n.t("mms.sender.dialog.create.error", errorMsg));
            log.error("Failed to create sender config", ex);
        } catch (Exception e) {
            append(I18n.t("mms.sender.dialog.create.error", e.getMessage()));
            log.error("Failed to create sender config", e);
        } finally {
            if (parentView != null) {
                parentView.showLoading(false);
            }
        }
        return false;
    }
}