package eu.isygoit.ui.kms.views.cryptography.keyStore.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreRequest;
import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreResponse;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import eu.isygoit.ui.kms.views.cryptography.keyStore.CustomKeyStoresView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * Create dialog for a custom key store ({@code CreateCustomKeyStoreRequest}). The
 * form itself lives in {@link AbstractCustomKeyStoreFormDialog}.
 */
public class CreateCustomKeyStoreDialog extends AbstractCustomKeyStoreFormDialog {

    private ComboBox<IEnumCustomKeyStoreType.Types> typeCombo;

    public CreateCustomKeyStoreDialog(CustomKeyStoresView parentView,
                                      KmsApiService kmsApiService,
                                      Runnable onSuccess) {
        super(I18n.t("kms.keystore.dialog.create.title"), onSuccess, parentView, kmsApiService,
                "kms.keystore.dialog.field.");
        setOkButtonText(I18n.t("kms.keystore.dialog.create.button"));
        buildForm();
    }

    @Override
    protected boolean onOk() {
        String name = nameField.getValue();
        if (!StringUtils.hasText(name)) {
            append(I18n.t("kms.keystore.dialog.field.name.required"));
            return false;
        }
        if (name.length() > 255) {
            append(I18n.t("kms.keystore.dialog.field.name.maxlength"));
            return false;
        }

        IEnumCustomKeyStoreType.Types type = typeCombo.getValue();
        if (type == null) {
            append(I18n.t("kms.keystore.dialog.field.type.required"));
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
                    append(I18n.t("kms.keystore.dialog.field.cloudhsm.cluster.required"));
                    return false;
                }
                String password = keyStorePassword.getValue();
                if (!StringUtils.hasText(password)) {
                    append(I18n.t("kms.keystore.dialog.field.password.required"));
                    return false;
                }
                String cert = trustAnchorCert.getValue();
                if (!StringUtils.hasText(cert)) {
                    append(I18n.t("kms.keystore.dialog.field.certificate.required"));
                    return false;
                }
                request.setCloudHsmClusterId(clusterId);
                request.setKeyStorePassword(password);
                request.setTrustAnchorCertificate(cert);
            } else {
                String endpoint = xksProxyUriEndpoint.getValue();
                if (!StringUtils.hasText(endpoint)) {
                    append(I18n.t("kms.keystore.dialog.field.xks.endpoint.required"));
                    return false;
                }
                if (!endpoint.matches("^https?://.+")) {
                    append(I18n.t("kms.keystore.dialog.field.xks.endpoint.invalid"));
                    return false;
                }
                String auth = xksProxyAuth.getValue();
                if (!StringUtils.hasText(auth)) {
                    append(I18n.t("kms.keystore.dialog.field.xks.auth.required"));
                    return false;
                }
                request.setXksProxyUriEndpoint(endpoint);
                request.setXksProxyUriPath(xksProxyUriPath.getValue());
                request.setXksProxyAuthenticationCredential(auth);
                request.setXksProxyConnectivity(xksProxyConnectivity.getValue());
            }

            ResponseEntity<CreateCustomKeyStoreResponse> response = kmsApiService.createCustomKeyStore(request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("kms.keystore.dialog.create.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("kms.keystore.dialog.create.success"));
            return true;

        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("kms.keystore.dialog.create.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }


    @Override
    Component[] additionalIdentityFields() {
        typeCombo = new ComboBox<>(I18n.t("kms.keystore.dialog.field.type"));
        typeCombo.setItems(IEnumCustomKeyStoreType.Types.values());
        KmsEnumTag.useTagRenderer(typeCombo, "kms.enum");
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setWidthFull();
        typeCombo.setValue(IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM);
        typeCombo.setHelperText(I18n.t("kms.keystore.dialog.field.type.helper"));
        return new Component[]{typeCombo};
    }

    @Override
    void decorateFields() {
        nameField.setRequiredIndicatorVisible(true);
        nameField.setMaxLength(255);
        nameField.setPlaceholder(I18n.t("kms.keystore.dialog.field.name.placeholder"));

        // CloudHSM fields
        cloudHsmClusterId.setPlaceholder(I18n.t("kms.keystore.dialog.field.cloudhsm.cluster.placeholder"));
        cloudHsmClusterId.setRequiredIndicatorVisible(true);
        keyStorePassword.setPlaceholder(I18n.t("kms.keystore.dialog.field.password.placeholder"));
        keyStorePassword.setRequiredIndicatorVisible(true);
        trustAnchorCert.setPlaceholder(I18n.t("kms.keystore.dialog.field.certificate.placeholder"));
        trustAnchorCert.setRequiredIndicatorVisible(true);

        // XKS fields
        xksProxyUriEndpoint.setPlaceholder(I18n.t("kms.keystore.dialog.field.xks.endpoint.placeholder"));
        xksProxyUriEndpoint.setRequiredIndicatorVisible(true);
        xksProxyUriPath.setPlaceholder(I18n.t("kms.keystore.dialog.field.xks.path.placeholder"));
        xksProxyAuth.setPlaceholder(I18n.t("kms.keystore.dialog.field.xks.auth.placeholder"));
        xksProxyAuth.setRequiredIndicatorVisible(true);
        xksProxyConnectivity.setPlaceholder(I18n.t("kms.keystore.dialog.field.xks.connectivity.placeholder"));

        // Common fields
        maxKeysField.setPlaceholder(I18n.t("kms.keystore.dialog.field.max.keys.placeholder"));
        timeoutField.setValue(30);
        timeoutField.setPlaceholder(I18n.t("kms.keystore.dialog.field.timeout.placeholder"));
        healthIntervalField.setValue(60);
        healthIntervalField.setPlaceholder(I18n.t("kms.keystore.dialog.field.health.placeholder"));
        autoReconnectCheck.setValue(true);

        // Metadata & tags & type-specific data
        metadataField.setPlaceholder(I18n.t("kms.keystore.dialog.field.metadata.placeholder"));
        tagsField.setPlaceholder(I18n.t("kms.keystore.dialog.field.tags.placeholder"));
        typeSpecificDataField.setPlaceholder(I18n.t("kms.keystore.dialog.field.type.specific.placeholder"));

        // The XKS block is hidden until the type selector asks for it
        showTypeSections(true);
        typeCombo.addValueChangeListener(e ->
                showTypeSections(e.getValue() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM));
    }
}
