package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.mms.views.common.MmsDialogSupport;
import eu.isygoit.ui.mms.views.sender.SenderConfigManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

/**
 * Shared form of the sender-configuration create/update dialogs: every editable
 * {@link SenderConfigDto} field is declared here once. {@code id} is never shown
 * and {@code code} is always read-only (assigned by the server). Subclasses only
 * decide how the DTO is identified and persisted.
 *
 * <p>Message keys are derived from {@code messagePrefix}
 * ({@code mms.sender.dialog.create} or {@code mms.sender.dialog.edit}).
 * Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
@Slf4j
abstract class AbstractSenderConfigFormDialog extends BaseSenderConfigDialog {

    private final String messagePrefix;

    TextField tenantField;
    TextField codeField;
    TextField nameField;
    TextArea descriptionField;
    TextField hostField;
    TextField portField;
    TextField usernameField;
    PasswordField passwordField;
    TextField transportProtocolField;
    TextField smtpAuthField;
    Checkbox smtpStarttlsEnableCheckbox;
    Checkbox smtpStarttlsRequiredCheckbox;
    Checkbox debugCheckbox;
    EmailField defaultSenderField;

    AbstractSenderConfigFormDialog(String title,
                                   String messagePrefix,
                                   SenderConfigManagementView parentView,
                                   SenderConfigService senderConfigService,
                                   Runnable onSuccess) {
        super(title, parentView, senderConfigService, onSuccess);
        this.messagePrefix = messagePrefix;
    }

    /** Whether the tenant can be typed (create) or is immutable (update). */
    abstract boolean tenantEditable();

    /** Whether the password must be provided (create) or an empty value keeps the current one (update). */
    abstract boolean passwordRequired();

    /** Current stored password, kept when the password field is left empty (update only). */
    String existingPassword() {
        return null;
    }

    /** A DTO carrying the identity of the configuration ({@code id}, {@code code}, {@code tenant}). */
    abstract SenderConfigDto newDto();

    /** Persists the DTO. */
    abstract ResponseEntity<SenderConfigDto> persist(SenderConfigDto dto);

    private String key(String suffix) {
        return messagePrefix + suffix;
    }

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_L);

        TabSheet tabs = new TabSheet();
        tabs.addClassName("wams-dialog-tabs");
        tabs.add(I18n.t("mms.dialog.tab.general"), buildGeneralForm());
        tabs.add(I18n.t("mms.dialog.tab.connection"), buildConnectionForm());
        tabs.add(I18n.t("mms.dialog.tab.credentials"), buildCredentialsForm());
        addContent(tabs);
    }

    private FormLayout buildGeneralForm() {
        FormLayout form = DialogLayout.responsiveForm();

        tenantField = new TextField(I18n.t(key(".field.tenant")));
        tenantField.setWidthFull();
        if (tenantEditable()) {
            tenantField.setPlaceholder(I18n.t(key(".field.tenant.placeholder")));
            tenantField.setRequiredIndicatorVisible(true);
        } else {
            tenantField.setReadOnly(true);
        }

        codeField = new TextField(I18n.t(key(".field.code")));
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        nameField = new TextField(I18n.t(key(".field.name")));
        nameField.setPlaceholder(I18n.t(key(".field.name.placeholder")));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        descriptionField = DialogLayout.tall(new TextArea(I18n.t(key(".field.description"))));
        descriptionField.setPlaceholder(I18n.t(key(".field.description.placeholder")));

        form.add(tenantField, codeField, nameField, descriptionField);
        form.setColspan(descriptionField, 2);
        return form;
    }

    private FormLayout buildConnectionForm() {
        FormLayout form = DialogLayout.responsiveForm();

        hostField = new TextField(I18n.t(key(".field.host")));
        hostField.setPlaceholder(I18n.t(key(".field.host.placeholder")));
        hostField.setRequiredIndicatorVisible(true);
        hostField.setWidthFull();

        // The DTO stores the port and the smtp auth flag as free text: kept as text fields
        // so the stored value is never rewritten.
        portField = new TextField(I18n.t(key(".field.port")));
        portField.setPlaceholder(I18n.t(key(".field.port.placeholder")));
        portField.setRequiredIndicatorVisible(true);
        portField.setWidthFull();

        transportProtocolField = new TextField(I18n.t(key(".field.protocol")));
        transportProtocolField.setPlaceholder(I18n.t(key(".field.protocol.placeholder")));
        transportProtocolField.setWidthFull();

        smtpAuthField = new TextField(I18n.t(key(".field.smtp.auth")));
        smtpAuthField.setPlaceholder(I18n.t(key(".field.smtp.auth.placeholder")));
        smtpAuthField.setWidthFull();

        smtpStarttlsEnableCheckbox = new Checkbox(I18n.t(key(".field.tls.enable")));
        smtpStarttlsRequiredCheckbox = new Checkbox(I18n.t(key(".field.tls.required")));
        debugCheckbox = new Checkbox(I18n.t(key(".field.debug")));

        defaultSenderField = new EmailField(I18n.t(key(".field.defaultSender")));
        defaultSenderField.setPlaceholder(I18n.t(key(".field.defaultSender.placeholder")));
        defaultSenderField.setHelperText(I18n.t(key(".field.defaultSender.helper")));
        defaultSenderField.setWidthFull();

        form.add(hostField, portField, transportProtocolField, smtpAuthField,
                smtpStarttlsEnableCheckbox, smtpStarttlsRequiredCheckbox, debugCheckbox, defaultSenderField);
        form.setColspan(hostField, 2);
        form.setColspan(defaultSenderField, 2);
        return form;
    }

    private FormLayout buildCredentialsForm() {
        FormLayout form = DialogLayout.responsiveForm();

        usernameField = new TextField(I18n.t(key(".field.username")));
        usernameField.setPlaceholder(I18n.t(key(".field.username.placeholder")));
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setWidthFull();

        passwordField = new PasswordField(I18n.t(key(".field.password")));
        passwordField.setPlaceholder(I18n.t(key(".field.password.placeholder")));
        if (passwordRequired()) {
            passwordField.setRequiredIndicatorVisible(true);
        } else {
            passwordField.setHelperText(I18n.t(key(".field.password.helper")));
        }
        passwordField.setWidthFull();

        form.add(usernameField, passwordField);
        form.setColspan(usernameField, 2);
        form.setColspan(passwordField, 2);
        return form;
    }

    /** Copies the form values into the DTO ({@code id}, {@code code} and {@code tenant} are left untouched). */
    private void applyTo(SenderConfigDto dto) {
        String defaultSender = defaultSenderField.getValue();
        dto.setName(nameField.getValue().trim());
        dto.setDescription(descriptionField.getValue() != null ? descriptionField.getValue().trim() : null);
        dto.setHost(hostField.getValue().trim());
        dto.setPort(portField.getValue().trim());
        dto.setUsername(usernameField.getValue().trim());
        dto.setPassword(passwordField.getValue() != null && !passwordField.getValue().isBlank()
                ? passwordField.getValue() : existingPassword());
        dto.setTransportProtocol(transportProtocolField.getValue() != null
                ? transportProtocolField.getValue().trim() : "smtp");
        dto.setSmtpAuth(smtpAuthField.getValue() != null ? smtpAuthField.getValue().trim() : "true");
        dto.setSmtpStarttlsEnable(smtpStarttlsEnableCheckbox.getValue());
        dto.setSmtpStarttlsRequired(smtpStarttlsRequiredCheckbox.getValue());
        dto.setDebug(debugCheckbox.getValue());
        dto.setDefaultSender(defaultSender != null && !defaultSender.isBlank() ? defaultSender.trim() : null);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValid() {
        if (tenantEditable() && isBlank(tenantField.getValue())) {
            append(I18n.t(key(".error.tenant.required")));
            return false;
        }
        if (isBlank(nameField.getValue())) {
            append(I18n.t(key(".error.name.required")));
            return false;
        }
        if (isBlank(hostField.getValue())) {
            append(I18n.t(key(".error.host.required")));
            return false;
        }
        if (isBlank(portField.getValue())) {
            append(I18n.t(key(".error.port.required")));
            return false;
        }
        if (isBlank(usernameField.getValue())) {
            append(I18n.t(key(".error.username.required")));
            return false;
        }
        if (passwordRequired() && isBlank(passwordField.getValue())) {
            append(I18n.t(key(".error.password.required")));
            return false;
        }
        String defaultSender = defaultSenderField.getValue();
        if (!isBlank(defaultSender) && !MmsDialogSupport.isValidEmail(defaultSender)) {
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

        if (parentView != null) {
            parentView.showLoading(true);
        }
        try {
            SenderConfigDto dto = newDto();
            applyTo(dto);

            ResponseEntity<SenderConfigDto> response = persist(dto);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t(key(".failed"),
                        response.getBody() != null ? response.getBody().toString() : I18n.t("mms.common.error.unknown")));
                return false;
            }

            append(I18n.t(key(".success")));
            return true;
        } catch (FeignException ex) {
            append(I18n.t(key(".error"), MmsDialogSupport.errorMessage(ex)));
            log.error("Failed to save sender config", ex);
        } catch (Exception e) {
            append(I18n.t(key(".error"), e.getMessage()));
            log.error("Failed to save sender config", e);
        } finally {
            if (parentView != null) {
                parentView.showLoading(false);
            }
        }
        return false;
    }
}
