package eu.isygoit.ui.mms.views.sender;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.mms.views.sender.dialog.DeleteSenderConfigDialog;
import eu.isygoit.ui.mms.views.sender.dialog.EditSenderConfigDialog;
import eu.isygoit.ui.mms.views.sender.dialog.TestConnectionDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
class SenderConfigCard extends BaseCard<SenderConfigManagementView, SenderConfigService> {

    private final SenderConfigDto config;
    private final Runnable onRefresh;

    // Dedicated body container – cleared and rebuilt on refresh
    private final VerticalLayout bodyContainer = new VerticalLayout();

    // UI components (updated on refresh)
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

    public SenderConfigCard(SenderConfigManagementView parentView,
                            SenderConfigService senderConfigService,
                            SenderConfigDto config,
                            Runnable onRefresh) {
        super(parentView, senderConfigService);
        this.config = config;
        this.onRefresh = onRefresh;

        // Initialize spans to avoid NPE
        hostSpan = new Span();
        portSpan = new Span();
        usernameSpan = new Span();
        tlsSpan = new Span();
        debugSpan = new Span();

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

    // ─── Refresh – fully reloads the card ──────────────────────────────────

    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                ResponseEntity<SenderConfigDto> response = objectService.findById(config.getId());
                if (response.getBody() != null) {
                    SenderConfigDto updatedConfig = response.getBody();
                    config.setHost(updatedConfig.getHost());
                    config.setPort(updatedConfig.getPort());
                    config.setUsername(updatedConfig.getUsername());
                    config.setTenant(updatedConfig.getTenant());
                    config.setSmtpStarttlsEnable(updatedConfig.getSmtpStarttlsEnable());
                    config.setSmtpStarttlsRequired(updatedConfig.getSmtpStarttlsRequired());
                    config.setDebug(updatedConfig.getDebug());
                    config.setTransportProtocol(updatedConfig.getTransportProtocol());
                    config.setSmtpAuth(updatedConfig.getSmtpAuth());

                    updateDisplay();
                }
            } catch (Exception e) {
                log.error("Failed to refresh sender config card for {}", config.getId(), e);
            }
        }));
    }

    private void updateDisplay() {
        // Null check for all span components
        if (titleSpan == null) {
            titleSpan = buildTitleSpan("", "");
        }
        if (statusChip == null) {
            statusChip = buildStatusChip("", "");
        }
        if (hostSpan == null) {
            hostSpan = new Span();
        }
        if (portSpan == null) {
            portSpan = new Span();
        }
        if (usernameSpan == null) {
            usernameSpan = new Span();
        }
        if (tlsSpan == null) {
            tlsSpan = new Span();
        }
        if (debugSpan == null) {
            debugSpan = new Span();
        }

        String displayName = config.getHost() != null ? config.getHost() : "Sender " + config.getId();
        titleSpan.setText(displayName);
        titleSpan.getElement().setAttribute("title", displayName);

        ChipColor color = isActive() ? ChipColor.SUCCESS : ChipColor.WARNING;
        statusChip.setText(isActive() ? "Active" : "Inactive");
        statusChip.getStyle()
                .set("background-color", color.background())
                .set("color", color.foreground());

        hostSpan.setText(config.getHost() != null ? config.getHost() : "N/A");
        portSpan.setText(config.getPort() != null ? config.getPort() : "N/A");
        usernameSpan.setText(config.getUsername() != null ? config.getUsername() : "N/A");

        boolean tlsEnabled = Boolean.TRUE.equals(config.getSmtpStarttlsEnable());
        tlsSpan.setText(tlsEnabled ? "✓ Enabled" : "✗ Disabled");
        tlsSpan.getStyle().set("color", tlsEnabled ?
                "var(--lumo-success-color)" : "var(--lumo-error-color)");

        boolean debugEnabled = Boolean.TRUE.equals(config.getDebug());
        debugSpan.setText(debugEnabled ? "✓ On" : "✗ Off");
        debugSpan.getStyle().set("color", debugEnabled ?
                "var(--lumo-warning-color)" : "var(--lumo-tertiary-text-color)");

        if (testButton != null) {
            testButton.setEnabled(isActive());
            testButton.setTooltipText(isActive() ?
                    I18n.t("sender.card.test.tooltip") :
                    I18n.t("sender.card.test.disabled.tooltip"));
        }

        bodyContainer.removeAll();
        buildBodyRows();
    }

    // ─── BaseCard Implementation ─────────────────────────────────────────────

    @Override
    protected String cardCssClassName() {
        return "sender-config-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");

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
        statusChip = buildStatusChip(isActive() ? "Active" : "Inactive", isActive() ? "Active" : "Inactive");
        ChipColor color = isActive() ? ChipColor.SUCCESS : ChipColor.WARNING;
        statusChip.getStyle()
                .set("background-color", color.background())
                .set("color", color.foreground());
        left.add(statusChip);

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
        deleteButton.addClickListener(e -> openDeleteDialog());
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

        // Host with copy
        String hostValue = config.getHost() != null ? config.getHost() : "N/A";
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.SERVER,
                I18n.t("sender.card.host"),
                hostValue,
                hostValue
        ));

        // Port
        String portValue = config.getPort() != null ? config.getPort() : "N/A";
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.COG,
                I18n.t("sender.card.port"),
                portValue,
                portValue
        ));

        // Username
        String usernameValue = config.getUsername() != null ? config.getUsername() : "N/A";
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.USER,
                I18n.t("sender.card.username"),
                usernameValue,
                usernameValue
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
        boolean tlsEnabled = Boolean.TRUE.equals(config.getSmtpStarttlsEnable());
        tlsSpan.setText(tlsEnabled ? "✓ Enabled" : "✗ Disabled");
        tlsSpan.getStyle().set("color", tlsEnabled ?
                "var(--lumo-success-color)" : "var(--lumo-error-color)");
        bodyContainer.add(createDetailRow(
                VaadinIcon.LOCK,
                I18n.t("sender.card.tls"),
                tlsSpan
        ));

        // TLS Required
        bodyContainer.add(createDetailRow(
                VaadinIcon.CHECK_CIRCLE,
                I18n.t("sender.card.tls.required"),
                Boolean.TRUE.equals(config.getSmtpStarttlsRequired()) ? "✓ Required" : "✗ Not Required"
        ));

        // Debug
        boolean debugEnabled = Boolean.TRUE.equals(config.getDebug());
        debugSpan.setText(debugEnabled ? "✓ On" : "✗ Off");
        debugSpan.getStyle().set("color", debugEnabled ?
                "var(--lumo-warning-color)" : "var(--lumo-tertiary-text-color)");
        bodyContainer.add(createDetailRow(
                VaadinIcon.BUG,
                I18n.t("sender.card.debug"),
                debugSpan
        ));
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, String value) {
        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");
        return createDetailRow(icon, label, valueSpan);
    }

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, Span valueSpan) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("margin-top", "var(--lumo-space-xs)");
        row.addClassName("detail-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "120px");

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
        new TestConnectionDialog(objectService, config, this::refresh).open();
    }

    private void openEditDialog() {
        new EditSenderConfigDialog(
                objectService,
                config,
                () -> {
                    refresh();
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                }
        ).open();
    }

    private void openDeleteDialog() {
        new DeleteSenderConfigDialog(parentView, objectService, config, () -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
            getParent().ifPresent(parent -> {
                if (parent instanceof VerticalLayout layout) {
                    layout.remove(this);
                }
            });
        }).open();
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCardAttach(AttachEvent event) {
        // No additional lifecycle actions needed
    }

    // ─── Extra Styles ──────────────────────────────────────────────────────

    @Override
    protected String buildExtraStyles() {
        return """
                .sender-config-card .detail-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .sender-config-card .detail-row:last-child {
                    border-bottom: none;
                }
                .sender-config-card .detail-row .detail-value {
                    font-weight: 500;
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