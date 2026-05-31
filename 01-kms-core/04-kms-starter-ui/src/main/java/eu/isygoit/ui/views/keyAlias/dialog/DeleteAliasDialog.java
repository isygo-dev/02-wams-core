package eu.isygoit.ui.views.keyAlias.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos.DeleteAliasResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.PinBaseActionDialog;
import eu.isygoit.ui.views.keyAlias.AliasesView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for deleting an alias.
 * If the alias is the primary key, a 9‑digit confirmation code is required.
 * Otherwise, it behaves like a simple confirmation dialog.
 */
public class DeleteAliasDialog extends PinBaseActionDialog {

    private final AliasesView parentView;
    private final KmsApiService kmsApiService;
    private final String aliasName;

    public DeleteAliasDialog(AliasesView parentView,
                             KmsApiService kmsApiService,
                             Runnable onSuccess,
                             String aliasName,
                             Boolean primaryKey) {
        super("Delete alias",
                primaryKey ? "WARNING: This is the primary key alias. Deleting it may affect default key operations."
                        : "Are you sure you want to delete alias '" + aliasName + "'?",
                onSuccess,
                Boolean.TRUE.equals(primaryKey)); // only require PIN for primary key
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.aliasName = aliasName;

        setOkButtonText("Delete");
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("500px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);

        // Extra safety: validate PIN again (the base class already validated the button, but double-check)
        if (!validatePin()) {
            String errorMsg = "Invalid confirmation code. Deletion aborted.";
            showError(errorMsg);
            Notification.show(errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            parentView.showLoading(false);
            return false;
        }

        try {
            ResponseEntity<DeleteAliasResponse> response = kmsApiService.deleteAlias(aliasName);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Deletion failed: " + response.getStatusCode();
                showError(errorMsg);
                Notification.show(errorMsg, 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }


            Notification.show("Alias deleted", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;

        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            showError(errorMsg);
            Notification.show("Deletion error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            showError(errorMsg);
            Notification.show("Error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}