package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.button.ButtonVariant;

import java.util.function.Consumer;

/**
 * Basic confirmation dialog for deletions: a {@link PinConfirmActionDialog} with the
 * red (destructive) action button. All delete dialogs of every module use it.
 */
public class DeleteActionDialog extends PinConfirmActionDialog {

    /**
     * @param texts       localised texts
     * @param action      the delete call
     * @param onSuccess   refresh callback, run once by the base dialog after success
     * @param loading     shows / hides the parent view loading bar; may be {@code null}
     * @param moduleClass CSS class of the owning module (for example {@code "ims-dialog"}); may be {@code null}
     */
    public DeleteActionDialog(Texts texts,
                              Action action,
                              Runnable onSuccess,
                              Consumer<Boolean> loading,
                              String moduleClass) {
        this(texts, action, onSuccess, loading, moduleClass, true);
    }

    /** Same as the main constructor, with control over the PIN ({@code false}: message only). */
    public DeleteActionDialog(Texts texts,
                              Action action,
                              Runnable onSuccess,
                              Consumer<Boolean> loading,
                              String moduleClass,
                              boolean requirePin) {
        super(texts, action, onSuccess, loading, moduleClass, requirePin, ButtonVariant.LUMO_ERROR);
    }
}
