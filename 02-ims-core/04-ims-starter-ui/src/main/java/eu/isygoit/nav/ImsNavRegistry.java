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
public final class ImsNavRegistry implements INavRegistry {

    private static final List<NavTarget> ALL = List.of(
            // IMS
            new NavTarget("ims.nav.dashboard", "ims", VaadinIcon.HOME, "ims"),
            new NavTarget("ims.nav.accounts", "ims/accounts", VaadinIcon.USER, "ims"),
            new NavTarget("ims.nav.customers", "ims/customers", VaadinIcon.GROUP, "ims"),
            new NavTarget("ims.nav.tenants", "ims/tenants", VaadinIcon.BUILDING, "ims"),
            new NavTarget("ims.nav.registeredUsers", "ims/registered-users", VaadinIcon.CLIPBOARD_USER, "ims"),
            new NavTarget("ims.nav.applications", "ims/applications", VaadinIcon.PAPERCLIP, "ims"),
            new NavTarget("ims.nav.roles", "ims/roles", VaadinIcon.SHIELD, "ims"),
            new NavTarget("ims.nav.parameters", "ims/parameters", VaadinIcon.KEYBOARD, "ims"),
            new NavTarget("ims.nav.annexes", "ims/annexes", VaadinIcon.FOLDER_OPEN, "ims")
    );

    public ImsNavRegistry() {
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