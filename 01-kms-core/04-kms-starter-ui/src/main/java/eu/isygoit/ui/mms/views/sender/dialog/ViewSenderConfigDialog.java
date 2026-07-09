package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import lombok.extern.slf4j.Slf4j;

/**
 * Read-only details dialog for a {@link SenderConfigDto}, split into sections
 * by data type (Identity, Configuration/Connection, Status) with a divider
 * between each — same convention as {@code ParameterDetailsDialog} and other
 * {@code *DetailsDialog} classes across the app.
 */
@Slf4j
public class ViewSenderConfigDialog extends NoActionDialog {

    private final SenderConfigDto config;

    public ViewSenderConfigDialog(SenderConfigDto config) {
        super(I18n.t("mms.sender.dialog.view.title",
                config.getName() != null ? config.getName() : config.getId()));
        this.config = config;

        setWidth("600px");
        setMaxWidth("95vw");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("sender-config-details-dialog");

        buildContent();
    }

    private void buildContent() {
        // ── Identity ─────────────────────────────────────────────────────────
        Div identityGrid = new Div();
        identityGrid.addClassName("wams-view-details");
        addDetailRow(identityGrid, I18n.t("mms.sender.dialog.view.id"), config.getId().toString());
        addDetailRow(identityGrid, I18n.t("mms.sender.dialog.view.code"),
                config.getCode() != null ? config.getCode() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(identityGrid, I18n.t("mms.sender.dialog.view.tenant"),
                config.getTenant() != null ? config.getTenant() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(identityGrid, I18n.t("mms.sender.dialog.view.name"),
                config.getName() != null ? config.getName() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(identityGrid, I18n.t("mms.sender.dialog.view.description"),
                config.getDescription() != null ? config.getDescription() : I18n.t("mms.common.value.notAvailable"));
        add(createSection(I18n.t("mms.sender.dialog.view.section.identity"), identityGrid));

        // ── Configuration / Connection ───────────────────────────────────────
        Div connectionGrid = new Div();
        connectionGrid.addClassName("wams-view-details");
        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.host"),
                config.getHost() != null ? config.getHost() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.port"),
                config.getPort() != null ? config.getPort() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.username"),
                config.getUsername() != null ? config.getUsername() : I18n.t("mms.common.value.notAvailable"));

        // Password – never render the raw secret, only whether one is configured
        boolean hasPassword = config.getPassword() != null && !config.getPassword().isEmpty();
        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.password"),
                hasPassword ? I18n.t("mms.sender.dialog.view.password.set") : I18n.t("mms.sender.dialog.view.password.notSet"));

        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.protocol"),
                config.getTransportProtocol() != null ? config.getTransportProtocol() : "smtp");
        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.smtp.auth"),
                config.getSmtpAuth() != null ? config.getSmtpAuth() : "true");

        // TLS Status
        boolean tlsEnabled = Boolean.TRUE.equals(config.getSmtpStarttlsEnable());
        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.tls.enable"),
                tlsEnabled ? I18n.t("mms.sender.dialog.view.tls.enabled") : I18n.t("mms.sender.dialog.view.tls.disabled"));

        boolean tlsRequired = Boolean.TRUE.equals(config.getSmtpStarttlsRequired());
        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.tls.required"),
                tlsRequired ? I18n.t("mms.sender.dialog.view.tls.required.yes") : I18n.t("mms.sender.dialog.view.tls.required.no"));

        // Debug Status
        boolean debugEnabled = Boolean.TRUE.equals(config.getDebug());
        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.debug"),
                debugEnabled ? I18n.t("mms.sender.dialog.view.debug.on") : I18n.t("mms.sender.dialog.view.debug.off"));

        // Default Sender
        addDetailRow(connectionGrid, I18n.t("mms.sender.dialog.view.defaultSender"),
                config.getDefaultSender() != null ? config.getDefaultSender() : I18n.t("mms.common.value.notAvailable"));
        add(createSection(I18n.t("mms.sender.dialog.view.section.configuration"), connectionGrid));

        // ── Status ───────────────────────────────────────────────────────────
        Div statusGrid = new Div();
        statusGrid.addClassName("wams-view-details");
        boolean isActive = config.getSmtpStarttlsEnable() != null && config.getSmtpStarttlsEnable();
        Span statusSpan = new Span(isActive ? I18n.t("mms.sender.dialog.view.status.active") : I18n.t("mms.sender.dialog.view.status.inactive"));
        statusSpan.addClassName(isActive ? "wams-status-active" : "wams-status-inactive");
        addDetailRow(statusGrid, I18n.t("mms.sender.dialog.view.status"), statusSpan);
        add(createSection(I18n.t("mms.sender.dialog.view.section.status"), statusGrid));
    }

    private Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName("wams-section-title");
        section.add(titleSpan, content);
        return section;
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