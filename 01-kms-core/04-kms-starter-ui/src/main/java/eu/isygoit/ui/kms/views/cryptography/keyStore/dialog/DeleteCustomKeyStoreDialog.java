package eu.isygoit.ui.kms.views.cryptography.keyStore.dialog;

import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyStore.CustomKeyStoresView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class DeleteCustomKeyStoreDialog extends PinBaseActionDialog {

    private final CustomKeyStoresView parentView;
    private final KmsApiService kmsApiService;
    private final KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store;

    public DeleteCustomKeyStoreDialog(CustomKeyStoresView parentView,
                                      KmsApiService kmsApiService,
                                      Runnable onSuccess,
                                      KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        super(I18n.t("kms.keystore.dialog.delete.title"),
                I18n.t("kms.keystore.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.store = store;
        setOkButtonText(I18n.t("kms.keystore.dialog.delete.button"));
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("kms.keystore.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.DeleteCustomKeyStoreResponse> response =
                    kmsApiService.deleteCustomKeyStore(store.getCustomKeyStoreId());
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("kms.keystore.dialog.delete.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("kms.keystore.dialog.delete.success"));
            return true;

        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("kms.keystore.dialog.delete.operation.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }
}