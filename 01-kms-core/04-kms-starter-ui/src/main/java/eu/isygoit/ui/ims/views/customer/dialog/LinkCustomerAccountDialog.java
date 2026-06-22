package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

public class LinkCustomerAccountDialog extends BaseActionDialog {

    private final CustomerManagementView parentView;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final Long customerId;
    private final Runnable onSuccess;

    private ComboBox<MinAccountDto> accountCombo;
    private List<MinAccountDto> availableAccounts = new ArrayList<>();

    public LinkCustomerAccountDialog(CustomerManagementView parentView,
                                     CustomerService customerService,
                                     AccountService accountService,
                                     Long customerId,
                                     Runnable onSuccess) {
        super("Link to Account", onSuccess);
        this.parentView = parentView;
        this.customerService = customerService;
        this.accountService = accountService;
        this.customerId = customerId;
        this.onSuccess = onSuccess;

        setOkButtonText("Link");
        setWidth("500px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildLayout());
        loadAccounts();
    }

    private void buildForm() {
        accountCombo = new ComboBox<>("Select account");
        accountCombo.setRequiredIndicatorVisible(true);
        accountCombo.setPlaceholder("Choose an account to link");
        accountCombo.setItemLabelGenerator(account ->
                account.getEmail() + " (" + account.getCode() + ")"
        );
        accountCombo.setWidthFull();
    }

    private FormLayout buildLayout() {
        FormLayout form = new FormLayout();
        form.add(accountCombo);
        return form;
    }

    private void loadAccounts() {
        parentView.showLoading(true);
        try {
            ResponseEntity<List<MinAccountDto>> response = accountService.getAccounts();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                availableAccounts = response.getBody();
                accountCombo.setItems(availableAccounts);
                if (availableAccounts.isEmpty()) {
                    Notification.show("No accounts available to link.", 3000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                }
            } else {
                append("Failed to load accounts list.");
            }
        } catch (FeignException ex) {
            append("Error loading accounts: " + extractErrorMessage(ex));
        } catch (Exception e) {
            append("Unexpected error: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
    }

    @Override
    protected boolean onOk() {
        MinAccountDto selectedAccount = accountCombo.getValue();
        if (selectedAccount == null) {
            append("Please select an account");
            return false;
        }

        String accountCode = selectedAccount.getCode();
        parentView.showLoading(true);
        try {
            ResponseEntity<CustomerDto> response = customerService.LinkToExistingAccount(customerId, accountCode);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Failed to link account: HTTP " + response.getStatusCodeValue());
                return false;
            }

            append("Customer linked to account '" + accountCode + "' successfully");
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Operation failed: " + e.getMessage());
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