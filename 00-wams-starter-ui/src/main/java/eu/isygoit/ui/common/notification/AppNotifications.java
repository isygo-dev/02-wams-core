package eu.isygoit.ui.common.notification;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * One way to tell the user what happened: same position, a duration per severity and the matching Lumo variant,
 * so every module gives the same feedback. Messages must already be translated.
 */
public final class AppNotifications {

    private static final Notification.Position POSITION = Notification.Position.BOTTOM_END;

    private AppNotifications() {
    }

    /**
     * Short confirmation of an action that worked.
     */
    public static Notification success(String message) {
        return show(message, 3000, NotificationVariant.LUMO_SUCCESS);
    }

    /**
     * Something the user should know but nothing failed.
     */
    public static Notification warning(String message) {
        return show(message, 5000, NotificationVariant.LUMO_WARNING);
    }

    /**
     * An action failed: stays longer so the explanation can be read.
     */
    public static Notification error(String message) {
        return show(message, 6000, NotificationVariant.LUMO_ERROR);
    }

    private static Notification show(String message, int durationMs, NotificationVariant variant) {
        Notification notification = Notification.show(message, durationMs, POSITION);
        notification.addThemeVariants(variant);
        return notification;
    }
}
