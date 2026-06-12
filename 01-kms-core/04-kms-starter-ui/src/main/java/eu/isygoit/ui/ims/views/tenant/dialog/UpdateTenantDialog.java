package eu.isygoit.ui.ims.views.tenant.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class UpdateTenantDialog extends BaseActionDialog {

    private final TenantManagementView parentView;
    private final TenantService tenantService;
    private final TenantDto tenant;
    private final Runnable onSuccess;

    private TextField nameField;
    private TextField codeField;
    private EmailField emailField;
    private TextField phoneField;
    private TextField industryField;
    private TextField urlField;
    private TextArea descriptionField;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;

    public UpdateTenantDialog(TenantManagementView parentView,
                              TenantService tenantService,
                              TenantDto tenant,
                              Runnable onSuccess) {
        super("Edit Tenant");
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenant = tenant;
        this.onSuccess = onSuccess;

        setOkButtonText("Save");
        setWidth("600px");

        buildForm();
        add(buildFormLayout());
        populateFields();
    }

    private void buildForm() {
        nameField = new TextField("Name *");
        nameField.setRequiredIndicatorVisible(true);
        codeField = new TextField("Code");
        emailField = new EmailField("Email *");
        emailField.setRequiredIndicatorVisible(true);
        phoneField = new TextField("Phone *");
        phoneField.setRequiredIndicatorVisible(true);
        industryField = new TextField("Industry");
        urlField = new TextField("Website URL");
        descriptionField = new TextArea("Description");
        adminStatusCombo = new ComboBox<>("Admin status");
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nameField, codeField, emailField, phoneField,
                industryField, urlField, descriptionField, adminStatusCombo);
        return form;
    }

    private void populateFields() {
        nameField.setValue(tenant.getName() != null ? tenant.getName() : "");
        codeField.setValue(tenant.getCode() != null ? tenant.getCode() : "");
        emailField.setValue(tenant.getEmail() != null ? tenant.getEmail() : "");
        phoneField.setValue(tenant.getPhone() != null ? tenant.getPhone() : "");
        industryField.setValue(tenant.getIndustry() != null ? tenant.getIndustry() : "");
        urlField.setValue(tenant.getUrl() != null ? tenant.getUrl() : "");
        descriptionField.setValue(tenant.getDescription() != null ? tenant.getDescription() : "");
        adminStatusCombo.setValue(tenant.getAdminStatus());
    }

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append("Name is required");
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append("Email is required");
            return false;
        }
        if (phoneField.getValue().isBlank()) {
            append("Phone is required");
            return false;
        }

        parentView.showLoading(true);
        try {
            tenant.setName(nameField.getValue());
            tenant.setCode(codeField.getValue());
            tenant.setEmail(emailField.getValue());
            tenant.setPhone(phoneField.getValue());
            tenant.setIndustry(industryField.getValue());
            tenant.setUrl(urlField.getValue());
            tenant.setDescription(descriptionField.getValue());
            tenant.setAdminStatus(adminStatusCombo.getValue());

            ResponseEntity<TenantDto> response = tenantService.update(tenant.getId(), tenant);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append("Update failed: " + (response.getBody() != null ? response.getBody() : "no response body"));
                return false;
            }

            append("Tenant updated successfully");
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
        } catch (Exception ignored) {}
        return ex.getMessage();
    }
}