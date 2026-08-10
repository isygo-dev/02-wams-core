package eu.isygoit.ui.kms.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.CollapsibleNavSection;
import eu.isygoit.ui.common.layout.BaseMainLayout;

import java.util.List;

@CssImport("./styles/kms.css")
public class KmsMainLayout extends BaseMainLayout {

    @Override
    protected String getModuleKey() {
        return "kms";
    }

    @Override
    protected String getTitle() {
        return I18n.t("kms.layout.title");
    }

    @Override
    protected Component createDrawerContent() {
        SideNav overview = new SideNav();
        overview.addItem(navItem(I18n.t("kms.nav.dashboard"), "kms/home", VaadinIcon.HOME));

        CollapsibleNavSection keyManagement = new CollapsibleNavSection(
                I18n.t("kms.nav.key.management"),
                navItem(I18n.t("kms.nav.keys"), "kms/keys", VaadinIcon.KEY),
                navItem(I18n.t("kms.nav.aliases"), "kms/aliases", VaadinIcon.LINK),
                navItem(I18n.t("kms.nav.tags"), "kms/tags", VaadinIcon.TAGS),
                navItem(I18n.t("kms.nav.policies"), "kms/policies", VaadinIcon.DIPLOMA),
                navItem(I18n.t("kms.nav.grants"), "kms/grants", VaadinIcon.SHIELD),
                navItem(I18n.t("kms.nav.custom.key.stores"), "kms/custom-key-stores", VaadinIcon.DATABASE),
                navItem(I18n.t("kms.nav.byok"), "kms/byok", VaadinIcon.DOWNLOAD_ALT),
                navItem(I18n.t("kms.nav.crypto.operations"), "kms/crypto", VaadinIcon.LOCK)
        );

        CollapsibleNavSection generators = new CollapsibleNavSection(
                I18n.t("kms.nav.key.value.generators"),
                navItem(I18n.t("kms.nav.random.keys"), "kms/random-keys", VaadinIcon.RANDOM),
                navItem(I18n.t("kms.nav.incremental.key"), "kms/incremental-key", VaadinIcon.CLOCK)
        );

        CollapsibleNavSection secrets = new CollapsibleNavSection(
                I18n.t("kms.nav.secrets"),
                navItem(I18n.t("kms.nav.peb.config"), "kms/peb-configs", VaadinIcon.COG),
                navItem(I18n.t("kms.nav.digest.config"), "kms/digest-configs", VaadinIcon.HASH),
                navItem(I18n.t("kms.nav.password.config"), "kms/password-configs", VaadinIcon.ASTERISK)
        );

        CollapsibleNavSection tokenizer = new CollapsibleNavSection(
                I18n.t("kms.nav.tokenizer"),
                navItem(I18n.t("kms.nav.token.configurations"), "kms/token-configs", VaadinIcon.TABLE),
                navItem(I18n.t("kms.nav.token.builder"), "kms/token-builder", VaadinIcon.COG)
        );

        // Accordion: expanding one section collapses the others.
        List<CollapsibleNavSection> sections = List.of(keyManagement, generators, secrets, tokenizer);
        sections.forEach(section -> section.setOtherSections(sections));
        keyManagement.expand();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setSizeUndefined();
        layout.setWidthFull();
        layout.add(overview, keyManagement, generators, secrets, tokenizer);

        Scroller scroller = new Scroller(layout);
        scroller.setSizeFull();
        return scroller;
    }
}
