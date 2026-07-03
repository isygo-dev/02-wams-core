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
import eu.isygoit.i18n.I18n;
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
        super(I18n.t("ims.account.details.title"));
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
                add(new Span(I18n.t("ims.account.details.not.found")));
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.account.details.load.error", extractErrorMessage(ex))));
            addCloseButton();
        } catch (Exception e) {
            add(new Span(I18n.t("ims.account.details.load.error", e.getMessage())));
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
        basicInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(basicInfo, VaadinIcon.USER, I18n.t("ims.account.details.field.full.name"), account.getFullName());
        addFieldToGrid(basicInfo, VaadinIcon.ENVELOPE, I18n.t("ims.account.details.field.email"), account.getEmail());
        addFieldToGrid(basicInfo, VaadinIcon.PHONE, I18n.t("ims.account.details.field.phone"), account.getPhoneNumber());
        addFieldToGrid(basicInfo, VaadinIcon.BUILDING, I18n.t("ims.account.details.field.tenant"), account.getTenant());
        addFieldToGrid(basicInfo, VaadinIcon.COG, I18n.t("ims.account.details.field.function.role"), account.getFunctionRole());
        addFieldToGrid(basicInfo, VaadinIcon.TAGS, I18n.t("ims.account.details.field.account.type"), account.getAccountType());
        addFieldToGrid(basicInfo, VaadinIcon.CLOUD, I18n.t("ims.account.details.field.origin"), account.getOrigin());
        addFieldToGrid(basicInfo, VaadinIcon.LOCATION_ARROW_CIRCLE, I18n.t("ims.account.details.field.language"), account.getLanguage() != null ? account.getLanguage().name() : null);

        mainLayout.add(createSection(I18n.t("ims.account.details.section.basic"), basicInfo));

        // Status section
        Div statusInfo = new Div();
        statusInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(statusInfo, VaadinIcon.SHIELD, I18n.t("ims.account.details.field.admin"), Boolean.TRUE.equals(account.getIsAdmin()) ? I18n.t("ims.account.details.yes") : I18n.t("ims.account.details.no"));
        addFieldToGrid(statusInfo, VaadinIcon.LOCK, I18n.t("ims.account.details.field.admin.status"), account.getAdminStatus() != null ? account.getAdminStatus().name() : null);
        addFieldToGrid(statusInfo, VaadinIcon.STETHOSCOPE, I18n.t("ims.account.details.field.system.status"), account.getSystemStatus() != null ? account.getSystemStatus().name() : null);
        addFieldToGrid(statusInfo, VaadinIcon.CLOCK, I18n.t("ims.account.details.field.last.login"), account.getLastConnectionDate() != null ? DateHelper.formatToHumanReadable(account.getLastConnectionDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.CALENDAR, I18n.t("ims.account.details.field.created"), account.getCreateDate() != null ? DateHelper.formatToHumanReadable(account.getCreateDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.USER_CHECK, I18n.t("ims.account.details.field.created.by"), account.getCreatedBy());

        mainLayout.add(createSection(I18n.t("ims.account.details.section.status"), statusInfo));

        // Roles (expandable)
        if (account.getRoleInfo() != null && !account.getRoleInfo().isEmpty()) {
            String rolesText = account.getRoleInfo().stream()
                    .map(RoleInfoDto::getName)
                    .collect(Collectors.joining(" • "));
            Component rolesComponent = createCompactList(VaadinIcon.TAG, I18n.t("ims.account.details.section.roles"), rolesText);
            mainLayout.add(new Details(I18n.t("ims.account.details.section.roles"), rolesComponent));
        }

        // Connection tracking (expandable)
        if (account.getConnectionTracking() != null && !account.getConnectionTracking().isEmpty()) {
            VerticalLayout connectionsLayout = new VerticalLayout();
            connectionsLayout.setPadding(false);
            connectionsLayout.setSpacing(true);
            for (ConnectionTrackingDto ct : account.getConnectionTracking()) {
                HorizontalLayout row = createIconRow(VaadinIcon.MOBILE,
                        ct.getCreateDate() != null ? DateHelper.formatToHumanReadable(ct.getCreateDate()) : I18n.t("ims.account.details.unknown.time"),
                        ct.getDevice() != null ? ct.getDevice() : I18n.t("ims.account.details.unknown.device"));
                connectionsLayout.add(row);
            }
            mainLayout.add(new Details(I18n.t("ims.account.details.section.connections"), connectionsLayout));
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
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.addClassName("detail-field-value");

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
        titleSpan.addClassName("wams-section-title");
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
        iconComponent.addClassName("detail-field-icon");

        Span titleSpan = new Span(title + ":");
        titleSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span valueSpan = new Span(items);
        valueSpan.addClassName("detail-field-value");
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
        iconComponent.addClassName("connection-row-icon");
        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("connection-row-label");
        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.addClassName("connection-row-value");
        row.add(iconComponent, labelSpan, valueSpan);
        return row;
    }

    private void addCloseButton() {
        Button closeButton = new Button(I18n.t("ims.account.details.close"), e -> close());
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