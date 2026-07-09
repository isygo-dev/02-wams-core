package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.Component;
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
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.stream.Collectors;

public class AccountDetailsViewDialog extends DetailsViewDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final Long accountId;

    public AccountDetailsViewDialog(AccountManagementView parentView, AccountService accountService, Long accountId) {
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
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.account.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.account.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AccountDto account) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity — name/email/code (text identifiers)
        Div identityInfo = new Div();
        identityInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(identityInfo, VaadinIcon.USER, I18n.t("ims.account.details.field.full.name"), account.getFullName());
        addFieldToGrid(identityInfo, VaadinIcon.ENVELOPE, I18n.t("ims.account.details.field.email"), account.getEmail(), true);
        addFieldToGrid(identityInfo, VaadinIcon.HASH, I18n.t("ims.account.details.field.code"), account.getCode(), true);

        mainLayout.add(createSection(I18n.t("ims.account.details.section.identity"), identityInfo));

        // Classification & status — type/role/status/flags/language
        Div classificationInfo = new Div();
        classificationInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(classificationInfo, VaadinIcon.COG, I18n.t("ims.account.details.field.function.role"), account.getFunctionRole());
        addFieldToGrid(classificationInfo, VaadinIcon.TAGS, I18n.t("ims.account.details.field.account.type"), account.getAccountType());
        addFieldToGrid(classificationInfo, VaadinIcon.SIGN_IN, I18n.t("ims.account.details.field.auth.type"), account.getAuthType() != null ? account.getAuthType().name() : null);
        addFieldToGrid(classificationInfo, VaadinIcon.COMMENT, I18n.t("ims.account.details.field.chat.status"), account.getChatStatus() != null ? account.getChatStatus().name() : null);
        addFieldToGrid(classificationInfo, VaadinIcon.LOCATION_ARROW_CIRCLE, I18n.t("ims.account.details.field.language"), account.getLanguage() != null ? account.getLanguage().name() : null);
        addFieldToGrid(classificationInfo, VaadinIcon.SHIELD, I18n.t("ims.account.details.field.admin"), Boolean.TRUE.equals(account.getIsAdmin()) ? I18n.t("ims.account.details.yes") : I18n.t("ims.account.details.no"));
        addFieldToGrid(classificationInfo, VaadinIcon.LOCK, I18n.t("ims.account.details.field.admin.status"), account.getAdminStatus() != null ? account.getAdminStatus().name() : null);
        addFieldToGrid(classificationInfo, VaadinIcon.STETHOSCOPE, I18n.t("ims.account.details.field.system.status"), account.getSystemStatus() != null ? account.getSystemStatus().name() : null);

        mainLayout.add(createSection(I18n.t("ims.account.details.section.classification"), classificationInfo));

        // Contact / relations — phone/tenant/origin/country/address/contacts/last login
        Div contactInfo = new Div();
        contactInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(contactInfo, VaadinIcon.PHONE, I18n.t("ims.account.details.field.phone"), account.getPhoneNumber(), true);
        addFieldToGrid(contactInfo, VaadinIcon.BUILDING, I18n.t("ims.account.details.field.tenant"), account.getTenant(), true);
        addFieldToGrid(contactInfo, VaadinIcon.CLOUD, I18n.t("ims.account.details.field.origin"), account.getOrigin());
        addFieldToGrid(contactInfo, VaadinIcon.CLOCK, I18n.t("ims.account.details.field.last.login"), account.getLastConnectionDate() != null ? DateHelper.formatToHumanReadable(account.getLastConnectionDate()) : null);
        if (account.getAccountDetails() != null) {
            addFieldToGrid(contactInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.account.details.field.country"), account.getAccountDetails().getCountry());
        }

        mainLayout.add(createSection(I18n.t("ims.account.details.section.contact"), contactInfo));

        // Address (if present)
        if (account.getAccountDetails() != null && account.getAccountDetails().getAddress() != null) {
            var addr = account.getAccountDetails().getAddress();
            String address = (addr.getStreet() != null ? addr.getStreet() : "") +
                    (addr.getCity() != null ? ", " + addr.getCity() : "") +
                    (addr.getCountry() != null ? ", " + addr.getCountry() : "");
            if (!address.isBlank()) {
                Div addressGrid = new Div();
                addressGrid.addClassName("wams-card__detail-grid");
                addFieldToGrid(addressGrid, VaadinIcon.MAP_MARKER, I18n.t("ims.account.details.field.address"), address);
                mainLayout.add(addressGrid);
            }
        }

        // Contacts (compact list, if present)
        if (account.getAccountDetails() != null && account.getAccountDetails().getContacts() != null
                && !account.getAccountDetails().getContacts().isEmpty()) {
            String contactsText = account.getAccountDetails().getContacts().stream()
                    .map(c -> (c.getType() != null ? c.getType().name() + ": " : "") + (c.getValue() != null ? c.getValue() : ""))
                    .collect(Collectors.joining(" • "));
            mainLayout.add(createCompactList(VaadinIcon.PAPERPLANE, I18n.t("ims.account.details.field.contacts"), contactsText));
        }

        // Audit — created/updated by & date
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.account.details.field.created"), account.getCreateDate() != null ? DateHelper.formatToHumanReadable(account.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.account.details.field.created.by"), account.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.account.details.field.updated"), account.getUpdateDate() != null ? DateHelper.formatToHumanReadable(account.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.account.details.field.updated.by"), account.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.account.details.section.audit"), auditInfo));

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
    }

    private Component createCompactList(VaadinIcon icon, String title, String items) {
        VerticalLayout field = new VerticalLayout();
        field.setPadding(false);
        field.setSpacing(false);
        field.addClassName("wams-card__detail-field");
        field.addClassName("compact-list");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(false);
        labelRow.addClassName("wams-card__detail-field-label-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("12px");
        iconComponent.addClassName("detail-field-icon");

        Span titleSpan = new Span(title);
        titleSpan.addClassName("wams-card__detail-field-label");

        labelRow.add(iconComponent, titleSpan);

        Span valueSpan = new Span(items);
        valueSpan.addClassName("wams-card__detail-field-value");

        field.add(labelRow, valueSpan);
        return field;
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

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}