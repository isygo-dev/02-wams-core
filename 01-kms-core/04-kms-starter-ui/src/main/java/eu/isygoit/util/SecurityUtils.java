package eu.isygoit.util;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.util.StringUtils;

public class SecurityUtils {

    public static boolean isUserLoggedIn() {
        return VaadinSession.getCurrent().getAttribute("user") != null;
    }

    public static String getCurrentUsername() {
        Object user = VaadinSession.getCurrent().getAttribute("user");
        return user != null ? user.toString() : null;
    }

    public static boolean isSafeInternalPath(String path) {
        return StringUtils.hasText(path)
                && !path.startsWith("/")      // Vaadin paths never have a leading slash — reject ones that do (likely tampered)
                && !path.contains("://")      // block "https://evil.com" or "javascript:" style payloads
                && !path.contains("..")
                && !path.contains("//");
    }
}