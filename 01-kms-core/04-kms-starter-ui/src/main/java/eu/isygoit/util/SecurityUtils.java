package eu.isygoit.util;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpSession;

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
            HttpSession httpSession = VaadinServletRequest.getCurrent().getSession(true);
            if (httpSession != null) {
                session = VaadinSession.getCurrent();
            }
        }

        if (session != null) {
            session.setAttribute(REDIRECT_SESSION_KEY, path);
            log.info("🔐 Stored redirect in session (id={}): {}", session.getSession().getId(), path);
        } else {
            log.warn("⚠️ Could not store redirect – no session available.");
        }
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
     * Checks if the user is logged in.
     * Always checks the HTTP session first (most reliable).
     * If the HTTP session has the user but the Vaadin session does not,
     * it synchronises the attribute back to the Vaadin session.
     */
    public static boolean isUserLoggedIn() {
        // 1. Try to get the HTTP session (without creating it)
        HttpSession httpSession = VaadinServletRequest.getCurrent().getSession(false);
        if (httpSession != null) {
            String sessionId = httpSession.getId();
            Object user = httpSession.getAttribute("user");
            if (user != null) {
                log.info("✅ User found in HTTP session (id={}): {}", sessionId, user);
                // Ensure Vaadin session also has the attribute (sync)
                VaadinSession vaadinSession = VaadinSession.getCurrent();
                if (vaadinSession != null) {
                    if (vaadinSession.getAttribute("user") == null) {
                        vaadinSession.setAttribute("user", user);
                        vaadinSession.setAttribute("accessToken", httpSession.getAttribute("accessToken"));
                        log.info("🔄 Synced user to Vaadin session (id={})", vaadinSession.getSession().getId());
                    }
                } else {
                    log.warn("⚠️ Vaadin session is null, but HTTP session has user");
                }
                return true;
            } else {
                log.debug("ℹ️ HTTP session exists (id={}) but no user attribute", sessionId);
            }
        } else {
            log.debug("ℹ️ No HTTP session found");
        }

        // 2. Fallback to Vaadin session check
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession != null) {
            Object user = vaadinSession.getAttribute("user");
            if (user != null) {
                log.info("✅ User found in Vaadin session (id={}): {}", vaadinSession.getSession().getId(), user);
                return true;
            }
        }

        log.info("❌ User not logged in");
        return false;
    }
}