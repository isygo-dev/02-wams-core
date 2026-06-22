package eu.isygoit.ui.ims.views.annex.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;

public class DeleteAnnexDialog extends PinBaseActionDialog {

    private final AnnexManagementView parentView;
    private final AnnexService annexService;
    private final Long annexId;

    public DeleteAnnexDialog(AnnexManagementView parentView,
                             AnnexService annexService,
                             Long annexId,
                             Runnable onSuccess) {
        super("Delete Annex",
                "This action is irreversible. The annex entry will be permanently removed.",
                onSuccess);
        this.parentView = parentView;
        this.annexService = annexService;
        this.annexId = annexId;

        setOkButtonText("Delete permanently");
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append("Invalid confirmation code");
            return false;
        }

        parentView.showLoading(true);
        try {
            annexService.delete(annexId);
            append("Annex deleted successfully");
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}