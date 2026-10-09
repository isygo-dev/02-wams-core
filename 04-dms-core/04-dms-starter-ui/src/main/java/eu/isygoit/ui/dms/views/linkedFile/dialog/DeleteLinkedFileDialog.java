package eu.isygoit.ui.dms.views.linkedFile.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.dms.views.linkedFile.LinkedFileManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Confirms the deletion of a linked file, with an extra irreversibility note.
 * The server answers {@code true} when the file was really removed.
 */
public class DeleteLinkedFileDialog extends DeleteActionDialog {

    public DeleteLinkedFileDialog(LinkedFileManagementView parentView,
                                  LinkedFileService linkedFileService,
                                  String fileCode,
                                  String fileName,
                                  Runnable onSuccess) {
        super(new Texts(
                        I18n.t("dms.linkedfile.dialog.delete.title"),
                        I18n.t("dms.linkedfile.dialog.delete.message", fileName != null ? fileName : fileCode),
                        I18n.t("dms.linkedfile.dialog.delete.button"),
                        I18n.t("dms.linkedfile.dialog.delete.invalid.code"),
                        I18n.t("dms.linkedfile.dialog.delete.success"),
                        detail -> I18n.t("dms.linkedfile.dialog.delete.error", detail)),
                () -> {
                    ResponseEntity<Boolean> response = linkedFileService.deleteFile(fileCode);
                    if (response.getStatusCode().is2xxSuccessful() && !Boolean.TRUE.equals(response.getBody())) {
                        throw new IllegalStateException(I18n.t("dms.linkedfile.dialog.delete.failed"));
                    }
                    return response;
                },
                onSuccess,
                parentView::showLoading,
                "dms-dialog");
        addNote(I18n.t("dms.linkedfile.dialog.delete.warning"));
    }
}