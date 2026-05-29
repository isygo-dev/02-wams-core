package eu.isygoit.ui.views.keyStore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.AbstractKmsCard;
import eu.isygoit.ui.views.keyStore.dialog.DeleteCustomKeyStoreDialog;
import eu.isygoit.ui.views.keyStore.dialog.UpdateCustomKeyStoreDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
class StoreCard extends AbstractKmsCard<CustomKeyStoresView> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store;
    private final Long storeId;
    private final String storeName;
    private final String storeType;
    private final String storeStatus;
    private final String connectionState;
    private final String creationDate;
    private final String errorCode;

    // ── Constructor ───────────────────────────────────────────────────────────

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
                ? store.getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "-";
        this.errorCode = store.getConnectionError();
        initCard();
    }

    // ── AbstractKmsCard contract ──────────────────────────────────────────────

    @Override
    protected String cardCssClassName() {
        return "store-card";
    }

    @Override
    protected Component buildTitle() {
        // For StoreCard the title is just the name span; chips are added in buildBodyRows
        return buildTitleSpan(storeName, storeName);
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
        // Type + connection-state chips
        HorizontalLayout typeStatusRow = new HorizontalLayout();
        typeStatusRow.setSpacing(true);
        typeStatusRow.setAlignItems(FlexComponent.Alignment.CENTER);
        typeStatusRow.getStyle().set("flex-wrap", "wrap");

        typeStatusRow.add(buildStatusChip(storeType, ChipColor.INFO));
        typeStatusRow.add(buildStatusChip(connectionState != null ? connectionState : storeStatus, storeStatus));
        add(typeStatusRow);

        // Meta row 1: created, updated, error
        String updated = store.getUpdateDate() != null
                ? "Updated: " + store.getUpdateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        String error = (errorCode != null && !errorCode.isEmpty()) ? "Error: " + errorCode : null;
        addMetaRow("Created: " + creationDate, updated, error);

        // CloudHSM or XKS details
        if (store.getCloudHsmClusterId() != null && !store.getCloudHsmClusterId().isEmpty()) {
            addMetaRow("CloudHSM: " + store.getCloudHsmClusterId());
        } else if (store.getXksProxyUriEndpoint() != null && !store.getXksProxyUriEndpoint().isEmpty()) {
            String path = (store.getXksProxyUriPath() != null && !store.getXksProxyUriPath().isEmpty())
                    ? "Path: " + store.getXksProxyUriPath() : null;
            String conn = (store.getXksProxyConnectivity() != null && !store.getXksProxyConnectivity().isEmpty())
                    ? "Connectivity: " + store.getXksProxyConnectivity() : null;
            addMetaRow("XKS Endpoint: " + store.getXksProxyUriEndpoint(), path, conn);
        }

        // Meta row 3: health, max keys, last connected, last attempt
        String health = store.getHealthStatus() != null ? "Health: " + store.getHealthStatus() : null;
        String maxKeys = store.getMaxKeys() != null ? "Max keys: " + store.getMaxKeys() : null;
        String lastConn = store.getLastSuccessfulConnection() != null
                ? "Last connected: " + store.getLastSuccessfulConnection().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        String lastAttempt = store.getLastConnectionAttempt() != null
                ? "Last attempt: " + store.getLastConnectionAttempt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        addMetaRow(health, maxKeys, lastConn, lastAttempt);

        // Meta row 4: connection settings
        String timeout = store.getConnectionTimeoutSeconds() != null
                ? "Timeout: " + store.getConnectionTimeoutSeconds() + "s" : null;
        String interval = store.getHealthCheckIntervalSeconds() != null
                ? "Health interval: " + store.getHealthCheckIntervalSeconds() + "s" : null;
        String autoRec = store.getAutoReconnect() != null
                ? "Auto‑reconnect: " + (store.getAutoReconnect() ? "ON" : "OFF") : null;
        addMetaRow(timeout, interval, autoRec);

        // Metadata key-value tags
        addKeyValueRow("📝 Metadata:", store.getMetadata());

        // Tags
        addKeyValueRow("🏷️ Tags:", store.getTags());
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .store-card .store-card__type-status,
                .store-card .store-card__meta-row {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-xs);
                    align-items: center;
                }
                @media (max-width: 640px) {
                    .store-card .store-card__type-status {
                        flex-direction: column;
                        align-items: flex-start;
                    }
                }
                """;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Parses a JSON string as {@code Map<String,String>} and renders pill chips.
     */
    private void addKeyValueRow(String label, String json) {
        if (!StringUtils.hasText(json)) return;
        try {
            Map<String, String> map = MAPPER.readValue(json, new TypeReference<>() {
            });
            if (map == null || map.isEmpty()) return;

            HorizontalLayout row = new HorizontalLayout();
            row.setSpacing(true);
            row.addClassName(LumoUtility.FontSize.XSMALL);
            row.addClassName(LumoUtility.TextColor.TERTIARY);
            row.getStyle().set("flex-wrap", "wrap");
            row.add(new Span(label));

            for (Map.Entry<String, String> entry : map.entrySet()) {
                Span chip = new Span(entry.getKey() + "=" + entry.getValue());
                chip.addClassName(LumoUtility.Background.CONTRAST_5);
                chip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
                chip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
                chip.addClassName(LumoUtility.BorderRadius.LARGE);
                row.add(chip);
            }
            add(row);
        } catch (Exception e) {
            // silently ignore malformed JSON
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void confirmConnect() {
        ConfirmDialog dlg = new ConfirmDialog();
        dlg.setHeader("Connect to store");
        dlg.setText("Are you sure you want to connect to the custom key store '" + storeName + "'?");
        dlg.setCancelable(true);
        dlg.setConfirmText("Connect");
        dlg.setConfirmButtonTheme(ButtonVariant.LUMO_SUCCESS.getVariantName());
        dlg.addConfirmListener(e -> connectStore());
        dlg.open();
    }

    private void confirmDisconnect() {
        ConfirmDialog dlg = new ConfirmDialog();
        dlg.setHeader("Disconnect from store");
        dlg.setText("Disconnecting may make keys hosted here temporarily unusable. Are you sure?");
        dlg.setCancelable(true);
        dlg.setConfirmText("Disconnect");
        dlg.setConfirmButtonTheme(ButtonVariant.LUMO_WARNING.getVariantName());
        dlg.addConfirmListener(e -> disconnectStore());
        dlg.open();
    }

    private void connectStore() {
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.ConnectCustomKeyStoreResponse> resp =
                    kmsApiService.connectCustomKeyStore(storeId);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            Notification.show(ok ? "Connection initiated" : "Connection failed", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(ok ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
            if (ok) parentView.loadStores();
        } catch (FeignException ex) {
            notify("Error: " + (ex.status() == 500 ? ex.contentUTF8() : ex.getMessage()), false);
            log.error("Failed to connect store {}", storeId, ex);
        } catch (Exception ex) {
            notify("Error: " + ex.getMessage(), false);
            log.error("Failed to connect store {}", storeId, ex);
        } finally {
            parentView.showLoading(false);
        }
    }

    private void disconnectStore() {
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.DisconnectCustomKeyStoreResponse> resp =
                    kmsApiService.disconnectCustomKeyStore(storeId);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            Notification.show(ok ? "Disconnected" : "Disconnect failed", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(ok ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
            if (ok) parentView.loadStores();
        } catch (FeignException ex) {
            notify("Error: " + (ex.status() == 500 ? ex.contentUTF8() : ex.getMessage()), false);
            log.error("Failed to disconnect store {}", storeId, ex);
        } catch (Exception ex) {
            notify("Error: " + ex.getMessage(), false);
            log.error("Failed to disconnect store {}", storeId, ex);
        } finally {
            parentView.showLoading(false);
        }
    }

    private void updateCustomKeyStore() {
        new UpdateCustomKeyStoreDialog(parentView, kmsApiService, parentView::loadStores, store).open();
    }

    private void confirmDelete() {
        new DeleteCustomKeyStoreDialog(parentView, kmsApiService, parentView::loadStores, store).open();
    }

    private void notify(String msg, boolean success) {
        Notification.show(msg, 6000, Notification.Position.TOP_END)
                .addThemeVariants(success ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
    }
}