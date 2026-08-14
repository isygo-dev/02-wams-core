package eu.isygoit.ui.sms.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
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
    protected Component createDrawerContent() {
        SideNav nav = new SideNav();
        nav.addItem(navItem(I18n.t("sms.nav.dashboard"), "sms", VaadinIcon.HOME));
        nav.addItem(navItem(I18n.t("sms.nav.storage.configs"), "sms/storageconfigs", VaadinIcon.DATABASE));
        nav.addItem(navItem(I18n.t("sms.nav.buckets"), "sms/buckets", VaadinIcon.FOLDER));
        nav.addItem(navItem(I18n.t("sms.nav.object.storage"), "sms/objectstorage", VaadinIcon.CLOUD));

        return new Scroller(nav);
    }
}