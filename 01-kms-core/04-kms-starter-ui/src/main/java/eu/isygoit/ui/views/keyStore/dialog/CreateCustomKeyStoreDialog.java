package eu.isygoit.ui.views.keyStore.dialog;

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
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.keyStore.CustomKeyStoresView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * Dialog for creating a custom key store (CloudHSM or XKS).
 */
public class CreateCustomKeyStoreDialog extends BaseActionDialog {

    private final CustomKeyStoresView parentView;
    private final KmsApiService kmsApiService;
    private final Runnable onSuccess;

    // Form fields
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

    public CreateCustomKeyStoreDialog(CustomKeyStoresView parentView,
                                      KmsApiService kmsApiService,
                                      Runnable onSuccess) {
        super("Create Custom Key Store", onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.onSuccess = onSuccess;

        setOkButtonText("Create");
        setWidth("600px");

        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);

        String name = nameField.getValue();
        if (!StringUtils.hasText(name)) {
            String errorMsg = "Store name is required";
            showError(errorMsg);
            Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        IEnumCustomKeyStoreType.Types type = typeCombo.getValue();
        CreateCustomKeyStoreRequest request = CreateCustomKeyStoreRequest.builder()
                .customKeyStoreName(name)
                .customKeyStoreType(type)
                .maxKeys(maxKeysField.getValue())
                .connectionTimeoutSeconds(timeoutField.getValue())
                .healthCheckIntervalSeconds(healthIntervalField.getValue())
                .autoReconnect(autoReconnectCheck.getValue())
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

        try {
            ResponseEntity<CreateCustomKeyStoreResponse> response = kmsApiService.createCustomKeyStore(request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Creation failed: " + response.getStatusCode();
                showError(errorMsg);
                Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false ;
            }

            close();
            Notification.show("Custom key store created", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            showError(errorMsg);
            Notification.show("Creation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            showError(errorMsg);
            Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        nameField = new TextField("Store Name");
        nameField.setRequired(true);
        nameField.setMaxLength(255);

        typeCombo = new ComboBox<>("Store Type");
        typeCombo.setItems(IEnumCustomKeyStoreType.Types.values());
        typeCombo.setRequired(true);
        typeCombo.setValue(IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM);

        // CloudHSM fields
        cloudHsmClusterId = new TextField("CloudHSM Cluster ID");
        keyStorePassword = new PasswordField("Key Store Password");
        trustAnchorCert = new TextArea("Trust Anchor Certificate (PEM)");

        // XKS fields
        xksProxyUriEndpoint = new TextField("XKS Proxy URI Endpoint");
        xksProxyUriPath = new TextField("XKS Proxy URI Path");
        xksProxyAuth = new PasswordField("XKS Proxy Authentication Credential");
        xksProxyConnectivity = new TextField("XKS Proxy Connectivity");

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

        // Common fields
        maxKeysField = new IntegerField("Max Keys (optional)");
        maxKeysField.setMin(1);
        maxKeysField.setMax(10000);

        timeoutField = new IntegerField("Connection Timeout (seconds)");
        timeoutField.setMin(1);

        healthIntervalField = new IntegerField("Health Check Interval (seconds)");
        healthIntervalField.setMin(10);

        autoReconnectCheck = new Checkbox("Auto‑reconnect");
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(nameField, typeCombo,
                cloudHsmClusterId, keyStorePassword, trustAnchorCert,
                xksProxyUriEndpoint, xksProxyUriPath, xksProxyAuth, xksProxyConnectivity,
                maxKeysField, timeoutField, healthIntervalField, autoReconnectCheck);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }
}