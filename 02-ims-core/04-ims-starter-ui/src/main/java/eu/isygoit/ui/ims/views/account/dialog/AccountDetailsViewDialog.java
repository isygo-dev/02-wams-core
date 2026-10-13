package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.DetailHero;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only view of a {@link AccountDto}: every DTO field except {@code id}
 * (never displayed) is shown, grouped in tabs. The hero on the identity tab
 * shows the account avatar, which is <b>downloaded</b> through
 * {@link AccountImageService} rather than read from a static path.
 */
@Slf4j
public class AccountDetailsViewDialog extends ImsDetailsDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final AccountImageService accountImageService;
    private final Long accountId;

    public AccountDetailsViewDialog(AccountManagementView parentView,
                                    AccountService accountService,
                                    AccountImageService accountImageService,
                                    Long accountId) {
        super(I18n.t("ims.account.details.title"));
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountImageService = accountImageService;
        this.accountId = accountId;

        applyWidth(DialogLayout.WIDTH_L);
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("account-details-dialog");

        loadAndShowDetails();
    }

    private static String formatDate(LocalDateTime date) {
        return date != null ? DateHelper.formatToHumanReadable(date) : null;
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

    /* ------------------------------------------------------------------
     * Hero
     * ------------------------------------------------------------------ */

    private void buildContent(AccountDto account) {
        // Identity: hero (avatar + name + email + chips) then the identity grid
        Div identityInfo = createDetailGrid();
        addFieldToGrid(identityInfo, VaadinIcon.USER, I18n.t("ims.account.details.field.full.name"), account.getFullName());
        addFieldToGrid(identityInfo, VaadinIcon.ENVELOPE, I18n.t("ims.account.details.field.email"), account.getEmail(), true);
        addFieldToGrid(identityInfo, VaadinIcon.HASH, I18n.t("ims.account.details.field.code"), account.getCode(), true);

        VerticalLayout identity = DialogLayout.stack();
        identity.add(buildHero(account),
                createSection(I18n.t("ims.account.details.section.identity"), identityInfo));
        addTab(I18n.t("ims.account.details.section.identity"), identity);

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

    private Component buildHero(AccountDto account) {
        String displayName = account.getFullName() != null ? account.getFullName() : account.getEmail();

        List<Component> chips = new ArrayList<>();
        if (account.getAdminStatus() != null) {
            chips.add(ImsEnumTag.of(account.getAdminStatus()));
        }
        if (account.getSystemStatus() != null) {
            chips.add(ImsEnumTag.of(account.getSystemStatus()));
        }

        return new DetailHero(downloadAvatar(account.getId()), null, displayName, account.getEmail(),
                chips.toArray(Component[]::new));
    }

    /**
     * Downloads the account avatar; returns null on failure so the hero shows its placeholder.
     */
    private StreamResource downloadAvatar(Long id) {
        if (accountImageService == null || id == null) return null;
        try {
            ResponseEntity<Resource> response = accountImageService.downloadImage(id);
            if (response != null
                    && response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                byte[] bytes = response.getBody().getContentAsByteArray();
                if (bytes != null && bytes.length > 0) {
                    return new StreamResource("account_" + id + ".jpg",
                            () -> new ByteArrayInputStream(bytes));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to download avatar for account {}", id, e);
        }
        return null;
    }
}