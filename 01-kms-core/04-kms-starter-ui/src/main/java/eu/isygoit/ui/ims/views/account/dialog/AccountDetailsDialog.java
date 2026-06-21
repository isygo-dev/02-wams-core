package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

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

        setWidth("750px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("account-details-dialog");

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
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span("Failed to load details: " + extractErrorMessage(ex)));
            addCloseButton();
        } catch (Exception e) {
            add(new Span("Error: " + e.getMessage()));
            addCloseButton();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AccountDto account) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Basic information section (compact two-column grid)
        Div basicInfo = new Div();
        basicInfo.addClassName("details-grid");
        basicInfo.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(basicInfo, VaadinIcon.USER, "Full name", account.getFullName());
        addFieldToGrid(basicInfo, VaadinIcon.ENVELOPE, "Email", account.getEmail());
        addFieldToGrid(basicInfo, VaadinIcon.PHONE, "Phone", account.getPhoneNumber());
        addFieldToGrid(basicInfo, VaadinIcon.BUILDING, "Tenant", account.getTenant());
        addFieldToGrid(basicInfo, VaadinIcon.COG, "Function role", account.getFunctionRole());
        addFieldToGrid(basicInfo, VaadinIcon.TAGS, "Account type", account.getAccountType());
        addFieldToGrid(basicInfo, VaadinIcon.CLOUD, "Origin", account.getOrigin());
        addFieldToGrid(basicInfo, VaadinIcon.LOCATION_ARROW_CIRCLE, "Language", account.getLanguage() != null ? account.getLanguage().name() : null);

        mainLayout.add(createSection("Basic Information", basicInfo));

        // Status section
        Div statusInfo = new Div();
        statusInfo.addClassName("details-grid");
        statusInfo.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(statusInfo, VaadinIcon.SHIELD, "Admin", Boolean.TRUE.equals(account.getIsAdmin()) ? "Yes" : "No");
        addFieldToGrid(statusInfo, VaadinIcon.LOCK, "Admin status", account.getAdminStatus() != null ? account.getAdminStatus().name() : null);
        addFieldToGrid(statusInfo, VaadinIcon.STETHOSCOPE, "System status", account.getSystemStatus() != null ? account.getSystemStatus().name() : null);
        addFieldToGrid(statusInfo, VaadinIcon.CLOCK, "Last login", account.getLastConnectionDate() != null ? DateHelper.formatToHumanReadable(account.getLastConnectionDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.CALENDAR, "Created", account.getCreateDate() != null ? DateHelper.formatToHumanReadable(account.getCreateDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.USER_CHECK, "Created by", account.getCreatedBy());

        mainLayout.add(createSection("Status & Audit", statusInfo));

        // Roles (expandable)
        if (account.getRoleInfo() != null && !account.getRoleInfo().isEmpty()) {
            String rolesText = account.getRoleInfo().stream()
                    .map(RoleInfoDto::getName)
                    .collect(Collectors.joining(" • "));
            Component rolesComponent = createCompactList(VaadinIcon.TAG, "Roles", rolesText);
            mainLayout.add(new Details("Assigned Roles", rolesComponent));
        }

        // Connection tracking (expandable)
        if (account.getConnectionTracking() != null && !account.getConnectionTracking().isEmpty()) {
            VerticalLayout connectionsLayout = new VerticalLayout();
            connectionsLayout.setPadding(false);
            connectionsLayout.setSpacing(true);
            for (ConnectionTrackingDto ct : account.getConnectionTracking()) {
                HorizontalLayout row = createIconRow(VaadinIcon.MOBILE,
                        ct.getCreateDate() != null ? DateHelper.formatToHumanReadable(ct.getCreateDate()) : "Unknown",
                        ct.getDevice() != null ? ct.getDevice() : "Unknown device");
                connectionsLayout.add(row);
            }
            mainLayout.add(new Details("Connection History", connectionsLayout));
        }

        add(mainLayout);
        addCloseButton();
    }

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("detail-field");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        container.add(row);
    }

    private Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("padding-bottom", "var(--lumo-space-xs)");
        section.add(titleSpan, content);
        return section;
    }

    private Component createCompactList(VaadinIcon icon, String title, String items) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.addClassName("compact-list");

        Icon iconComponent = icon.create();
        iconComponent.setSize("18px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Span titleSpan = new Span(title + ":");
        titleSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span valueSpan = new Span(items);
        valueSpan.getStyle().set("flex", "1");
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);

        layout.add(iconComponent, titleSpan, valueSpan);
        layout.expand(valueSpan);
        return layout;
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        Icon iconComponent = icon.create();
        iconComponent.setSize("14px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "160px");
        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        row.add(iconComponent, labelSpan, valueSpan);
        return row;
    }

    private void addCloseButton() {
        Button closeButton = new Button("Close", e -> close());
        closeButton.addClassName(LumoUtility.Margin.Top.MEDIUM);
        HorizontalLayout buttonBar = new HorizontalLayout(closeButton);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.setWidthFull();
        add(buttonBar);
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