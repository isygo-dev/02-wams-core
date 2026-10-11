package eu.isygoit.ui.ims.views.account;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.card.ProfileCard;
import eu.isygoit.ui.ims.views.account.dialog.AccountDetailsViewDialog;
import eu.isygoit.ui.ims.views.account.dialog.DeleteAccountDialog;
import eu.isygoit.ui.ims.views.account.dialog.EnableDisableAccountDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Card view for a single {@link MinAccountDto}.
 *
 * <p>Layout (inherited from {@link ProfileCard}):
 * <ul>
 *   <li><b>Header</b> — downloaded avatar + (full name over email).</li>
 *   <li><b>Body</b>   — icon row: tenant (email is already in the header).</li>
 *   <li><b>Enum tags</b> — system status chip, bottom of the body.</li>
 *   <li><b>Footer</b> — admin status tag (left) + Details / Edit / Reset
 *       password / Toggle status / Delete (right).</li>
 * </ul>
 */
@Slf4j
public class AccountCard extends ProfileCard<AccountManagementView, AccountService, MinAccountDto> {

    private final AccountImageService accountImageService;
    private final Runnable onRefresh;

    private Span adminStatusChip;
    private Span systemStatusChip;
    private Button toggleStatusBtn;

    public AccountCard(AccountManagementView parentView,
                       AccountService accountService,
                       AccountImageService accountImageService,
                       MinAccountDto minAccount,
                       Runnable onRefresh) {
        super(parentView, accountService, minAccount);
        this.accountImageService = accountImageService;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "account-card";
    }

    /* ══════════════════════════════════════════════════════════════
     * Profile accessors
     * ══════════════════════════════════════════════════════════════ */

    /**
     * No static path — the avatar is fetched via {@link #profileImageLoader()}.
     */
    @Override
    protected String profileImagePath() {
        return null;
    }

    /**
     * Downloads the account avatar through {@link AccountImageService}.
     */
    @Override
    protected ProfileImageLoader profileImageLoader() {
        if (accountImageService == null || profile == null || profile.getId() == null) {
            return null;
        }
        return () -> {
            ResponseEntity<Resource> response = accountImageService.downloadImage(profile.getId());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getContentAsByteArray();
            }
            return null;
        };
    }

    @Override
    protected String profileFullName() {
        return profile.getFullName() != null ? profile.getFullName() : profile.getEmail();
    }

    @Override
    protected String profileSubtitle() {
        return profile.getEmail();
    }

    /**
     * Admin status → footer status tag. Field is captured for in-place refresh.
     */
    @Override
    protected Span profileStatusTag() {
        adminStatusChip = ImsEnumTag.ofOrUnknown(profile.getAdminStatus(), null);
        return adminStatusChip;
    }

    /* ══════════════════════════════════════════════════════════════
     * Body — email lives in the header, only tenant remains
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected void buildBodyRows() {
        if (profile.getTenant() != null) {
            add(createIconRow(VaadinIcon.BUILDING,
                    I18n.t("ims.account.card.tenant"), profile.getTenant()));
        }
    }

    /* ══════════════════════════════════════════════════════════════
     * Enum tags — system status chip
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected Component buildEnumTags() {
        systemStatusChip = ImsEnumTag.ofOrUnknown(profile.getSystemStatus(), null);
        return buildEnumTagsRow(systemStatusChip);
    }

    /* ══════════════════════════════════════════════════════════════
     * Actions
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("ims.account.card.details.tooltip"),
                () -> new AccountDetailsViewDialog(parentView, objectService, profile.getId()).open());

        Button editBtn = createEditButton(I18n.t("ims.account.card.edit.tooltip"),
                () -> parentView.openUpdateAccountDialog(profile.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }));

        Button resetPwdBtn = createIconButton(VaadinIcon.KEY,
                I18n.t("ims.account.card.reset.password.tooltip"));
        resetPwdBtn.addClickListener(e ->
                parentView.openResetPasswordDialog(profile.getId(), profile.getEmail()));

        toggleStatusBtn = createToggleButton(
                profile.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED,
                I18n.t("ims.account.card.enable.tooltip"),
                I18n.t("ims.account.card.disable.tooltip"),
                this::openToggleStatusDialog
        );

        Button deleteBtn = createDeleteButton(I18n.t("ims.account.card.delete.tooltip"),
                () -> new DeleteAccountDialog(parentView, objectService, profile.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());

        return List.of(detailsBtn, editBtn, resetPwdBtn, toggleStatusBtn, deleteBtn);
    }

    /* ══════════════════════════════════════════════════════════════
     * Toggle + refresh
     * ══════════════════════════════════════════════════════════════ */

    private void openToggleStatusDialog() {
        new EnableDisableAccountDialog(parentView, objectService, profile.getId(), () -> {
            refreshAccountData();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void refreshAccountData() {
        try {
            ResponseEntity<AccountDto> response = objectService.findById(profile.getId());
            if (response.getBody() != null) {
                profile.setAdminStatus(response.getBody().getAdminStatus());
                profile.setSystemStatus(response.getBody().getSystemStatus());
                updateStatusChips();
                updateToggleButtonIcon();
            }
        } catch (Exception e) {
            log.error("Failed to refresh account data", e);
        }
    }

    private void updateStatusChips() {
        if (adminStatusChip != null) {
            ImsEnumTag.update(adminStatusChip, profile.getAdminStatus(), null);
        }
        if (systemStatusChip != null) {
            ImsEnumTag.update(systemStatusChip, profile.getSystemStatus(), null);
        }
    }

    private void updateToggleButtonIcon() {
        if (toggleStatusBtn != null) {
            boolean enabled = profile.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            toggleStatusBtn.setIcon(enabled ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleStatusBtn.setTooltipText(enabled
                    ? I18n.t("ims.account.card.disable.tooltip")
                    : I18n.t("ims.account.card.enable.tooltip"));
        }
    }
}