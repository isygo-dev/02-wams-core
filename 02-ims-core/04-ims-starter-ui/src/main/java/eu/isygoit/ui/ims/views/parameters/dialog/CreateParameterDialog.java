package eu.isygoit.ui.ims.views.parameters.dialog;

import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link AppParameterDto}. The form itself lives in
 * {@link AbstractParameterFormDialog}; this class only creates the parameter.
 */
public class CreateParameterDialog extends AbstractParameterFormDialog {

    public CreateParameterDialog(ParameterManagementView parentView,
                                 AppParameterService parameterService,
                                 Runnable onSuccess) {
        super(I18n.t("ims.parameter.dialog.create.title"), onSuccess, "ims.parameter.dialog.create",
                parentView, parameterService);
        setOkButtonText(I18n.t("ims.parameter.dialog.create.button"));

        buildForm();
    }

    @Override
    AppParameterDto target() {
        return new AppParameterDto();
    }

    @Override
    boolean persist(AppParameterDto dto) {
        ResponseEntity<AppParameterDto> response = parameterService.create(dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.parameter.dialog.create.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
