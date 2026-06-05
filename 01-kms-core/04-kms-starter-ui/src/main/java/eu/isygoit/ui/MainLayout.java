package eu.isygoit.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.ui.views.cryptography.byok.ByokView;
import eu.isygoit.ui.views.cryptography.crypto.CryptoOperationsView;
import eu.isygoit.ui.views.cryptography.incremental.IncrementalKeyView;
import eu.isygoit.ui.views.cryptography.key.KeyManagementView;
import eu.isygoit.ui.views.cryptography.keyAlias.AliasesView;
import eu.isygoit.ui.views.cryptography.keyGrants.GrantsView;
import eu.isygoit.ui.views.cryptography.keyPolicy.PoliciesView;
import eu.isygoit.ui.views.cryptography.keyStore.CustomKeyStoresView;
import eu.isygoit.ui.views.cryptography.keyTag.TagsView;
import eu.isygoit.ui.views.tokenizer.builder.TokenBuilderView;
import eu.isygoit.ui.views.tokenizer.config.TokenConfigView;
// import eu.isygoit.ui.views.tokenizer.TokenManagementView; // to be implemented

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 title = new H1("KMS Console");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);
        addToNavbar(new DrawerToggle(), title);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        // ================= HOME =================
        SideNavItem home = new SideNavItem("Home", "home", VaadinIcon.HOME.create());

        // ================= CRYPTOGRAPHY (parent) =================
        SideNavItem cryptoParent = new SideNavItem("Cryptography");
        cryptoParent.setPrefixComponent(VaadinIcon.LOCK.create());

        cryptoParent.addItem(new SideNavItem("Key Management", KeyManagementView.class, VaadinIcon.KEY.create()));
        cryptoParent.addItem(new SideNavItem("Key Aliases", AliasesView.class, VaadinIcon.LINK.create()));
        cryptoParent.addItem(new SideNavItem("Key Tagging", TagsView.class, VaadinIcon.TAGS.create()));
        cryptoParent.addItem(new SideNavItem("Key Policies", PoliciesView.class, VaadinIcon.DIPLOMA.create()));
        cryptoParent.addItem(new SideNavItem("Grants", GrantsView.class, VaadinIcon.SHIELD.create()));
        cryptoParent.addItem(new SideNavItem("BYOK", ByokView.class, VaadinIcon.DOWNLOAD_ALT.create()));
        cryptoParent.addItem(new SideNavItem("Custom Key Stores", CustomKeyStoresView.class, VaadinIcon.DATABASE.create()));
        cryptoParent.addItem(new SideNavItem("Cryptographic Operations", CryptoOperationsView.class, VaadinIcon.LOCK.create()));
        cryptoParent.addItem(new SideNavItem("Incremental Key", IncrementalKeyView.class, VaadinIcon.CLOCK.create()));

        // ================= TOKENIZER (parent) =================
        SideNavItem tokenParent = new SideNavItem("Tokenizer");
        tokenParent.setPrefixComponent(VaadinIcon.CODE.create());

        tokenParent.addItem(new SideNavItem("Token Configurations", TokenConfigView.class, VaadinIcon.TABLE.create()));
        tokenParent.addItem(new SideNavItem("Token Builder", TokenBuilderView.class, VaadinIcon.COG.create()));

        // Assemble main navigation
        nav.addItem(home, cryptoParent, tokenParent);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}