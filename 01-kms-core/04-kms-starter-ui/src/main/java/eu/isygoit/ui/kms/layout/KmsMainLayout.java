package eu.isygoit.ui.kms.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

@CssImport("./styles/kms.css")
public class KmsMainLayout extends BaseMainLayout {

    @Override
    protected String getModuleKey() {
        return "kms";
    }

    @Override
    protected String getTitle() {
        return I18n.t("kms.layout.title");
    }

    @Override
    protected Component createDrawerContent() {
        SideNav overview = new SideNav();
        overview.addItem(navItem(I18n.t("kms.nav.dashboard"), "kms/home", VaadinIcon.HOME));

        // Managing key resources and using them for crypto operations — flat
        // list under one section instead of an expand/collapse tree, so all 8
        // destinations are visible without an extra click.
        SideNav keyManagement = new SideNav(I18n.t("kms.nav.key.management"));
        keyManagement.addItem(navItem(I18n.t("kms.nav.keys"), "kms/keys", VaadinIcon.KEY));
        keyManagement.addItem(navItem(I18n.t("kms.nav.aliases"), "kms/aliases", VaadinIcon.LINK));
        keyManagement.addItem(navItem(I18n.t("kms.nav.tags"), "kms/tags", VaadinIcon.TAGS));
        keyManagement.addItem(navItem(I18n.t("kms.nav.policies"), "kms/policies", VaadinIcon.DIPLOMA));
        keyManagement.addItem(navItem(I18n.t("kms.nav.grants"), "kms/grants", VaadinIcon.SHIELD));
        keyManagement.addItem(navItem(I18n.t("kms.nav.custom.key.stores"), "kms/custom-key-stores", VaadinIcon.DATABASE));
        keyManagement.addItem(navItem(I18n.t("kms.nav.byok"), "kms/byok", VaadinIcon.DOWNLOAD_ALT));
        keyManagement.addItem(navItem(I18n.t("kms.nav.crypto.operations"), "kms/crypto", VaadinIcon.LOCK));

        SideNav generators = new SideNav(I18n.t("kms.nav.key.value.generators"));
        generators.addItem(navItem(I18n.t("kms.nav.random.keys"), "kms/random-keys", VaadinIcon.RANDOM));
        generators.addItem(navItem(I18n.t("kms.nav.incremental.key"), "kms/incremental-key", VaadinIcon.CLOCK));

        SideNav secrets = new SideNav(I18n.t("kms.nav.secrets"));
        secrets.addItem(navItem(I18n.t("kms.nav.peb.config"), "kms/peb-configs", VaadinIcon.COG));
        secrets.addItem(navItem(I18n.t("kms.nav.digest.config"), "kms/digest-configs", VaadinIcon.HASH));
        secrets.addItem(navItem(I18n.t("kms.nav.password.config"), "kms/password-configs", VaadinIcon.ASTERISK));

        SideNav tokenizer = new SideNav(I18n.t("kms.nav.tokenizer"));
        tokenizer.addItem(navItem(I18n.t("kms.nav.token.configurations"), "kms/token-configs", VaadinIcon.TABLE));
        tokenizer.addItem(navItem(I18n.t("kms.nav.token.builder"), "kms/token-builder", VaadinIcon.COG));

        VerticalLayout layout = new VerticalLayout(overview, keyManagement, generators, secrets, tokenizer);
        layout.setPadding(false);
        layout.setSpacing(false);
        return new Scroller(layout);
    }
}
