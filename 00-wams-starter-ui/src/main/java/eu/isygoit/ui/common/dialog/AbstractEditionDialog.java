package eu.isygoit.ui.common.dialog;

/**
 * Shared ancestry for dialogs that create or edit entity data.
 *
 * <p>The base action dialog supplies the common header, validation feedback,
 * and save/cancel footer; concrete forms retain their domain-specific behavior.
 */
public abstract class AbstractEditionDialog extends BaseActionDialog {

    protected AbstractEditionDialog(String title, Runnable onSuccess) {
        super(title, onSuccess);
    }

    protected AbstractEditionDialog(String title) {
        super(title);
    }
}
