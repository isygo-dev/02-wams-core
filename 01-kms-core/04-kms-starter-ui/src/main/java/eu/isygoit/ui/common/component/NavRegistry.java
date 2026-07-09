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
 */
public final class NavRegistry {

    public record NavTarget(String labelKey, String route, VaadinIcon icon) {
    }

    public static final List<NavTarget> ALL = List.of(
            // IMS
            new NavTarget("ims.nav.dashboard", "ims", VaadinIcon.HOME),
            new NavTarget("ims.nav.accounts", "ims/accounts", VaadinIcon.USER),
            new NavTarget("ims.nav.customers", "ims/customers", VaadinIcon.GROUP),
            new NavTarget("ims.nav.tenants", "ims/tenants", VaadinIcon.BUILDING),
            new NavTarget("ims.nav.applications", "ims/applications", VaadinIcon.PAPERCLIP),
            new NavTarget("ims.nav.roles", "ims/roles", VaadinIcon.SHIELD),
            new NavTarget("ims.nav.parameters", "ims/parameters", VaadinIcon.KEYBOARD),
            new NavTarget("ims.nav.annexes", "ims/annexes", VaadinIcon.FOLDER_OPEN),

            // DMS
            new NavTarget("dms.nav.dashboard", "dms", VaadinIcon.HOME),
            new NavTarget("dms.nav.categories", "dms/categories", VaadinIcon.FOLDER),

            // SMS
            new NavTarget("sms.nav.dashboard", "sms", VaadinIcon.HOME),
            new NavTarget("sms.nav.storage.configs", "sms/storageconfigs", VaadinIcon.DATABASE),

            // CMS
            new NavTarget("cms.nav.dashboard", "cms", VaadinIcon.HOME),
            new NavTarget("cms.nav.calendars", "cms/calendars", VaadinIcon.CALENDAR),

            // MMS
            new NavTarget("mms.nav.dashboard", "mms/home", VaadinIcon.HOME),
            new NavTarget("mms.nav.sender.config", "mms/sender-config", VaadinIcon.MAILBOX),
            new NavTarget("mms.nav.templates", "mms/templates", VaadinIcon.FILE_TEXT),

            // KMS
            new NavTarget("kms.nav.dashboard", "kms/home", VaadinIcon.HOME),
            new NavTarget("kms.nav.keys", "kms/keys", VaadinIcon.KEY),
            new NavTarget("kms.nav.aliases", "kms/aliases", VaadinIcon.LINK),
            new NavTarget("kms.nav.tags", "kms/tags", VaadinIcon.TAGS),
            new NavTarget("kms.nav.policies", "kms/policies", VaadinIcon.DIPLOMA),
            new NavTarget("kms.nav.grants", "kms/grants", VaadinIcon.SHIELD),
            new NavTarget("kms.nav.custom.key.stores", "kms/custom-key-stores", VaadinIcon.DATABASE),
            new NavTarget("kms.nav.byok", "kms/byok", VaadinIcon.DOWNLOAD_ALT),
            new NavTarget("kms.nav.crypto.operations", "kms/crypto", VaadinIcon.LOCK),
            new NavTarget("kms.nav.random.keys", "kms/random-keys", VaadinIcon.RANDOM),
            new NavTarget("kms.nav.incremental.key", "kms/incremental-key", VaadinIcon.CLOCK),
            new NavTarget("kms.nav.peb.config", "kms/peb-configs", VaadinIcon.COG),
            new NavTarget("kms.nav.digest.config", "kms/digest-configs", VaadinIcon.HASH),
            new NavTarget("kms.nav.password.config", "kms/password-configs", VaadinIcon.ASTERISK),
            new NavTarget("kms.nav.token.configurations", "kms/token-configs", VaadinIcon.TABLE),
            new NavTarget("kms.nav.token.builder", "kms/token-builder", VaadinIcon.COG)
    );

    private NavRegistry() {
    }
}
