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
import eu.isygoit.ui.mms.views.sender.dialog.SenderConfigDetailsViewDialog;
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
    private Span codeSpan;
    private Span nameSpan;
    private Span hostSpan;
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
        codeSpan = new Span();
        nameSpan = new Span();
        hostSpan = new Span();
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

    public String getCode() {
        return config.getCode();
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
                    config.setCode(updatedConfig.getCode());
                    config.setName(updatedConfig.getName());
                    config.setDescription(updatedConfig.getDescription());
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
        if (codeSpan == null) {
            codeSpan = new Span();
        }
        if (nameSpan == null) {
            nameSpan = new Span();
        }
        if (hostSpan == null) {
            hostSpan = new Span();
        }
        if (defaultSenderSpan == null) {
            defaultSenderSpan = new Span();
        }

        String displayName = config.getName() != null ? config.getName() :
                (config.getCode() != null ? config.getCode() : I18n.t("mms.sender.card.fallback.name", config.getId()));
        titleSpan.setText(displayName);
        titleSpan.getElement().setAttribute("title", displayName);

        statusChip.setText(isActive() ? I18n.t("mms.sender.card.status.active") : I18n.t("mms.sender.card.status.inactive"));
        applyChipColor(statusChip, isActive() ? ChipColor.SUCCESS : ChipColor.WARNING);

        codeSpan.setText(config.getCode() != null ? config.getCode() : I18n.t("mms.common.value.notAvailable"));
        nameSpan.setText(config.getName() != null ? config.getName() : I18n.t("mms.common.value.notAvailable"));
        hostSpan.setText(config.getHost() != null ? config.getHost() : I18n.t("mms.common.value.notAvailable"));
        defaultSenderSpan.setText(config.getDefaultSender() != null ? config.getDefaultSender() : I18n.t("mms.common.value.notAvailable"));

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

        // Code badge
        if (config.getCode() != null) {
            Span codeBadge = new Span(config.getCode());
            codeBadge.addClassName(LumoUtility.Background.CONTRAST_10);
            codeBadge.addClassName(LumoUtility.Padding.XSMALL);
            codeBadge.addClassName(LumoUtility.BorderRadius.SMALL);
            codeBadge.addClassName(LumoUtility.FontSize.XSMALL);
            codeBadge.addClassName("wams-code-badge");
            left.add(codeBadge);
        }

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

        // Title (name)
        String displayName = config.getName() != null ? config.getName() :
                (config.getCode() != null ? config.getCode() : I18n.t("mms.sender.card.fallback.name", config.getId()));
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

        // Details/View
        buttons.add(createDetailsButton(I18n.t("mms.sender.card.view.tooltip"), this::viewConfig));

        // Edit
        editButton = createEditButton(I18n.t("mms.sender.card.edit.tooltip"), this::openEditDialog);
        buttons.add(editButton);

        // Entity-specific extra: test connection
        testButton = createIconButton(VaadinIcon.START_COG, I18n.t("mms.sender.card.test.tooltip"));
        testButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        testButton.addClickListener(e -> testConnection());
        testButton.setEnabled(isActive());
        buttons.add(testButton);

        // Delete – always last, always danger-styled
        deleteButton = createDeleteButton(I18n.t("mms.sender.card.delete.tooltip"), this::openDeleteDialog);
        buttons.add(deleteButton);

        return buttons;
    }

    private void viewConfig() {
        new SenderConfigDetailsViewDialog(config).open();
    }

    @Override
    protected void buildBodyRows() {
        bodyContainer.removeAll();

        // Essential fields only – quick-scan card. Configuration minutiae
        // (description, tenant, port, username, transport protocol, smtp
        // auth, starttls enabled/required, debug) live in SenderConfigDetailsViewDialog,
        // which shows every SenderConfigDto field.

        // Code
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.CODE,
                I18n.t("mms.sender.card.code"),
                config.getCode() != null ? config.getCode() : I18n.t("mms.common.value.notAvailable"),
                config.getCode() != null ? config.getCode() : ""
        ));

        // Name
        bodyContainer.add(createDetailRow(
                VaadinIcon.TAG,
                I18n.t("mms.sender.card.name"),
                config.getName() != null ? config.getName() : I18n.t("mms.common.value.notAvailable")
        ));

        // Host with copy
        String hostValue = config.getHost() != null ? config.getHost() : I18n.t("mms.common.value.notAvailable");
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.SERVER,
                I18n.t("mms.sender.card.host"),
                hostValue,
                hostValue
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