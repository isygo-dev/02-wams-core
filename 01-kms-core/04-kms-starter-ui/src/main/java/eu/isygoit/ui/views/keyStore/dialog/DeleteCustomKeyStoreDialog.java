package eu.isygoit.ui.views.keyStore.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.PinBaseActionDialog;
import eu.isygoit.ui.views.keyStore.CustomKeyStoresView;
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

            Notification.show("Custom key store deleted", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
            return true;

        } catch (FeignException ex) {
            append("Server error: " + (ex.status() == 500 ? ex.contentUTF8() : ex.getMessage()));
        } catch (Exception ex) {
            append("Unexpected error: " + ex.getMessage());
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}