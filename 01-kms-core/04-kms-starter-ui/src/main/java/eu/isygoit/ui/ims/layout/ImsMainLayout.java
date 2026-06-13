package eu.isygoit.ui.ims.layout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.ui.ImsMainView;
import eu.isygoit.ui.common.layout.BaseMainLayout;
import eu.isygoit.ui.ims.views.AnnexView;
import eu.isygoit.ui.ims.views.ApplicationView;
import eu.isygoit.ui.ims.views.CustomerView;
import eu.isygoit.ui.ims.views.RoleView;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import org.springframework.stereotype.Component;

@Component
public class ImsMainLayout extends BaseMainLayout {

    @Override
    protected String getTitle() {
        return "Identity Management System";
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem("Dashboard", ImsMainView.class, VaadinIcon.HOME.create());
        SideNavItem accounts = new SideNavItem("Accounts", AccountManagementView.class, VaadinIcon.USER.create());
        SideNavItem tenants = new SideNavItem("Tenants", TenantManagementView.class, VaadinIcon.BUILDING.create());
        SideNavItem applications = new SideNavItem("Applications", ApplicationView.class, VaadinIcon.PAPERCLIP.create());
        SideNavItem customers = new SideNavItem("Customers", CustomerView.class, VaadinIcon.GROUP.create());
        SideNavItem roles = new SideNavItem("Roles", RoleView.class, VaadinIcon.SHIELD.create());
        SideNavItem annexes = new SideNavItem("Annexes", AnnexView.class, VaadinIcon.FOLDER_OPEN.create());

        nav.addItem(dashboard, accounts, tenants, applications, customers, roles, annexes);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}