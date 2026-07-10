package eu.isygoit.ui.ims.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
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
    protected Component createDrawerContent() {
        SideNav overview = new SideNav();
        overview.addItem(navItem(I18n.t("ims.nav.dashboard"), "ims", VaadinIcon.HOME));

        // "Who's who": people/organization records.
        SideNav directory = new SideNav(I18n.t("ims.nav.section.directory"));
        directory.addItem(navItem(I18n.t("ims.nav.accounts"), "ims/accounts", VaadinIcon.USER));
        directory.addItem(navItem(I18n.t("ims.nav.customers"), "ims/customers", VaadinIcon.GROUP));
        directory.addItem(navItem(I18n.t("ims.nav.tenants"), "ims/tenants", VaadinIcon.BUILDING));

        // Access control: what can reach the system, and with which permissions.
        SideNav access = new SideNav(I18n.t("ims.nav.section.access"));
        access.addItem(navItem(I18n.t("ims.nav.roles"), "ims/roles", VaadinIcon.SHIELD));
        access.addItem(navItem(I18n.t("ims.nav.applications"), "ims/applications", VaadinIcon.PAPERCLIP));

        // System-level configuration/extensibility records.
        SideNav configuration = new SideNav(I18n.t("ims.nav.section.configuration"));
        configuration.addItem(navItem(I18n.t("ims.nav.parameters"), "ims/parameters", VaadinIcon.KEYBOARD));
        configuration.addItem(navItem(I18n.t("ims.nav.annexes"), "ims/annexes", VaadinIcon.FOLDER_OPEN));

        VerticalLayout layout = new VerticalLayout(overview, directory, access, configuration);
        layout.setPadding(false);
        layout.setSpacing(false);
        return new Scroller(layout);
    }
}
