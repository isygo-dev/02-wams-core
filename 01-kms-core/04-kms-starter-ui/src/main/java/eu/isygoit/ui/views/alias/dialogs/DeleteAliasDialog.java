package eu.isygoit.ui.views.alias.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.KmsDtos.DeleteAliasResponse;
import eu.isygoit.remote.kms.KmsApiService;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for deleting an alias with confirmation.
 */
public class DeleteAliasDialog extends Dialog {

    private final KmsApiService kmsApiService;
    private final String aliasName;
    private final Runnable onSuccess;

    public DeleteAliasDialog(KmsApiService kmsApiService, String aliasName, Runnable onSuccess) {
        this.kmsApiService = kmsApiService;
        this.aliasName = aliasName;
        this.onSuccess = onSuccess;

        setHeaderTitle("Delete alias");
        setWidth("450px");
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        layout.add(new Span("Are you sure you want to delete alias '" + aliasName + "'?"));
        layout.add(new Span("The underlying KMS key will not be affected."));

        Span errorSpan = new Span();
        errorSpan.getStyle().set("color", "var(--lumo-error-text-color)");
        errorSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        errorSpan.getStyle().set("margin-right", "auto");
        errorSpan.setVisible(false);
        layout.add(errorSpan);

        Button confirmBtn = new Button("Delete", e -> {
            errorSpan.setText("");
            errorSpan.setVisible(false);
            try {
                ResponseEntity<DeleteAliasResponse> response = kmsApiService.deleteAlias(aliasName);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    String errorMsg = "Deletion failed: " + response.getStatusCode();
                    errorSpan.setText(errorMsg);
                    errorSpan.setVisible(true);
                    Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                close();
                Notification.show("Alias deleted", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                if (onSuccess != null) onSuccess.run();
            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                errorSpan.setText(errorMsg);
                errorSpan.setVisible(true);
                Notification.show("Deletion error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                errorSpan.setText(errorMsg);
                errorSpan.setVisible(true);
                Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button cancelBtn = new Button("Cancel", e -> close());

        HorizontalLayout buttonBar = new HorizontalLayout(cancelBtn, confirmBtn);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        layout.add(buttonBar);

        add(layout);
    }
}