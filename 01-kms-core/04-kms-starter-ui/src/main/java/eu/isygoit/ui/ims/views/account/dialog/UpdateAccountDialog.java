package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
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

public class UpdateAccountDialog extends BaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final Long accountId;
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

    private AccountDto currentAccount;

    public UpdateAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               Long accountId,
                               Runnable onSuccess) {
        super("Edit account");
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountId = accountId;
        this.onSuccess = onSuccess;

        setOkButtonText("Save");
        setWidth("550px");

        buildForm();
        add(buildFormLayout());

        loadAccountData();
    }

    private void buildForm() {
        tenantField = new TextField("Tenant");
        tenantField.setRequiredIndicatorVisible(true);
        emailField = new EmailField("Email");
        emailField.setRequiredIndicatorVisible(true);
        firstNameField = new TextField("First name");
        lastNameField = new TextField("Last name");
        phoneNumberField = new TextField("Phone number");
        languageCombo = new ComboBox<>("Language");
        languageCombo.setItems(IEnumLanguage.Types.values());
        functionRoleField = new TextField("Function role");
        isAdminCheckbox = new Checkbox("Is administrator");
        adminStatusCombo = new ComboBox<>("Admin status");
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        accountTypeField = new TextField("Account type");
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(tenantField, emailField, firstNameField, lastNameField,
                phoneNumberField, languageCombo, functionRoleField,
                isAdminCheckbox, adminStatusCombo, accountTypeField);
        return form;
    }

    private void loadAccountData() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AccountDto> response = accountService.findById(accountId);
            if (response.getBody() != null) {
                currentAccount = response.getBody();
                populateFields();
            } else {
                append("Account not found");
                close();
            }
        } catch (FeignException ex) {
            append("Failed to load account: " + extractErrorMessage(ex));
            close();
        } catch (Exception e) {
            append("Failed to load account: " + e.getMessage());
            close();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void populateFields() {
        tenantField.setValue(currentAccount.getTenant() != null ? currentAccount.getTenant() : "");
        emailField.setValue(currentAccount.getEmail() != null ? currentAccount.getEmail() : "");
        if (currentAccount.getAccountDetails() != null) {
            firstNameField.setValue(currentAccount.getAccountDetails().getFirstName() != null ? currentAccount.getAccountDetails().getFirstName() : "");
            lastNameField.setValue(currentAccount.getAccountDetails().getLastName() != null ? currentAccount.getAccountDetails().getLastName() : "");
        }
        phoneNumberField.setValue(currentAccount.getPhoneNumber() != null ? currentAccount.getPhoneNumber() : "");
        languageCombo.setValue(currentAccount.getLanguage());
        functionRoleField.setValue(currentAccount.getFunctionRole() != null ? currentAccount.getFunctionRole() : "");
        isAdminCheckbox.setValue(Boolean.TRUE.equals(currentAccount.getIsAdmin()));
        adminStatusCombo.setValue(currentAccount.getAdminStatus());
        accountTypeField.setValue(currentAccount.getAccountType() != null ? currentAccount.getAccountType() : "");
    }

    @Override
    protected boolean onOk() {
        if (tenantField.getValue().isBlank()) {
            append("Tenant is required");
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append("Email is required");
            return false;
        }

        parentView.showLoading(true);
        try {
            currentAccount.setTenant(tenantField.getValue());
            currentAccount.setEmail(emailField.getValue());
            currentAccount.setPhoneNumber(phoneNumberField.getValue());
            currentAccount.setLanguage(languageCombo.getValue());
            currentAccount.setFunctionRole(functionRoleField.getValue());
            currentAccount.setIsAdmin(isAdminCheckbox.getValue());
            currentAccount.setAdminStatus(adminStatusCombo.getValue());
            currentAccount.setAccountType(accountTypeField.getValue());

            if (currentAccount.getAccountDetails() == null) {
                currentAccount.setAccountDetails(new AccountDetailsDto());
            }
            currentAccount.getAccountDetails().setFirstName(firstNameField.getValue());
            currentAccount.getAccountDetails().setLastName(lastNameField.getValue());

            ResponseEntity<AccountDto> response = accountService.update(accountId, currentAccount);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Update failed: " + (response.getBody() != null ? response.getBody() : "unknown error"));
                return false;
            }

            append("Account updated successfully");
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
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
                return ex.contentUTF8();
            }
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}