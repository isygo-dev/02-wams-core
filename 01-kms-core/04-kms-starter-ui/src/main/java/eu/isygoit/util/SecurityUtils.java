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

    private SecurityUtils() {}

    public static boolean isSafeInternalPath(String path) {
        if (!StringUtils.hasText(path)) return false;
        return path.startsWith("/")
                && !path.contains("://")
                && !path.contains("..")
                && !path.contains("//")
                && !path.contains("\\")
                && path.matches("^/[a-zA-Z0-9\\-/_?=&]+$"); // Allow query params
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
            log.info("🔐 Stored redirect in VaadinSession (id={}): {}", vaadinSession.getSession().getId(), path);
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

    // ... keep isUserLoggedIn() as is
    public static boolean isUserLoggedIn() {
        // (your current implementation)
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) return false;

        WrappedSession httpSession = vaadinSession.getSession();
        if (httpSession != null) {
            Object user = httpSession.getAttribute("user");
            if (user != null) {
                if (vaadinSession.getAttribute("user") == null) {
                    vaadinSession.setAttribute("user", user);
                    vaadinSession.setAttribute("accessToken", httpSession.getAttribute("accessToken"));
                }
                return true;
            }
        }
        return vaadinSession.getAttribute("user") != null;
    }
}