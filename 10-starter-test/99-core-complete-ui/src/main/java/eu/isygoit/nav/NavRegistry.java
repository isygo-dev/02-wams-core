package eu.isygoit.nav;

import eu.isygoit.ui.common.component.AppSearchBar;
import eu.isygoit.ui.common.component.INavRegistry;
import eu.isygoit.ui.kms.nav.KmsNavRegistry;

import java.util.List;
import java.util.stream.Stream;

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
public final class NavRegistry implements INavRegistry {

    private static final List<NavTarget> ALL = Stream.of(
                    new ImsNavRegistry().getAll(),
                    new DmsNavRegistry().getAll(),
                    new SmsNavRegistry().getAll(),
                    new CmsNavRegistry().getAll(),
                    new MmsNavRegistry().getAll(),
                    new KmsNavRegistry().getAll()
            ).flatMap(List::stream)
            .toList();

    public NavRegistry() {
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