package eu.isygoit.ui.mms.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

@CssImport("./styles/mms.css")
public class MmsMainLayout extends BaseMainLayout {

    @Override
    protected String getModuleKey() {
        return "mms";
    }

    @Override
    protected String getTitle() {
        return I18n.t("mms.layout.title");
    }

    @Override
    protected Component createDrawerContent() {
        SideNav nav = new SideNav();
        nav.addItem(navItem(I18n.t("mms.nav.dashboard"), "mms/home", VaadinIcon.HOME));
        // Sender Configuration / Message Templates used to be single-child
        // "groups" (an extra expand/collapse click for one item each) — now
        // flat, direct links. "Email Operations" (send-email/email-logs) had
        // no backing view/route, so it's intentionally left off the sidebar
        // rather than pointing at a page that 404s.
        nav.addItem(navItem(I18n.t("mms.nav.sender.config"), "mms/sender-config", VaadinIcon.MAILBOX));
        nav.addItem(navItem(I18n.t("mms.nav.templates"), "mms/templates", VaadinIcon.FILE_TEXT));

        return new Scroller(nav);
    }
}
