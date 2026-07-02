package eu.isygoit.ui.cms.layout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

public class CmsMainLayout extends BaseMainLayout {

    @Override
    protected String getTitle() {
        return I18n.t("cms.layout.title");
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem(I18n.t("cms.nav.dashboard"), "cms", VaadinIcon.HOME.create());
        SideNavItem calendars = new SideNavItem(I18n.t("cms.nav.calendars"), "cms/calendars", VaadinIcon.CALENDAR.create());
        SideNavItem events = new SideNavItem(I18n.t("cms.nav.events"), "cms/events", VaadinIcon.CALENDAR_CLOCK.create());
        SideNavItem schedules = new SideNavItem(I18n.t("cms.nav.schedules"), "cms/schedules", VaadinIcon.CLOCK.create());

        nav.addItem(dashboard, calendars, events, schedules);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}