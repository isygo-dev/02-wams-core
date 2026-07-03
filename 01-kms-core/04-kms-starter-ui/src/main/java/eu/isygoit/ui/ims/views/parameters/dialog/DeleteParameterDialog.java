package eu.isygoit.ui.ims.views.parameters.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
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
        super(I18n.t("ims.parameter.dialog.delete.title"),
                I18n.t("ims.parameter.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.parameterService = parameterService;
        this.parameterId = parameterId;

        setOkButtonText(I18n.t("ims.parameter.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("ims.parameter.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            parameterService.delete(parameterId);
            append(I18n.t("ims.parameter.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.parameter.dialog.delete.error", e.getMessage()));
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