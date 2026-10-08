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
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.stream.Collectors;

public class AccountDetailsViewDialog extends ImsDetailsDialog {

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
        // Identity — name/email/code (text identifiers)
        Div identityInfo = new Div();
        identityInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(identityInfo, VaadinIcon.USER, I18n.t("ims.account.details.field.full.name"), account.getFullName());
        addFieldToGrid(identityInfo, VaadinIcon.ENVELOPE, I18n.t("ims.account.details.field.email"), account.getEmail(), true);
        addFieldToGrid(identityInfo, VaadinIcon.HASH, I18n.t("ims.account.details.field.code"), account.getCode(), true);

        addTab(I18n.t("ims.account.details.section.identity"), createSection(I18n.t("ims.account.details.section.identity"), identityInfo));

        // Classification & status — type/role/status/flags/language
        Div classificationInfo = new Div();
        classificationInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(classificationInfo, VaadinIcon.COG, I18n.t("ims.account.details.field.function.role"), account.getFunctionRole());
        addFieldToGrid(classificationInfo, VaadinIcon.TAGS, I18n.t("ims.account.details.field.account.type"), account.getAccountType());
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.SIGN_IN, I18n.t("ims.account.details.field.auth.type"), account.getAuthType(), null);
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.COMMENT, I18n.t("ims.account.details.field.chat.status"), account.getChatStatus(), null);
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.LOCATION_ARROW_CIRCLE, I18n.t("ims.account.details.field.language"), account.getLanguage(), "ims.enum.language");
        addFieldToGrid(classificationInfo, VaadinIcon.SHIELD, I18n.t("ims.account.details.field.admin"), Boolean.TRUE.equals(account.getIsAdmin()) ? I18n.t("ims.account.details.yes") : I18n.t("ims.account.details.no"));
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.LOCK, I18n.t("ims.account.details.field.admin.status"), account.getAdminStatus(), null);
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.STETHOSCOPE, I18n.t("ims.account.details.field.system.status"), account.getSystemStatus(), null);

        addTab(I18n.t("ims.account.details.section.classification"), createSection(I18n.t("ims.account.details.section.classification"), classificationInfo));

        // Contact / relations — phone/tenant/origin/country/address/contacts/last login
        Div contactInfo = new Div();
        contactInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(contactInfo, VaadinIcon.PHONE, I18n.t("ims.account.details.field.phone"), account.getPhoneNumber(), true);
        addFieldToGrid(contactInfo, VaadinIcon.BUILDING, I18n.t("ims.account.details.field.tenant"), account.getTenant(), true);
        if (account.getOrigin() != null && !account.getOrigin().isBlank()) {
            contactInfo.add(ImsEnumTag.detailField(VaadinIcon.CLOUD, I18n.t("ims.account.details.field.origin"),
                    ImsEnumTag.ofValue(account.getOrigin(), "ims.enum.origin")));
        }
        addFieldToGrid(contactInfo, VaadinIcon.CLOCK, I18n.t("ims.account.details.field.last.login"), account.getLastConnectionDate() != null ? DateHelper.formatToHumanReadable(account.getLastConnectionDate()) : null);
        if (account.getAccountDetails() != null) {
            addFieldToGrid(contactInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.account.details.field.country"), account.getAccountDetails().getCountry());
        }

        addTab(I18n.t("ims.account.details.section.contact"), createSection(I18n.t("ims.account.details.section.contact"), contactInfo));

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
                addTab(I18n.t("ims.dialog.tab.address"), addressGrid);
            }
        }

        // Contacts (compact list, if present)
        if (account.getAccountDetails() != null && account.getAccountDetails().getContacts() != null
                && !account.getAccountDetails().getContacts().isEmpty()) {
            VerticalLayout contactsLayout = new VerticalLayout();
            contactsLayout.setPadding(false);
            contactsLayout.setSpacing(true);
            account.getAccountDetails().getContacts().forEach(contact -> {
                HorizontalLayout row = new HorizontalLayout();
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.setSpacing(true);
                if (contact.getType() != null) {
                    row.add(ImsEnumTag.of(contact.getType(), "ims.enum.contact"));
                }
                row.add(new Span(contact.getValue() == null ? "" : contact.getValue()));
                contactsLayout.add(row);
            });
            addTab(I18n.t("ims.dialog.tab.contacts"), contactsLayout);
        }

        // Audit — created/updated by & date
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.account.details.field.created"), account.getCreateDate() != null ? DateHelper.formatToHumanReadable(account.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.account.details.field.created.by"), account.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.account.details.field.updated"), account.getUpdateDate() != null ? DateHelper.formatToHumanReadable(account.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.account.details.field.updated.by"), account.getUpdatedBy());

        addTab(I18n.t("ims.account.details.section.audit"), createSection(I18n.t("ims.account.details.section.audit"), auditInfo));

        // Roles (expandable)
        if (account.getRoleInfo() != null && !account.getRoleInfo().isEmpty()) {
            String rolesText = account.getRoleInfo().stream()
                    .map(RoleInfoDto::getName)
                    .collect(Collectors.joining(" • "));
            Component rolesComponent = createCompactList(VaadinIcon.TAG, I18n.t("ims.account.details.section.roles"), rolesText);
            addTab(I18n.t("ims.account.details.section.roles"), new Details(I18n.t("ims.account.details.section.roles"), rolesComponent));
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
            addTab(I18n.t("ims.account.details.section.connections"), new Details(I18n.t("ims.account.details.section.connections"), connectionsLayout));
        }

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