package eu.isygoit.ui.ims.views.parameters.dialog;

import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;
import feign.FeignException;

public class DeleteParameterDialog extends PinBaseActionDialog {

    private final ParameterManagementView parentView;
    private final AppParameterService parameterService;
    private final Long parameterId;

    public DeleteParameterDialog(ParameterManagementView parentView,
                                 AppParameterService parameterService,
                                 Long parameterId,
                                 Runnable onSuccess) {
        super("Delete Parameter",
                "This action is irreversible. The parameter will be permanently removed.",
                onSuccess);
        this.parentView = parentView;
        this.parameterService = parameterService;
        this.parameterId = parameterId;

        setOkButtonText("Delete permanently");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            parameterService.delete(parameterId);
            append("Parameter deleted successfully");
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