package eu.isygoit.ui.kms.views.common;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public final class KmsConfirmationDialog extends Dialog {

    public KmsConfirmationDialog(
            String title,
            String message,
            String confirmText,
            String cancelText,
            Runnable onConfirm,
            ButtonVariant... confirmVariants) {
        setHeaderTitle(title);
        setModal(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setWidth("500px");
        setMaxWidth("90%");
        addClassName("wams-dialog-responsive");

        VerticalLayout content = new VerticalLayout(new Span(message));
        content.setPadding(false);
        content.setSpacing(false);
        content.setWidthFull();
        add(content);

        Button cancelButton = new Button(cancelText, event -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button confirmButton = new Button(confirmText, event -> {
            close();
            onConfirm.run();
        });
        if (confirmVariants != null && confirmVariants.length > 0) {
            confirmButton.addThemeVariants(confirmVariants);
        } else {
            confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, confirmButton);
        buttons.setSpacing(true);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        getFooter().add(buttons);
    }
}
