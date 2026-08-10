package eu.isygoit.ui.ims.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.CollapsibleNavSection;
import eu.isygoit.ui.common.layout.BaseMainLayout;

import java.util.List;

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
        CollapsibleNavSection directory = new CollapsibleNavSection(
                I18n.t("ims.nav.section.directory"),
                navItem(I18n.t("ims.nav.accounts"), "ims/accounts", VaadinIcon.USER),
                navItem(I18n.t("ims.nav.customers"), "ims/customers", VaadinIcon.GROUP),
                navItem(I18n.t("ims.nav.tenants"), "ims/tenants", VaadinIcon.BUILDING),
                navItem(I18n.t("ims.nav.registeredUsers"), "ims/registered-users", VaadinIcon.CLIPBOARD_USER)
        );

        // Access control: what can reach the system, and with which permissions.
        CollapsibleNavSection access = new CollapsibleNavSection(
                I18n.t("ims.nav.section.access"),
                navItem(I18n.t("ims.nav.roles"), "ims/roles", VaadinIcon.SHIELD),
                navItem(I18n.t("ims.nav.applications"), "ims/applications", VaadinIcon.PAPERCLIP)
        );

        // System-level configuration/extensibility records.
        CollapsibleNavSection configuration = new CollapsibleNavSection(
                I18n.t("ims.nav.section.configuration"),
                navItem(I18n.t("ims.nav.parameters"), "ims/parameters", VaadinIcon.KEYBOARD),
                navItem(I18n.t("ims.nav.annexes"), "ims/annexes", VaadinIcon.FOLDER_OPEN)
        );

        // Accordion: expanding one section collapses the others.
        List<CollapsibleNavSection> sections = List.of(directory, access, configuration);
        sections.forEach(section -> section.setOtherSections(sections));
        directory.expand();

        VerticalLayout layout = new VerticalLayout(overview, directory, access, configuration);
        layout.setPadding(false);
        layout.setSpacing(false);
        return new Scroller(layout);
    }
}
