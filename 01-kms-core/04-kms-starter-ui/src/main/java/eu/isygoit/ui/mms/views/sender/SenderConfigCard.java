package eu.isygoit.ui.mms.views.sender;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.mms.views.sender.dialog.EditSenderConfigDialog;
import eu.isygoit.ui.mms.views.sender.dialog.TestConnectionDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Card component for displaying and managing a Sender Configuration.
 */
@Slf4j
public class SenderConfigCard extends BaseCard<SenderConfigManagementView, SenderConfigService> {

    private final SenderConfigDto config;
    private final Runnable onRefresh;

    // UI Components
    private Span titleSpan;
    private Span statusChip;
    private Span hostSpan;
    private Span portSpan;
    private Span usernameSpan;
    private Span tlsSpan;
    private Span debugSpan;
    private Button testButton;
    private Button editButton;
    private Button deleteButton;

    private final VerticalLayout bodyContainer = new VerticalLayout();

    public SenderConfigCard(SenderConfigManagementView parentView,
                            SenderConfigService senderConfigService,
                            SenderConfigDto config,
                            Runnable onRefresh) {
        super(parentView, senderConfigService);
        this.config = config;
        this.onRefresh = onRefresh;

        // Setup body container
        bodyContainer.setPadding(false);
        bodyContainer.setSpacing(true);
        bodyContainer.setWidthFull();
        bodyContainer.addClassName("sender-card-body");
        add(bodyContainer);

        initCard();
    }

    // ─── Public Accessors ─────────────────────────────────────────────────────

    public SenderConfigDto getConfig() {
        return config;
    }

    public Long getSenderConfigId() {
        return config.getId();
    }

    public String getHost() {
        return config.getHost();
    }

    public boolean isActive() {
        return config.getSmtpStarttlsEnable() != null && config.getSmtpStarttlsEnable();
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                // Reload config from server
                ResponseEntity<SenderConfigDto> response = objectService.findById(config.getId());
                if (response.getBody() != null) {
                    SenderConfigDto updatedConfig = response.getBody();
                    // Update fields
                    config.setHost(updatedConfig.getHost());
                    config.setPort(updatedConfig.getPort());
                    config.setUsername(updatedConfig.getUsername());
                    config.setTenant(updatedConfig.getTenant());
                    config.setSmtpStarttlsEnable(updatedConfig.getSmtpStarttlsEnable());
                    config.setSmtpStarttlsRequired(updatedConfig.getSmtpStarttlsRequired());
                    config.setDebug(updatedConfig.getDebug());
                    config.setTransportProtocol(updatedConfig.getTransportProtocol());
                    config.setSmtpAuth(updatedConfig.getSmtpAuth());

                    // Update UI
                    updateDisplay();
                }
            } catch (Exception e) {
                log.error("Failed to refresh sender config card for {}", config.getId(), e);
            }
        }));
    }

    private void updateDisplay() {
        // Update title
        String displayName = config.getHost() != null ? config.getHost() : "Sender " + config.getId();
        titleSpan.setText(displayName);
        titleSpan.getElement().setAttribute("title", displayName);

        // Update status chip
        String status = isActive() ? "Active" : "Inactive";
        ChipColor color = isActive() ? ChipColor.SUCCESS : ChipColor.WARNING;
        statusChip.setText(status);
        statusChip.getStyle()
                .set("background-color", color.background())
                .set("color", color.foreground());

        // Update details
        hostSpan.setText(config.getHost() != null ? config.getHost() : "N/A");
        portSpan.setText(config.getPort() != null ? config.getPort() : "N/A");
        usernameSpan.setText(config.getUsername() != null ? config.getUsername() : "N/A");
        tlsSpan.setText(Boolean.TRUE.equals(config.getSmtpStarttlsEnable()) ? "✓ Enabled" : "✗ Disabled");
        tlsSpan.getStyle().set("color", Boolean.TRUE.equals(config.getSmtpStarttlsEnable()) ?
                "var(--lumo-success-color)" : "var(--lumo-error-color)");
        debugSpan.setText(Boolean.TRUE.equals(config.getDebug()) ? "✓ On" : "✗ Off");
        debugSpan.getStyle().set("color", Boolean.TRUE.equals(config.getDebug()) ?
                "var(--lumo-warning-color)" : "var(--lumo-tertiary-text-color)");

        // Update test button
        testButton.setEnabled(isActive());
        testButton.setTooltipText(isActive() ?
                I18n.t("sender.card.test.tooltip") :
                I18n.t("sender.card.test.disabled.tooltip"));
    }

    // ─── BaseCard Implementation ─────────────────────────────────────────────

    @Override
    protected String cardCssClassName() {
        return "sender-config-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");
        left.getStyle().set("gap", "var(--lumo-space-xs)");

        // Tenant badge
        if (config.getTenant() != null) {
            Span tenantBadge = new Span(config.getTenant());
            tenantBadge.addClassName(LumoUtility.Background.CONTRAST_5);
            tenantBadge.addClassName(LumoUtility.Padding.XSMALL);
            tenantBadge.addClassName(LumoUtility.BorderRadius.SMALL);
            tenantBadge.addClassName(LumoUtility.FontSize.XSMALL);
            tenantBadge.getStyle().set("font-family", "monospace");
            left.add(tenantBadge);
        }

        // Title
        String displayName = config.getHost() != null ? config.getHost() : "Sender " + config.getId();
        titleSpan = buildTitleSpan(displayName, displayName);
        left.add(titleSpan);

        // Status chip
        String status = isActive() ? "Active" : "Inactive";
        statusChip = buildStatusChip(status, "Status: " + status);
        ChipColor color = isActive() ? ChipColor.SUCCESS : ChipColor.WARNING;
        statusChip.getStyle()
                .set("background-color", color.background())
                .set("color", color.foreground());
        left.add(statusChip);

        // ID badge
        Span idBadge = new Span("ID: " + config.getId());
        idBadge.addClassName(LumoUtility.FontSize.XSMALL);
        idBadge.addClassName(LumoUtility.TextColor.TERTIARY);
        idBadge.getStyle().set("font-family", "monospace");
        left.add(idBadge);

        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        List<Button> buttons = new ArrayList<>();

        // Test Connection Button
        testButton = createIconButton(VaadinIcon.START_COG, I18n.t("sender.card.test.tooltip"));
        testButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        testButton.addClickListener(e -> testConnection());
        testButton.setEnabled(isActive());
        buttons.add(testButton);

        // Edit Button
        editButton = createIconButton(VaadinIcon.EDIT, I18n.t("sender.card.edit.tooltip"));
        editButton.addClickListener(e -> openEditDialog());
        buttons.add(editButton);

        // Delete Button
        deleteButton = createIconButton(VaadinIcon.TRASH, I18n.t("sender.card.delete.tooltip"));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> confirmDelete());
        buttons.add(deleteButton);

        return buttons;
    }

    @Override
    protected void buildBodyRows() {
        bodyContainer.removeAll();

        // Tenant
        bodyContainer.add(createDetailRow(
                VaadinIcon.GLOBE,
                I18n.t("sender.card.tenant"),
                config.getTenant() != null ? config.getTenant() : "N/A"
        ));

        // Host
        hostSpan = new Span();
        bodyContainer.add(createDetailRow(
                VaadinIcon.SERVER,
                I18n.t("sender.card.host"),
                config.getHost() != null ? config.getHost() : "N/A"
        ));

        // Port
        portSpan = new Span();
        bodyContainer.add(createDetailRow(
                VaadinIcon.COG,
                I18n.t("sender.card.port"),
                config.getPort() != null ? config.getPort() : "N/A"
        ));

        // Username
        usernameSpan = new Span();
        bodyContainer.add(createDetailRow(
                VaadinIcon.USER,
                I18n.t("sender.card.username"),
                config.getUsername() != null ? config.getUsername() : "N/A"
        ));

        // Transport Protocol
        bodyContainer.add(createDetailRow(
                VaadinIcon.CLOUD,
                I18n.t("sender.card.protocol"),
                config.getTransportProtocol() != null ? config.getTransportProtocol() : "smtp"
        ));

        // SMTP Auth
        bodyContainer.add(createDetailRow(
                VaadinIcon.SHIELD,
                I18n.t("sender.card.smtp.auth"),
                config.getSmtpAuth() != null ? config.getSmtpAuth() : "true"
        ));

        // TLS
        tlsSpan = new Span();
        bodyContainer.add(createDetailRow(
                VaadinIcon.LOCK,
                I18n.t("sender.card.tls"),
                Boolean.TRUE.equals(config.getSmtpStarttlsEnable()) ? "✓ Enabled" : "✗ Disabled"
        ));

        // TLS Required
        bodyContainer.add(createDetailRow(
                VaadinIcon.CHECK_CIRCLE,
                I18n.t("sender.card.tls.required"),
                Boolean.TRUE.equals(config.getSmtpStarttlsRequired()) ? "✓ Required" : "✗ Not Required"
        ));

        // Debug
        debugSpan = new Span();
        bodyContainer.add(createDetailRow(
                VaadinIcon.BUG,
                I18n.t("sender.card.debug"),
                Boolean.TRUE.equals(config.getDebug()) ? "✓ On" : "✗ Off"
        ));
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("padding", "var(--lumo-space-xs) var(--lumo-space-s)");
        row.addClassName("detail-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");
        iconComponent.getStyle().set("min-width", "20px");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "120px");
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        // Store reference to value span for updates
        if (label.equals(I18n.t("sender.card.host"))) {
            hostSpan = valueSpan;
        } else if (label.equals(I18n.t("sender.card.port"))) {
            portSpan = valueSpan;
        } else if (label.equals(I18n.t("sender.card.username"))) {
            usernameSpan = valueSpan;
        } else if (label.equals(I18n.t("sender.card.tls"))) {
            tlsSpan = valueSpan;
        } else if (label.equals(I18n.t("sender.card.debug"))) {
            debugSpan = valueSpan;
        }

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);

        return row;
    }

    private HorizontalLayout createDetailRowWithCopy(VaadinIcon icon, String label, String value, String copyValue) {
        HorizontalLayout row = createDetailRow(icon, label, value);

        Button copyBtn = new Button(new Icon(VaadinIcon.COPY));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        copyBtn.setTooltipText(I18n.t("sender.card.copy.tooltip"));
        copyBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.getPage().executeJs(
                    "navigator.clipboard.writeText($0)",
                    copyValue
            ));
            // Show notification
        });
        copyBtn.getStyle().set("padding", "var(--lumo-space-xs)");

        row.add(copyBtn);
        return row;
    }

    // ─── Action Methods ─────────────────────────────────────────────────────

    private void testConnection() {
        if (!isActive()) {
            return;
        }
        TestConnectionDialog dialog = new TestConnectionDialog(objectService, config, this::refresh);
        dialog.open();
    }

    private void openEditDialog() {
        EditSenderConfigDialog dialog = new EditSenderConfigDialog(
                objectService,
                config,
                () -> {
                    refresh();
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                }
        );
        dialog.open();
    }

    private void confirmDelete() {
        // Simplified delete - in real implementation use a confirmation dialog
        try {
            objectService.delete(config.getId());
            if (onRefresh != null) {
                onRefresh.run();
            }
            // Remove card from parent
            getParent().ifPresent(parent -> {
                if (parent instanceof VerticalLayout layout) {
                    layout.remove(this);
                }
            });
        } catch (Exception e) {
            log.error("Failed to delete sender config {}", config.getId(), e);
        }
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCardAttach(AttachEvent event) {
        updateDisplay();
    }

    // ─── Extra Styles ──────────────────────────────────────────────────────

    @Override
    protected String buildExtraStyles() {
        return """
                .sender-config-card .detail-row {
                    border-bottom: 1px solid var(--lumo-contrast-5pct);
                }
                .sender-config-card .detail-row:last-child {
                    border-bottom: none;
                }
                .sender-config-card .detail-row:hover {
                    background: var(--lumo-primary-color-5pct);
                }
                @media (max-width: 640px) {
                    .sender-config-card .detail-row {
                        flex-wrap: wrap;
                    }
                    .sender-config-card .detail-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}