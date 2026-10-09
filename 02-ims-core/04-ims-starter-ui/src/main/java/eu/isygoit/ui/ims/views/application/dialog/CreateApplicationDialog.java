package eu.isygoit.ui.ims.views.application.dialog;

import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationImageService;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link ApplicationDto}. The form itself lives in
 * {@link AbstractApplicationFormDialog}; this class only creates the application.
 */
public class CreateApplicationDialog extends AbstractApplicationFormDialog {

    public CreateApplicationDialog(ApplicationManagementView parentView,
                                   ApplicationService applicationService,
                                   ApplicationImageService applicationImageService,
                                   Runnable onSuccess) {
        super(I18n.t("ims.app.dialog.create.title"), onSuccess, "ims.app.dialog.create",
                parentView, applicationService, applicationImageService);
        setOkButtonText(I18n.t("ims.app.dialog.create.button"));

        buildForm();
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);
    }

    @Override
    ApplicationDto target() {
        return new ApplicationDto();
    }

    @Override
    boolean isTenantReadOnly() {
        return false;
    }

    @Override
    String uploadLabelKey() {
        return "ims.app.dialog.field.upload.image";
    }

    @Override
    boolean imageRequired() {
        return true;
    }

    @Override
    Long persist(ApplicationDto dto) {
        ResponseEntity<ApplicationDto> response = applicationService.create(dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.app.dialog.create.failed", response.getStatusCodeValue()));
            return null;
        }
        Long applicationId = response.getBody().getId();
        if (applicationId == null) {
            append(I18n.t("ims.app.dialog.create.no.id"));
        }
        return applicationId;
    }
}
