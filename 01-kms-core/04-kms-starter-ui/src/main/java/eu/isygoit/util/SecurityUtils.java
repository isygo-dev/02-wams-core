package eu.isygoit.util;

import com.vaadin.flow.server.VaadinSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public final class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);
    private static final String REDIRECT_SESSION_KEY = "login.redirect";

    private SecurityUtils() {}

    public static boolean isSafeInternalPath(String path) {
        if (!StringUtils.hasText(path)) return false;
        return path.startsWith("/")
                && !path.contains("://")
                && !path.contains("..")
                && !path.contains("//")
                && !path.contains("\\")
                && path.matches("^/[a-zA-Z0-9\\-/_]+$");
    }

    public static void storeRedirect(String path) {
        if (!isSafeInternalPath(path)) {
            log.warn("❌ Rejected unsafe redirect path: {}", path);
            return;
        }
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            log.warn("⚠️ No VaadinSession – redirect not stored: {}", path);
            return;
        }
        session.setAttribute(REDIRECT_SESSION_KEY, path);
        log.info("🔐 Stored redirect in session (id={}): {}", session.getSession().getId(), path);
    }

    public static String consumeRedirect() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            log.warn("⚠️ No VaadinSession – cannot consume redirect");
            return null;
        }
        String path = (String) session.getAttribute(REDIRECT_SESSION_KEY);
        session.setAttribute(REDIRECT_SESSION_KEY, null);
        if (path != null) {
            log.info("🔐 Consumed redirect from session (id={}): {}", session.getSession().getId(), path);
        }
        return path;
    }

    /**
     * Checks if the current session has a logged‑in user.
     */
    public static boolean isUserLoggedIn() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null && session.getAttribute("user") != null;
    }
}