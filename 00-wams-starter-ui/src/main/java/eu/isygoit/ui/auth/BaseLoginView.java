package eu.isygoit.ui.auth;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import eu.isygoit.i18n.I18n;
import eu.isygoit.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all authentication views (Login, OTP, Password, QR, Register).
 * <p>
 * Handles the shared plumbing every screen needs — redirect capture,
 * already-authenticated short-circuiting, and error display — and provides
 * the shared "Jira-style" building blocks (page shell, card, brand header,
 * error banner, footer, links) so each view only assembles its own
 * form fields instead of re-declaring the same layout/CSS wiring five times.
 */
@Slf4j
@CssImport("./styles/auth.css")
public abstract class BaseLoginView extends VerticalLayout implements BeforeEnterObserver {

    protected String redirectTarget;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Always try session first
        redirectTarget = SecurityUtils.consumeRedirect();

        // Then query param (for direct links)
        if (redirectTarget == null) {
            redirectTarget = event.getLocation()
                    .getQueryParameters()
                    .getSingleParameter("redirect")
                    .filter(SecurityUtils::isSafeInternalPath)
                    .orElse(null);
        }

        if (SecurityUtils.isUserLoggedIn()) {
            String target = redirectTarget != null ? redirectTarget : "landing";
            event.forwardTo(target);
            return;
        }

        onBeforeEnter(event);
    }

    /**
     * Hook for subclasses to perform extra logic before the view renders,
     * e.g., clearing error messages or resetting fields.
     * Default implementation does nothing.
     */
    protected void onBeforeEnter(BeforeEnterEvent event) {
        // subclasses may override
    }

    /**
     * Displays an error notification and (optionally) updates an inline error container.
     * Subclasses can override to update their own error component.
     */
    protected void showError(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /**
     * Shows the toast AND the inline error banner in one call, which is the
     * pattern every view previously duplicated at each error site.
     */
    protected void showError(Div errorBanner, String message) {
        showError(message);
        errorBanner.setText(message);
        errorBanner.setVisible(true);
    }

    // ─── Shared "Jira-style" building blocks ───────────────────────────────

    /**
     * Sets up the full-viewport page shell (flat neutral background, centered
     * content) that every auth view uses as its root layout.
     */
    protected void configureAsAuthPage(String viewClassName) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("wams-auth-page");
        addClassName(viewClassName);
    }

    /**
     * Card wrapper that centers and constrains the form content, matching the
     * single-card layout used across every Atlassian auth screen.
     */
    protected VerticalLayout createCard() {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setWidthFull();
        card.setPadding(false);
        card.setSpacing(true);
        card.addClassName("wams-auth-card");
        return card;
    }

    /**
     * Brand header: logo + title (+ optional subtitle), centered above the form.
     */
    protected Div createBrand(String title, String subtitle) {
        Div brand = new Div();
        brand.addClassName("wams-auth-brand");

        Avatar logo = new Avatar("IsyGo");
        logo.setColorIndex(0);
        logo.setWidth("56px");
        logo.setHeight("56px");
        logo.addClassName("wams-auth-logo");

        H2 titleEl = new H2(title);
        titleEl.addClassName("wams-auth-title");
        brand.add(logo, titleEl);

        if (subtitle != null && !subtitle.isEmpty()) {
            Paragraph subtitleEl = new Paragraph(subtitle);
            subtitleEl.addClassName("wams-auth-subtitle");
            brand.add(subtitleEl);
        }
        return brand;
    }

    /**
     * Inline error banner, hidden until {@link #showError(Div, String)} is called.
     */
    protected Div createErrorBanner() {
        Div errorBanner = new Div();
        errorBanner.addClassName("wams-auth-error");
        errorBanner.setVisible(false);
        return errorBanner;
    }

    /**
     * Secondary navigation link (back to sign in, register, etc.).
     */
    protected Anchor createLink(String href, String text) {
        Anchor link = new Anchor(href, text);
        link.addClassName("wams-auth-link");
        return link;
    }

    /**
     * Copyright footer, identical on every auth screen.
     */
    protected Div createFooter() {
        Div footer = new Div();
        footer.addClassName("wams-auth-footer");
        Paragraph footerText = new Paragraph(I18n.t("auth.common.footer"));
        footerText.addClassName("wams-auth-footer-text");
        footer.add(footerText);
        return footer;
    }
}
