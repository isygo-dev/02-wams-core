package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
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

@Route(value = "custom-key-stores", layout = MainLayout.class)
@PageTitle("Custom Key Stores")
@PermitAll
public class CustomKeyStoresView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final Grid<DescribeCustomKeyStoreResponse.CustomKeyStore> storesGrid = new Grid<>(DescribeCustomKeyStoreResponse.CustomKeyStore.class);
    private final Button createButton = new Button("Create Store", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
    private final Button connectButton = new Button("Connect", new Icon(VaadinIcon.CONNECT));
    private final Button disconnectButton = new Button("Disconnect", new Icon(VaadinIcon.OUT));
    private final Button updateButton = new Button("Update", new Icon(VaadinIcon.EDIT));
    private final Button deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
    private final ProgressBar loadingBar = new ProgressBar();

    private List<DescribeCustomKeyStoreResponse.CustomKeyStore> allStores = new ArrayList<>();
    private DescribeCustomKeyStoreResponse.CustomKeyStore selectedStore = null;

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

        // Action bar
        HorizontalLayout actionBar = new HorizontalLayout(createButton, refreshButton, connectButton, disconnectButton, updateButton, deleteButton);
        actionBar.setSpacing(true);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        connectButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        disconnectButton.addThemeVariants(ButtonVariant.LUMO_WARNING);
        updateButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        add(actionBar);

        // Stores grid
        storesGrid.setWidthFull();
        storesGrid.setHeight("500px");
        storesGrid.setColumns("customKeyStoreId", "customKeyStoreName", "customKeyStoreType", "connectionState");
        storesGrid.getColumnByKey("customKeyStoreId").setHeader("ID").setSortable(true);
        storesGrid.getColumnByKey("customKeyStoreName").setHeader("Name").setSortable(true);
        storesGrid.getColumnByKey("customKeyStoreType").setHeader("Type");
        storesGrid.getColumnByKey("connectionState").setHeader("Connection State");
        // Custom date column
        storesGrid.addColumn(store -> store.getCreateDate() != null ?
                        store.getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "-")
                .setHeader("Creation Date").setSortable(true);
        // Error code column
        storesGrid.addColumn(store -> store.getConnectionErrorCode() != null ? store.getConnectionErrorCode() : "-")
                .setHeader("Error Code");
        storesGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        storesGrid.asSingleSelect().addValueChangeListener(e -> selectedStore = e.getValue());
        add(storesGrid);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Event listeners
        createButton.addClickListener(e -> openCreateDialog());
        refreshButton.addClickListener(e -> loadStores());
        connectButton.addClickListener(e -> connectStore());
        disconnectButton.addClickListener(e -> disconnectStore());
        updateButton.addClickListener(e -> openUpdateDialog());
        deleteButton.addClickListener(e -> deleteStore());

        // Initial load
        loadStores();
    }

    private void loadStores() {
        showLoading(true);
        try {
            ResponseEntity<ListCustomKeyStoresResponse> response = kmsApiService.listCustomKeyStores(100, null, null, null);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                allStores = response.getBody().getCustomKeyStores();
                storesGrid.setItems(allStores);
            } else {
                storesGrid.setItems(new ArrayList<>());
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
        storesGrid.setVisible(!show);
        createButton.setEnabled(!show);
        refreshButton.setEnabled(!show);
        connectButton.setEnabled(!show);
        disconnectButton.setEnabled(!show);
        updateButton.setEnabled(!show);
        deleteButton.setEnabled(!show);
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
        TextField xksProxyConnectivity = new TextField("XKS Proxy Connectivity (e.g., PUBLIC_ENDPOINT)");

        // Initially hide XKS fields
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
        typeCombo.setValue(IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM); // trigger initial visibility

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

    private void openUpdateDialog() {
        if (selectedStore == null) {
            Notification.show("No store selected", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Update Custom Key Store");
        dialog.setWidth("600px");

        FormLayout form = new FormLayout();

        TextField nameField = new TextField("New Store Name (optional)");
        nameField.setValue(selectedStore.getCustomKeyStoreName());

        boolean isCloudHsm = IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM.name().equals(selectedStore.getCustomKeyStoreType());
        TextField cloudHsmClusterId = new TextField("CloudHSM Cluster ID");
        if (isCloudHsm) cloudHsmClusterId.setValue(selectedStore.getCloudHsmClusterId());

        PasswordField keyStorePassword = new PasswordField("Key Store Password (new)");
        TextArea trustAnchorCert = new TextArea("Trust Anchor Certificate");

        TextField xksProxyUriEndpoint = new TextField("XKS Proxy URI Endpoint");
        if (!isCloudHsm) xksProxyUriEndpoint.setValue(selectedStore.getXksProxyUriEndpoint());

        TextField xksProxyUriPath = new TextField("XKS Proxy URI Path");
        if (!isCloudHsm) xksProxyUriPath.setValue(selectedStore.getXksProxyUriPath());

        PasswordField xksProxyAuth = new PasswordField("XKS Proxy Authentication Credential (new)");
        TextField xksProxyConnectivity = new TextField("XKS Proxy Connectivity");
        if (!isCloudHsm) xksProxyConnectivity.setValue(selectedStore.getXksProxyConnectivity());

        if (isCloudHsm) {
            form.add(nameField, cloudHsmClusterId, keyStorePassword, trustAnchorCert);
        } else {
            form.add(nameField, xksProxyUriEndpoint, xksProxyUriPath, xksProxyAuth, xksProxyConnectivity);
        }

        Button updateBtn = new Button("Update", e -> {
            UpdateCustomKeyStoreRequest request = UpdateCustomKeyStoreRequest.builder()
                    .customKeyStoreId(selectedStore.getCustomKeyStoreId())
                    .newCustomKeyStoreName(StringUtils.hasText(nameField.getValue()) ? nameField.getValue() : null)
                    .build();
            if (isCloudHsm) {
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
                ResponseEntity<UpdateCustomKeyStoreResponse> response = kmsApiService.updateCustomKeyStore(selectedStore.getCustomKeyStoreId(), request);
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

    private void connectStore() {
        if (selectedStore == null) {
            Notification.show("No store selected", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        showLoading(true);
        try {
            ResponseEntity<ConnectCustomKeyStoreResponse> response = kmsApiService.connectCustomKeyStore(selectedStore.getCustomKeyStoreId());
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
        if (selectedStore == null) {
            Notification.show("No store selected", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        showLoading(true);
        try {
            ResponseEntity<DisconnectCustomKeyStoreResponse> response = kmsApiService.disconnectCustomKeyStore(selectedStore.getCustomKeyStoreId());
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

    private void deleteStore() {
        if (selectedStore == null) {
            Notification.show("No store selected", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Custom Key Store");
        confirm.setText("Are you sure you want to delete the custom key store '" + selectedStore.getCustomKeyStoreName() + "'?\nThis action cannot be undone.");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        confirm.addConfirmListener(event -> {
            showLoading(true);
            try {
                ResponseEntity<DeleteCustomKeyStoreResponse> response = kmsApiService.deleteCustomKeyStore(selectedStore.getCustomKeyStoreId());
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
        });
        confirm.open();
    }
}