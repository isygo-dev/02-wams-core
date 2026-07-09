package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ViewSenderConfigDialog extends Dialog {

    private final SenderConfigDto config;

    public ViewSenderConfigDialog(SenderConfigDto config) {
        this.config = config;

        setHeaderTitle(I18n.t("mms.sender.dialog.view.title",
                config.getName() != null ? config.getName() : config.getId()));
        setWidth("600px");
        setMaxWidth("95vw");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        buildContent();
    }

    private void buildContent() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(true);
        mainLayout.setSpacing(true);
        mainLayout.setWidthFull();

        // Config details
        Div detailsDiv = new Div();
        detailsDiv.addClassName("wams-view-details");

        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.id"), config.getId().toString());
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.code"),
                config.getCode() != null ? config.getCode() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.tenant"),
                config.getTenant() != null ? config.getTenant() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.name"),
                config.getName() != null ? config.getName() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.description"),
                config.getDescription() != null ? config.getDescription() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.host"),
                config.getHost() != null ? config.getHost() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.port"),
                config.getPort() != null ? config.getPort() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.username"),
                config.getUsername() != null ? config.getUsername() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.protocol"),
                config.getTransportProtocol() != null ? config.getTransportProtocol() : "smtp");
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.smtp.auth"),
                config.getSmtpAuth() != null ? config.getSmtpAuth() : "true");

        // TLS Status
        boolean tlsEnabled = Boolean.TRUE.equals(config.getSmtpStarttlsEnable());
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.tls.enable"),
                tlsEnabled ? I18n.t("mms.sender.dialog.view.tls.enabled") : I18n.t("mms.sender.dialog.view.tls.disabled"));

        boolean tlsRequired = Boolean.TRUE.equals(config.getSmtpStarttlsRequired());
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.tls.required"),
                tlsRequired ? I18n.t("mms.sender.dialog.view.tls.required.yes") : I18n.t("mms.sender.dialog.view.tls.required.no"));

        // Debug Status
        boolean debugEnabled = Boolean.TRUE.equals(config.getDebug());
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.debug"),
                debugEnabled ? I18n.t("mms.sender.dialog.view.debug.on") : I18n.t("mms.sender.dialog.view.debug.off"));

        // Default Sender
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.defaultSender"),
                config.getDefaultSender() != null ? config.getDefaultSender() : I18n.t("mms.common.value.notAvailable"));

        // Status
        boolean isActive = config.getSmtpStarttlsEnable() != null && config.getSmtpStarttlsEnable();
        Span statusSpan = new Span(isActive ? I18n.t("mms.sender.dialog.view.status.active") : I18n.t("mms.sender.dialog.view.status.inactive"));
        statusSpan.addClassName(isActive ? "wams-status-active" : "wams-status-inactive");
        addDetailRow(detailsDiv, I18n.t("mms.sender.dialog.view.status"), statusSpan);

        mainLayout.add(detailsDiv);

        // Actions
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actions.setSpacing(true);

        // Close button
        Button closeBtn = new Button(I18n.t("common.dialog.close"), e -> close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actions.add(closeBtn);

        mainLayout.add(actions);
        add(mainLayout);
    }

    private void addDetailRow(Div container, String label, String value) {
        Div row = new Div();
        row.addClassName("wams-view-detail-row");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName("wams-view-detail-label");

        Span valueSpan = new Span(value != null ? value : I18n.t("mms.common.value.notAvailable"));
        valueSpan.addClassName("wams-view-detail-value");

        row.add(labelSpan, valueSpan);
        container.add(row);
    }

    private void addDetailRow(Div container, String label, Span valueSpan) {
        Div row = new Div();
        row.addClassName("wams-view-detail-row");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName("wams-view-detail-label");

        row.add(labelSpan, valueSpan);
        container.add(row);
    }
}