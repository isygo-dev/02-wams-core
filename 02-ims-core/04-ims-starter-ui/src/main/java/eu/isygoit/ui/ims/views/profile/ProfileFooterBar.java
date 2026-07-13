package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.i18n.I18n;

/**
 * Bottom bar: "last active" timestamp on the left, logout action on the
 * right.
 */
class ProfileFooterBar extends HorizontalLayout {

    ProfileFooterBar(AccountStatDto stats, Runnable onLogout) {
        addClassName("profile-footer");
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout lastActive = new HorizontalLayout(VaadinIcon.CLOCK.create(), new Span(
                stats.getLastLogin() != null
                        ? I18n.t("profile.footer.last.active", ProfileFormatUtils.formatRelativeTime(ProfileFormatUtils.toDate(stats.getLastLogin())))
                        : I18n.t("profile.time.never")));
        lastActive.setAlignItems(Alignment.CENTER);
        lastActive.addClassName("profile-last-active");

        Button logoutButton = new Button(I18n.t("common.layout.avatar.logout"), VaadinIcon.SIGN_OUT.create());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        logoutButton.addClassName("profile-logout-btn");
        logoutButton.addClickListener(e -> onLogout.run());

        add(lastActive, logoutButton);
    }
}
