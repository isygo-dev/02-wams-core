package eu.isygoit.util;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Session helpers for the Vaadin UI.
 *
 * <p>Notes:
 * <ul>
 *   <li>VaadinSession attribute access requires the session lock; call from UI/request threads
 *       or inside {@code ui.access(...)}.</li>
 *   <li>Token lifecycle (refresh, revocation, expiry) is handled by the external KMS service.</li>
 *   <li>The HttpSession is intentionally not invalidated (shared across apps/UIs). Make sure the
 *       login code rotates the session id after authentication (session fixation protection).</li>
 * </ul>
 */
public final class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    private static final String USER_KEY = "user";
    private static final String ACCESS_TOKEN_KEY = "accessToken";
    private static final String REFRESH_TOKEN_KEY = "refreshToken";
    private static final String TENANT_KEY = "tenant";
    private static final String ROLES_KEY = "roles";
    private static final String REDIRECT_SESSION_KEY = "login.redirect";

    private static final List<String> AUTH_KEYS =
            List.of(USER_KEY, ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, TENANT_KEY, ROLES_KEY);

    /**
     * Letters, digits, '-', '_' and '/' only (no dots, colons, backslashes, query, fragment).
     */
    private static final Pattern SAFE_PATH = Pattern.compile("^[a-zA-Z0-9/_-]+$");

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Clears all authentication data from both the VaadinSession and the underlying HttpSession.
     * Sessions themselves are preserved.
     *
     * @return true if authentication data was cleared, false if there was no session or clearing failed
     */
    public static boolean logout() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) {
            log.debug("🔐 Logout called without a VaadinSession");
            return false;
        }

        try {
            AUTH_KEYS.forEach(key -> vaadinSession.setAttribute(key, null));
            vaadinSession.setAttribute(REDIRECT_SESSION_KEY, null);
            log.info("🔐 Cleared VaadinSession authentication attributes");
        } catch (Exception e) {
            // e.g. session lock not held
            log.error("❌ Could not clear VaadinSession authentication attributes", e);
            return false;
        }

        // Clear the same HttpSession through the Vaadin wrapper (no servlet request needed),
        // otherwise isUserLoggedIn() would re-sync the user from it.
        try {
            WrappedSession httpSession = vaadinSession.getSession();
            if (httpSession != null) {
                AUTH_KEYS.forEach(httpSession::removeAttribute);
                httpSession.removeAttribute(REDIRECT_SESSION_KEY);
                log.info("🔐 Cleared HttpSession authentication attributes");
            }
        } catch (IllegalStateException e) {
            // HttpSession already invalidated: nothing left to clear
            log.debug("🔐 HttpSession already invalidated during logout");
        } catch (Exception e) {
            log.error("❌ Could not clear HttpSession authentication attributes", e);
            return false;
        }

        log.info("🔐 User logged out successfully");
        return true;
    }

    /**
     * Validates an internal navigation path such as {@code "/"}, {@code "/dashboard"}
     * or {@code "keys/list"}.
     *
     * @param path the path to validate
     * @return true if the path is safe for internal navigation
     */
    public static boolean isSafeInternalPath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }

        String trimmed = path.trim();

        // Home page
        if ("/".equals(trimmed)) {
            return true;
        }

        // Must be checked on the full value: "//host" is a protocol-relative external URL.
        if (trimmed.contains("//")) {
            return false;
        }

        // The whitelist already excludes '.', ':', '\', '?', '#', whitespace, etc.
        return SAFE_PATH.matcher(trimmed).matches();
    }

    /**
     * Stores a validated redirect path in the current VaadinSession.
     *
     * @param path the internal redirect path
     */
    public static void storeRedirect(String path) {
        if (!isSafeInternalPath(path)) {
            // Never log the raw value (log injection); length is enough for diagnostics.
            log.warn("❌ Rejected unsafe redirect path (length={})", path == null ? 0 : path.length());
            return;
        }

        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) {
            log.warn("⚠️ Could not store redirect: no VaadinSession available");
            return;
        }

        vaadinSession.setAttribute(REDIRECT_SESSION_KEY, path.trim());
        log.debug("🔐 Redirect stored in VaadinSession");
    }

    /**
     * Retrieves and removes the stored redirect path.
     *
     * @return the stored redirect path, or null if none exists or it is no longer valid
     */
    public static String consumeRedirect() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) {
            return null;
        }

        Object value = vaadinSession.getAttribute(REDIRECT_SESSION_KEY);
        vaadinSession.setAttribute(REDIRECT_SESSION_KEY, null);

        if (value instanceof String path) {
            if (isSafeInternalPath(path)) {
                log.debug("🔐 Redirect consumed from VaadinSession");
                return path;
            }
            log.warn("❌ Rejected unsafe stored redirect path (length={})", path.length());
        }
        return null;
    }

    /**
     * Checks whether a user is currently authenticated.
     *
     * <p>If the user is only present in the HttpSession (e.g. set by a login flow outside Vaadin),
     * the authentication data is copied into the VaadinSession.</p>
     *
     * @return true if a user is authenticated
     */
    public static boolean isUserLoggedIn() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) {
            return false;
        }

        if (vaadinSession.getAttribute(USER_KEY) != null) {
            return true;
        }

        try {
            WrappedSession httpSession = vaadinSession.getSession();
            if (httpSession == null || httpSession.getAttribute(USER_KEY) == null) {
                return false;
            }

            for (String key : AUTH_KEYS) {
                vaadinSession.setAttribute(key, httpSession.getAttribute(key));
            }
            log.debug("🔐 Synced authentication data from HttpSession to VaadinSession");
            return true;

        } catch (IllegalStateException e) {
            log.debug("🔐 HttpSession invalidated while checking authentication");
            return false;
        } catch (Exception e) {
            log.warn("⚠️ Error checking authentication from HttpSession", e);
            return false;
        }
    }

    /**
     * @return the current user object, or null if not logged in
     */
    public static Object getCurrentUser() {
        return getAttribute(USER_KEY, Object.class);
    }

    /**
     * Typed variant of {@link #getCurrentUser()}.
     *
     * @return the current user, or null if not logged in or of a different type
     */
    public static <T> T getCurrentUser(Class<T> type) {
        return getAttribute(USER_KEY, type);
    }

    /**
     * @return the current access token, or null if not available
     */
    public static String getAccessToken() {
        return getAttribute(ACCESS_TOKEN_KEY, String.class);
    }

    /**
     * @return the current tenant identifier, or null if not available
     */
    public static String getCurrentTenant() {
        return getAttribute(TENANT_KEY, String.class);
    }

    private static <T> T getAttribute(String key, Class<T> type) {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) {
            return null;
        }
        Object value = vaadinSession.getAttribute(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }
}