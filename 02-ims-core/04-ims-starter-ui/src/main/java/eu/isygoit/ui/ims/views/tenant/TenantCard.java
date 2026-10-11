package eu.isygoit.ui.ims.views.tenant;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantImageService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.card.ProfileCard;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.tenant.dialog.DeleteTenantDialog;
import eu.isygoit.ui.ims.views.tenant.dialog.TenantDetailsViewDialog;
import eu.isygoit.ui.ims.views.tenant.dialog.ToggleTenantStatusDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Card view for a single {@link TenantDto}.
 *
 * <p>Layout (inherited from {@link ProfileCard}):
 * <ul>
 *   <li><b>Header</b> — downloaded avatar + (name over email).</li>
 *   <li><b>Body</b>   — empty (email lives in the header subtitle).</li>
 *   <li><b>Footer</b> — admin status tag (left) + Details / Edit / Toggle
 *       status / Delete (right).</li>
 * </ul>
 *
 * <p>The avatar uses a tenant-specific placeholder (building/columns glyph)
 * rather than the default person silhouette.
 */
@Slf4j
public class TenantCard extends ProfileCard<TenantManagementView, TenantService, TenantDto> {

    /** Tenant-specific placeholder: a building / columns glyph. */
    private static final String TENANT_IMAGE_PLACEHOLDER =
            "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='48' height='48' " +
                    "viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='1' " +
                    "stroke-linecap='round' stroke-linejoin='round'%3E" +
                    "%3Crect x='2' y='2' width='20' height='20' rx='2.18' ry='2.18'%3E%3C/rect%3E" +
                    "%3Cpath d='M7 2v20M17 2v20M2 12h20M2 7h5M2 17h5M17 17h5M17 7h5'%3E%3C/path%3E%3C/svg%3E";

    private final TenantImageService tenantImageService;
    private final Runnable onRefresh;

    private Span adminStatusChip;
    private Button toggleStatusBtn;

    public TenantCard(TenantManagementView parentView,
                      TenantService tenantService,
                      TenantImageService tenantImageService,
                      TenantDto tenant,
                      Runnable onRefresh) {
        super(parentView, tenantService, tenant);
        this.tenantImageService = tenantImageService;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "tenant-card";
    }

    /* ══════════════════════════════════════════════════════════════
     * Profile accessors
     * ══════════════════════════════════════════════════════════════ */

    /** No static path — the avatar is fetched via {@link #profileImageLoader()}. */
    @Override
    protected String profileImagePath() {
        return null;
    }

    /** Keep the tenant-specific building glyph instead of the default user icon. */
    @Override
    protected String profileImagePlaceholder() {
        return TENANT_IMAGE_PLACEHOLDER;
    }

    /** Downloads the tenant avatar through {@link TenantImageService}. */
    @Override
    protected ProfileImageLoader profileImageLoader() {
        if (tenantImageService == null || profile == null || profile.getId() == null) {
            return null;
        }
        return () -> {
            ResponseEntity<Resource> response = tenantImageService.downloadImage(profile.getId());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getContentAsByteArray();
            }
            return null;
        };
    }

    @Override
    protected String profileFullName() {
        return profile.getName();
    }

    @Override
    protected String profileSubtitle() {
        return profile.getEmail();
    }

    /** Admin status → footer status tag. Field captured for in-place refresh. */
    @Override
    protected Span profileStatusTag() {
        adminStatusChip = ImsEnumTag.ofOrUnknown(profile.getAdminStatus(), null);
        return adminStatusChip;
    }

    /* ══════════════════════════════════════════════════════════════
     * Body — nothing extra (email lives in the header subtitle)
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected void buildBodyRows() {
        // intentionally empty
    }

    /* ══════════════════════════════════════════════════════════════
     * Enum tags — none for tenants
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected Component buildEnumTags() {
        return null;
    }

    /* ══════════════════════════════════════════════════════════════
     * Actions
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("ims.tenant.card.details.tooltip"),
                () -> new TenantDetailsViewDialog(parentView, objectService, profile.getId()).open());

        Button editBtn = createEditButton(I18n.t("ims.tenant.card.edit.tooltip"),
                () -> parentView.openUpdateTenantDialog(profile, () -> {
                    if (onRefresh != null) onRefresh.run();
                }));

        toggleStatusBtn = createToggleButton(
                profile.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED,
                I18n.t("ims.tenant.card.enable.tooltip"),
                I18n.t("ims.tenant.card.disable.tooltip"),
                this::openToggleStatusDialog
        );

        Button deleteBtn = createDeleteButton(I18n.t("ims.tenant.card.delete.tooltip"),
                () -> new DeleteTenantDialog(parentView, objectService, profile.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());

        return List.of(detailsBtn, editBtn, toggleStatusBtn, deleteBtn);
    }

    /* ══════════════════════════════════════════════════════════════
     * Toggle + refresh
     * ══════════════════════════════════════════════════════════════ */

    private void openToggleStatusDialog() {
        new ToggleTenantStatusDialog(parentView, objectService, profile.getId(),
                profile.getAdminStatus(), () -> {
            refreshTenantData();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void refreshTenantData() {
        try {
            ResponseEntity<TenantDto> response = objectService.findById(profile.getId());
            if (response.getBody() != null) {
                profile.setAdminStatus(response.getBody().getAdminStatus());
                updateStatusChip();
                updateToggleButtonIcon();
            }
        } catch (Exception e) {
            log.error("Failed to refresh tenant data", e);
        }
    }

    private void updateStatusChip() {
        if (adminStatusChip != null) {
            ImsEnumTag.update(adminStatusChip, profile.getAdminStatus(), null);
        }
    }

    private void updateToggleButtonIcon() {
        if (toggleStatusBtn != null) {
            boolean enabled = profile.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            toggleStatusBtn.setIcon(enabled ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleStatusBtn.setTooltipText(enabled
                    ? I18n.t("ims.tenant.card.disable.tooltip")
                    : I18n.t("ims.tenant.card.enable.tooltip"));
        }
    }
}