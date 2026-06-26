package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.PasswordField;
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
    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField transportProtocolField;
    private TextField smtpAuthField;
    private Checkbox smtpStarttlsEnableCheckbox;
    private Checkbox smtpStarttlsRequiredCheckbox;
    private Checkbox debugCheckbox;

    public CreateSenderConfigDialog(SenderConfigManagementView parentView,
                                    SenderConfigService senderConfigService,
                                    Runnable onSuccess) {
        super(I18n.t("sender.dialog.create.title"), parentView, senderConfigService, onSuccess);
        setOkButtonText(I18n.t("sender.dialog.create.button"));
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

        tenantField = new TextField(I18n.t("sender.dialog.create.field.tenant"));
        tenantField.setPlaceholder(I18n.t("sender.dialog.create.field.tenant.placeholder"));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setWidthFull();

        hostField = new TextField(I18n.t("sender.dialog.create.field.host"));
        hostField.setPlaceholder(I18n.t("sender.dialog.create.field.host.placeholder"));
        hostField.setRequiredIndicatorVisible(true);
        hostField.setWidthFull();

        portField = new TextField(I18n.t("sender.dialog.create.field.port"));
        portField.setPlaceholder(I18n.t("sender.dialog.create.field.port.placeholder"));
        portField.setRequiredIndicatorVisible(true);
        portField.setWidthFull();

        usernameField = new TextField(I18n.t("sender.dialog.create.field.username"));
        usernameField.setPlaceholder(I18n.t("sender.dialog.create.field.username.placeholder"));
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setWidthFull();

        passwordField = new PasswordField(I18n.t("sender.dialog.create.field.password"));
        passwordField.setPlaceholder(I18n.t("sender.dialog.create.field.password.placeholder"));
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidthFull();

        transportProtocolField = new TextField(I18n.t("sender.dialog.create.field.protocol"));
        transportProtocolField.setPlaceholder(I18n.t("sender.dialog.create.field.protocol.placeholder"));
        transportProtocolField.setValue("smtp");
        transportProtocolField.setWidthFull();

        smtpAuthField = new TextField(I18n.t("sender.dialog.create.field.smtp.auth"));
        smtpAuthField.setPlaceholder(I18n.t("sender.dialog.create.field.smtp.auth.placeholder"));
        smtpAuthField.setValue("true");
        smtpAuthField.setWidthFull();

        smtpStarttlsEnableCheckbox = new Checkbox(I18n.t("sender.dialog.create.field.tls.enable"));
        smtpStarttlsEnableCheckbox.setValue(false);

        smtpStarttlsRequiredCheckbox = new Checkbox(I18n.t("sender.dialog.create.field.tls.required"));
        smtpStarttlsRequiredCheckbox.setValue(false);

        debugCheckbox = new Checkbox(I18n.t("sender.dialog.create.field.debug"));
        debugCheckbox.setValue(false);

        form.add(tenantField, hostField, portField, usernameField, passwordField,
                transportProtocolField, smtpAuthField,
                smtpStarttlsEnableCheckbox, smtpStarttlsRequiredCheckbox,
                debugCheckbox);

        // Set column spans
        form.setColspan(tenantField, 2);
        form.setColspan(hostField, 2);
        form.setColspan(usernameField, 2);
        form.setColspan(passwordField, 2);

        contentArea.add(form);
    }

    private void prefillData() {
        // Default values can be set here if needed
    }

    @Override
    protected boolean onOk() {
        clearErrors();

        // Validate required fields
        if (tenantField.getValue() == null || tenantField.getValue().isBlank()) {
            append(I18n.t("sender.dialog.create.error.tenant.required"));
            return false;
        }
        if (hostField.getValue() == null || hostField.getValue().isBlank()) {
            append(I18n.t("sender.dialog.create.error.host.required"));
            return false;
        }
        if (portField.getValue() == null || portField.getValue().isBlank()) {
            append(I18n.t("sender.dialog.create.error.port.required"));
            return false;
        }
        if (usernameField.getValue() == null || usernameField.getValue().isBlank()) {
            append(I18n.t("sender.dialog.create.error.username.required"));
            return false;
        }
        if (passwordField.getValue() == null || passwordField.getValue().isBlank()) {
            append(I18n.t("sender.dialog.create.error.password.required"));
            return false;
        }

        if (parentView != null) {
            parentView.showLoading(true);
        }
        try {
            SenderConfigDto config = SenderConfigDto.builder()
                    .tenant(tenantField.getValue().trim())
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
                    .build();

            ResponseEntity<SenderConfigDto> response = senderConfigService.create(config);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("sender.dialog.create.failed",
                        response.getBody() != null ? response.getBody().toString() : "unknown error"));
                return false;
            }

            showSuccess(I18n.t("sender.dialog.create.success"));
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ?
                    ex.contentUTF8() : ex.getMessage();
            append(I18n.t("sender.dialog.create.error", errorMsg));
            log.error("Failed to create sender config", ex);
        } catch (Exception e) {
            append(I18n.t("sender.dialog.create.error", e.getMessage()));
            log.error("Failed to create sender config", e);
        } finally {
            if (parentView != null) {
                parentView.showLoading(false);
            }
        }
        return false;
    }
}