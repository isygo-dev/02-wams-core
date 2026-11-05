package eu.isygoit.ui.common.view;

import eu.isygoit.i18n.I18n;

import java.util.Locale;

public final class ViewTitleUtils {

    private ViewTitleUtils() {
    }

    public static String humanize(Class<?> viewClass) {
        String simpleName = viewClass.getSimpleName().replaceFirst("View$", "");
        String title = simpleName
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
        String titleKey = simpleName
                .replaceAll("([a-z0-9])([A-Z])", "$1.$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1.$2")
                .toLowerCase(Locale.ROOT);
        return I18n.tWithDefault("common.page.title." + titleKey, title);
    }
}
