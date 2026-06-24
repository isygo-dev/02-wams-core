package eu.isygoit.ui.kms.views.cryptography.keyStore.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
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

public class UpdateCustomKeyStoreDialog extends BaseActionDialog {

    private final CustomKeyStoresView parentView;
    private final KmsApiService kmsApiService;
    private final KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store;

    private TextField nameField;
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

    public UpdateCustomKeyStoreDialog(CustomKeyStoresView parentView,
                                      KmsApiService kmsApiService,
                                      Runnable onSuccess,
                                      KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        super(I18n.t("keystore.dialog.update.title", store.getName()), onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.store = store;
        setOkButtonText(I18n.t("keystore.dialog.update.button"));
        setWidth("700px");
        setMaxWidth("95%");
        setResizable(true);
        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            KmsDtos.UpdateCustomKeyStoreRequest request = KmsDtos.UpdateCustomKeyStoreRequest.builder()
                    .customKeyStoreId(store.getCustomKeyStoreId())
                    .newCustomKeyStoreName(StringUtils.hasText(nameField.getValue()) ? nameField.getValue() : null)
                    .maxKeys(maxKeysField.getValue())
                    .connectionTimeoutSeconds(timeoutField.getValue())
                    .healthCheckIntervalSeconds(healthIntervalField.getValue())
                    .autoReconnect(autoReconnectCheck.getValue())
                    .metadata(parseJsonToMap(metadataField.getValue()))
                    .tags(parseJsonToMap(tagsField.getValue()))
                    .customKeyStoreTypeSpecificData(StringUtils.hasText(typeSpecificDataField.getValue()) ? typeSpecificDataField.getValue() : null)
                    .build();

            boolean isCloudHsm = IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM.name().equals(store.getCustomKeyStoreType());

            if (isCloudHsm) {
                if (StringUtils.hasText(cloudHsmClusterId.getValue()))
                    request.setCloudHsmClusterId(cloudHsmClusterId.getValue());
                if (StringUtils.hasText(keyStorePassword.getValue()))
                    request.setKeyStorePassword(keyStorePassword.getValue());
                if (StringUtils.hasText(trustAnchorCert.getValue()))
                    request.setTrustAnchorCertificate(trustAnchorCert.getValue());
            } else {
                if (StringUtils.hasText(xksProxyUriEndpoint.getValue()))
                    request.setXksProxyUriEndpoint(xksProxyUriEndpoint.getValue());
                if (StringUtils.hasText(xksProxyUriPath.getValue()))
                    request.setXksProxyUriPath(xksProxyUriPath.getValue());
                if (StringUtils.hasText(xksProxyAuth.getValue()))
                    request.setXksProxyAuthenticationCredential(xksProxyAuth.getValue());
                if (StringUtils.hasText(xksProxyConnectivity.getValue()))
                    request.setXksProxyConnectivity(xksProxyConnectivity.getValue());
            }

            ResponseEntity<KmsDtos.UpdateCustomKeyStoreResponse> response =
                    kmsApiService.updateCustomKeyStore(store.getCustomKeyStoreId(), request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("keystore.dialog.update.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("keystore.dialog.update.success"));
            return true;

        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("keystore.dialog.update.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        boolean isCloudHsm = IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM.name().equals(store.getCustomKeyStoreType());

        nameField = new TextField(I18n.t("keystore.dialog.update.field.name"));
        nameField.setPlaceholder(I18n.t("keystore.dialog.update.field.name.placeholder", store.getName()));

        if (isCloudHsm) {
            cloudHsmClusterId = new TextField(I18n.t("keystore.dialog.update.field.cloudhsm.cluster"));
            cloudHsmClusterId.setValue(nullToEmpty(store.getCloudHsmClusterId()));
            cloudHsmClusterId.setPlaceholder(I18n.t("keystore.dialog.update.field.cloudhsm.cluster.placeholder"));

            keyStorePassword = new PasswordField(I18n.t("keystore.dialog.update.field.password"));
            keyStorePassword.setHelperText(I18n.t("keystore.dialog.update.field.password.helper"));
            keyStorePassword.setPlaceholder(I18n.t("keystore.dialog.update.field.password.placeholder"));

            trustAnchorCert = new TextArea(I18n.t("keystore.dialog.update.field.certificate"));
            trustAnchorCert.setHeight("120px");
            trustAnchorCert.setValue(nullToEmpty(store.getTrustAnchorCertificate()));
            trustAnchorCert.setPlaceholder(I18n.t("keystore.dialog.update.field.certificate.placeholder"));
        } else {
            xksProxyUriEndpoint = new TextField(I18n.t("keystore.dialog.update.field.xks.endpoint"));
            xksProxyUriEndpoint.setValue(nullToEmpty(store.getXksProxyUriEndpoint()));
            xksProxyUriEndpoint.setPlaceholder(I18n.t("keystore.dialog.update.field.xks.endpoint.placeholder"));

            xksProxyUriPath = new TextField(I18n.t("keystore.dialog.update.field.xks.path"));
            xksProxyUriPath.setValue(nullToEmpty(store.getXksProxyUriPath()));

            xksProxyAuth = new PasswordField(I18n.t("keystore.dialog.update.field.xks.auth"));
            xksProxyAuth.setHelperText(I18n.t("keystore.dialog.update.field.xks.auth.helper"));

            xksProxyConnectivity = new TextField(I18n.t("keystore.dialog.update.field.xks.connectivity"));
            xksProxyConnectivity.setValue(nullToEmpty(store.getXksProxyConnectivity()));
        }

        maxKeysField = new IntegerField(I18n.t("keystore.dialog.update.field.max.keys"));
        if (store.getMaxKeys() != null) maxKeysField.setValue(store.getMaxKeys());
        maxKeysField.setMin(1);
        maxKeysField.setMax(10000);

        timeoutField = new IntegerField(I18n.t("keystore.dialog.update.field.timeout"));
        if (store.getConnectionTimeoutSeconds() != null) timeoutField.setValue(store.getConnectionTimeoutSeconds());
        timeoutField.setMin(1);

        healthIntervalField = new IntegerField(I18n.t("keystore.dialog.update.field.health"));
        if (store.getHealthCheckIntervalSeconds() != null)
            healthIntervalField.setValue(store.getHealthCheckIntervalSeconds());
        healthIntervalField.setMin(10);

        autoReconnectCheck = new Checkbox(I18n.t("keystore.dialog.update.field.auto.reconnect"));
        if (store.getAutoReconnect() != null) autoReconnectCheck.setValue(store.getAutoReconnect());

        metadataField = new TextArea(I18n.t("keystore.dialog.update.field.metadata"));
        metadataField.setValue(nullToEmpty(store.getMetadata()));
        metadataField.setHeight("100px");
        metadataField.setPlaceholder(I18n.t("keystore.dialog.update.field.metadata.placeholder"));

        tagsField = new TextArea(I18n.t("keystore.dialog.update.field.tags"));
        tagsField.setValue(nullToEmpty(store.getTags()));
        tagsField.setHeight("100px");
        tagsField.setPlaceholder(I18n.t("keystore.dialog.update.field.tags.placeholder"));

        typeSpecificDataField = new TextArea(I18n.t("keystore.dialog.update.field.type.specific"));
        typeSpecificDataField.setValue(nullToEmpty(store.getCustomKeyStoreTypeSpecificData()));
        typeSpecificDataField.setHeight("80px");
        typeSpecificDataField.setPlaceholder(I18n.t("keystore.dialog.update.field.type.specific.placeholder"));
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(nameField);
        if (cloudHsmClusterId != null) {
            form.add(cloudHsmClusterId, keyStorePassword, trustAnchorCert);
        } else {
            form.add(xksProxyUriEndpoint, xksProxyUriPath, xksProxyAuth, xksProxyConnectivity);
        }
        form.add(maxKeysField, timeoutField, healthIntervalField, autoReconnectCheck,
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}