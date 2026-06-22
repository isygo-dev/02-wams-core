package eu.isygoit.ui.ims.views.parameters.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import org.springframework.http.ResponseEntity;

public class UpdateParameterDialog extends BaseActionDialog {

    private final ParameterManagementView parentView;
    private final AppParameterService parameterService;
    private final AppParameterDto parameter;
    private final Runnable onSuccess;

    private TextField nameField;
    private TextField valueField;
    private TextField tenantField;
    private TextArea descriptionArea;

    public UpdateParameterDialog(ParameterManagementView parentView,
                                 AppParameterService parameterService,
                                 AppParameterDto parameter,
                                 Runnable onSuccess) {
        super("Edit Parameter", onSuccess);
        this.parentView = parentView;
        this.parameterService = parameterService;
        this.parameter = parameter;
        this.onSuccess = onSuccess;

        setOkButtonText("Save");
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
        populateFields();
    }

    private void buildForm() {
        nameField = new TextField("Name *");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        valueField = new TextField("Value *");
        valueField.setRequiredIndicatorVisible(true);
        valueField.setWidthFull();

        tenantField = new TextField("Tenant");
        tenantField.setPlaceholder("Leave empty for global parameter");
        tenantField.setWidthFull();

        descriptionArea = new TextArea("Description");
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

    private void populateFields() {
        nameField.setValue(parameter.getName() != null ? parameter.getName() : "");
        valueField.setValue(parameter.getValue() != null ? parameter.getValue() : "");
        tenantField.setValue(parameter.getTenant() != null ? parameter.getTenant() : "");
        descriptionArea.setValue(parameter.getDescription() != null ? parameter.getDescription() : "");
    }

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append("Name is required");
            return false;
        }
        if (valueField.getValue().isBlank()) {
            append("Value is required");
            return false;
        }

        parentView.showLoading(true);
        try {
            parameter.setName(nameField.getValue());
            parameter.setValue(valueField.getValue());
            parameter.setTenant(tenantField.getValue().isBlank() ? null : tenantField.getValue());
            parameter.setDescription(descriptionArea.getValue());

            ResponseEntity<AppParameterDto> response = parameterService.update(parameter.getId(), parameter);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append("Update failed: HTTP " + response.getStatusCodeValue());
                return false;
            }

            append("Parameter updated successfully");
            if (onSuccess != null) onSuccess.run();
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