package eu.isygoit.util;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public final class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);
    private static final String REDIRECT_SESSION_KEY = "login.redirect";

    private SecurityUtils() {
    }

    /**
     * Logs out the current user by invalidating the session and clearing all session attributes.
     * This method should be called before redirecting to the login page.
     */
    public static void logout() {
        try {
            // 1. Clear VaadinSession attributes
            VaadinSession vaadinSession = VaadinSession.getCurrent();
            if (vaadinSession != null) {
                // Clear known attributes
                vaadinSession.setAttribute("user", null);
                vaadinSession.setAttribute("accessToken", null);
                vaadinSession.setAttribute("refreshToken", null);
                vaadinSession.setAttribute("tenant", null);
                vaadinSession.setAttribute("roles", null);

                // Remove any redirect that might be stored
                vaadinSession.setAttribute(REDIRECT_SESSION_KEY, null);

                log.info("🔐 Cleared VaadinSession attributes (id={})",
                        vaadinSession.getSession() != null ? vaadinSession.getSession().getId() : "unknown");
            }

            // 2. Invalidate HttpSession
            try {
                var req = VaadinServletRequest.getCurrent();
                if (req != null) {
                    HttpSession httpSession = req.getSession(false);
                    if (httpSession != null) {
                        httpSession.invalidate();
                        log.info("🔐 Invalidated HttpSession (id={})", httpSession.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not invalidate HttpSession", e);
            }

            // 3. Close VaadinSession
            if (vaadinSession != null) {
                try {
                    vaadinSession.close();
                    log.info("🔐 Closed VaadinSession");
                } catch (Exception e) {
                    log.warn("Could not close VaadinSession", e);
                }
            }

            log.info("🔐 User logged out successfully");

        } catch (Exception e) {
            log.error("❌ Error during logout", e);
            // Try to force cleanup even if something fails
            forceCleanup();
        }
    }

    /**
     * Forces cleanup of session attributes as a fallback when normal logout fails.
     */
    private static void forceCleanup() {
        try {
            VaadinSession vaadinSession = VaadinSession.getCurrent();
            if (vaadinSession != null) {
                vaadinSession.setAttribute("user", null);
                vaadinSession.setAttribute("accessToken", null);
                vaadinSession.setAttribute("refreshToken", null);
                vaadinSession.setAttribute("tenant", null);
                vaadinSession.setAttribute("roles", null);
                vaadinSession.setAttribute(REDIRECT_SESSION_KEY, null);
            }
        } catch (Exception ignored) {
            // Last resort - ignore
        }
    }

    public static boolean isSafeInternalPath(String path) {
        if (!StringUtils.hasText(path)) return false;

        // Normalise: remove leading slash for validation, but allow it
        String normalized = path.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // Block attempts to go up directories, external URLs, or double slashes
        if (normalized.isEmpty() || normalized.contains("..") || normalized.contains("//")
                || normalized.contains("\\") || normalized.contains(":")) {
            return false;
        }

        // Only allow safe characters (letters, digits, hyphens, underscores, slashes)
        return normalized.matches("^[a-zA-Z0-9\\-/_]+$");
    }

    public static void storeRedirect(String path) {
        if (!isSafeInternalPath(path)) {
            log.warn("❌ Rejected unsafe redirect path: {}", path);
            return;
        }

        // Prefer VaadinSession
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession != null) {
            vaadinSession.setAttribute(REDIRECT_SESSION_KEY, path);
            log.info("🔐 Stored redirect in VaadinSession (id={}): {}",
                    vaadinSession.getSession() != null ? vaadinSession.getSession().getId() : "unknown", path);
            return;
        }

        // Fallback to HttpSession
        try {
            var req = VaadinServletRequest.getCurrent();
            if (req != null) {
                HttpSession httpSession = req.getSession(true);
                if (httpSession != null) {
                    httpSession.setAttribute(REDIRECT_SESSION_KEY, path);
                    log.info("🔐 Stored redirect in HttpSession: {}", path);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Could not store redirect in HttpSession", e);
        }

        log.warn("⚠️ Could not store redirect – no session available.");
    }

    public static String consumeRedirect() {
        // Try VaadinSession first
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession != null) {
            String path = (String) vaadinSession.getAttribute(REDIRECT_SESSION_KEY);
            if (path != null) {
                vaadinSession.setAttribute(REDIRECT_SESSION_KEY, null);
                log.info("🔐 Consumed redirect from VaadinSession: {}", path);
                return path;
            }
        }

        // Fallback to HttpSession
        try {
            var req = VaadinServletRequest.getCurrent();
            if (req != null) {
                HttpSession httpSession = req.getSession(false);
                if (httpSession != null) {
                    String path = (String) httpSession.getAttribute(REDIRECT_SESSION_KEY);
                    if (path != null) {
                        httpSession.removeAttribute(REDIRECT_SESSION_KEY);
                        log.info("🔐 Consumed redirect from HttpSession: {}", path);
                        return path;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error consuming redirect from HttpSession", e);
        }

        return null;
    }

    public static boolean isUserLoggedIn() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) return false;

        // First check if user is in VaadinSession
        Object user = vaadinSession.getAttribute("user");
        if (user != null) {
            return true;
        }

        // If not, check HttpSession and copy to VaadinSession
        try {
            WrappedSession httpSession = vaadinSession.getSession();
            if (httpSession != null) {
                Object httpUser = httpSession.getAttribute("user");
                if (httpUser != null) {
                    // Sync from HttpSession to VaadinSession
                    vaadinSession.setAttribute("user", httpUser);
                    vaadinSession.setAttribute("accessToken", httpSession.getAttribute("accessToken"));
                    vaadinSession.setAttribute("refreshToken", httpSession.getAttribute("refreshToken"));
                    vaadinSession.setAttribute("tenant", httpSession.getAttribute("tenant"));
                    vaadinSession.setAttribute("roles", httpSession.getAttribute("roles"));
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Error checking HttpSession for user", e);
        }

        return false;
    }

    /**
     * Gets the current logged-in user from the session.
     *
     * @return the user object, or null if not logged in
     */
    public static Object getCurrentUser() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) return null;
        return vaadinSession.getAttribute("user");
    }

    /**
     * Gets the current access token from the session.
     *
     * @return the access token, or null if not available
     */
    public static String getAccessToken() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) return null;
        return (String) vaadinSession.getAttribute("accessToken");
    }

    /**
     * Gets the current tenant from the session.
     *
     * @return the tenant, or null if not available
     */
    public static String getCurrentTenant() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) return null;
        return (String) vaadinSession.getAttribute("tenant");
    }
}