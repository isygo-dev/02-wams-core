package eu.isygoit.ui.common.view;

public final class ViewTitleUtils {

    private ViewTitleUtils() {
    }

    public static String humanize(Class<?> viewClass) {
        String title = viewClass.getSimpleName().replaceFirst("View$", "");
        return title
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
    }
}
