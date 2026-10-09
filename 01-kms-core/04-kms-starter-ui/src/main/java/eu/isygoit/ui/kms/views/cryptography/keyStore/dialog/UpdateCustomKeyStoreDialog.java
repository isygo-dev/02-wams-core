package eu.isygoit.ui.kms.views.cryptography.keyStore.dialog;

import com.vaadin.flow.component.Component;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.views.cryptography.keyStore.CustomKeyStoresView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * Update dialog for a custom key store ({@code UpdateCustomKeyStoreRequest}). The
 * form itself lives in {@link AbstractCustomKeyStoreFormDialog}; only the fields
 * matching the store type (CloudHSM or XKS) are shown.
 */
public class UpdateCustomKeyStoreDialog extends AbstractCustomKeyStoreFormDialog {

    private final KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store;

    public UpdateCustomKeyStoreDialog(CustomKeyStoresView parentView,
                                      KmsApiService kmsApiService,
                                      Runnable onSuccess,
                                      KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        super(I18n.t("kms.keystore.dialog.update.title", store.getName()), onSuccess, parentView, kmsApiService,
                "kms.keystore.dialog.update.field.");
        this.store = store;
        setOkButtonText(I18n.t("kms.keystore.dialog.update.button"));
        buildForm();
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
                append(I18n.t("kms.keystore.dialog.update.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("kms.keystore.dialog.update.success"));
            return true;

        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("kms.keystore.dialog.update.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    @Override
    Component[] additionalIdentityFields() {
        return new Component[0];
    }

    @Override
    void decorateFields() {
        boolean isCloudHsm = IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM.name().equals(store.getCustomKeyStoreType());
        showTypeSections(isCloudHsm);

        nameField.setPlaceholder(I18n.t("kms.keystore.dialog.update.field.name.placeholder", store.getName()));

        if (isCloudHsm) {
            cloudHsmClusterId.setValue(nullToEmpty(store.getCloudHsmClusterId()));
            cloudHsmClusterId.setPlaceholder(I18n.t("kms.keystore.dialog.update.field.cloudhsm.cluster.placeholder"));
            keyStorePassword.setHelperText(I18n.t("kms.keystore.dialog.update.field.password.helper"));
            keyStorePassword.setPlaceholder(I18n.t("kms.keystore.dialog.update.field.password.placeholder"));
            trustAnchorCert.setValue(nullToEmpty(store.getTrustAnchorCertificate()));
            trustAnchorCert.setPlaceholder(I18n.t("kms.keystore.dialog.update.field.certificate.placeholder"));
        } else {
            xksProxyUriEndpoint.setValue(nullToEmpty(store.getXksProxyUriEndpoint()));
            xksProxyUriEndpoint.setPlaceholder(I18n.t("kms.keystore.dialog.update.field.xks.endpoint.placeholder"));
            xksProxyUriPath.setValue(nullToEmpty(store.getXksProxyUriPath()));
            xksProxyAuth.setHelperText(I18n.t("kms.keystore.dialog.update.field.xks.auth.helper"));
            xksProxyConnectivity.setValue(nullToEmpty(store.getXksProxyConnectivity()));
        }

        if (store.getMaxKeys() != null) maxKeysField.setValue(store.getMaxKeys());
        if (store.getConnectionTimeoutSeconds() != null) timeoutField.setValue(store.getConnectionTimeoutSeconds());
        if (store.getHealthCheckIntervalSeconds() != null)
            healthIntervalField.setValue(store.getHealthCheckIntervalSeconds());
        if (store.getAutoReconnect() != null) autoReconnectCheck.setValue(store.getAutoReconnect());

        metadataField.setValue(nullToEmpty(store.getMetadata()));
        metadataField.setPlaceholder(I18n.t("kms.keystore.dialog.update.field.metadata.placeholder"));
        tagsField.setValue(nullToEmpty(store.getTags()));
        tagsField.setPlaceholder(I18n.t("kms.keystore.dialog.update.field.tags.placeholder"));
        typeSpecificDataField.setValue(nullToEmpty(store.getCustomKeyStoreTypeSpecificData()));
        typeSpecificDataField.setPlaceholder(I18n.t("kms.keystore.dialog.update.field.type.specific.placeholder"));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
