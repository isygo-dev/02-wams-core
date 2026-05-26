package eu.isygoit.ui.views.keyStore.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.keyStore.CustomKeyStoresView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * Dialog for updating an existing custom key store (CloudHSM or XKS).
 * Extends BaseActionDialog to reuse Ok/Cancel buttons and error handling.
 */
public class UpdateCustomKeyStoreDialog extends BaseActionDialog {

    private final CustomKeyStoresView parentView;
    private final KmsApiService kmsApiService;
    private final Runnable onSuccess;

    private final KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store;

    // UI fields
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

    public UpdateCustomKeyStoreDialog(CustomKeyStoresView parentView,
                                      KmsApiService kmsApiService,
                                      Runnable onSuccess,
                                      KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store
    ) {
        super("Update Custom Key Store: " + store.getName(), onSuccess);
        this.onSuccess = onSuccess;
        this.store = store;
        this.kmsApiService = kmsApiService;
        this.parentView = parentView;

        setOkButtonText("Update");
        buildForm();
    }

    private void buildForm() {
        FormLayout form = new FormLayout();
        boolean isCloudHsm = IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM.name().equals(store.getCustomKeyStoreType());

        // Common field: name (optional)
        nameField = new TextField("New Store Name (optional)");
        nameField.setValue(store.getName());

        // CloudHSM specific fields
        if (isCloudHsm) {
            cloudHsmClusterId = new TextField("CloudHSM Cluster ID");
            cloudHsmClusterId.setValue(store.getCloudHsmClusterId());

            keyStorePassword = new PasswordField("Key Store Password (new)");
            trustAnchorCert = new TextArea("Trust Anchor Certificate");

            form.add(nameField, cloudHsmClusterId, keyStorePassword, trustAnchorCert);
        } else {
            // XKS specific fields
            xksProxyUriEndpoint = new TextField("XKS Proxy URI Endpoint");
            xksProxyUriEndpoint.setValue(store.getXksProxyUriEndpoint());

            xksProxyUriPath = new TextField("XKS Proxy URI Path");
            xksProxyUriPath.setValue(store.getXksProxyUriPath());

            xksProxyAuth = new PasswordField("XKS Proxy Authentication Credential (new)");

            xksProxyConnectivity = new TextField("XKS Proxy Connectivity");
            xksProxyConnectivity.setValue(store.getXksProxyConnectivity());

            form.add(nameField, xksProxyUriEndpoint, xksProxyUriPath, xksProxyAuth, xksProxyConnectivity);
        }

        // Common updatable fields
        maxKeysField = new IntegerField("Max Keys");
        maxKeysField.setMin(1);
        maxKeysField.setMax(10000);
        if (store.getMaxKeys() != null) maxKeysField.setValue(store.getMaxKeys());

        timeoutField = new IntegerField("Connection Timeout (seconds)");
        timeoutField.setMin(1);
        if (store.getConnectionTimeoutSeconds() != null) timeoutField.setValue(store.getConnectionTimeoutSeconds());

        healthIntervalField = new IntegerField("Health Check Interval (seconds)");
        healthIntervalField.setMin(10);
        if (store.getHealthCheckIntervalSeconds() != null)
            healthIntervalField.setValue(store.getHealthCheckIntervalSeconds());

        autoReconnectCheck = new Checkbox("Auto‑reconnect");
        if (store.getAutoReconnect() != null) autoReconnectCheck.setValue(store.getAutoReconnect());

        form.add(maxKeysField, timeoutField, healthIntervalField, autoReconnectCheck);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Add the form to the dialog (BaseActionDialog extends Dialog)
        add(form);
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);

        KmsDtos.UpdateCustomKeyStoreRequest request = KmsDtos.UpdateCustomKeyStoreRequest.builder()
                .customKeyStoreId(store.getCustomKeyStoreId())
                .newCustomKeyStoreName(StringUtils.hasText(nameField.getValue()) ? nameField.getValue() : null)
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

        // Common
        if (maxKeysField.getValue() != null) request.setMaxKeys(maxKeysField.getValue());
        if (timeoutField.getValue() != null) request.setConnectionTimeoutSeconds(timeoutField.getValue());
        if (healthIntervalField.getValue() != null)
            request.setHealthCheckIntervalSeconds(healthIntervalField.getValue());
        request.setAutoReconnect(autoReconnectCheck.getValue());

        try {
            ResponseEntity<KmsDtos.UpdateCustomKeyStoreResponse> response =
                    kmsApiService.updateCustomKeyStore(store.getCustomKeyStoreId(), request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Key store update error: " + response.getStatusCode();
                this.append(errorMsg);
                Notification.show("Key store update error: " + errorMsg, 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }

            close();
            Notification.show("Key store updated", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Creation error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }
}