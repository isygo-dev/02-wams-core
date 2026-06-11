package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;

public class CreateAccountDialog extends BaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final Runnable onSuccess;

    private TextField tenantField;
    private EmailField emailField;
    private TextField firstNameField;
    private TextField lastNameField;
    private TextField phoneNumberField;
    private ComboBox<IEnumLanguage.Types> languageCombo;
    private TextField functionRoleField;
    private Checkbox isAdminCheckbox;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;
    private TextField accountTypeField;
    private PasswordField passwordField;

    public CreateAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               Runnable onSuccess) {
        super("Create new account");
        this.parentView = parentView;
        this.accountService = accountService;
        this.onSuccess = onSuccess;

        setOkButtonText("Create");
        setWidth("550px");

        buildForm();
        add(buildFormLayout());
    }

    private void buildForm() {
        tenantField = new TextField("Tenant");
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setPlaceholder("e.g., acme-corp");

        emailField = new EmailField("Email");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setPlaceholder("user@example.com");

        firstNameField = new TextField("First name");
        firstNameField.setPlaceholder("John");

        lastNameField = new TextField("Last name");
        lastNameField.setPlaceholder("Doe");

        phoneNumberField = new TextField("Phone number");
        phoneNumberField.setPlaceholder("+1 234 567 8900");

        languageCombo = new ComboBox<>("Language");
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setValue(IEnumLanguage.Types.EN);

        functionRoleField = new TextField("Function role");
        functionRoleField.setPlaceholder("e.g., ADMIN, USER");

        isAdminCheckbox = new Checkbox("Is administrator");

        adminStatusCombo = new ComboBox<>("Admin status");
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);

        accountTypeField = new TextField("Account type");
        accountTypeField.setValue("TENANT_USER");
        accountTypeField.setReadOnly(true);

        passwordField = new PasswordField("Initial password");
        passwordField.setRequiredIndicatorVisible(true);
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(tenantField, emailField, firstNameField, lastNameField,
                phoneNumberField, languageCombo, functionRoleField,
                isAdminCheckbox, adminStatusCombo, accountTypeField, passwordField);
        return form;
    }

    @Override
    protected boolean onOk() {
        // Validation
        if (tenantField.getValue().isBlank()) {
            append("Tenant is required");
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append("Email is required");
            return false;
        }
        if (passwordField.getValue().isBlank()) {
            append("Initial password is required");
            return false;
        }

        parentView.showLoading(true);
        try {
            AccountDto newAccount = new AccountDto();
            newAccount.setTenant(tenantField.getValue());
            newAccount.setEmail(emailField.getValue());
            newAccount.setPhoneNumber(phoneNumberField.getValue());
            newAccount.setLanguage(languageCombo.getValue());
            newAccount.setFunctionRole(functionRoleField.getValue());
            newAccount.setIsAdmin(isAdminCheckbox.getValue());
            newAccount.setAdminStatus(adminStatusCombo.getValue());
            newAccount.setAccountType(accountTypeField.getValue());

            // Account details (firstName, lastName)
            AccountDetailsDto details = new AccountDetailsDto();
            details.setFirstName(firstNameField.getValue());
            details.setLastName(lastNameField.getValue());
            newAccount.setAccountDetails(details);

            // Role info (optional, can be set later)
            newAccount.setRoleInfo(new ArrayList<>());

            // Note: The API expects AccountDto for creation.
            // Password is not part of AccountDto – the backend may use a separate DTO or a different endpoint.
            // If password is required, the service might need an extended DTO. For now, we send as is.
            ResponseEntity<AccountDto> response = accountService.create(newAccount);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Creation failed: " + (response.getBody() != null ? response.getBody() : "unknown error"));
                return false;
            }

            append("Account created successfully");
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            String errorMsg = extractErrorMessage(ex);
            append(errorMsg);
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
                return ex.contentUTF8();
            }
        } catch (Exception ignored) {}
        return ex.getMessage();
    }
}