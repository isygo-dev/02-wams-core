package eu.isygoit.ui.views.kms.cryptography.random.dialog;

import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.views.common.dialog.PinBaseActionDialog;

public class DeleteRandomKeyDialog extends PinBaseActionDialog {

    private final RandomKeyService keyService;
    private final String keyName;

    public DeleteRandomKeyDialog(RandomKeyService keyService, String keyName, Runnable onSuccess) {
        super("Delete Random Key",
                "Deleting this random key is permanent and cannot be undone.",
                onSuccess, true);
        this.keyService = keyService;
        this.keyName = keyName;
        setOkButtonText("Delete");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        try {
            keyService.deleteRandomKey(keyName);
            return true;
        } catch (Exception e) {
            append("Delete failed: " + e.getMessage());
            return false;
        }
    }
}