package eu.isygoit.ui.mms.layout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;

public class MmsMainLayout extends BaseMainLayout {

    @Override
    protected String getTitle() {
        return I18n.t("mms.layout.title");
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        // Dashboard
        SideNavItem dashboard = new SideNavItem(I18n.t("mms.nav.dashboard"), "mms/home", VaadinIcon.HOME.create());

        // Sender Configuration
        SideNavItem senderConfig = new SideNavItem(I18n.t("mms.nav.sender.config"));
        senderConfig.setPrefixComponent(VaadinIcon.MAILBOX.create());
        senderConfig.addItem(new SideNavItem(I18n.t("mms.nav.sender.config"), "mms/sender-config", VaadinIcon.MAILBOX.create()));

        // Message Templates
        SideNavItem templates = new SideNavItem(I18n.t("mms.nav.templates"));
        templates.setPrefixComponent(VaadinIcon.FILE_TEXT.create());
        templates.addItem(new SideNavItem(I18n.t("mms.nav.templates"), "mms/templates", VaadinIcon.FILE_TEXT.create()));
        templates.addItem(new SideNavItem(I18n.t("mms.nav.template.names"), "mms/template-names", VaadinIcon.LIST.create()));

        // Email Operations
        SideNavItem emailOps = new SideNavItem(I18n.t("mms.nav.email.operations"));
        emailOps.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailOps.addItem(new SideNavItem(I18n.t("mms.nav.send.email"), "mms/send-email", VaadinIcon.START_COG.create()));
        emailOps.addItem(new SideNavItem(I18n.t("mms.nav.email.logs"), "mms/email-logs", VaadinIcon.FILE_TEXT.create()));

        nav.addItem(dashboard, senderConfig, templates, emailOps);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}