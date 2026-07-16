package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.i18n.I18n;

/**
 * Identity header: avatar (with click-to-change overlay), name, status/role/
 * tenant/account-type badges, and the "Edit Profile" call to action. Purely
 * presentational — avatar image loading and the edit/upload actions are
 * wired in by the caller so this component doesn't need to know about
 * {@code ProfileService}.
 */
class ProfileHeaderCard extends HorizontalLayout {

    private final Avatar avatar;

    ProfileHeaderCard(AccountDto account, Runnable onEditClick, Runnable onAvatarClick) {
        addClassName("profile-header-card");
        setAlignItems(Alignment.START);
        setSpacing(true);
        setPadding(true);
        setWidthFull();

        Div avatarSection = new Div();
        avatarSection.addClassName("profile-avatar-section");

        avatar = new Avatar();
        avatar.setWidth("112px");
        avatar.setHeight("112px");
        avatar.addClassName("profile-avatar");
        String fullName = account.getFullName();
        avatar.setName(fullName != null && !fullName.isBlank() ? fullName : account.getEmail());

        Div avatarUploadOverlay = new Div();
        avatarUploadOverlay.addClassName("avatar-upload-overlay");
        avatarUploadOverlay.add(VaadinIcon.CAMERA.create());
        avatarUploadOverlay.getElement().setAttribute("aria-label", I18n.t("profile.change.avatar"));
        avatarUploadOverlay.getElement().setAttribute("role", "button");
        avatarUploadOverlay.addClickListener(e -> onAvatarClick.run());
        Tooltip.forComponent(avatarUploadOverlay).setText(I18n.t("profile.change.avatar"));

        avatarSection.add(avatar, avatarUploadOverlay);

        VerticalLayout infoSection = new VerticalLayout();
        infoSection.setSpacing(false);
        infoSection.setPadding(false);
        infoSection.addClassName("profile-info-section");

        H2 name = new H2(fullName != null && !fullName.isBlank() ? fullName : account.getEmail());
        name.addClassName("profile-name");
        name.addClassName(LumoUtility.Margin.Bottom.NONE);

        boolean active = account.getAdminStatus() != null && "ENABLED".equals(account.getAdminStatus().name());
        Span statusBadge = new Span(active ? I18n.t("profile.status.active") : I18n.t("profile.status.inactive"));
        statusBadge.addClassName("wams-chip");
        statusBadge.addClassName(active ? "wams-chip--success" : "wams-chip--error");
        statusBadge.addClassName("profile-status-badge");

        HorizontalLayout topRow = new HorizontalLayout(name, statusBadge);
        topRow.setAlignItems(Alignment.CENTER);
        topRow.setSpacing(true);
        topRow.addClassName("profile-name-row");

        HorizontalLayout emailRow = new HorizontalLayout(VaadinIcon.ENVELOPE_O.create(), new Span(account.getEmail()));
        emailRow.setAlignItems(Alignment.CENTER);
        emailRow.setSpacing(false);
        emailRow.addClassName("profile-email-row");

        Div badgeRow = new Div();
        badgeRow.addClassName("profile-badge-row");
        badgeRow.add(buildBadge(VaadinIcon.BRIEFCASE, account.getFunctionRole(), "wams-chip--info"));
        badgeRow.add(buildBadge(VaadinIcon.OFFICE, account.getTenant(), "wams-chip--neutral"));
        badgeRow.add(buildBadge(VaadinIcon.KEY, ProfileFormatUtils.formatAccountType(account.getAccountType()), "wams-chip--neutral"));
        if (Boolean.TRUE.equals(account.getIsAdmin())) {
            badgeRow.add(buildBadge(VaadinIcon.STAR, I18n.t("profile.badge.admin"), "wams-chip--warning"));
        }

        Button editButton = new Button(I18n.t("profile.edit"), VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editButton.addClassName("profile-edit-btn");
        editButton.addClickListener(e -> onEditClick.run());

        HorizontalLayout actions = new HorizontalLayout(editButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.addClassName("profile-header-actions");

        infoSection.add(topRow, emailRow, badgeRow, actions);

        add(avatarSection, infoSection);
        expand(infoSection);
    }

    /**
     * Exposed so the page can asynchronously fetch and apply the avatar image
     * after construction, without this component needing a ProfileService.
     */
    Avatar getAvatar() {
        return avatar;
    }

    private Component buildBadge(VaadinIcon icon, String label, String variantClass) {
        Span badge = new Span();
        badge.addClassName("wams-chip");
        badge.addClassName(variantClass);
        badge.addClassName("profile-badge");
        if (label == null || label.isBlank()) {
            badge.setVisible(false);
            return badge;
        }
        Icon iconEl = icon.create();
        iconEl.setSize("12px");
        badge.add(iconEl, new Span(label));
        return badge;
    }
}
