package eu.isygoit.ui.kms.views.cryptography.keyStore.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreRequest;
import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreResponse;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyStore.CustomKeyStoresView;
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
        super(I18n.t("keystore.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        setOkButtonText(I18n.t("keystore.dialog.create.button"));
        setWidth("700px");
        setMaxWidth("95%");
        setResizable(true);
        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        String name = nameField.getValue();
        if (!StringUtils.hasText(name)) {
            append(I18n.t("keystore.dialog.field.name.required"));
            return false;
        }
        if (name.length() > 255) {
            append(I18n.t("keystore.dialog.field.name.maxlength"));
            return false;
        }

        IEnumCustomKeyStoreType.Types type = typeCombo.getValue();
        if (type == null) {
            append(I18n.t("keystore.dialog.field.type.required"));
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
                    append(I18n.t("keystore.dialog.field.cloudhsm.cluster.required"));
                    return false;
                }
                String password = keyStorePassword.getValue();
                if (!StringUtils.hasText(password)) {
                    append(I18n.t("keystore.dialog.field.password.required"));
                    return false;
                }
                String cert = trustAnchorCert.getValue();
                if (!StringUtils.hasText(cert)) {
                    append(I18n.t("keystore.dialog.field.certificate.required"));
                    return false;
                }
                request.setCloudHsmClusterId(clusterId);
                request.setKeyStorePassword(password);
                request.setTrustAnchorCertificate(cert);
            } else {
                String endpoint = xksProxyUriEndpoint.getValue();
                if (!StringUtils.hasText(endpoint)) {
                    append(I18n.t("keystore.dialog.field.xks.endpoint.required"));
                    return false;
                }
                if (!endpoint.matches("^https?://.+")) {
                    append(I18n.t("keystore.dialog.field.xks.endpoint.invalid"));
                    return false;
                }
                String auth = xksProxyAuth.getValue();
                if (!StringUtils.hasText(auth)) {
                    append(I18n.t("keystore.dialog.field.xks.auth.required"));
                    return false;
                }
                request.setXksProxyUriEndpoint(endpoint);
                request.setXksProxyUriPath(xksProxyUriPath.getValue());
                request.setXksProxyAuthenticationCredential(auth);
                request.setXksProxyConnectivity(xksProxyConnectivity.getValue());
            }

            ResponseEntity<CreateCustomKeyStoreResponse> response = kmsApiService.createCustomKeyStore(request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("keystore.dialog.create.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("keystore.dialog.create.success"));
            return true;

        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("keystore.dialog.create.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        nameField = new TextField(I18n.t("keystore.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setMaxLength(255);
        nameField.setPlaceholder(I18n.t("keystore.dialog.field.name.placeholder"));

        typeCombo = new ComboBox<>(I18n.t("keystore.dialog.field.type"));
        typeCombo.setItems(IEnumCustomKeyStoreType.Types.values());
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setValue(IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM);
        typeCombo.setHelperText(I18n.t("keystore.dialog.field.type.helper"));

        // CloudHSM fields
        cloudHsmClusterId = new TextField(I18n.t("keystore.dialog.field.cloudhsm.cluster"));
        cloudHsmClusterId.setPlaceholder(I18n.t("keystore.dialog.field.cloudhsm.cluster.placeholder"));
        cloudHsmClusterId.setRequiredIndicatorVisible(true);
        keyStorePassword = new PasswordField(I18n.t("keystore.dialog.field.password"));
        keyStorePassword.setPlaceholder(I18n.t("keystore.dialog.field.password.placeholder"));
        keyStorePassword.setRequiredIndicatorVisible(true);
        trustAnchorCert = new TextArea(I18n.t("keystore.dialog.field.certificate"));
        trustAnchorCert.setPlaceholder(I18n.t("keystore.dialog.field.certificate.placeholder"));
        trustAnchorCert.setHeight("120px");
        trustAnchorCert.setRequiredIndicatorVisible(true);

        // XKS fields
        xksProxyUriEndpoint = new TextField(I18n.t("keystore.dialog.field.xks.endpoint"));
        xksProxyUriEndpoint.setPlaceholder(I18n.t("keystore.dialog.field.xks.endpoint.placeholder"));
        xksProxyUriEndpoint.setRequiredIndicatorVisible(true);
        xksProxyUriPath = new TextField(I18n.t("keystore.dialog.field.xks.path"));
        xksProxyUriPath.setPlaceholder(I18n.t("keystore.dialog.field.xks.path.placeholder"));
        xksProxyAuth = new PasswordField(I18n.t("keystore.dialog.field.xks.auth"));
        xksProxyAuth.setPlaceholder(I18n.t("keystore.dialog.field.xks.auth.placeholder"));
        xksProxyAuth.setRequiredIndicatorVisible(true);
        xksProxyConnectivity = new TextField(I18n.t("keystore.dialog.field.xks.connectivity"));
        xksProxyConnectivity.setPlaceholder(I18n.t("keystore.dialog.field.xks.connectivity.placeholder"));

        // Common fields
        maxKeysField = new IntegerField(I18n.t("keystore.dialog.field.max.keys"));
        maxKeysField.setMin(1);
        maxKeysField.setMax(10000);
        maxKeysField.setPlaceholder(I18n.t("keystore.dialog.field.max.keys.placeholder"));
        timeoutField = new IntegerField(I18n.t("keystore.dialog.field.timeout"));
        timeoutField.setMin(1);
        timeoutField.setValue(30);
        timeoutField.setPlaceholder(I18n.t("keystore.dialog.field.timeout.placeholder"));
        healthIntervalField = new IntegerField(I18n.t("keystore.dialog.field.health"));
        healthIntervalField.setMin(10);
        healthIntervalField.setValue(60);
        healthIntervalField.setPlaceholder(I18n.t("keystore.dialog.field.health.placeholder"));
        autoReconnectCheck = new Checkbox(I18n.t("keystore.dialog.field.auto.reconnect"));
        autoReconnectCheck.setValue(true);

        // Metadata & tags & type‑specific data
        metadataField = new TextArea(I18n.t("keystore.dialog.field.metadata"));
        metadataField.setPlaceholder(I18n.t("keystore.dialog.field.metadata.placeholder"));
        metadataField.setHeight("100px");
        tagsField = new TextArea(I18n.t("keystore.dialog.field.tags"));
        tagsField.setPlaceholder(I18n.t("keystore.dialog.field.tags.placeholder"));
        tagsField.setHeight("100px");
        typeSpecificDataField = new TextArea(I18n.t("keystore.dialog.field.type.specific"));
        typeSpecificDataField.setPlaceholder(I18n.t("keystore.dialog.field.type.specific.placeholder"));
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
            append(I18n.t("keystore.dialog.error.invalid.json", e.getMessage()));
            return null;
        }
        return map.isEmpty() ? null : map;
    }
}