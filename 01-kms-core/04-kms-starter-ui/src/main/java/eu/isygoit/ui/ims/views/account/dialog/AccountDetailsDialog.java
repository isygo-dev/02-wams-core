package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class AccountDetailsDialog extends NoActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final Long accountId;

    public AccountDetailsDialog(AccountManagementView parentView, AccountService accountService, Long accountId) {
        super("Account Details");
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountId = accountId;

        setWidth("650px");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AccountDto> response = accountService.findById(accountId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span("Account not found"));
            }
        } catch (FeignException ex) {
            add(new Span("Failed to load details: " + extractErrorMessage(ex)));
        } catch (Exception e) {
            add(new Span("Error: " + e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AccountDto account) {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        addField(form, "Tenant", account.getTenant());
        addField(form, "Email", account.getEmail());
        addField(form, "Full name", account.getFullName());
        addField(form, "Phone", account.getPhoneNumber());
        addField(form, "Language", account.getLanguage() != null ? account.getLanguage().name() : "-");
        addField(form, "Function role", account.getFunctionRole());
        addField(form, "Admin", Boolean.TRUE.equals(account.getIsAdmin()) ? "Yes" : "No");
        addField(form, "Admin status", account.getAdminStatus() != null ? account.getAdminStatus().name() : "-");
        addField(form, "System status", account.getSystemStatus() != null ? account.getSystemStatus().name() : "-");
        addField(form, "Account type", account.getAccountType());
        addField(form, "Origin", account.getOrigin());
        addField(form, "Created", account.getCreateDate() != null ? account.getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "-");
        addField(form, "Last login", account.getLastConnectionDate() != null ? account.getLastConnectionDate().toString() : "-");

        if (account.getRoleInfo() != null && !account.getRoleInfo().isEmpty()) {
            String roles = account.getRoleInfo().stream()
                    .map(RoleInfoDto::getName)
                    .collect(Collectors.joining(", "));
            addField(form, "Roles", roles);
        }

        if (account.getConnectionTracking() != null && !account.getConnectionTracking().isEmpty()) {
            String connections = account.getConnectionTracking().stream()
                    .map(ct -> ct.getCreateDate() + " (" + ct.getDevice() + ")")
                    .collect(Collectors.joining("\n"));
            Span connectionsSpan = new Span(connections);
            connectionsSpan.getStyle().set("white-space", "pre-wrap");
            form.addFormItem(connectionsSpan, "Connections");
        }

        VerticalLayout layout = new VerticalLayout(form);
        layout.setPadding(true);
        add(layout);
    }

    private void addField(FormLayout form, String label, String value) {
        if (value != null && !value.isBlank()) {
            form.addFormItem(new Span(value), label);
        }
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {}
        return ex.getMessage();
    }
}