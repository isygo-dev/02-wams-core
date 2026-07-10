package eu.isygoit.ui.cms.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

@CssImport("./styles/cms.css")
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
        // Calendars is the only CMS entity implemented today; standalone event
        // and schedule browsing have no backing view/route yet, so they're
        // intentionally left off the sidebar rather than pointing at a page
        // that 404s.
        nav.addItem(navItem(I18n.t("cms.nav.calendars"), "cms/calendars", VaadinIcon.CALENDAR));

        return new Scroller(nav);
    }
}
