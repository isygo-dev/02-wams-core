package eu.isygoit.ui.dms.layout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

public class DmsMainLayout extends BaseMainLayout {

    @Override
    protected String getTitle() {
        return I18n.t("dms.layout.title");
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem(I18n.t("dms.nav.dashboard"), "dms", VaadinIcon.HOME.create());
        SideNavItem categories = new SideNavItem(I18n.t("dms.nav.categories"), "dms/categories", VaadinIcon.FOLDER.create());
        SideNavItem linkedFiles = new SideNavItem(I18n.t("dms.nav.linkedfiles"), "dms/linkedfiles", VaadinIcon.FILE.create());
        SideNavItem tags = new SideNavItem(I18n.t("dms.nav.tags"), "dms/tags", VaadinIcon.TAGS.create());

        nav.addItem(dashboard, categories, linkedFiles, tags);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}