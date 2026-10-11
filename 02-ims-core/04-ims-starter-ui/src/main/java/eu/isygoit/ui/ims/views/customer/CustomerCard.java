package eu.isygoit.ui.ims.views.customer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.CustomerImageService;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.card.ProfileCard;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.customer.dialog.CustomerDetailsViewDialog;
import eu.isygoit.ui.ims.views.customer.dialog.DeleteCustomerDialog;
import eu.isygoit.ui.ims.views.customer.dialog.LinkCustomerAccountDialog;
import eu.isygoit.ui.ims.views.customer.dialog.ToggleCustomerStatusDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Card view for a single {@link CustomerDto}.
 *
 * <p>Layout (inherited from {@link ProfileCard}):
 * <ul>
 *   <li><b>Header</b> — downloaded avatar + (name over email).</li>
 *   <li><b>Body</b>   — empty (email is in the header).</li>
 *   <li><b>Footer</b> — admin status tag (left) + Details / Edit / Link
 *       account / Toggle status / Delete (right).</li>
 * </ul>
 */
@Slf4j
public class CustomerCard extends ProfileCard<CustomerManagementView, CustomerService, CustomerDto> {

    private final CustomerImageService customerImageService;
    private final AccountService accountService;
    private final Runnable onRefresh;

    private Span adminStatusChip;
    private Button toggleStatusBtn;

    public CustomerCard(CustomerManagementView parentView,
                        CustomerService customerService,
                        CustomerImageService customerImageService,
                        AccountService accountService,
                        CustomerDto customer,
                        Runnable onRefresh) {
        super(parentView, customerService, customer);
        this.customerImageService = customerImageService;
        this.accountService = accountService;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "customer-card";
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
     * Downloads the customer avatar through {@link CustomerImageService}.
     */
    @Override
    protected ProfileImageLoader profileImageLoader() {
        if (customerImageService == null || profile == null || profile.getId() == null) {
            return null;
        }
        return () -> {
            ResponseEntity<Resource> response = customerImageService.downloadImage(profile.getId());
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

    /**
     * Admin status → footer status tag. Field captured for in-place refresh.
     */
    @Override
    protected Span profileStatusTag() {
        adminStatusChip = ImsEnumTag.ofOrUnknown(profile.getAdminStatus(), null);
        return adminStatusChip;
    }

    /* ══════════════════════════════════════════════════════════════
     * Body — nothing extra (email lives in the header)
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected void buildBodyRows() {
        // intentionally empty
    }

    /* ══════════════════════════════════════════════════════════════
     * Enum tags — none for customers
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
        Button detailsBtn = createDetailsButton(I18n.t("ims.customer.card.details.tooltip"),
                () -> new CustomerDetailsViewDialog(parentView, objectService, profile.getId()).open());

        Button editBtn = createEditButton(I18n.t("ims.customer.card.edit.tooltip"),
                () -> parentView.openUpdateCustomerDialog(profile, () -> {
                    if (onRefresh != null) onRefresh.run();
                }));

        Button linkAccountBtn = createIconButton(VaadinIcon.LINK,
                I18n.t("ims.customer.card.link.account.tooltip"));
        linkAccountBtn.addClickListener(e -> openLinkAccountDialog());

        toggleStatusBtn = createToggleButton(
                profile.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED,
                I18n.t("ims.customer.card.enable.tooltip"),
                I18n.t("ims.customer.card.disable.tooltip"),
                this::openToggleStatusDialog
        );

        Button deleteBtn = createDeleteButton(I18n.t("ims.customer.card.delete.tooltip"),
                () -> new DeleteCustomerDialog(parentView, objectService, profile.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());

        return List.of(detailsBtn, editBtn, linkAccountBtn, toggleStatusBtn, deleteBtn);
    }

    /* ══════════════════════════════════════════════════════════════
     * Toggle / link / refresh
     * ══════════════════════════════════════════════════════════════ */

    private void openToggleStatusDialog() {
        new ToggleCustomerStatusDialog(parentView, objectService, profile.getId(),
                profile.getAdminStatus(), () -> {
            refreshCustomerData();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void openLinkAccountDialog() {
        new LinkCustomerAccountDialog(parentView, objectService, accountService,
                profile.getId(), () -> {
            refreshCustomerData();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void refreshCustomerData() {
        try {
            ResponseEntity<CustomerDto> response = objectService.findById(profile.getId());
            if (response.getBody() != null) {
                CustomerDto updated = response.getBody();
                profile.setAdminStatus(updated.getAdminStatus());
                profile.setAccountCode(updated.getAccountCode());
                updateStatusChip();
                updateToggleButtonIcon();
            }
        } catch (Exception e) {
            log.error("Failed to refresh customer data", e);
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
                    ? I18n.t("ims.customer.card.disable.tooltip")
                    : I18n.t("ims.customer.card.enable.tooltip"));
        }
    }
}