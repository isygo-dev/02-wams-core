package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
@UIScope
public class EditSenderConfigDialog extends Dialog {

    private final SenderConfigService senderConfigService;
    private final SenderConfigDto originalConfig;
    private final Runnable onSuccess;

    private final TextField tenantField = new TextField(I18n.t("sender.dialog.tenant"));
    private final TextField hostField = new TextField(I18n.t("sender.dialog.host"));
    private final TextField portField = new TextField(I18n.t("sender.dialog.port"));
    private final TextField usernameField = new TextField(I18n.t("sender.dialog.username"));
    private final PasswordField passwordField = new PasswordField(I18n.t("sender.dialog.password"));
    private final TextField transportProtocolField = new TextField(I18n.t("sender.dialog.transport.protocol"));
    private final TextField smtpAuthField = new TextField(I18n.t("sender.dialog.smtp.auth"));
    private final Checkbox smtpStarttlsEnable = new Checkbox(I18n.t("sender.dialog.smtp.starttls.enable"));
    private final Checkbox smtpStarttlsRequired = new Checkbox(I18n.t("sender.dialog.smtp.starttls.required"));
    private final Checkbox debugField = new Checkbox(I18n.t("sender.dialog.debug"));

    private final Button saveButton = new Button(I18n.t("sender.dialog.save"));
    private final Button cancelButton = new Button(I18n.t("sender.dialog.cancel"));

    public EditSenderConfigDialog(SenderConfigService senderConfigService,
                                  SenderConfigDto config,
                                  Runnable onSuccess) {
        this.senderConfigService = senderConfigService;
        this.originalConfig = config;
        this.onSuccess = onSuccess;

        setHeaderTitle(I18n.t("sender.dialog.edit.title", config.getId()));
        setWidth("600px");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        populateFields();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();

        layout.add(buildForm());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setSpacing(true);

        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> close());

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> updateConfig());

        buttons.add(cancelButton, saveButton);
        layout.add(buttons);

        add(layout);
    }

    private void populateFields() {
        tenantField.setValue(originalConfig.getTenant() != null ? originalConfig.getTenant() : "");
        hostField.setValue(originalConfig.getHost() != null ? originalConfig.getHost() : "");
        portField.setValue(originalConfig.getPort() != null ? originalConfig.getPort() : "");
        usernameField.setValue(originalConfig.getUsername() != null ? originalConfig.getUsername() : "");
        passwordField.setValue(originalConfig.getPassword() != null ? originalConfig.getPassword() : "");
        transportProtocolField.setValue(originalConfig.getTransportProtocol() != null ? originalConfig.getTransportProtocol() : "smtp");
        smtpAuthField.setValue(originalConfig.getSmtpAuth() != null ? originalConfig.getSmtpAuth() : "true");
        smtpStarttlsEnable.setValue(originalConfig.getSmtpStarttlsEnable() != null ? originalConfig.getSmtpStarttlsEnable() : false);
        smtpStarttlsRequired.setValue(originalConfig.getSmtpStarttlsRequired() != null ? originalConfig.getSmtpStarttlsRequired() : false);
        debugField.setValue(originalConfig.getDebug() != null ? originalConfig.getDebug() : false);

        tenantField.setReadOnly(true); // Tenant cannot be changed
    }

    private VerticalLayout buildForm() {
        VerticalLayout form = new VerticalLayout();
        form.setSpacing(true);
        form.setPadding(false);

        tenantField.setWidthFull();
        tenantField.setRequired(true);
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setTooltipText(I18n.t("sender.dialog.tenant.tooltip"));

        hostField.setWidthFull();
        hostField.setRequired(true);
        hostField.setRequiredIndicatorVisible(true);

        portField.setWidthFull();
        portField.setRequired(true);
        portField.setRequiredIndicatorVisible(true);

        usernameField.setWidthFull();
        usernameField.setRequired(true);
        usernameField.setRequiredIndicatorVisible(true);

        passwordField.setWidthFull();
        passwordField.setRequired(true);
        passwordField.setRequiredIndicatorVisible(true);

        transportProtocolField.setWidthFull();
        smtpAuthField.setWidthFull();

        HorizontalLayout tlsLayout = new HorizontalLayout();
        tlsLayout.setSpacing(true);
        tlsLayout.setWidthFull();
        tlsLayout.add(smtpStarttlsEnable, smtpStarttlsRequired);

        form.add(
                tenantField,
                hostField,
                portField,
                usernameField,
                passwordField,
                transportProtocolField,
                smtpAuthField,
                tlsLayout,
                debugField
        );

        return form;
    }

    private void updateConfig() {
        if (!validateForm()) {
            return;
        }

        SenderConfigDto dto = new SenderConfigDto();
        dto.setId(originalConfig.getId());
        dto.setTenant(tenantField.getValue());
        dto.setHost(hostField.getValue());
        dto.setPort(portField.getValue());
        dto.setUsername(usernameField.getValue());
        dto.setPassword(passwordField.getValue());
        dto.setTransportProtocol(transportProtocolField.getValue());
        dto.setSmtpAuth(smtpAuthField.getValue());
        dto.setSmtpStarttlsEnable(smtpStarttlsEnable.getValue());
        dto.setSmtpStarttlsRequired(smtpStarttlsRequired.getValue());
        dto.setDebug(debugField.getValue());

        try {
            senderConfigService.update(dto.getId(), dto);
            Notification.show(I18n.t("sender.update.success"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
            if (onSuccess != null) {
                onSuccess.run();
            }
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 400 ? I18n.t("sender.update.validation.error") : ex.getMessage();
            Notification.show(I18n.t("sender.update.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification.show(I18n.t("sender.update.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (hostField.getValue() == null || hostField.getValue().isEmpty()) {
            errors.append(I18n.t("sender.validation.host.required")).append("\n");
        }
        if (portField.getValue() == null || portField.getValue().isEmpty()) {
            errors.append(I18n.t("sender.validation.port.required")).append("\n");
        }
        if (usernameField.getValue() == null || usernameField.getValue().isEmpty()) {
            errors.append(I18n.t("sender.validation.username.required")).append("\n");
        }
        if (passwordField.getValue() == null || passwordField.getValue().isEmpty()) {
            errors.append(I18n.t("sender.validation.password.required")).append("\n");
        }

        if (errors.length() > 0) {
            Notification.show(errors.toString(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        return true;
    }
}