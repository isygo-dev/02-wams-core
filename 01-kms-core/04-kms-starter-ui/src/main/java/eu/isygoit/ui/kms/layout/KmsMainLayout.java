package eu.isygoit.ui.kms.layout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinSession;
import eu.isygoit.ui.common.layout.BaseMainLayout;
import eu.isygoit.util.SecurityUtils;

import java.util.HashMap;
import java.util.Map;

public class KmsMainLayout extends BaseMainLayout {

    @Override
    protected String getTitle() {
        return "Key Management System";
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem("Dashboard", "kms", VaadinIcon.HOME.create());
        SideNavItem keys = new SideNavItem("Keys", "kms/keys", VaadinIcon.KEY.create());
        SideNavItem aliases = new SideNavItem("Aliases", "kms/aliases", VaadinIcon.TAG.create());
        SideNavItem crypto = new SideNavItem("Cryptographic Operations", "kms/crypto", VaadinIcon.COG.create());
        SideNavItem randomKeys = new SideNavItem("Random Keys", "kms/random-keys", VaadinIcon.RANDOM.create());
        SideNavItem incrementalKey = new SideNavItem("Incremental Key", "kms/incremental-key", VaadinIcon.CODE.create());
        SideNavItem tokenizer = new SideNavItem("Tokenizer", "kms/token-builder", VaadinIcon.FILE_CODE.create());
        SideNavItem tokenConfigs = new SideNavItem("Token Configs", "kms/token-configs", VaadinIcon.COG.create());
        SideNavItem byok = new SideNavItem("BYOK", "kms/byok", VaadinIcon.CLOUD.create());
        SideNavItem customKeyStores = new SideNavItem("Custom Key Stores", "kms/custom-key-stores", VaadinIcon.STORAGE.create());
        SideNavItem policies = new SideNavItem("Policies", "kms/policies", VaadinIcon.SHIELD.create());
        SideNavItem grants = new SideNavItem("Grants", "kms/grants", VaadinIcon.SHARE.create());
        SideNavItem tags = new SideNavItem("Tags", "kms/tags", VaadinIcon.TAGS.create());
        SideNavItem pebConfigs = new SideNavItem("PEB Configs", "kms/peb-configs", VaadinIcon.COG.create());
        SideNavItem passwordConfigs = new SideNavItem("Password Configs", "kms/password-configs", VaadinIcon.USER.create());
        SideNavItem digestConfigs = new SideNavItem("Digest Configs", "kms/digest-configs", VaadinIcon.DIPLOMA.create());

        nav.addItem(dashboard, keys, aliases, crypto, randomKeys, incrementalKey, tokenizer,
                tokenConfigs, byok, customKeyStores, policies, grants, tags,
                pebConfigs, passwordConfigs, digestConfigs);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}