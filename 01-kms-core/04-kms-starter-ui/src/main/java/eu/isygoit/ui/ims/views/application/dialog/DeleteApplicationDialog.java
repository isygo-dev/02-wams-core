package eu.isygoit.ui.ims.views.application.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import feign.FeignException;

public class DeleteApplicationDialog extends PinBaseActionDialog {

    private final ApplicationManagementView parentView;
    private final ApplicationService applicationService;
    private final Long applicationId;

    public DeleteApplicationDialog(ApplicationManagementView parentView,
                                   ApplicationService applicationService,
                                   Long applicationId,
                                   Runnable onSuccess) {
        super("Delete Application",
                "This action is irreversible. The application and all associated data will be permanently removed.",
                onSuccess);
        this.parentView = parentView;
        this.applicationService = applicationService;
        this.applicationId = applicationId;

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
            applicationService.delete(applicationId);
            append("Application deleted successfully");
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