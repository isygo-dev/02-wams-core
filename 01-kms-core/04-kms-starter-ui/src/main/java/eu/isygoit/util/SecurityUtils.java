package eu.isygoit.util;

import com.vaadin.flow.server.VaadinSession;

public class SecurityUtils {

    public static boolean isUserLoggedIn() {
        return VaadinSession.getCurrent().getAttribute("user") != null;
    }

    public static String getCurrentUsername() {
        Object user = VaadinSession.getCurrent().getAttribute("user");
        return user != null ? user.toString() : null;
    }
}