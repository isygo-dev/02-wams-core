package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Route(value = "custom-key-stores", layout = MainLayout.class)
@PageTitle("Custom Key Stores")
@PermitAll
public class CustomKeyStoresView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final Button createButton = new Button("Create Store", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final ProgressBar loadingBar = new ProgressBar();

    private List<StoreCard> allCards = new ArrayList<>();

    @Autowired
    public CustomKeyStoresView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-custom-stores-view");

        H2 header = new H2("Custom Key Stores");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        // Toolbar
        HorizontalLayout toolbar = new HorizontalLayout(createButton, refreshButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add(toolbar);

        // Cards container
        cardsContainer.setWidthFull();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        add(cardsContainer);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        createButton.addClickListener(e -> openCreateDialog());
        refreshButton.addClickListener(e -> loadStores());

        loadStores();
    }

    private void loadStores() {
        showLoading(true);
        allCards.clear();
        cardsContainer.removeAll();
        try {
            ResponseEntity<ListCustomKeyStoresResponse> response = kmsApiService.listCustomKeyStores(100, null, null, null);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<DescribeCustomKeyStoreResponse.CustomKeyStore> stores = response.getBody().getCustomKeyStores();
                if (stores != null) {
                    for (DescribeCustomKeyStoreResponse.CustomKeyStore store : stores) {
                        allCards.add(new StoreCard(store));
                    }
                }
            }
            if (allCards.isEmpty()) {
                Div emptyState = new Div();
                emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
                emptyState.addClassName(LumoUtility.Padding.XLARGE);
                Icon emptyIcon = VaadinIcon.STORAGE.create();
                emptyIcon.setSize("48px");
                emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
                H4 emptyTitle = new H4("No custom key stores found");
                Span emptyDesc = new Span("Click 'Create Store' to add one.");
                emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
                emptyState.add(emptyIcon, emptyTitle, emptyDesc);
                cardsContainer.add(emptyState);
            } else {
                allCards.forEach(cardsContainer::add);
            }
        } catch (Exception e) {
            Notification.show("Failed to load custom key stores: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        createButton.setEnabled(!show);
        refreshButton.setEnabled(!show);
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Custom Key Store");
        dialog.setWidth("600px");
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        FormLayout form = new FormLayout();

        TextField nameField = new TextField("Store Name");
        nameField.setRequired(true);
        nameField.setMaxLength(255);

        ComboBox<IEnumCustomKeyStoreType.Types> typeCombo = new ComboBox<>("Store Type");
        typeCombo.setItems(IEnumCustomKeyStoreType.Types.values());
        typeCombo.setRequired(true);
        typeCombo.setValue(IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM);

        // CloudHSM fields
        TextField cloudHsmClusterId = new TextField("CloudHSM Cluster ID");
        PasswordField keyStorePassword = new PasswordField("Key Store Password");
        TextArea trustAnchorCert = new TextArea("Trust Anchor Certificate (PEM)");

        // XKS fields
        TextField xksProxyUriEndpoint = new TextField("XKS Proxy URI Endpoint");
        TextField xksProxyUriPath = new TextField("XKS Proxy URI Path");
        PasswordField xksProxyAuth = new PasswordField("XKS Proxy Authentication Credential");
        TextField xksProxyConnectivity = new TextField("XKS Proxy Connectivity");

        // Initially hide XKS fields
        cloudHsmClusterId.setVisible(true);
        keyStorePassword.setVisible(true);
        trustAnchorCert.setVisible(true);
        xksProxyUriEndpoint.setVisible(false);
        xksProxyUriPath.setVisible(false);
        xksProxyAuth.setVisible(false);
        xksProxyConnectivity.setVisible(false);

        typeCombo.addValueChangeListener(e -> {
            boolean isCloudHsm = e.getValue() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM;
            cloudHsmClusterId.setVisible(isCloudHsm);
            keyStorePassword.setVisible(isCloudHsm);
            trustAnchorCert.setVisible(isCloudHsm);
            xksProxyUriEndpoint.setVisible(!isCloudHsm);
            xksProxyUriPath.setVisible(!isCloudHsm);
            xksProxyAuth.setVisible(!isCloudHsm);
            xksProxyConnectivity.setVisible(!isCloudHsm);
        });

        form.add(nameField, typeCombo,
                cloudHsmClusterId, keyStorePassword, trustAnchorCert,
                xksProxyUriEndpoint, xksProxyUriPath, xksProxyAuth, xksProxyConnectivity);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Create", e -> {
            String name = nameField.getValue();
            if (!StringUtils.hasText(name)) {
                Notification.show("Store name is required", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            IEnumCustomKeyStoreType.Types type = typeCombo.getValue();
            CreateCustomKeyStoreRequest request = CreateCustomKeyStoreRequest.builder()
                    .customKeyStoreName(name)
                    .customKeyStoreType(type)
                    .build();
            if (type == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
                request.setCloudHsmClusterId(cloudHsmClusterId.getValue());
                request.setKeyStorePassword(keyStorePassword.getValue());
                request.setTrustAnchorCertificate(trustAnchorCert.getValue());
            } else {
                request.setXksProxyUriEndpoint(xksProxyUriEndpoint.getValue());
                request.setXksProxyUriPath(xksProxyUriPath.getValue());
                request.setXksProxyAuthenticationCredential(xksProxyAuth.getValue());
                request.setXksProxyConnectivity(xksProxyConnectivity.getValue());
            }
            dialog.close();
            showLoading(true);
            try {
                ResponseEntity<CreateCustomKeyStoreResponse> response = kmsApiService.createCustomKeyStore(request);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Custom key store created", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadStores();
                } else {
                    Notification.show("Creation failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } finally {
                showLoading(false);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(form);
        dialog.open();
    }

    // ========== Inner class: Store Card ==========
    private class StoreCard extends VerticalLayout {
        private final DescribeCustomKeyStoreResponse.CustomKeyStore store;
        private final Long storeId;
        private final String storeName;
        private final String storeType;
        private final String connectionState;
        private final String creationDate;
        private final String errorCode;

        public StoreCard(DescribeCustomKeyStoreResponse.CustomKeyStore store) {
            this.store = store;
            this.storeId = store.getCustomKeyStoreId();
            this.storeName = store.getName();
            this.storeType = store.getCustomKeyStoreType();
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

            // Header row: store name + type (left) and action buttons (right)
            HorizontalLayout headerRow = new HorizontalLayout();
            headerRow.setWidthFull();
            headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            headerRow.setAlignItems(FlexComponent.Alignment.CENTER);

            // Left side: name and type chip
            HorizontalLayout leftHeader = new HorizontalLayout();
            leftHeader.setAlignItems(FlexComponent.Alignment.CENTER);
            leftHeader.setSpacing(true);
            Span nameSpan = new Span(storeName);
            nameSpan.addClassName(LumoUtility.FontWeight.BOLD);
            nameSpan.addClassName(LumoUtility.FontSize.MEDIUM);
            Span typeChip = new Span(storeType);
            typeChip.addClassName(LumoUtility.FontSize.XSMALL);
            typeChip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
            typeChip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
            typeChip.addClassName(LumoUtility.BorderRadius.LARGE);
            typeChip.getStyle().set("background-color", "#E9ECEF").set("color", "#495057");
            leftHeader.add(nameSpan, typeChip);

            // Right side: action buttons
            HorizontalLayout buttonBar = new HorizontalLayout();
            buttonBar.setSpacing(true);
            buttonBar.setPadding(false);

            Button connectBtn = createIconButton(VaadinIcon.CONNECT, "Connect");
            connectBtn.addClickListener(e -> connectStore());
            if ("CONNECTED".equalsIgnoreCase(connectionState)) {
                connectBtn.setEnabled(false);
                connectBtn.setTooltipText("Already connected");
            }

            Button disconnectBtn = createIconButton(VaadinIcon.OUT, "Disconnect");
            disconnectBtn.addClickListener(e -> disconnectStore());
            if (!"CONNECTED".equalsIgnoreCase(connectionState)) {
                disconnectBtn.setEnabled(false);
                disconnectBtn.setTooltipText("Not connected");
            }

            Button updateBtn = createIconButton(VaadinIcon.EDIT, "Update store");
            updateBtn.addClickListener(e -> openUpdateDialog());

            Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete store");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> confirmDelete());

            buttonBar.add(connectBtn, disconnectBtn, updateBtn, deleteBtn);
            headerRow.add(leftHeader, buttonBar);
            headerRow.expand(leftHeader);
            add(headerRow);

            // Connection state chip (full width below header)
            Span stateChip = new Span(connectionState);
            stateChip.addClassName(LumoUtility.FontSize.XSMALL);
            stateChip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
            stateChip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
            stateChip.addClassName(LumoUtility.BorderRadius.LARGE);
            if ("CONNECTED".equalsIgnoreCase(connectionState)) {
                stateChip.getStyle().set("background-color", "#E3F7E5").set("color", "#1E7B2E");
            } else if ("DISCONNECTED".equalsIgnoreCase(connectionState)) {
                stateChip.getStyle().set("background-color", "#F2F4F8").set("color", "#5E6C84");
            } else {
                stateChip.getStyle().set("background-color", "#FEF3F2").set("color", "#C73A2B");
            }
            add(stateChip);

            // Metadata row 1: creation date, error code, store ID, update date
            HorizontalLayout metaRow1 = new HorizontalLayout();
            metaRow1.setSpacing(true);
            metaRow1.addClassName(LumoUtility.FontSize.XSMALL);
            metaRow1.addClassName(LumoUtility.TextColor.TERTIARY);
            metaRow1.getStyle().set("margin-top", "var(--lumo-space-xs)");
            metaRow1.add(new Span("ID: " + storeId));
            metaRow1.add(new Span("•"));
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

            // Metadata row 2: CloudHSM or XKS specific details
            if (store.getCloudHsmClusterId() != null && !store.getCloudHsmClusterId().isEmpty()) {
                HorizontalLayout hsmRow = new HorizontalLayout();
                hsmRow.setSpacing(true);
                hsmRow.addClassName(LumoUtility.FontSize.XSMALL);
                hsmRow.addClassName(LumoUtility.TextColor.TERTIARY);
                hsmRow.add(new Span("CloudHSM: " + store.getCloudHsmClusterId()));
                if (store.getTrustAnchorCertificate() != null && !store.getTrustAnchorCertificate().isEmpty()) {
                    String certPreview = store.getTrustAnchorCertificate().length() > 50 ?
                            store.getTrustAnchorCertificate().substring(0, 50) + "…" : store.getTrustAnchorCertificate();
                    hsmRow.add(new Span("•"));
                    hsmRow.add(new Span("Trust Anchor: " + certPreview));
                }
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

        private void connectStore() {
            showLoading(true);
            try {
                ResponseEntity<ConnectCustomKeyStoreResponse> response = kmsApiService.connectCustomKeyStore(storeId);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Connection initiated", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadStores();
                } else {
                    Notification.show("Connection failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception e) {
                Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } finally {
                showLoading(false);
            }
        }

        private void disconnectStore() {
            showLoading(true);
            try {
                ResponseEntity<DisconnectCustomKeyStoreResponse> response = kmsApiService.disconnectCustomKeyStore(storeId);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Disconnected", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadStores();
                } else {
                    Notification.show("Disconnect failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception e) {
                Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } finally {
                showLoading(false);
            }
        }

        private void openUpdateDialog() {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Update Custom Key Store: " + storeName);
            dialog.setWidth("600px");

            FormLayout form = new FormLayout();

            TextField nameField = new TextField("New Store Name (optional)");
            nameField.setValue(storeName);

            boolean isCloudHsm = IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM.name().equals(storeType);

            // CloudHSM updatable fields
            TextField cloudHsmClusterId = new TextField("CloudHSM Cluster ID");
            if (isCloudHsm) cloudHsmClusterId.setValue(store.getCloudHsmClusterId());

            PasswordField keyStorePassword = new PasswordField("Key Store Password (new)");
            TextArea trustAnchorCert = new TextArea("Trust Anchor Certificate");

            // XKS updatable fields
            TextField xksProxyUriEndpoint = new TextField("XKS Proxy URI Endpoint");
            if (!isCloudHsm) xksProxyUriEndpoint.setValue(store.getXksProxyUriEndpoint());

            TextField xksProxyUriPath = new TextField("XKS Proxy URI Path");
            if (!isCloudHsm) xksProxyUriPath.setValue(store.getXksProxyUriPath());

            PasswordField xksProxyAuth = new PasswordField("XKS Proxy Authentication Credential (new)");
            TextField xksProxyConnectivity = new TextField("XKS Proxy Connectivity");
            if (!isCloudHsm) xksProxyConnectivity.setValue(store.getXksProxyConnectivity());

            // Common updatable fields
            IntegerField maxKeysField = new IntegerField("Max Keys");
            maxKeysField.setMin(1);
            maxKeysField.setMax(10000);
            if (store.getMaxKeys() != null) maxKeysField.setValue(store.getMaxKeys());

            IntegerField timeoutField = new IntegerField("Connection Timeout (seconds)");
            timeoutField.setMin(1);
            if (store.getConnectionTimeoutSeconds() != null) timeoutField.setValue(store.getConnectionTimeoutSeconds());

            IntegerField healthIntervalField = new IntegerField("Health Check Interval (seconds)");
            healthIntervalField.setMin(10);
            if (store.getHealthCheckIntervalSeconds() != null) healthIntervalField.setValue(store.getHealthCheckIntervalSeconds());

            Checkbox autoReconnectCheck = new Checkbox("Auto‑reconnect");
            if (store.getAutoReconnect() != null) autoReconnectCheck.setValue(store.getAutoReconnect());

            if (isCloudHsm) {
                form.add(nameField, cloudHsmClusterId, keyStorePassword, trustAnchorCert,
                        maxKeysField, timeoutField, healthIntervalField, autoReconnectCheck);
            } else {
                form.add(nameField, xksProxyUriEndpoint, xksProxyUriPath, xksProxyAuth, xksProxyConnectivity,
                        maxKeysField, timeoutField, healthIntervalField, autoReconnectCheck);
            }
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

            Button updateBtn = new Button("Update", e -> {
                UpdateCustomKeyStoreRequest request = UpdateCustomKeyStoreRequest.builder()
                        .customKeyStoreId(storeId)
                        .newCustomKeyStoreName(StringUtils.hasText(nameField.getValue()) ? nameField.getValue() : null)
                        .build();
                if (isCloudHsm) {
                    if (StringUtils.hasText(cloudHsmClusterId.getValue())) request.setCloudHsmClusterId(cloudHsmClusterId.getValue());
                    if (StringUtils.hasText(keyStorePassword.getValue())) request.setKeyStorePassword(keyStorePassword.getValue());
                    if (StringUtils.hasText(trustAnchorCert.getValue())) request.setTrustAnchorCertificate(trustAnchorCert.getValue());
                } else {
                    if (StringUtils.hasText(xksProxyUriEndpoint.getValue())) request.setXksProxyUriEndpoint(xksProxyUriEndpoint.getValue());
                    if (StringUtils.hasText(xksProxyUriPath.getValue())) request.setXksProxyUriPath(xksProxyUriPath.getValue());
                    if (StringUtils.hasText(xksProxyAuth.getValue())) request.setXksProxyAuthenticationCredential(xksProxyAuth.getValue());
                    if (StringUtils.hasText(xksProxyConnectivity.getValue())) request.setXksProxyConnectivity(xksProxyConnectivity.getValue());
                }
                // Common
                if (maxKeysField.getValue() != null) request.setMaxKeys(maxKeysField.getValue());
                if (timeoutField.getValue() != null) request.setConnectionTimeoutSeconds(timeoutField.getValue());
                if (healthIntervalField.getValue() != null) request.setHealthCheckIntervalSeconds(healthIntervalField.getValue());
                request.setAutoReconnect(autoReconnectCheck.getValue());

                dialog.close();
                showLoading(true);
                try {
                    ResponseEntity<UpdateCustomKeyStoreResponse> response = kmsApiService.updateCustomKeyStore(storeId, request);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Key store updated", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadStores();
                    } else {
                        Notification.show("Update failed", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                } finally {
                    showLoading(false);
                }
            });
            updateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            Button cancelBtn = new Button("Cancel", e -> dialog.close());
            dialog.getFooter().add(cancelBtn, updateBtn);
            dialog.add(form);
            dialog.open();
        }

        private void confirmDelete() {
            String confirmationCode = String.valueOf(ThreadLocalRandom.current().nextInt(100_000_000, 1_000_000_000));

            Dialog deleteConfirmDialog = new Dialog();
            deleteConfirmDialog.setHeaderTitle("Delete Custom Key Store");
            deleteConfirmDialog.setWidth("450px");
            deleteConfirmDialog.setCloseOnEsc(false);
            deleteConfirmDialog.setCloseOnOutsideClick(false);

            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(true);
            layout.setPadding(true);

            layout.add(new Span("This action is irreversible. The store will be permanently deleted."));
            layout.add(new Span("To confirm, enter the 9‑digit code below:"));

            Span codeSpan = new Span(confirmationCode);
            codeSpan.getStyle().set("font-weight", "bold");
            codeSpan.getStyle().set("font-size", "28px");
            codeSpan.getStyle().set("font-family", "monospace");
            codeSpan.getStyle().set("background-color", "#f0f0f0");
            codeSpan.getStyle().set("padding", "12px 20px");
            codeSpan.getStyle().set("border-radius", "8px");
            codeSpan.getStyle().set("text-align", "center");
            codeSpan.getStyle().set("letter-spacing", "4px");
            Div codeDiv = new Div(codeSpan);
            codeDiv.getStyle().set("text-align", "center");
            layout.add(codeDiv);

            TextField pinField = new TextField();
            pinField.setPlaceholder("Enter 9‑digit code");
            pinField.setWidthFull();
            pinField.setPattern("[0-9]*");
            pinField.setMaxLength(9);
            pinField.setAllowedCharPattern("[0-9]");
            layout.add(pinField);

            Button confirmDeleteBtn = new Button("Delete permanently", e -> {
                if (pinField.getValue().equals(confirmationCode)) {
                    deleteConfirmDialog.close();
                    performDelete();
                } else {
                    Notification.show("Incorrect confirmation code", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            confirmDeleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            confirmDeleteBtn.setEnabled(false);

            pinField.addValueChangeListener(e -> {
                String value = e.getValue();
                boolean isValid = value != null && value.matches("\\d{9}");
                confirmDeleteBtn.setEnabled(isValid && value.equals(confirmationCode));
            });

            Button cancelDeleteBtn = new Button("Cancel", e -> deleteConfirmDialog.close());

            HorizontalLayout buttonBar = new HorizontalLayout(cancelDeleteBtn, confirmDeleteBtn);
            buttonBar.setJustifyContentMode(JustifyContentMode.END);
            layout.add(buttonBar);

            deleteConfirmDialog.add(layout);
            deleteConfirmDialog.open();
        }

        private void performDelete() {
            showLoading(true);
            try {
                ResponseEntity<DeleteCustomKeyStoreResponse> response = kmsApiService.deleteCustomKeyStore(storeId);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Store deleted", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadStores();
                } else {
                    Notification.show("Deletion failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception e) {
                Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } finally {
                showLoading(false);
            }
        }
    }
}