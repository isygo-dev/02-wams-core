package eu.isygoit.ui.views.alias.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.KmsDtos.DeleteAliasResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.alias.AliasesView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for deleting an alias with confirmation.
 * Extends BaseActionDialog to reuse standard Ok/Cancel buttons and error handling.
 */
public class DeleteAliasDialog extends BaseActionDialog {

    private final AliasesView parentView;
    private final KmsApiService kmsApiService;
    private final Runnable onSuccess;

    private final String aliasName;

    public DeleteAliasDialog(AliasesView parentView,
                             KmsApiService kmsApiService,
                             Runnable onSuccess,
                             String aliasName) {
        super("Delete alias", onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.aliasName = aliasName;
        this.onSuccess = onSuccess;

        setOkButtonText("Delete");
        okButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        buildContent();
        setWidth("450px");
    }

    private void buildContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        layout.add(new Span("Are you sure you want to delete alias '" + aliasName + "'?"));
        layout.add(new Span("The underlying KMS key will not be affected."));

        // Add the content to the dialog (BaseActionDialog extends Dialog)
        add(layout);
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            ResponseEntity<DeleteAliasResponse> response = kmsApiService.deleteAlias(aliasName);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Deletion failed: " + response.getStatusCode();
                showError(errorMsg);
                Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            close();
            Notification.show("Alias deleted", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            showError(errorMsg);
            Notification.show("Deletion error: " + errorMsg, 5000, Notification.Position.TOP_END)
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
}