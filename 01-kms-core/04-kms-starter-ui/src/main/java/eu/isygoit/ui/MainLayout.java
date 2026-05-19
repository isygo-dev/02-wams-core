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
import eu.isygoit.ui.views.*;
import eu.isygoit.ui.views.alias.AliasesView;
import eu.isygoit.ui.views.key.KeyManagementView;

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
        SideNavItem home = new SideNavItem(
                "Home",
                "home",
                VaadinIcon.HOME.create()
        );

        // ================= KEY MANAGEMENT =================
        SideNavItem keyMgmt = new SideNavItem(
                "Key Management",
                KeyManagementView.class,
                VaadinIcon.KEY.create()
        );

        // ================= ALIASES =================
        SideNavItem aliases = new SideNavItem(
                "Key Aliases",
                AliasesView.class,
                VaadinIcon.LINK.create()
        );

        // ================= TAGS =================
        SideNavItem tags = new SideNavItem(
                "Key Tagging",
                TagsView.class,
                VaadinIcon.TAGS.create()
        );

        // ================= POLICIES =================
        SideNavItem policies = new SideNavItem(
                "Key Policies",
                PoliciesView.class,
                VaadinIcon.DIPLOMA.create()
        );

        // ================= GRANTS =================
        SideNavItem grants = new SideNavItem(
                "Grants",
                GrantsView.class,
                VaadinIcon.SHIELD.create()
        );

        // ================= BYOK =================
        SideNavItem byok = new SideNavItem(
                "BYOK",
                ByokView.class,
                VaadinIcon.DOWNLOAD_ALT.create()
        );

        // ================= CUSTOM KEY STORES =================
        SideNavItem stores = new SideNavItem(
                "Custom Key Stores",
                CustomKeyStoresView.class,
                VaadinIcon.DATABASE.create()
        );

        // ================= CRYPTO =================
        SideNavItem crypto = new SideNavItem(
                "Cryptographic Operations",
                CryptoOperationsView.class,
                VaadinIcon.LOCK.create()
        );

        nav.addItem(
                home,
                keyMgmt,
                aliases,
                tags,
                policies,
                grants,
                byok,
                stores,
                crypto
        );

        Scroller scroller = new Scroller(nav);

        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();

        addToDrawer(layout);
    }
}