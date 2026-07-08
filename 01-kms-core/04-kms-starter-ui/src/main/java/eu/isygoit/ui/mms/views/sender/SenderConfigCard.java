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
import eu.isygoit.ui.mms.views.sender.dialog.ViewSenderConfigDialog;
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
    private Span defaultSenderSpan;
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
        defaultSenderSpan = new Span();

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
                    config.setDefaultSender(updatedConfig.getDefaultSender());

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
        if (defaultSenderSpan == null) {
            defaultSenderSpan = new Span();
        }

        String displayName = config.getHost() != null ? config.getHost() : I18n.t("mms.sender.card.fallback.name", config.getId());
        titleSpan.setText(displayName);
        titleSpan.getElement().setAttribute("title", displayName);

        statusChip.setText(isActive() ? I18n.t("mms.sender.card.status.active") : I18n.t("mms.sender.card.status.inactive"));
        applyChipColor(statusChip, isActive() ? ChipColor.SUCCESS : ChipColor.WARNING);

        hostSpan.setText(config.getHost() != null ? config.getHost() : I18n.t("mms.common.value.notAvailable"));
        portSpan.setText(config.getPort() != null ? config.getPort() : I18n.t("mms.common.value.notAvailable"));
        usernameSpan.setText(config.getUsername() != null ? config.getUsername() : I18n.t("mms.common.value.notAvailable"));
        defaultSenderSpan.setText(config.getDefaultSender() != null ? config.getDefaultSender() : I18n.t("mms.common.value.notAvailable"));

        boolean tlsEnabled = Boolean.TRUE.equals(config.getSmtpStarttlsEnable());
        tlsSpan.setText(tlsEnabled ? I18n.t("mms.sender.card.tls.enabled") : I18n.t("mms.sender.card.tls.disabled"));
        tlsSpan.removeClassName("detail-value--enabled");
        tlsSpan.removeClassName("detail-value--disabled");
        tlsSpan.addClassName(tlsEnabled ? "detail-value--enabled" : "detail-value--disabled");

        boolean debugEnabled = Boolean.TRUE.equals(config.getDebug());
        debugSpan.setText(debugEnabled ? I18n.t("mms.sender.card.debug.on") : I18n.t("mms.sender.card.debug.off"));
        debugSpan.removeClassName("detail-value--warning");
        debugSpan.removeClassName("detail-value--muted");
        debugSpan.addClassName(debugEnabled ? "detail-value--warning" : "detail-value--muted");

        if (testButton != null) {
            testButton.setEnabled(isActive());
            testButton.setTooltipText(isActive() ?
                    I18n.t("mms.sender.card.test.tooltip") :
                    I18n.t("mms.sender.card.test.disabled.tooltip"));
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
        left.addClassName("wams-title-row");

        // Tenant badge
        if (config.getTenant() != null) {
            Span tenantBadge = new Span(config.getTenant());
            tenantBadge.addClassName(LumoUtility.Background.CONTRAST_5);
            tenantBadge.addClassName(LumoUtility.Padding.XSMALL);
            tenantBadge.addClassName(LumoUtility.BorderRadius.SMALL);
            tenantBadge.addClassName(LumoUtility.FontSize.XSMALL);
            tenantBadge.addClassName("wams-tenant-badge");
            left.add(tenantBadge);
        }

        // Title
        String displayName = config.getHost() != null ? config.getHost() : I18n.t("mms.sender.card.fallback.name", config.getId());
        titleSpan = buildTitleSpan(displayName, displayName);
        left.add(titleSpan);

        // Status chip
        String statusText = isActive() ? I18n.t("mms.sender.card.status.active") : I18n.t("mms.sender.card.status.inactive");
        statusChip = buildStatusChip(statusText, statusText);
        applyChipColor(statusChip, isActive() ? ChipColor.SUCCESS : ChipColor.WARNING);
        left.add(statusChip);

        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        List<Button> buttons = new ArrayList<>();

        // View Button
        Button viewBtn = createIconButton(VaadinIcon.EYE, I18n.t("mms.sender.card.view.tooltip"));
        viewBtn.addClickListener(e -> viewConfig());
        buttons.add(viewBtn);

        // Test Connection Button
        testButton = createIconButton(VaadinIcon.START_COG, I18n.t("mms.sender.card.test.tooltip"));
        testButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        testButton.addClickListener(e -> testConnection());
        testButton.setEnabled(isActive());
        buttons.add(testButton);

        // Edit Button
        editButton = createIconButton(VaadinIcon.EDIT, I18n.t("mms.sender.card.edit.tooltip"));
        editButton.addClickListener(e -> openEditDialog());
        buttons.add(editButton);

        // Delete Button
        deleteButton = createIconButton(VaadinIcon.TRASH, I18n.t("mms.sender.card.delete.tooltip"));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> openDeleteDialog());
        buttons.add(deleteButton);

        return buttons;
    }

    // Add this method
    private void viewConfig() {
        new ViewSenderConfigDialog(config).open();
    }

    @Override
    protected void buildBodyRows() {
        bodyContainer.removeAll();

        // Tenant
        bodyContainer.add(createDetailRow(
                VaadinIcon.GLOBE,
                I18n.t("mms.sender.card.tenant"),
                config.getTenant() != null ? config.getTenant() : I18n.t("mms.common.value.notAvailable")
        ));

        // Host with copy
        String hostValue = config.getHost() != null ? config.getHost() : I18n.t("mms.common.value.notAvailable");
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.SERVER,
                I18n.t("mms.sender.card.host"),
                hostValue,
                hostValue
        ));

        // Port
        String portValue = config.getPort() != null ? config.getPort() : I18n.t("mms.common.value.notAvailable");
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.COG,
                I18n.t("mms.sender.card.port"),
                portValue,
                portValue
        ));

        // Username
        String usernameValue = config.getUsername() != null ? config.getUsername() : I18n.t("mms.common.value.notAvailable");
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.USER,
                I18n.t("mms.sender.card.username"),
                usernameValue,
                usernameValue
        ));

        // Transport Protocol
        bodyContainer.add(createDetailRow(
                VaadinIcon.CLOUD,
                I18n.t("mms.sender.card.protocol"),
                config.getTransportProtocol() != null ? config.getTransportProtocol() : "smtp"
        ));

        // SMTP Auth
        bodyContainer.add(createDetailRow(
                VaadinIcon.SHIELD,
                I18n.t("mms.sender.card.smtp.auth"),
                config.getSmtpAuth() != null ? config.getSmtpAuth() : "true"
        ));

        // TLS
        boolean tlsEnabled = Boolean.TRUE.equals(config.getSmtpStarttlsEnable());
        tlsSpan.setText(tlsEnabled ? I18n.t("mms.sender.card.tls.enabled") : I18n.t("mms.sender.card.tls.disabled"));
        tlsSpan.removeClassName("detail-value--enabled");
        tlsSpan.removeClassName("detail-value--disabled");
        tlsSpan.addClassName(tlsEnabled ? "detail-value--enabled" : "detail-value--disabled");
        bodyContainer.add(createDetailRow(
                VaadinIcon.LOCK,
                I18n.t("mms.sender.card.tls"),
                tlsSpan
        ));

        // TLS Required
        bodyContainer.add(createDetailRow(
                VaadinIcon.CHECK_CIRCLE,
                I18n.t("mms.sender.card.tls.required"),
                Boolean.TRUE.equals(config.getSmtpStarttlsRequired()) ? I18n.t("mms.sender.card.tls.required.yes") : I18n.t("mms.sender.card.tls.required.no")
        ));

        // Debug
        boolean debugEnabled = Boolean.TRUE.equals(config.getDebug());
        debugSpan.setText(debugEnabled ? I18n.t("mms.sender.card.debug.on") : I18n.t("mms.sender.card.debug.off"));
        debugSpan.removeClassName("detail-value--warning");
        debugSpan.removeClassName("detail-value--muted");
        debugSpan.addClassName(debugEnabled ? "detail-value--warning" : "detail-value--muted");
        bodyContainer.add(createDetailRow(
                VaadinIcon.BUG,
                I18n.t("mms.sender.card.debug"),
                debugSpan
        ));

        // Default Sender
        String defaultSenderValue = config.getDefaultSender() != null ? config.getDefaultSender() : I18n.t("mms.common.value.notAvailable");
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.ENVELOPE,
                I18n.t("mms.sender.card.defaultSender"),
                defaultSenderValue,
                defaultSenderValue
        ));
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, String value) {
        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.addClassName("detail-value");
        return createDetailRow(icon, label, valueSpan);
    }

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, Span valueSpan) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("detail-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("detail-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("detail-label");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private HorizontalLayout createDetailRowWithCopy(VaadinIcon icon, String label, String value, String copyValue) {
        HorizontalLayout row = createDetailRow(icon, label, value);

        Button copyBtn = new Button(new Icon(VaadinIcon.COPY));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        copyBtn.setTooltipText(I18n.t("mms.sender.card.copy.tooltip"));
        copyBtn.addClassName("detail-copy-button");
        copyBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.getPage().executeJs(
                    "navigator.clipboard.writeText($0)",
                    copyValue
            ));
        });

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
}