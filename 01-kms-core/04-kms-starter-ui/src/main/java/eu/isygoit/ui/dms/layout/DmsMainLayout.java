package eu.isygoit.ui.dms.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

@CssImport("./styles/dms.css")
public class DmsMainLayout extends BaseMainLayout {

    @Override
    protected String getModuleKey() {
        return "dms";
    }

    @Override
    protected String getTitle() {
        return I18n.t("dms.layout.title");
    }

    @Override
    protected Component createDrawerContent() {
        SideNav nav = new SideNav();
        nav.addItem(navItem(I18n.t("dms.nav.dashboard"), "dms", VaadinIcon.HOME));
        // Categories is the only DMS entity implemented today; linked-file and
        // tag browsing have no backing view/route yet, so they're intentionally
        // left off the sidebar rather than pointing at a page that 404s.
        nav.addItem(navItem(I18n.t("dms.nav.categories"), "dms/categories", VaadinIcon.FOLDER));

        return new Scroller(nav);
    }
}
