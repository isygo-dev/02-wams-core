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

public class CreateTenantDialog extends BaseActionDialog {

    private final TenantManagementView parentView;
    private final TenantService tenantService;
    private final Runnable onSuccess;

    private TextField nameField;
    private TextField codeField;
    private EmailField emailField;
    private TextField phoneField;
    private TextField industryField;
    private TextField urlField;
    private TextArea descriptionField;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;

    public CreateTenantDialog(TenantManagementView parentView,
                              TenantService tenantService,
                              Runnable onSuccess) {
        super("Create Tenant");
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.onSuccess = onSuccess;

        setOkButtonText("Create");
        setWidth("600px");

        buildForm();
        add(buildFormLayout());
    }

    private void buildForm() {
        nameField = new TextField("Name *");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder("Acme Corporation");

        codeField = new TextField("Code (unique)");
        codeField.setPlaceholder("acme-corp");

        emailField = new EmailField("Email *");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setPlaceholder("contact@acme.com");

        phoneField = new TextField("Phone *");
        phoneField.setRequiredIndicatorVisible(true);
        phoneField.setPlaceholder("+1 234 567 8900");

        industryField = new TextField("Industry");
        industryField.setPlaceholder("Technology");

        urlField = new TextField("Website URL");
        urlField.setPlaceholder("https://acme.com");

        descriptionField = new TextArea("Description");
        descriptionField.setPlaceholder("Brief description of the tenant");

        adminStatusCombo = new ComboBox<>("Admin status");
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nameField, codeField, emailField, phoneField,
                industryField, urlField, descriptionField, adminStatusCombo);
        return form;
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
            TenantDto newTenant = TenantDto.builder()
                    .name(nameField.getValue())
                    .code(codeField.getValue())
                    .email(emailField.getValue())
                    .phone(phoneField.getValue())
                    .industry(industryField.getValue())
                    .url(urlField.getValue())
                    .description(descriptionField.getValue())
                    .adminStatus(adminStatusCombo.getValue())
                    .build();

            ResponseEntity<TenantDto> response = tenantService.create(newTenant);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append("Creation failed: " + (response.getBody() != null ? response.getBody() : "no response body"));
                return false;
            }

            append("Tenant created successfully");
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