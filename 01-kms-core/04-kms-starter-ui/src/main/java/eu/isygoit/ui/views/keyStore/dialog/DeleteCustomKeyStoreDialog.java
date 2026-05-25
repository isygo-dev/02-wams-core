package eu.isygoit.ui.views.keyStore.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.PinBaseActionDialog;
import eu.isygoit.ui.views.keyStore.CustomKeyStoresView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for deleting a custom key store with a 9‑digit confirmation code.
 */
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
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.DeleteCustomKeyStoreResponse> response =
                    kmsApiService.deleteCustomKeyStore(store.getCustomKeyStoreId());
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Deletion failed: " + response.getStatusCode();
                this.append(errorMsg);
                Notification.show("Deletion error: " + errorMsg, 8000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }

            close();
            Notification.show("Store deleted", 8000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Deletion error: " + errorMsg, 8000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Error: " + errorMsg, 8000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }
}