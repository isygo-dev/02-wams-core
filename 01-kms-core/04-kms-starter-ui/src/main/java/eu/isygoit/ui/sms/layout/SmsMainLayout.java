package eu.isygoit.ui.sms.layout;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

@CssImport("./styles/sms.css")
public class SmsMainLayout extends BaseMainLayout {

    @Override
    protected String getModuleKey() {
        return "sms";
    }

    @Override
    protected String getTitle() {
        return I18n.t("sms.layout.title");
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem(I18n.t("sms.nav.dashboard"), "sms", VaadinIcon.HOME.create());
        SideNavItem storageConfigs = new SideNavItem(I18n.t("sms.nav.storage.configs"), "sms/storageconfigs", VaadinIcon.DATABASE.create());
        SideNavItem buckets = new SideNavItem(I18n.t("sms.nav.buckets"), "sms/buckets", VaadinIcon.FOLDER_O.create());
        SideNavItem storageStats = new SideNavItem(I18n.t("sms.nav.storage.stats"), "sms/stats", VaadinIcon.CHART.create());

        nav.addItem(dashboard, storageConfigs, buckets, storageStats);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}