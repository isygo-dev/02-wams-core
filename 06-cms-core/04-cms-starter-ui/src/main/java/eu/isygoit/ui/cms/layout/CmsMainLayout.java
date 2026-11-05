package eu.isygoit.ui.cms.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

@CssImport("./styles/cms.scss")
public class CmsMainLayout extends BaseMainLayout {

    @Override
    protected String getModuleKey() {
        return "cms";
    }

    @Override
    protected String getTitle() {
        return I18n.t("cms.layout.title");
    }

    @Override
    protected Component createDrawerContent() {
        SideNav nav = new SideNav();
        nav.addItem(navItem(I18n.t("cms.nav.dashboard"), "cms", VaadinIcon.HOME));
        nav.addItem(navItem(I18n.t("cms.nav.calendars"), "cms/calendars", VaadinIcon.CALENDAR));
        nav.addItem(navItem(I18n.t("cms.nav.calendar.view"), "cms/calendar", VaadinIcon.CALENDAR_CLOCK));

        return new Scroller(nav);
    }
}
