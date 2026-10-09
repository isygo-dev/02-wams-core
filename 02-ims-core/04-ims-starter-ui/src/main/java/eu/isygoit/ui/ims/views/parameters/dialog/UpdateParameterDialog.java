package eu.isygoit.ui.ims.views.parameters.dialog;

import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link AppParameterDto}. The form itself lives in
 * {@link AbstractParameterFormDialog}; this class only updates the parameter.
 */
public class UpdateParameterDialog extends AbstractParameterFormDialog {

    private final AppParameterDto original;

    public UpdateParameterDialog(ParameterManagementView parentView,
                                 AppParameterService parameterService,
                                 AppParameterDto parameter,
                                 Runnable onSuccess) {
        super(I18n.t("ims.parameter.dialog.update.title"), onSuccess, "ims.parameter.dialog.update",
                parentView, parameterService);
        this.original = parameter;
        setOkButtonText(I18n.t("ims.parameter.dialog.update.button"));

        buildForm();
        fillFrom(parameter);
    }

    @Override
    AppParameterDto target() {
        return original;
    }

    @Override
    boolean persist(AppParameterDto dto) {
        ResponseEntity<AppParameterDto> response = parameterService.update(dto.getId(), dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.parameter.dialog.update.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
