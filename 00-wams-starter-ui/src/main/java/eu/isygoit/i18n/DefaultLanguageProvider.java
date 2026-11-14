package eu.isygoit.i18n;

import java.util.Locale;
import java.util.Optional;

/**
 * Supplies the language the application shows by default, e.g. the default language an institution sets in its
 * profile. It applies to users who have not picked a language themselves; a user's own choice always wins. An
 * application provides at most one; without one the browser or provider default is kept.
 */
@FunctionalInterface
public interface DefaultLanguageProvider {

    /** The default language, or empty when none is set or it cannot be read (not signed in, service down...). */
    Optional<Locale> defaultLanguage();

    /**
     * Digits to write numbers and dates with: {@code "latn"} (1,600,000) or {@code "arab"} (١٬٦٠٠٬٠٠٠); empty when
     * unknown, in which case the session keeps what it has (Latin by default).
     */
    default Optional<String> numeralSystem() {
        return Optional.empty();
    }
}
