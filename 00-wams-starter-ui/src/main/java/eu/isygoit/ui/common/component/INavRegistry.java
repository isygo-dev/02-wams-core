package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.icon.VaadinIcon;

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
 * <p>{@code moduleKey} (kms/ims/dms/sms/cms/mms/edyra-pilot) classifies each
 * target by module so {@link AppSearchBar} can group/filter results by module.
 *
 * <p>{@code sectionKey} and {@code parentRoute} are optional and only used by
 * modules that build a hierarchical drawer (e.g. PILOT). Left null they mean
 * "no section" and "top-level item" respectively, which keeps every existing
 * registry untouched.
 */
public interface INavRegistry {

    /**
     * i18n key for a module's full display name (e.g. "Identity Management"
     * for "ims") shown as the group header above that module's results in
     * {@link AppSearchBar}. Deliberately shorter than the landing page's
     * "... Service" card titles ({@code common.landing.<key>.title}) — this
     * is a compact dropdown group header, not a full module-picker card.
     */
    String moduleLabelKey(String moduleKey);

    /**
     * Get all navigation targets.
     *
     * @return list of all navigation targets
     */
    List<NavTarget> getAll();

    /**
     * One menu entry.
     * <ul>
     *   <li>{@code sectionKey} — nullable. i18n key of the section (e.g.
     *       {@code "edyra.pilot.section.life"}). Null = no section.</li>
     *   <li>{@code parentRoute} — nullable. Route of the parent entry when this
     *       target is a sub-item. Null = top-level item.</li>
     * </ul>
     */
    record NavTarget(
            String labelKey,
            String route,
            VaadinIcon icon,
            String moduleKey,
            String sectionKey,
            String parentRoute
    ) {
        /** Backward-compatible constructor: no section, top-level item. */
        public NavTarget(String labelKey, String route, VaadinIcon icon, String moduleKey) {
            this(labelKey, route, icon, moduleKey, null, null);
        }

        public boolean isSubItem() {
            return parentRoute != null;
        }

        public boolean hasSection() {
            return sectionKey != null;
        }
    }
}