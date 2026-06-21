package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos.DeleteAliasResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.AliasesView;
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
                primaryKey); // only require PIN for primary key
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.aliasName = aliasName;

        setOkButtonText("Delete");
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("500px");
    }

    @Override
    protected boolean onOk() {
        // Extra safety: validate PIN again (the base class already validated the button, but double-check)
        if (!validatePin()) {
            append("Invalid confirmation code. Deletion aborted.");
            parentView.showLoading(false);
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<DeleteAliasResponse> response = kmsApiService.deleteAlias(aliasName);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Deletion failed: " + response.getStatusCode());
                return false;
            }

            append("Alias deleted");
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }
}