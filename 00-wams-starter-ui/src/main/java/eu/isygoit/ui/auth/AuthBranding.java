package eu.isygoit.ui.auth;

import com.vaadin.flow.component.html.Image;
import eu.isygoit.i18n.I18n;

/**
 * Brand of the authentication pages. The default brand keeps the compact centred card; an application opts in to
 * another brand by defining {@code auth.brand.variant} in its own message bundle (for example {@code edyra}), which
 * switches on the split-screen layout with its hero section.
 *
 * <p>Everything specific to a brand lives in the module that defines it, not here:
 * <ul>
 *   <li>texts: {@code auth.brand.<variant>.*} and {@code auth.hero.<variant>.*} (a missing key falls back to the
 *       shared one);</li>
 *   <li>images: {@code auth.brand.<variant>.assets} is the folder (under {@code META-INF/resources}) of the module's
 *       SVG files. The files are looked up by name: {@code logo.svg}, {@code background.svg},
 *       {@code illustration.svg}, {@code input-field.svg} and the {@code icon-*.svg} icons. None of them carries text,
 *       every word shown comes from the message bundles.</li>
 * </ul>
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
     * Any shared authentication text, or the brand's own wording of it: the key {@code auth.login.field.tenant.label}
     * is overridden by {@code auth.brand.<variant>.login.field.tenant.label}.
     */
    static String text(String sharedKey) {
        String suffix = sharedKey.startsWith("auth.") ? sharedKey.substring("auth.".length()) : sharedKey;
        return text("auth.brand." + variant() + "." + suffix, sharedKey);
    }

    /**
     * URL of one of the brand's SVG files, {@code null} when the brand has none.
     */
    static String asset(String file) {
        String key = "auth.brand." + variant() + ".assets";
        if (!I18n.hasKey(key)) {
            return null;
        }
        String base = I18n.t(key).trim();
        return base.isEmpty() ? null : base + "/" + file;
    }

    /**
     * A decorative image of the brand ({@code null} when it has no assets). It is hidden from assistive technology:
     * the text next to it carries the meaning.
     */
    static Image image(String file, String cssClass) {
        String url = asset(file);
        if (url == null) {
            return null;
        }
        Image image = new Image(url, "");
        image.addClassName(cssClass);
        image.getElement().setAttribute("aria-hidden", "true");
        return image;
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
