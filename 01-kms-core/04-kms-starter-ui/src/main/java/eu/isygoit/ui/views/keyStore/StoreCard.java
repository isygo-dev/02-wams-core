package eu.isygoit.ui.views.keyStore;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.keyStore.dialog.DeleteCustomKeyStoreDialog;
import eu.isygoit.ui.views.keyStore.dialog.UpdateCustomKeyStoreDialog;
import org.springframework.http.ResponseEntity;

import java.time.format.DateTimeFormatter;

// ========== Inner class: Store Card ==========
class StoreCard extends VerticalLayout {
    private final CustomKeyStoresView parentView;
    private final KmsApiService kmsApiService;
    private final KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store;
    private final Long storeId;
    private final String storeName;
    private final String storeType;
    private final String storeStatus;
    private final String connectionState;
    private final String creationDate;
    private final String errorCode;

    public StoreCard(CustomKeyStoresView customKeyStoresView, KmsApiService kmsApiService, KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        this.parentView = customKeyStoresView;
        this.kmsApiService = kmsApiService;
        this.store = store;
        this.storeId = store.getCustomKeyStoreId();
        this.storeName = store.getName();
        this.storeType = store.getCustomKeyStoreType();
        this.storeStatus = store.getStatus() != null ? store.getStatus().name() : "UNKNOWN";
        this.connectionState = store.getConnectionState();
        this.creationDate = store.getCreateDate() != null ?
                store.getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "-";
        this.errorCode = store.getConnectionError();
        buildCard();
    }

    private void buildCard() {
        setWidthFull();
        setMargin(false);
        setPadding(true);
        addClassName(LumoUtility.BorderRadius.LARGE);
        addClassName(LumoUtility.Background.BASE);
        addClassName(LumoUtility.BoxShadow.XSMALL);
        getStyle().set("transition", "all 0.2s ease-in-out");
        addClassName("hover:shadow-m");

        // Header row: store name (left) and action buttons (right)
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(Alignment.CENTER);

        // Left side: name only (type+status will be below)
        Span nameSpan = new Span(storeName);
        nameSpan.addClassName(LumoUtility.FontWeight.BOLD);
        nameSpan.addClassName(LumoUtility.FontSize.MEDIUM);

        // Right side: action buttons
        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);

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

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete store");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.addClickListener(e -> confirmDelete());

        buttonBar.add(connectBtn, disconnectBtn, updateBtn, deleteBtn);
        headerRow.add(nameSpan, buttonBar);
        headerRow.expand(nameSpan);
        add(headerRow);

        // Type and status chips (same line)
        HorizontalLayout typeStatusRow = new HorizontalLayout();
        typeStatusRow.setSpacing(true);
        typeStatusRow.setAlignItems(Alignment.CENTER);
        Span typeChip = new Span(storeType);
        typeChip.addClassName(LumoUtility.FontSize.XSMALL);
        typeChip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
        typeChip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
        typeChip.addClassName(LumoUtility.BorderRadius.LARGE);
        typeChip.getStyle().set("background-color", "#E9ECEF").set("color", "#495057");

        Span statusChip = new Span(storeStatus);
        statusChip.addClassName(LumoUtility.FontSize.XSMALL);
        statusChip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
        statusChip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
        statusChip.addClassName(LumoUtility.BorderRadius.LARGE);
        if ("CONNECTED".equalsIgnoreCase(storeStatus)) {
            statusChip.getStyle().set("background-color", "#E3F7E5").set("color", "#1E7B2E");
        } else if ("DISCONNECTED".equalsIgnoreCase(storeStatus)) {
            statusChip.getStyle().set("background-color", "#F2F4F8").set("color", "#5E6C84");
        } else {
            statusChip.getStyle().set("background-color", "#FEF3F2").set("color", "#C73A2B");
        }
        typeStatusRow.add(typeChip, statusChip);
        add(typeStatusRow);

        // Metadata row 1: creation date, error code, update date
        HorizontalLayout metaRow1 = new HorizontalLayout();
        metaRow1.setSpacing(true);
        metaRow1.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow1.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow1.getStyle().set("margin-top", "var(--lumo-space-xs)");
        metaRow1.add(new Span("Created: " + creationDate));
        if (store.getUpdateDate() != null) {
            metaRow1.add(new Span("•"));
            metaRow1.add(new Span("Updated: " + store.getUpdateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
        if (errorCode != null && !errorCode.isEmpty()) {
            metaRow1.add(new Span("•"));
            metaRow1.add(new Span("Error: " + errorCode));
        }
        add(metaRow1);

        // Metadata row 2: CloudHSM or XKS specific details (ID and trust anchor NOT shown)
        if (store.getCloudHsmClusterId() != null && !store.getCloudHsmClusterId().isEmpty()) {
            HorizontalLayout hsmRow = new HorizontalLayout();
            hsmRow.setSpacing(true);
            hsmRow.addClassName(LumoUtility.FontSize.XSMALL);
            hsmRow.addClassName(LumoUtility.TextColor.TERTIARY);
            hsmRow.add(new Span("CloudHSM: " + store.getCloudHsmClusterId()));
            add(hsmRow);
        } else if (store.getXksProxyUriEndpoint() != null && !store.getXksProxyUriEndpoint().isEmpty()) {
            HorizontalLayout xksRow = new HorizontalLayout();
            xksRow.setSpacing(true);
            xksRow.addClassName(LumoUtility.FontSize.XSMALL);
            xksRow.addClassName(LumoUtility.TextColor.TERTIARY);
            xksRow.add(new Span("XKS Endpoint: " + store.getXksProxyUriEndpoint()));
            if (store.getXksProxyUriPath() != null && !store.getXksProxyUriPath().isEmpty()) {
                xksRow.add(new Span("•"));
                xksRow.add(new Span("Path: " + store.getXksProxyUriPath()));
            }
            if (store.getXksProxyConnectivity() != null && !store.getXksProxyConnectivity().isEmpty()) {
                xksRow.add(new Span("•"));
                xksRow.add(new Span("Connectivity: " + store.getXksProxyConnectivity()));
            }
            add(xksRow);
        }

        // Metadata row 3: health status, max keys, last successful connection, last connection attempt
        HorizontalLayout metaRow3 = new HorizontalLayout();
        metaRow3.setSpacing(true);
        metaRow3.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow3.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow3.getStyle().set("margin-top", "var(--lumo-space-xs)");

        if (store.getHealthStatus() != null) {
            metaRow3.add(new Span("Health: " + store.getHealthStatus()));
        }
        if (store.getMaxKeys() != null) {
            if (metaRow3.getComponentCount() > 0) metaRow3.add(new Span("•"));
            metaRow3.add(new Span("Max keys: " + store.getMaxKeys()));
        }
        if (store.getLastSuccessfulConnection() != null) {
            if (metaRow3.getComponentCount() > 0) metaRow3.add(new Span("•"));
            metaRow3.add(new Span("Last connected: " + store.getLastSuccessfulConnection().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
        if (store.getLastConnectionAttempt() != null) {
            if (metaRow3.getComponentCount() > 0) metaRow3.add(new Span("•"));
            metaRow3.add(new Span("Last attempt: " + store.getLastConnectionAttempt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }

        if (metaRow3.getComponentCount() == 0) {
            metaRow3.add(new Span("No additional info"));
        }
        add(metaRow3);

        // Metadata row 4: connection settings (timeout, health interval, auto‑reconnect)
        if (store.getConnectionTimeoutSeconds() != null || store.getHealthCheckIntervalSeconds() != null || store.getAutoReconnect() != null) {
            HorizontalLayout settingsRow = new HorizontalLayout();
            settingsRow.setSpacing(true);
            settingsRow.addClassName(LumoUtility.FontSize.XSMALL);
            settingsRow.addClassName(LumoUtility.TextColor.TERTIARY);
            settingsRow.getStyle().set("margin-top", "var(--lumo-space-xs)");

            if (store.getConnectionTimeoutSeconds() != null) {
                settingsRow.add(new Span("Timeout: " + store.getConnectionTimeoutSeconds() + "s"));
            }
            if (store.getHealthCheckIntervalSeconds() != null) {
                if (settingsRow.getComponentCount() > 0) settingsRow.add(new Span("•"));
                settingsRow.add(new Span("Health interval: " + store.getHealthCheckIntervalSeconds() + "s"));
            }
            if (store.getAutoReconnect() != null) {
                if (settingsRow.getComponentCount() > 0) settingsRow.add(new Span("•"));
                settingsRow.add(new Span("Auto‑reconnect: " + (store.getAutoReconnect() ? "ON" : "OFF")));
            }
            add(settingsRow);
        }
    }

    private Button createIconButton(VaadinIcon icon, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.setTooltipText(tooltip);
        return btn;
    }

    private void confirmConnect() {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Connect to store");
        confirm.setText("Are you sure you want to connect to the custom key store '" + storeName + "'?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Connect");
        confirm.setConfirmButtonTheme(ButtonVariant.LUMO_SUCCESS.getVariantName());
        confirm.addConfirmListener(event -> connectStore());
        confirm.open();
    }

    private void confirmDisconnect() {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Disconnect from store");
        confirm.setText("Disconnecting the store may make any keys hosted in this store temporarily unusable.\nAre you sure you want to disconnect?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Disconnect");
        confirm.setConfirmButtonTheme(ButtonVariant.LUMO_WARNING.getVariantName());
        confirm.addConfirmListener(event -> disconnectStore());
        confirm.open();
    }

    private void connectStore() {
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.ConnectCustomKeyStoreResponse> response = this.kmsApiService.connectCustomKeyStore(storeId);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Connection initiated", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                parentView.loadStores();
            } else {
                Notification.show("Connection failed", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }
    }

    private void disconnectStore() {
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.DisconnectCustomKeyStoreResponse> response = this.kmsApiService.disconnectCustomKeyStore(storeId);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Disconnected", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                parentView.loadStores();
            } else {
                Notification.show("Disconnect failed", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }
    }

    private void updateCustomKeyStore() {
        (new UpdateCustomKeyStoreDialog(parentView, kmsApiService, parentView::loadStores, store)).open();
    }

    private void confirmDelete() {
        (new DeleteCustomKeyStoreDialog(parentView, kmsApiService, parentView::loadStores, store)).open();
    }
}
