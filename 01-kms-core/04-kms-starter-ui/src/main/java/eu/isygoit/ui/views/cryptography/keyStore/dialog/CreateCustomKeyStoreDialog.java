package eu.isygoit.ui.views.cryptography.keyStore.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreRequest;
import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreResponse;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.common.dialog.BaseActionDialog;
import eu.isygoit.ui.views.cryptography.keyStore.CustomKeyStoresView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class CreateCustomKeyStoreDialog extends BaseActionDialog {

    private final CustomKeyStoresView parentView;
    private final KmsApiService kmsApiService;

    private TextField nameField;
    private ComboBox<IEnumCustomKeyStoreType.Types> typeCombo;
    private TextField cloudHsmClusterId;
    private PasswordField keyStorePassword;
    private TextArea trustAnchorCert;
    private TextField xksProxyUriEndpoint;
    private TextField xksProxyUriPath;
    private PasswordField xksProxyAuth;
    private TextField xksProxyConnectivity;
    private IntegerField maxKeysField;
    private IntegerField timeoutField;
    private IntegerField healthIntervalField;
    private Checkbox autoReconnectCheck;
    private TextArea metadataField;
    private TextArea tagsField;
    private TextArea typeSpecificDataField;

    public CreateCustomKeyStoreDialog(CustomKeyStoresView parentView,
                                      KmsApiService kmsApiService,
                                      Runnable onSuccess) {
        super("Create Custom Key Store", onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        setOkButtonText("Create");
        setWidth("700px");
        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        String name = nameField.getValue();
        if (!StringUtils.hasText(name)) {
            append("Store name is required.");
            return false;
        }
        if (name.length() > 255) {
            append("Store name must not exceed 255 characters.");
            return false;
        }

        IEnumCustomKeyStoreType.Types type = typeCombo.getValue();
        if (type == null) {
            append("Store type must be selected.");
            return false;
        }

        parentView.showLoading(true);
        try {
            CreateCustomKeyStoreRequest request = CreateCustomKeyStoreRequest.builder()
                    .customKeyStoreName(name)
                    .customKeyStoreType(type)
                    .maxKeys(maxKeysField.getValue())
                    .connectionTimeoutSeconds(timeoutField.getValue())
                    .healthCheckIntervalSeconds(healthIntervalField.getValue())
                    .autoReconnect(autoReconnectCheck.getValue())
                    .metadata(parseJsonToMap(metadataField.getValue()))
                    .tags(parseJsonToMap(tagsField.getValue()))
                    .customKeyStoreTypeSpecificData(StringUtils.hasText(typeSpecificDataField.getValue()) ? typeSpecificDataField.getValue() : null)
                    .build();

            if (type == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
                String clusterId = cloudHsmClusterId.getValue();
                if (!StringUtils.hasText(clusterId)) {
                    append("CloudHSM Cluster ID is required.");
                    return false;
                }
                String password = keyStorePassword.getValue();
                if (!StringUtils.hasText(password)) {
                    append("Key store password is required.");
                    return false;
                }
                String cert = trustAnchorCert.getValue();
                if (!StringUtils.hasText(cert)) {
                    append("Trust anchor certificate (PEM) is required.");
                    return false;
                }
                request.setCloudHsmClusterId(clusterId);
                request.setKeyStorePassword(password);
                request.setTrustAnchorCertificate(cert);
            } else {
                String endpoint = xksProxyUriEndpoint.getValue();
                if (!StringUtils.hasText(endpoint)) {
                    append("XKS Proxy URI Endpoint is required.");
                    return false;
                }
                if (!endpoint.matches("^https?://.+")) {
                    append("XKS Proxy URI Endpoint must start with http:// or https://");
                    return false;
                }
                String auth = xksProxyAuth.getValue();
                if (!StringUtils.hasText(auth)) {
                    append("XKS proxy authentication credential is required.");
                    return false;
                }
                request.setXksProxyUriEndpoint(endpoint);
                request.setXksProxyUriPath(xksProxyUriPath.getValue());
                request.setXksProxyAuthenticationCredential(auth);
                request.setXksProxyConnectivity(xksProxyConnectivity.getValue());
            }

            ResponseEntity<CreateCustomKeyStoreResponse> response = kmsApiService.createCustomKeyStore(request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Creation failed: " + response.getStatusCode());
                return false;
            }

            Notification.show("Custom key store created", 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;

        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed operation: " + e.getMessage();
            this.append(errorMsg);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        nameField = new TextField("Store Name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setMaxLength(255);
        nameField.setPlaceholder("e.g., my-production-hsm-store");

        typeCombo = new ComboBox<>("Store Type");
        typeCombo.setItems(IEnumCustomKeyStoreType.Types.values());
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setValue(IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM);
        typeCombo.setHelperText("CloudHSM or External Key Store (XKS)");

        // CloudHSM fields
        cloudHsmClusterId = new TextField("CloudHSM Cluster ID");
        cloudHsmClusterId.setPlaceholder("cluster-1234abcd");
        cloudHsmClusterId.setRequiredIndicatorVisible(true);
        keyStorePassword = new PasswordField("Key Store Password");
        keyStorePassword.setPlaceholder("Enter password for the HSM cluster");
        keyStorePassword.setRequiredIndicatorVisible(true);
        trustAnchorCert = new TextArea("Trust Anchor Certificate (PEM)");
        trustAnchorCert.setPlaceholder("-----BEGIN CERTIFICATE-----\nMIIC...\n-----END CERTIFICATE-----");
        trustAnchorCert.setHeight("120px");
        trustAnchorCert.setRequiredIndicatorVisible(true);

        // XKS fields
        xksProxyUriEndpoint = new TextField("XKS Proxy URI Endpoint");
        xksProxyUriEndpoint.setPlaceholder("https://xks.example.com:8443");
        xksProxyUriEndpoint.setRequiredIndicatorVisible(true);
        xksProxyUriPath = new TextField("XKS Proxy URI Path (optional)");
        xksProxyUriPath.setPlaceholder("/api/v1/kms");
        xksProxyAuth = new PasswordField("XKS Proxy Authentication Credential");
        xksProxyAuth.setPlaceholder("API key or token");
        xksProxyAuth.setRequiredIndicatorVisible(true);
        xksProxyConnectivity = new TextField("XKS Proxy Connectivity (optional)");
        xksProxyConnectivity.setPlaceholder("PUBLIC_ENDPOINT");

        // Common fields
        maxKeysField = new IntegerField("Max Keys (optional)");
        maxKeysField.setMin(1);
        maxKeysField.setMax(10000);
        maxKeysField.setPlaceholder("1000");
        timeoutField = new IntegerField("Connection Timeout (seconds)");
        timeoutField.setMin(1);
        timeoutField.setValue(30);
        timeoutField.setPlaceholder("30");
        healthIntervalField = new IntegerField("Health Check Interval (seconds)");
        healthIntervalField.setMin(10);
        healthIntervalField.setValue(60);
        healthIntervalField.setPlaceholder("60");
        autoReconnectCheck = new Checkbox("Auto‑reconnect");
        autoReconnectCheck.setValue(true);

        // Metadata & tags & type‑specific data
        metadataField = new TextArea("Metadata (JSON)");
        metadataField.setPlaceholder("{\"environment\":\"production\",\"team\":\"security\"}");
        metadataField.setHeight("100px");
        tagsField = new TextArea("Tags (JSON)");
        tagsField.setPlaceholder("{\"Project\":\"PCI\",\"CostCenter\":\"12345\"}");
        tagsField.setHeight("100px");
        typeSpecificDataField = new TextArea("Type‑specific data (JSON)");
        typeSpecificDataField.setPlaceholder("Optional configuration JSON");
        typeSpecificDataField.setHeight("80px");

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
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(nameField, typeCombo,
                cloudHsmClusterId, keyStorePassword, trustAnchorCert,
                xksProxyUriEndpoint, xksProxyUriPath, xksProxyAuth, xksProxyConnectivity,
                maxKeysField, timeoutField, healthIntervalField, autoReconnectCheck,
                metadataField, tagsField, typeSpecificDataField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    private Map<String, String> parseJsonToMap(String json) {
        if (!StringUtils.hasText(json)) return null;
        // Remove the literal string "null" if present
        if ("null".equalsIgnoreCase(json.trim())) return null;
        Map<String, String> map = new HashMap<>();
        try {
            String stripped = json.trim();
            if (stripped.startsWith("{") && stripped.endsWith("}")) {
                stripped = stripped.substring(1, stripped.length() - 1);
            }
            if (stripped.isEmpty()) return null;
            for (String pair : stripped.split(",")) {
                if (pair.trim().isEmpty()) continue;
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    if (!key.isEmpty()) map.put(key, value);
                }
            }
        } catch (Exception e) {
            append("Invalid JSON in metadata/tags: " + e.getMessage());
            return null;
        }
        return map.isEmpty() ? null : map;
    }
}