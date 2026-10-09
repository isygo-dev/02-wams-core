package eu.isygoit.ui.ims.views.tenant.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.DetailHero;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only view of a {@link TenantDto}: every DTO field except {@code id}
 * (never displayed) is shown, grouped in tabs (identity, contact, social links,
 * address, audit).
 */
public class TenantDetailsViewDialog extends ImsDetailsDialog {

    private final TenantManagementView parentView;
    private final TenantService tenantService;
    private final Long tenantId;

    public TenantDetailsViewDialog(TenantManagementView parentView,
                                   TenantService tenantService,
                                   Long tenantId) {
        super(I18n.t("ims.tenant.details.title"));
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenantId = tenantId;

        applyWidth(DialogLayout.WIDTH_L);
        setModal(true);
        setDraggable(true);

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<TenantDto> response = tenantService.findById(tenantId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.tenant.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.tenant.details.load.error", TenantDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.tenant.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(TenantDto tenant) {
        addIdentityTab(tenant);
        addContactTab(tenant);
        if (hasAnySocialLink(tenant)) {
            addSocialTab(tenant);
        }
        if (tenant.getAddress() != null) {
            addAddressTab(tenant.getAddress());
        }
        addAuditTab(tenant.getCreatedBy(), formatDate(tenant.getCreateDate()),
                tenant.getUpdatedBy(), formatDate(tenant.getUpdateDate()));
    }

    private void addIdentityTab(TenantDto tenant) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.BUILDING, I18n.t("ims.tenant.details.field.name"), dash(tenant.getName()));
        addFieldToGrid(grid, VaadinIcon.CODE, I18n.t("ims.tenant.details.field.code"), dash(tenant.getCode()), true);
        addFieldToGrid(grid, VaadinIcon.INSTITUTION, I18n.t("ims.tenant.details.field.industry"),
                dash(tenant.getIndustry()));
        ImsEnumTag.addDetailField(grid, VaadinIcon.SHIELD, I18n.t("ims.tenant.details.field.admin.status"),
                tenant.getAdminStatus(), null);
        addFieldToGrid(grid, VaadinIcon.FILE_TEXT, I18n.t("ims.tenant.details.field.description"),
                dash(tenant.getDescription()));

        VerticalLayout identity = DialogLayout.stack();
        identity.add(buildHero(tenant),
                createSection(I18n.t("ims.tenant.details.section.identity"), grid));
        addTab(I18n.t("ims.tenant.details.section.identity"), identity);
    }

    private void addContactTab(TenantDto tenant) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.ENVELOPE, I18n.t("ims.tenant.details.field.email"),
                dash(tenant.getEmail()), true);
        addFieldToGrid(grid, VaadinIcon.PHONE, I18n.t("ims.tenant.details.field.phone"),
                dash(tenant.getPhone()), true);
        addFieldToGrid(grid, VaadinIcon.GLOBE, I18n.t("ims.tenant.details.field.website"),
                dash(tenant.getUrl()), true);
        addTab(I18n.t("ims.tenant.details.section.contact"),
                createSection(I18n.t("ims.tenant.details.section.contact"), grid));
    }

    private void addSocialTab(TenantDto tenant) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.LINK, I18n.t("ims.tenant.details.field.facebook"),
                dash(tenant.getLnk_facebook()), true);
        addFieldToGrid(grid, VaadinIcon.LINK, I18n.t("ims.tenant.details.field.linkedin"),
                dash(tenant.getLnk_linkedin()), true);
        addFieldToGrid(grid, VaadinIcon.LINK, I18n.t("ims.tenant.details.field.xing"),
                dash(tenant.getLnk_xing()), true);
        addTab(I18n.t("ims.tenant.details.section.social"),
                createSection(I18n.t("ims.tenant.details.section.social"), grid));
    }

    private void addAddressTab(AddressDto addr) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.country"), dash(addr.getCountry()));
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.state"), dash(addr.getState()));
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.city"), dash(addr.getCity()));
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.street"), dash(addr.getStreet()));
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.zip.code"),
                dash(addr.getZipCode()), true);
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.additional.info"),
                dash(addr.getAdditionalInfo()));
        addTab(I18n.t("ims.tenant.details.section.address"),
                createSection(I18n.t("ims.tenant.details.section.address"), grid));
    }

    private Component buildHero(TenantDto tenant) {
        List<Component> chips = new ArrayList<>();
        if (tenant.getAdminStatus() != null) {
            chips.add(ImsEnumTag.of(tenant.getAdminStatus()));
        }
        return new DetailHero(null, dash(tenant.getName()), tenant.getEmail(), chips.toArray(Component[]::new));
    }

    private static boolean hasAnySocialLink(TenantDto tenant) {
        return (tenant.getLnk_facebook() != null && !tenant.getLnk_facebook().isBlank())
                || (tenant.getLnk_linkedin() != null && !tenant.getLnk_linkedin().isBlank())
                || (tenant.getLnk_xing() != null && !tenant.getLnk_xing().isBlank());
    }

    private static String formatDate(LocalDateTime date) {
        return date == null ? null : DateHelper.formatToHumanReadable(date);
    }
}
