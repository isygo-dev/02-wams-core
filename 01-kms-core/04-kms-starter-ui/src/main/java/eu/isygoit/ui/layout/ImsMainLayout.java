package eu.isygoit.ui.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.ui.ImsMainView;
import eu.isygoit.ui.ims.views.*;
import eu.isygoit.ui.views.*;
import eu.isygoit.ui.views.TenantView;

public class ImsMainLayout extends AppLayout {

    public ImsMainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 title = new H1("Identity Management System");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);
        addToNavbar(new DrawerToggle(), title);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        // Dashboard
        SideNavItem dashboard = new SideNavItem("Dashboard", ImsMainView.class, VaadinIcon.HOME.create());

        // Accounts – one entry for everything (list, details, images, stats)
        SideNavItem accounts = new SideNavItem("Accounts", AccountView.class, VaadinIcon.USER.create());

        // Tenants
        SideNavItem tenants = new SideNavItem("Tenants", TenantView.class, VaadinIcon.BUILDING.create());

        // Applications
        SideNavItem applications = new SideNavItem("Applications", ApplicationView.class, VaadinIcon.PAPERCLIP.create());

        // Customers
        SideNavItem customers = new SideNavItem("Customers", CustomerView.class, VaadinIcon.GROUP.create());

        // Roles & Permissions
        SideNavItem roles = new SideNavItem("Roles", RoleView.class, VaadinIcon.SHIELD.create());

        // Annexes
        SideNavItem annexes = new SideNavItem("Annexes", AnnexView.class, VaadinIcon.FOLDER_OPEN.create());

        nav.addItem(dashboard, accounts, tenants, applications, customers, roles, annexes);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}