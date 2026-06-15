package eu.isygoit.ui.ims.layout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.ui.common.layout.BaseMainLayout;

public class ImsMainLayout extends BaseMainLayout {

    @Override
    protected String getTitle() {
        return "Identity Management System";
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem("Dashboard", "ims", VaadinIcon.HOME.create());
        SideNavItem accounts = new SideNavItem("Accounts", "ims/accounts", VaadinIcon.USER.create());
        SideNavItem tenants = new SideNavItem("Tenants", "ims/tenants", VaadinIcon.BUILDING.create());
        SideNavItem applications = new SideNavItem("Applications", "ims/applications", VaadinIcon.PAPERCLIP.create());

        SideNavItem roles = new SideNavItem("Roles", "ims/roles", VaadinIcon.SHIELD.create());

        SideNavItem annexes = new SideNavItem("Annexes", "ims/annexes", VaadinIcon.FOLDER_OPEN.create());
        SideNavItem parameters = new SideNavItem("Parameters", "ims/parameters", VaadinIcon.KEYBOARD.create());

        SideNavItem customers = new SideNavItem("Customers", "ims/customers", VaadinIcon.GROUP.create());

        nav.addItem(dashboard, accounts, tenants, applications, roles, annexes, parameters, customers);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}