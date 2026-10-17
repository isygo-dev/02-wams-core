package eu.isygoit.ui.auth;

import eu.isygoit.i18n.I18n;

/**
 * Brand of the authentication pages. The default brand keeps the compact centred card; an application opts in to
 * another brand by defining {@code auth.brand.variant} in its own message bundle (for example {@code edyra}), which
 * switches on the split-screen layout with its hero section and the matching colours.
 *
 * <p>Texts of a brand live under {@code auth.brand.<variant>.*} and {@code auth.hero.<variant>.*}; a missing key
 * falls back to the shared one, so a brand only has to provide what differs.
 */
final class AuthBranding {

    static final String DEFAULT = "default";
    private static final String VARIANT_KEY = "auth.brand.variant";

    private AuthBranding() {
    }

    /**
     * The active brand variant, {@code "default"} when the application did not choose one.
     */
    static String variant() {
        if (I18n.hasKey(VARIANT_KEY)) {
            String value = I18n.t(VARIANT_KEY);
            if (value != null && !value.isBlank()) {
                return value.trim().toLowerCase(java.util.Locale.ROOT);
            }
        }
        return DEFAULT;
    }

    static boolean isBranded() {
        return !DEFAULT.equals(variant());
    }

    /**
     * Brand name (also the first heading of the login page).
     */
    static String title() {
        return text("auth.brand." + variant() + ".title", "auth.common.brand.title");
    }

    static String subtitle() {
        return text("auth.brand." + variant() + ".subtitle", "auth.common.brand.subtitle");
    }

    /**
     * Heading of the login page; the brand name when the brand has no dedicated sentence.
     */
    static String loginHeading() {
        return text("auth.brand." + variant() + ".login.heading", "auth.brand." + variant() + ".title",
                "auth.common.brand.title");
    }

    /**
     * Text of the hero section of the active brand.
     */
    static String hero(String name) {
        return text("auth.hero." + variant() + "." + name, "auth.hero." + DEFAULT + "." + name);
    }

    /**
     * Illustration of the hero: the bundled drawing unless the application points to its own image with
     * {@code auth.hero.<variant>.image} (a path under {@code META-INF/resources}, for example {@code auth/edyra-hero.webp}).
     */
    static String heroImage() {
        String key = "auth.hero." + variant() + ".image";
        return I18n.hasKey(key) ? I18n.t(key) : "auth/edyra-hero.svg";
    }

    /**
     * The first key that exists wins; the last one is returned as it is (the usual i18n fallback).
     */
    private static String text(String... keys) {
        for (String key : keys) {
            if (I18n.hasKey(key)) {
                return I18n.t(key);
            }
        }
        return I18n.t(keys[keys.length - 1]);
    }
}
