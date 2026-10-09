package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
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

        applyWidth(DialogLayout.WIDTH_L);
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
            add(new Span(I18n.t("ims.account.details.load.error", ImsDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.account.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AccountDto account) {
        // Identity: name, email, code
        Div identityInfo = createDetailGrid();
        addFieldToGrid(identityInfo, VaadinIcon.USER, I18n.t("ims.account.details.field.full.name"), account.getFullName());
        addFieldToGrid(identityInfo, VaadinIcon.ENVELOPE, I18n.t("ims.account.details.field.email"), account.getEmail(), true);
        addFieldToGrid(identityInfo, VaadinIcon.HASH, I18n.t("ims.account.details.field.code"), account.getCode(), true);
        addTab(I18n.t("ims.account.details.section.identity"), createSection(I18n.t("ims.account.details.section.identity"), identityInfo));

        // Classification and status: type, role, status, flags, language
        Div classificationInfo = createDetailGrid();
        addFieldToGrid(classificationInfo, VaadinIcon.COG, I18n.t("ims.account.details.field.function.role"), account.getFunctionRole());
        ImsEnumTag.addTagDetailField(classificationInfo, VaadinIcon.TAGS, I18n.t("ims.account.details.field.account.type"), ImsEnumTag.ofValue(account.getAccountType(), null));
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.SIGN_IN, I18n.t("ims.account.details.field.auth.type"), account.getAuthType(), null);
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.COMMENT, I18n.t("ims.account.details.field.chat.status"), account.getChatStatus(), null);
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.LOCATION_ARROW_CIRCLE, I18n.t("ims.account.details.field.language"), account.getLanguage(), "ims.enum.language");
        addFieldToGrid(classificationInfo, VaadinIcon.SHIELD, I18n.t("ims.account.details.field.admin"), Boolean.TRUE.equals(account.getIsAdmin()) ? I18n.t("ims.account.details.yes") : I18n.t("ims.account.details.no"));
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.LOCK, I18n.t("ims.account.details.field.admin.status"), account.getAdminStatus(), null);
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.STETHOSCOPE, I18n.t("ims.account.details.field.system.status"), account.getSystemStatus(), null);
        addTab(I18n.t("ims.account.details.section.classification"), createSection(I18n.t("ims.account.details.section.classification"), classificationInfo));

        // Contact and relations: phone, tenant, origin, last login, country
        Div contactInfo = createDetailGrid();
        addFieldToGrid(contactInfo, VaadinIcon.PHONE, I18n.t("ims.account.details.field.phone"), account.getPhoneNumber(), true);
        addFieldToGrid(contactInfo, VaadinIcon.BUILDING, I18n.t("ims.account.details.field.tenant"), account.getTenant(), true);
        if (account.getOrigin() != null && !account.getOrigin().isBlank()) {
            contactInfo.add(ImsEnumTag.detailField(VaadinIcon.CLOUD, I18n.t("ims.account.details.field.origin"),
                    ImsEnumTag.ofValue(account.getOrigin(), "ims.enum.origin")));
        }
        addFieldToGrid(contactInfo, VaadinIcon.CLOCK, I18n.t("ims.account.details.field.last.login"), formatDate(account.getLastConnectionDate()));
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
                Div addressGrid = createDetailGrid();
                addFieldToGrid(addressGrid, VaadinIcon.MAP_MARKER, I18n.t("ims.account.details.field.address"), address);
                addTab(I18n.t("ims.dialog.tab.address"), addressGrid);
            }
        }

        // Contacts (if present)
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

        // Roles
        if (account.getRoleInfo() != null && !account.getRoleInfo().isEmpty()) {
            String rolesText = account.getRoleInfo().stream()
                    .map(RoleInfoDto::getName)
                    .collect(Collectors.joining(" • "));
            Div rolesGrid = createDetailGrid();
            addFieldToGrid(rolesGrid, VaadinIcon.TAG, I18n.t("ims.account.details.section.roles"), rolesText);
            addTab(I18n.t("ims.account.details.section.roles"), rolesGrid);
        }

        // Connection tracking
        if (account.getConnectionTracking() != null && !account.getConnectionTracking().isEmpty()) {
            Div connectionsGrid = createDetailGrid();
            for (ConnectionTrackingDto ct : account.getConnectionTracking()) {
                String when = ct.getCreateDate() != null
                        ? DateHelper.formatToHumanReadable(ct.getCreateDate())
                        : I18n.t("ims.account.details.unknown.time");
                String device = ct.getDevice() != null ? ct.getDevice() : I18n.t("ims.account.details.unknown.device");
                addFieldToGrid(connectionsGrid, VaadinIcon.MOBILE, when, device);
            }
            addTab(I18n.t("ims.account.details.section.connections"), connectionsGrid);
        }

        addAuditTab(account.getCreatedBy(), formatDate(account.getCreateDate()),
                account.getUpdatedBy(), formatDate(account.getUpdateDate()));
    }

    private static String formatDate(LocalDateTime date) {
        return date != null ? DateHelper.formatToHumanReadable(date) : null;
    }
}
