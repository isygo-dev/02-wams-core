package eu.isygoit.ui.kms.nav;

import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.ui.common.component.AppSearchBar;
import eu.isygoit.ui.common.component.INavRegistry;

import java.util.List;

/**
 * Static registry of every real, navigable destination in the app (one entry
 * per implemented {@code @Route}, dead/unimplemented routes intentionally
 * excluded). Used by {@link AppSearchBar} to power header-level quick
 * navigation across all modules.
 *
 * <p>{@code labelKey} is an i18n key, not display text — resolve it with
 * {@code I18n.t(...)} at render/search time so results always reflect the
 * current request's locale.
 *
 * <p>{@code moduleKey} (kms/ims/dms/sms/cms/mms) classifies each target by
 * module so {@link AppSearchBar} can group/filter results by module.
 */
public final class KmsNavRegistry implements INavRegistry {

    public static final List<NavTarget> ALL = List.of(

            // KMS
            new NavTarget("kms.nav.dashboard", "kms/home", VaadinIcon.HOME, "kms"),
            new NavTarget("kms.nav.keys", "kms/keys", VaadinIcon.KEY, "kms"),
            new NavTarget("kms.nav.aliases", "kms/aliases", VaadinIcon.LINK, "kms"),
            new NavTarget("kms.nav.tags", "kms/tags", VaadinIcon.TAGS, "kms"),
            new NavTarget("kms.nav.policies", "kms/policies", VaadinIcon.DIPLOMA, "kms"),
            new NavTarget("kms.nav.grants", "kms/grants", VaadinIcon.SHIELD, "kms"),
            new NavTarget("kms.nav.custom.key.stores", "kms/custom-key-stores", VaadinIcon.DATABASE, "kms"),
            new NavTarget("kms.nav.byok", "kms/byok", VaadinIcon.DOWNLOAD_ALT, "kms"),
            new NavTarget("kms.nav.crypto.operations", "kms/crypto", VaadinIcon.LOCK, "kms"),
            new NavTarget("kms.nav.random.keys", "kms/random-keys", VaadinIcon.RANDOM, "kms"),
            new NavTarget("kms.nav.incremental.key", "kms/incremental-key", VaadinIcon.CLOCK, "kms"),
            new NavTarget("kms.nav.peb.config", "kms/peb-configs", VaadinIcon.COG, "kms"),
            new NavTarget("kms.nav.digest.config", "kms/digest-configs", VaadinIcon.HASH, "kms"),
            new NavTarget("kms.nav.password.config", "kms/password-configs", VaadinIcon.ASTERISK, "kms"),
            new NavTarget("kms.nav.token.configurations", "kms/token-configs", VaadinIcon.TABLE, "kms"),
            new NavTarget("kms.nav.token.builder", "kms/token-builder", VaadinIcon.COG, "kms")
    );

    public KmsNavRegistry() {
    }

    /**
     * i18n key for a module's full display name (e.g. "Identity Management"
     * for "ims") shown as the group header above that module's results in
     * {@link AppSearchBar}. Deliberately shorter than the landing page's
     * "... Service" card titles ({@code common.landing.<key>.title}) — this
     * is a compact dropdown group header, not a full module-picker card.
     */
    public String moduleLabelKey(String moduleKey) {
        return "common.nav.module." + moduleKey;
    }

    /**
     * Get all navigation targets.
     *
     * @return list of all navigation targets
     */
    public List<NavTarget> getAll() {
        return ALL;
    }
}