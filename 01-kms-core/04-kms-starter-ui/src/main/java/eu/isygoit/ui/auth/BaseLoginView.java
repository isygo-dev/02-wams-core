package eu.isygoit.ui.auth;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import eu.isygoit.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all login views (Login, OTP, Password, QR).
 * Handles redirect capture, authentication check, and error display.
 */
@Slf4j
public abstract class BaseLoginView extends VerticalLayout implements BeforeEnterObserver {

    protected String redirectTarget;
    protected boolean stylesInjected = false;

    /**
     * Common before‑enter logic for all login views:
     * 1. Capture redirect from session (first) or query parameter.
     * 2. If the user is already logged in, forward to the target (or default "/kms").
     * 3. Otherwise, the view renders normally – subclasses may override {@link #onBeforeEnter(BeforeEnterEvent)}.
     */
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
            String target = redirectTarget != null ? redirectTarget : "kms";
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
     * Injects responsive CSS styles into the page.
     * Subclasses must implement this to provide their own styles.
     */
    protected abstract void injectResponsiveStyles();

    /**
     * Utility method to inject a CSS block.
     * Called by subclasses from their constructors or on attach.
     */
    protected void injectStyles(String css) {
        com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}