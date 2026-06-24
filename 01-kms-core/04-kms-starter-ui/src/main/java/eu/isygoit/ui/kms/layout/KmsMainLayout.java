package eu.isygoit.ui.kms.layout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

public class KmsMainLayout extends BaseMainLayout {

    @Override
    protected String getTitle() {
        return I18n.t("kms.layout.title");
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem(I18n.t("kms.nav.dashboard"), "kms/home", VaadinIcon.HOME.create());

        SideNavItem keyMgmt = new SideNavItem(I18n.t("kms.nav.key.management"));
        keyMgmt.setPrefixComponent(VaadinIcon.KEY.create());
        keyMgmt.addItem(new SideNavItem(I18n.t("kms.nav.keys"), "kms/keys", VaadinIcon.KEY.create()));
        keyMgmt.addItem(new SideNavItem(I18n.t("kms.nav.aliases"), "kms/aliases", VaadinIcon.LINK.create()));
        keyMgmt.addItem(new SideNavItem(I18n.t("kms.nav.tags"), "kms/tags", VaadinIcon.TAGS.create()));
        keyMgmt.addItem(new SideNavItem(I18n.t("kms.nav.policies"), "kms/policies", VaadinIcon.DIPLOMA.create()));
        keyMgmt.addItem(new SideNavItem(I18n.t("kms.nav.grants"), "kms/grants", VaadinIcon.SHIELD.create()));
        keyMgmt.addItem(new SideNavItem(I18n.t("kms.nav.custom.key.stores"), "kms/custom-key-stores", VaadinIcon.DATABASE.create()));
        keyMgmt.addItem(new SideNavItem(I18n.t("kms.nav.byok"), "kms/byok", VaadinIcon.DOWNLOAD_ALT.create()));

        SideNavItem cryptoOps = new SideNavItem(I18n.t("kms.nav.crypto.operations"));
        cryptoOps.setPrefixComponent(VaadinIcon.LOCK.create());
        cryptoOps.addItem(new SideNavItem(I18n.t("kms.nav.encrypt.decrypt.sign.verify"), "kms/crypto", VaadinIcon.LOCK.create()));

        SideNavItem valueGen = new SideNavItem(I18n.t("kms.nav.key.value.generators"));
        valueGen.setPrefixComponent(VaadinIcon.HASH.create());
        valueGen.addItem(new SideNavItem(I18n.t("kms.nav.random.keys"), "kms/random-keys", VaadinIcon.RANDOM.create()));
        valueGen.addItem(new SideNavItem(I18n.t("kms.nav.incremental.key"), "kms/incremental-key", VaadinIcon.CLOCK.create()));

        SideNavItem passwordMenu = new SideNavItem(I18n.t("kms.nav.secrets"));
        passwordMenu.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordMenu.addItem(new SideNavItem(I18n.t("kms.nav.peb.config"), "kms/peb-configs", VaadinIcon.COG.create()));
        passwordMenu.addItem(new SideNavItem(I18n.t("kms.nav.digest.config"), "kms/digest-configs", VaadinIcon.DIPLOMA.create()));
        passwordMenu.addItem(new SideNavItem(I18n.t("kms.nav.password.config"), "kms/password-configs", VaadinIcon.USER.create()));

        SideNavItem tokenizer = new SideNavItem(I18n.t("kms.nav.tokenizer"));
        tokenizer.setPrefixComponent(VaadinIcon.CODE.create());
        tokenizer.addItem(new SideNavItem(I18n.t("kms.nav.token.configurations"), "kms/token-configs", VaadinIcon.TABLE.create()));
        tokenizer.addItem(new SideNavItem(I18n.t("kms.nav.token.builder"), "kms/token-builder", VaadinIcon.COG.create()));

        nav.addItem(dashboard, keyMgmt, cryptoOps, valueGen, passwordMenu, tokenizer);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}