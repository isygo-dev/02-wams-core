package eu.isygoit.ui.views.key.dialogs;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Dialog for permanently deleting a KMS key with a 9‑digit confirmation code.
 */
public class PermanentKeyDeleteDialog extends BaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final KeyManagementView parentView;
    private final String confirmationCode;

    private TextField pinField;

    public PermanentKeyDeleteDialog(KmsApiService kmsApiService, String keyId, KeyManagementView parentView) {
        super("Permanently delete key");
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.parentView = parentView;
        this.confirmationCode = generateConfirmationCode();

        setOkButtonText("Delete permanently");
        setWidth("450px");

        buildContent();

        // Disable ok button initially – will be enabled when code matches
        okButton.setEnabled(false);
    }

    @Override
    protected void onOk() {
        clearError();
        if (pinField.getValue().equals(confirmationCode)) {
            deleteKey();
        } else {
            String errorMsg = "Incorrect confirmation code";
            showError(errorMsg);
            Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void buildContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        layout.add(new Span("This action is irreversible. The key will be permanently removed."));
        layout.add(new Span("To confirm, enter the 9‑digit code below:"));

        Span codeSpan = new Span(confirmationCode);
        codeSpan.getStyle().set("font-weight", "bold");
        codeSpan.getStyle().set("font-size", "28px");
        codeSpan.getStyle().set("font-family", "monospace");
        codeSpan.getStyle().set("background-color", "#f0f0f0");
        codeSpan.getStyle().set("padding", "12px 20px");
        codeSpan.getStyle().set("border-radius", "8px");
        codeSpan.getStyle().set("text-align", "center");
        codeSpan.getStyle().set("letter-spacing", "4px");
        Div codeDiv = new Div(codeSpan);
        codeDiv.getStyle().set("text-align", "center");
        layout.add(codeDiv);

        pinField = new TextField();
        pinField.setPlaceholder("Enter 9‑digit code");
        pinField.setWidthFull();
        pinField.setPattern("[0-9]*");
        pinField.setMaxLength(9);
        pinField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        pinField.setAllowedCharPattern("[0-9]");
        pinField.addValueChangeListener(e -> {
            String value = e.getValue();
            boolean isExactNineDigits = value != null && value.matches("\\d{9}");
            okButton.setEnabled(isExactNineDigits && value.equals(confirmationCode));
        });
        layout.add(pinField);

        add(layout);
    }

    private String generateConfirmationCode() {
        int code = ThreadLocalRandom.current().nextInt(100_000_000, 1_000_000_000);
        return String.valueOf(code);
    }

    private void deleteKey() {
        try {
            ResponseEntity<KmsDtos.DeleteKeyResponse> response = kmsApiService.deleteKey(keyId);
            if (response.getStatusCode().is2xxSuccessful()) {
                close();
                Notification.show("Key permanently deleted", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                parentView.loadKeys();
            } else {
                String errorMsg = "Deletion failed: " + response.getStatusCode();
                showError(errorMsg);
                Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            showError(errorMsg);
            Notification.show("Update error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            showError(errorMsg);
            Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}