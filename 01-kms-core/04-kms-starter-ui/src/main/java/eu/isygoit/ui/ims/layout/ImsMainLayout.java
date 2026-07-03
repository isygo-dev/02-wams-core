package eu.isygoit.ui.ims.layout;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

@CssImport("./styles/ims.css")
public class ImsMainLayout extends BaseMainLayout {

    @Override
    protected String getModuleKey() {
        return "ims";
    }

    @Override
    protected String getTitle() {
        return I18n.t("ims.layout.title");
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem(I18n.t("ims.nav.dashboard"), "ims", VaadinIcon.HOME.create());
        SideNavItem accounts = new SideNavItem(I18n.t("ims.nav.accounts"), "ims/accounts", VaadinIcon.USER.create());
        SideNavItem tenants = new SideNavItem(I18n.t("ims.nav.tenants"), "ims/tenants", VaadinIcon.BUILDING.create());
        SideNavItem applications = new SideNavItem(I18n.t("ims.nav.applications"), "ims/applications", VaadinIcon.PAPERCLIP.create());

        SideNavItem roles = new SideNavItem(I18n.t("ims.nav.roles"), "ims/roles", VaadinIcon.SHIELD.create());

        SideNavItem annexes = new SideNavItem(I18n.t("ims.nav.annexes"), "ims/annexes", VaadinIcon.FOLDER_OPEN.create());
        SideNavItem parameters = new SideNavItem(I18n.t("ims.nav.parameters"), "ims/parameters", VaadinIcon.KEYBOARD.create());

        SideNavItem customers = new SideNavItem(I18n.t("ims.nav.customers"), "ims/customers", VaadinIcon.GROUP.create());

        nav.addItem(dashboard, accounts, tenants, applications, roles, annexes, parameters, customers);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}