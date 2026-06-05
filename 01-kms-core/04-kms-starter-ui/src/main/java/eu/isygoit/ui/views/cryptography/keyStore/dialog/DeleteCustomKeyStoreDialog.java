package eu.isygoit.ui.views.cryptography.keyStore.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.views.cryptography.keyStore.CustomKeyStoresView;
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
        super("Delete Custom Key Store",
                "This action is irreversible. The store will be permanently deleted.",
                onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.store = store;
        setOkButtonText("Delete permanently");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append("Invalid confirmation code");
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.DeleteCustomKeyStoreResponse> response =
                    kmsApiService.deleteCustomKeyStore(store.getCustomKeyStoreId());
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Deletion failed: " + response.getStatusCode());
                return false;
            }

            Notification.show("Custom key store deleted", 6000, Notification.Position.BOTTOM_END)
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
}