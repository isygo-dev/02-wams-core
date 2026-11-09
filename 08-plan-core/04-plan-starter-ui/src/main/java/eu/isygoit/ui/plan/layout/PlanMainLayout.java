package eu.isygoit.ui.plan.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

/**
 * Planning module shell: header, drawer navigation (dashboard, jobs, logs) and the {@code plan} module accent.
 */
@CssImport("./styles/plan.scss")
public class PlanMainLayout extends BaseMainLayout {

    @Override
    protected String getModuleKey() {
        return "plan";
    }

    @Override
    protected String getTitle() {
        return I18n.t("plan.ui.layout.title");
    }

    @Override
    protected Component createDrawerContent() {
        SideNav nav = new SideNav();
        nav.addItem(navItem(I18n.t("plan.ui.nav.dashboard"), "plan", VaadinIcon.HOME));
        nav.addItem(navItem(I18n.t("plan.ui.nav.jobs"), "plan/jobs", VaadinIcon.LIST));
        nav.addItem(navItem(I18n.t("plan.ui.nav.logs"), "plan/logs", VaadinIcon.LIST));
        return new Scroller(nav);
    }
}
