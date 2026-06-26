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
public class CreateSenderConfigDialog extends Dialog {

    private final SenderConfigService senderConfigService;
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

    public CreateSenderConfigDialog(SenderConfigService senderConfigService, Runnable onSuccess) {
        this.senderConfigService = senderConfigService;
        this.onSuccess = onSuccess;

        setHeaderTitle(I18n.t("sender.dialog.create.title"));
        setWidth("600px");
        setModal(true);
        setDraggable(true);
        setResizable(true);

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
        saveButton.addClickListener(e -> saveConfig());

        buttons.add(cancelButton, saveButton);
        layout.add(buttons);

        add(layout);
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
        hostField.setTooltipText(I18n.t("sender.dialog.host.tooltip"));

        portField.setWidthFull();
        portField.setRequired(true);
        portField.setRequiredIndicatorVisible(true);
        portField.setTooltipText(I18n.t("sender.dialog.port.tooltip"));

        usernameField.setWidthFull();
        usernameField.setRequired(true);
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setTooltipText(I18n.t("sender.dialog.username.tooltip"));

        passwordField.setWidthFull();
        passwordField.setRequired(true);
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setTooltipText(I18n.t("sender.dialog.password.tooltip"));

        transportProtocolField.setWidthFull();
        transportProtocolField.setValue("smtp");
        transportProtocolField.setTooltipText(I18n.t("sender.dialog.transport.protocol.tooltip"));

        smtpAuthField.setWidthFull();
        smtpAuthField.setValue("true");
        smtpAuthField.setTooltipText(I18n.t("sender.dialog.smtp.auth.tooltip"));

        HorizontalLayout tlsLayout = new HorizontalLayout();
        tlsLayout.setSpacing(true);
        tlsLayout.setWidthFull();
        smtpStarttlsEnable.setValue(true);
        smtpStarttlsRequired.setValue(false);
        tlsLayout.add(smtpStarttlsEnable, smtpStarttlsRequired);

        debugField.setValue(false);

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

    private void saveConfig() {
        if (!validateForm()) {
            return;
        }

        SenderConfigDto dto = new SenderConfigDto();
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
            senderConfigService.create(dto);
            Notification.show(I18n.t("sender.create.success"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
            if (onSuccess != null) {
                onSuccess.run();
            }
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 400 ? I18n.t("sender.create.validation.error") : ex.getMessage();
            Notification.show(I18n.t("sender.create.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification.show(I18n.t("sender.create.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (tenantField.getValue() == null || tenantField.getValue().isEmpty()) {
            errors.append(I18n.t("sender.validation.tenant.required")).append("\n");
        }
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