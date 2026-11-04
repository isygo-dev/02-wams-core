package eu.isygoit.ui.kms.views.common;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;

public final class KmsConfirmationDialog extends KmsActionDialog {

    private final Runnable onConfirm;

    public KmsConfirmationDialog(
            String title,
            String message,
            String confirmText,
            String cancelText,
            Runnable onConfirm,
            ButtonVariant... confirmVariants) {
        super(title);
        this.onConfirm = onConfirm;
        setModal(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        if (confirmVariants != null && confirmVariants.length > 0) {
            addThemeVariantsOkButton(confirmVariants);
        } else {
            addThemeVariantsOkButton(ButtonVariant.LUMO_PRIMARY);
        }
        setOkButtonText(confirmText);
        setCancelButtonText(cancelText);
        addContent(new Span(message));
    }

    @Override
    protected boolean onOk() {
        onConfirm.run();
        return true;
    }
}
