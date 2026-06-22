package eu.isygoit.ui.kms.layout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.ui.common.layout.BaseMainLayout;

public class KmsMainLayout extends BaseMainLayout {

    @Override
    protected String getTitle() {
        return "Key Management System";
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem("Dashboard", "kms", VaadinIcon.HOME.create());

        SideNavItem keyMgmt = new SideNavItem("Key Management");
        keyMgmt.setPrefixComponent(VaadinIcon.KEY.create());
        keyMgmt.addItem(new SideNavItem("Keys", "kms/keys", VaadinIcon.KEY.create()));
        keyMgmt.addItem(new SideNavItem("Aliases", "kms/aliases", VaadinIcon.LINK.create()));
        keyMgmt.addItem(new SideNavItem("Tags", "kms/tags", VaadinIcon.TAGS.create()));
        keyMgmt.addItem(new SideNavItem("Policies", "kms/policies", VaadinIcon.DIPLOMA.create()));
        keyMgmt.addItem(new SideNavItem("Grants", "kms/grants", VaadinIcon.SHIELD.create()));
        keyMgmt.addItem(new SideNavItem("Custom Key Stores", "kms/custom-key-stores", VaadinIcon.DATABASE.create()));
        keyMgmt.addItem(new SideNavItem("BYOK", "kms/byok", VaadinIcon.DOWNLOAD_ALT.create()));

        SideNavItem cryptoOps = new SideNavItem("Crypto Operations");
        cryptoOps.setPrefixComponent(VaadinIcon.LOCK.create());
        cryptoOps.addItem(new SideNavItem("E/D & S/V", "kms/crypto", VaadinIcon.LOCK.create()));

        SideNavItem valueGen = new SideNavItem("Key Value Generators");
        valueGen.setPrefixComponent(VaadinIcon.HASH.create());
        valueGen.addItem(new SideNavItem("Random Keys", "kms/random-keys", VaadinIcon.RANDOM.create()));
        valueGen.addItem(new SideNavItem("Incremental Key", "kms/incremental-key", VaadinIcon.CLOCK.create()));

        SideNavItem passwordMenu = new SideNavItem("Secrets");
        passwordMenu.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordMenu.addItem(new SideNavItem("PEB Config", "kms/peb-configs", VaadinIcon.COG.create()));
        passwordMenu.addItem(new SideNavItem("Digest Config", "kms/digest-configs", VaadinIcon.DIPLOMA.create()));
        passwordMenu.addItem(new SideNavItem("Password Config", "kms/password-configs", VaadinIcon.USER.create()));

        SideNavItem tokenizer = new SideNavItem("Tokenizer");
        tokenizer.setPrefixComponent(VaadinIcon.CODE.create());
        tokenizer.addItem(new SideNavItem("Token Configurations", "kms/token-configs", VaadinIcon.TABLE.create()));
        tokenizer.addItem(new SideNavItem("Token Builder", "kms/token-builder", VaadinIcon.COG.create()));

        nav.addItem(dashboard, keyMgmt, cryptoOps, valueGen, passwordMenu, tokenizer);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}