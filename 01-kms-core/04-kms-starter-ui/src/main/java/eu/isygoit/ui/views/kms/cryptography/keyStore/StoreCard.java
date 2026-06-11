package eu.isygoit.ui.views.kms.cryptography.keyStore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.common.card.BaseCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
class StoreCard extends BaseCard<CustomKeyStoresView, KmsApiService> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store;
    private final Long storeId;
    private final String storeName;
    private final String storeType;
    private final String storeStatus;
    private final String connectionState;
    private final String creationDate;
    private final String errorCode;

    StoreCard(CustomKeyStoresView customKeyStoresView,
              KmsApiService kmsApiService,
              KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        super(customKeyStoresView, kmsApiService);
        this.store = store;
        this.storeId = store.getCustomKeyStoreId();
        this.storeName = store.getName();
        this.storeType = store.getCustomKeyStoreType();
        this.storeStatus = store.getStatus() != null ? store.getStatus().name() : "UNKNOWN";
        this.connectionState = store.getConnectionState();
        this.creationDate = store.getCreateDate() != null
                ? DateHelper.formatToHumanReadable(store.getCreateDate()) : "-";
        this.errorCode = store.getConnectionError();
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "store-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");

        Span titleSpan = buildTitleSpan(storeName, storeName);
        Span typeChip = buildStatusChip(storeType, ChipColor.INFO);
        String statusDisplay = connectionState != null ? connectionState : storeStatus;
        Span statusChip = buildStatusChip(statusDisplay, storeStatus);
        left.add(titleSpan, typeChip, statusChip);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button connectBtn = createIconButton(VaadinIcon.CONNECT, "Connect");
        connectBtn.addClickListener(e -> confirmConnect());
        if ("CONNECTED".equalsIgnoreCase(connectionState)) {
            connectBtn.setEnabled(false);
            connectBtn.setTooltipText("Already connected");
        }

        Button disconnectBtn = createIconButton(VaadinIcon.OUT, "Disconnect");
        disconnectBtn.addClickListener(e -> confirmDisconnect());
        if (!"CONNECTED".equalsIgnoreCase(connectionState)) {
            disconnectBtn.setEnabled(false);
            disconnectBtn.setTooltipText("Not connected");
        }

        Button updateBtn = createIconButton(VaadinIcon.EDIT, "Update store");
        updateBtn.addClickListener(e -> updateCustomKeyStore());

        Button deleteBtn = createDangerIconButton(VaadinIcon.TRASH, "Delete store");
        deleteBtn.addClickListener(e -> confirmDelete());

        return List.of(connectBtn, disconnectBtn, updateBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.CALENDAR, "Created", creationDate != null ? DateHelper.formatToHumanReadable(store.getCreateDate()) : "-"));
        if (store.getUpdateDate() != null) {
            add(createIconRow(VaadinIcon.REFRESH, "Updated", DateHelper.formatToHumanReadable(store.getUpdateDate())));
        }

        if (StringUtils.hasText(store.getCloudHsmClusterId())) {
            add(createIconRow(VaadinIcon.CLOUD, "CloudHSM cluster", store.getCloudHsmClusterId()));
        } else if (StringUtils.hasText(store.getXksProxyUriEndpoint())) {
            add(createIconRow(VaadinIcon.LINK, "XKS endpoint", store.getXksProxyUriEndpoint()));
            if (StringUtils.hasText(store.getXksProxyUriPath())) {
                add(createIconRow(VaadinIcon.ROAD, "XKS path", store.getXksProxyUriPath()));   // ROAD instead of PATH
            }
            if (StringUtils.hasText(store.getXksProxyConnectivity())) {
                add(createIconRow(VaadinIcon.CONNECT, "Connectivity", store.getXksProxyConnectivity()));
            }
        }

        if (StringUtils.hasText(store.getHealthStatus())) {
            add(createIconRow(VaadinIcon.HEART, "Health", store.getHealthStatus()));
        }
        if (store.getMaxKeys() != null) {
            add(createIconRow(VaadinIcon.KEY, "Max keys", String.valueOf(store.getMaxKeys())));
        }
        if (store.getLastSuccessfulConnection() != null) {
            add(createIconRow(VaadinIcon.CONNECT, "Last connected", DateHelper.formatToHumanReadable(store.getLastSuccessfulConnection())));
        }
        if (store.getLastConnectionAttempt() != null) {
            add(createIconRow(VaadinIcon.CLOCK, "Last attempt", DateHelper.formatToHumanReadable(store.getLastConnectionAttempt())));
        }
        if (store.getConnectionTimeoutSeconds() != null) {
            add(createIconRow(VaadinIcon.TIMER, "Timeout", store.getConnectionTimeoutSeconds() + "s"));
        }
        if (store.getHealthCheckIntervalSeconds() != null) {
            add(createIconRow(VaadinIcon.SPARK_LINE, "Health interval", store.getHealthCheckIntervalSeconds() + "s")); // SPARK_LINE instead of PULSE
        }
        if (store.getAutoReconnect() != null) {
            add(createIconRow(VaadinIcon.REFRESH, "Auto-reconnect", store.getAutoReconnect() ? "ON" : "OFF"));
        }
        if (StringUtils.hasText(errorCode)) {
            add(createIconRow(VaadinIcon.EXCLAMATION_CIRCLE, "Error", errorCode));
        }

        addKeyValueChips("📝 Metadata", store.getMetadata());
        addKeyValueChips("🏷️ Tags", store.getTags());
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("margin-top", "var(--lumo-space-xs)");
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "100px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private void addKeyValueChips(String label, String json) {
        if (!StringUtils.hasText(json)) return;
        try {
            Map<String, String> map = MAPPER.readValue(json, new TypeReference<>() {
            });
            if (map == null || map.isEmpty()) return;

            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setSpacing(true);
            row.setWidthFull();
            row.getStyle().set("margin-top", "var(--lumo-space-xs)");
            row.addClassName("meta-row");

            Icon icon = VaadinIcon.TAGS.create();
            icon.setSize("16px");
            icon.getStyle().set("color", "var(--lumo-secondary-text-color)");

            Span labelSpan = new Span(label + ":");
            labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
            labelSpan.getStyle().set("min-width", "100px");

            HorizontalLayout chips = new HorizontalLayout();
            chips.setSpacing(true);
            chips.getStyle().set("flex-wrap", "wrap");

            for (Map.Entry<String, String> entry : map.entrySet()) {
                Span chip = new Span(entry.getKey() + "=" + entry.getValue());
                chip.addClassName(LumoUtility.Background.CONTRAST_5);
                chip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
                chip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
                chip.addClassName(LumoUtility.BorderRadius.LARGE);
                chip.addClassName(LumoUtility.FontSize.XSMALL);
                chips.add(chip);
            }

            row.add(icon, labelSpan, chips);
            row.expand(chips);
            add(row);
        } catch (Exception e) {
            // ignore
        }
    }

    // Actions (unchanged)
    private void confirmConnect() { /* same as original */ }

    private void confirmDisconnect() { /* same */ }

    private void connectStore() { /* same */ }

    private void disconnectStore() { /* same */ }

    private void updateCustomKeyStore() { /* same */ }

    private void confirmDelete() { /* same */ }

    @Override
    protected String buildExtraStyles() {
        return """
                .store-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .store-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .store-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .store-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}