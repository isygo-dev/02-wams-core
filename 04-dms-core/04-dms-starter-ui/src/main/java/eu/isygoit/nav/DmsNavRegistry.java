package eu.isygoit.nav;

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
public final class DmsNavRegistry implements INavRegistry {

    private static final List<NavTarget> ALL = List.of(
            // DMS
            new NavTarget("dms.nav.dashboard", "dms", VaadinIcon.HOME, "dms"),
            new NavTarget("dms.nav.categories", "dms/categories", VaadinIcon.FOLDER, "dms")
    );

    public DmsNavRegistry() {
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