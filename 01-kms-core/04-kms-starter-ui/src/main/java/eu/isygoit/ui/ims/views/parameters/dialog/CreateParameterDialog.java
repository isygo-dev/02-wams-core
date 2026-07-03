package eu.isygoit.ui.ims.views.parameters.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class CreateParameterDialog extends BaseActionDialog {

    private final ParameterManagementView parentView;
    private final AppParameterService parameterService;
    private final Runnable onSuccess;

    private TextField nameField;
    private TextField valueField;
    private TextField tenantField;
    private TextArea descriptionArea;

    public CreateParameterDialog(ParameterManagementView parentView,
                                 AppParameterService parameterService,
                                 Runnable onSuccess) {
        super(I18n.t("ims.parameter.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.parameterService = parameterService;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.parameter.dialog.create.button"));
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
    }

    private void buildForm() {
        nameField = new TextField(I18n.t("ims.parameter.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder(I18n.t("ims.parameter.dialog.field.name.placeholder"));
        nameField.setWidthFull();

        valueField = new TextField(I18n.t("ims.parameter.dialog.field.value"));
        valueField.setRequiredIndicatorVisible(true);
        valueField.setPlaceholder(I18n.t("ims.parameter.dialog.field.value.placeholder"));
        valueField.setWidthFull();

        tenantField = new TextField(I18n.t("ims.parameter.dialog.field.tenant"));
        tenantField.setPlaceholder(I18n.t("ims.parameter.dialog.field.tenant.placeholder"));
        tenantField.setWidthFull();

        descriptionArea = new TextArea(I18n.t("ims.parameter.dialog.field.description"));
        descriptionArea.setPlaceholder(I18n.t("ims.parameter.dialog.field.description.placeholder"));
        descriptionArea.setWidthFull();
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(nameField, valueField, tenantField, descriptionArea);
        form.setColspan(descriptionArea, 2);
        return form;
    }

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("ims.parameter.dialog.field.name.required"));
            return false;
        }
        if (valueField.getValue().isBlank()) {
            append(I18n.t("ims.parameter.dialog.field.value.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            AppParameterDto newParam = AppParameterDto.builder()
                    .name(nameField.getValue())
                    .value(valueField.getValue())
                    .tenant(tenantField.getValue().isBlank() ? null : tenantField.getValue())
                    .description(descriptionArea.getValue())
                    .build();

            ResponseEntity<AppParameterDto> response = parameterService.create(newParam);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("ims.parameter.dialog.create.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("ims.parameter.dialog.create.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.parameter.dialog.create.error", e.getMessage()));
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