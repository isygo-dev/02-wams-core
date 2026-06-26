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

    private final VerticalLayout bodyContainer = new VerticalLayout();

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

        bodyContainer.setPadding(false);
        bodyContainer.setSpacing(true);
        bodyContainer.setWidthFull();
        bodyContainer.addClassName("sender-card-body");
        add(bodyContainer);

        initCard();
    }

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

        testButton.setEnabled(isActive());
        testButton.setTooltipText(isActive() ?
                I18n.t("sender.card.test.tooltip") :
                I18n.t("sender.card.test.disabled.tooltip"));

        bodyContainer.removeAll();
        buildBodyRows();
    }

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

        if (config.getTenant() != null) {
            Span tenantBadge = new Span(config.getTenant());
            tenantBadge.addClassName(LumoUtility.Background.CONTRAST_5);
            tenantBadge.addClassName(LumoUtility.Padding.XSMALL);
            tenantBadge.addClassName(LumoUtility.BorderRadius.SMALL);
            tenantBadge.addClassName(LumoUtility.FontSize.XSMALL);
            tenantBadge.getStyle().set("font-family", "monospace");
            left.add(tenantBadge);
        }

        String displayName = config.getHost() != null ? config.getHost() : "Sender " + config.getId();
        titleSpan = buildTitleSpan(displayName, displayName);
        left.add(titleSpan);

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

        testButton = createIconButton(VaadinIcon.START_COG, I18n.t("sender.card.test.tooltip"));
        testButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        testButton.addClickListener(e -> testConnection());
        testButton.setEnabled(isActive());
        buttons.add(testButton);

        editButton = createIconButton(VaadinIcon.EDIT, I18n.t("sender.card.edit.tooltip"));
        editButton.addClickListener(e -> openEditDialog());
        buttons.add(editButton);

        deleteButton = createIconButton(VaadinIcon.TRASH, I18n.t("sender.card.delete.tooltip"));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> openDeleteDialog());
        buttons.add(deleteButton);

        return buttons;
    }

    @Override
    protected void buildBodyRows() {
        bodyContainer.removeAll();

        bodyContainer.add(createDetailRow(
                VaadinIcon.GLOBE,
                I18n.t("sender.card.tenant"),
                config.getTenant() != null ? config.getTenant() : "N/A"
        ));

        String hostValue = config.getHost() != null ? config.getHost() : "N/A";
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.SERVER,
                I18n.t("sender.card.host"),
                hostValue,
                hostValue
        ));

        String portValue = config.getPort() != null ? config.getPort() : "N/A";
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.COG,
                I18n.t("sender.card.port"),
                portValue,
                portValue
        ));

        String usernameValue = config.getUsername() != null ? config.getUsername() : "N/A";
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.USER,
                I18n.t("sender.card.username"),
                usernameValue,
                usernameValue
        ));

        bodyContainer.add(createDetailRow(
                VaadinIcon.CLOUD,
                I18n.t("sender.card.protocol"),
                config.getTransportProtocol() != null ? config.getTransportProtocol() : "smtp"
        ));

        bodyContainer.add(createDetailRow(
                VaadinIcon.SHIELD,
                I18n.t("sender.card.smtp.auth"),
                config.getSmtpAuth() != null ? config.getSmtpAuth() : "true"
        ));

        tlsSpan = new Span();
        boolean tlsEnabled = Boolean.TRUE.equals(config.getSmtpStarttlsEnable());
        tlsSpan.setText(tlsEnabled ? "✓ Enabled" : "✗ Disabled");
        tlsSpan.getStyle().set("color", tlsEnabled ?
                "var(--lumo-success-color)" : "var(--lumo-error-color)");
        bodyContainer.add(createDetailRow(
                VaadinIcon.LOCK,
                I18n.t("sender.card.tls"),
                tlsSpan
        ));

        bodyContainer.add(createDetailRow(
                VaadinIcon.CHECK_CIRCLE,
                I18n.t("sender.card.tls.required"),
                Boolean.TRUE.equals(config.getSmtpStarttlsRequired()) ? "✓ Required" : "✗ Not Required"
        ));

        debugSpan = new Span();
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

    private void testConnection() {
        if (!isActive()) {
            return;
        }
        new TestConnectionDialog(objectService, config, this::refresh).open();
    }

    private void openEditDialog() {
        new EditSenderConfigDialog(objectService, config, () -> {
            refresh();
            if (onRefresh != null) {
                onRefresh.run();
            }
        }).open();
    }

    private void openDeleteDialog() {
        // Use DeleteSenderConfigDialog with PIN confirmation
        new DeleteSenderConfigDialog(
                parentView,           // SenderConfigManagementView
                objectService,        // SenderConfigService
                config,               // SenderConfigDto
                () -> {               // Runnable onSuccess
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                    // Remove card from parent
                    getParent().ifPresent(parent -> {
                        if (parent instanceof VerticalLayout layout) {
                            layout.remove(this);
                        }
                    });
                }
        ).open();
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        // No additional lifecycle actions needed
    }

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