package eu.isygoit.ui.ims.views.application.dialog;

import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationImageService;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link ApplicationDto}. The form itself lives in
 * {@link AbstractApplicationFormDialog}; this class only updates the application.
 * The code and the tenant are read-only.
 */
public class UpdateApplicationDialog extends AbstractApplicationFormDialog {

    private final ApplicationDto application;

    public UpdateApplicationDialog(ApplicationManagementView parentView,
                                   ApplicationService applicationService,
                                   ApplicationImageService applicationImageService,
                                   ApplicationDto application,
                                   Runnable onSuccess) {
        super(I18n.t("ims.app.dialog.update.title"), onSuccess, "ims.app.dialog.update",
                parentView, applicationService, applicationImageService);
        this.application = application;
        setOkButtonText(I18n.t("ims.app.dialog.update.button"));

        buildForm();
        fillFrom(application);
        loadExistingImage();
    }

    @Override
    ApplicationDto target() {
        return application;
    }

    @Override
    boolean isTenantReadOnly() {
        return true;
    }

    @Override
    String uploadLabelKey() {
        return "ims.app.dialog.field.change.image";
    }

    @Override
    boolean imageRequired() {
        return false;
    }

    @Override
    Long persist(ApplicationDto dto) {
        ResponseEntity<ApplicationDto> response = applicationService.update(dto.getId(), dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.app.dialog.update.failed",
                    response.getBody() != null ? response.getBody() : I18n.t("ims.app.dialog.update.no.response.body")));
            return null;
        }
        return dto.getId();
    }

    private void loadExistingImage() {
        try {
            photoSection.showExisting(ImsDialogSupport.toImageDataUri(
                    applicationImageService.downloadImage(application.getId())));
        } catch (Exception ignored) {
            // No existing image or error: keep the placeholder
        }
    }
}
