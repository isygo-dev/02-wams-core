package eu.isygoit.ui.kms.layout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import eu.isygoit.ui.common.layout.BaseMainLayout;
import eu.isygoit.ui.kms.views.cryptography.byok.ByokView;
import eu.isygoit.ui.kms.views.cryptography.crypto.CryptoOperationsView;
import eu.isygoit.ui.kms.views.cryptography.incremental.IncrementalKeyView;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.AliasesView;
import eu.isygoit.ui.kms.views.cryptography.keyGrants.GrantsView;
import eu.isygoit.ui.kms.views.cryptography.keyPolicy.PoliciesView;
import eu.isygoit.ui.kms.views.cryptography.keyStore.CustomKeyStoresView;
import eu.isygoit.ui.kms.views.cryptography.keyTag.TagsView;
import eu.isygoit.ui.kms.views.cryptography.random.RandomKeyView;
import eu.isygoit.ui.kms.views.secrets.digest.DigestConfigView;
import eu.isygoit.ui.kms.views.secrets.password.PasswordConfigView;
import eu.isygoit.ui.kms.views.secrets.peb.PEBConfigView;
import eu.isygoit.ui.kms.views.tokenizer.builder.TokenBuilderView;
import eu.isygoit.ui.kms.views.tokenizer.config.TokenConfigView;
import org.springframework.stereotype.Component;

@Component
public class KmsMainLayout extends BaseMainLayout {

    @Override
    protected String getTitle() {
        return "KMS Console";
    }

    @Override
    protected void createDrawer() {
        SideNav nav = new SideNav();

        // Dashboard
        SideNavItem dashboard = new SideNavItem("Dashboard", "home", VaadinIcon.HOME.create());

        // Key Management
        SideNavItem keyMgmt = new SideNavItem("Key Management");
        keyMgmt.setPrefixComponent(VaadinIcon.KEY.create());
        keyMgmt.addItem(new SideNavItem("Keys", KeyManagementView.class, VaadinIcon.KEY.create()));
        keyMgmt.addItem(new SideNavItem("Aliases", AliasesView.class, VaadinIcon.LINK.create()));
        keyMgmt.addItem(new SideNavItem("Tags", TagsView.class, VaadinIcon.TAGS.create()));
        keyMgmt.addItem(new SideNavItem("Policies", PoliciesView.class, VaadinIcon.DIPLOMA.create()));
        keyMgmt.addItem(new SideNavItem("Grants", GrantsView.class, VaadinIcon.SHIELD.create()));
        keyMgmt.addItem(new SideNavItem("Custom Key Stores", CustomKeyStoresView.class, VaadinIcon.DATABASE.create()));
        keyMgmt.addItem(new SideNavItem("BYOK", ByokView.class, VaadinIcon.DOWNLOAD_ALT.create()));

        // Crypto Operations
        SideNavItem cryptoOps = new SideNavItem("Crypto Operations");
        cryptoOps.setPrefixComponent(VaadinIcon.LOCK.create());
        cryptoOps.addItem(new SideNavItem("E/D & S/V", CryptoOperationsView.class, VaadinIcon.LOCK.create()));

        // Key Value Generators
        SideNavItem valueGen = new SideNavItem("Key Value Generators");
        valueGen.setPrefixComponent(VaadinIcon.HASH.create());
        valueGen.addItem(new SideNavItem("Random Keys", RandomKeyView.class, VaadinIcon.RANDOM.create()));
        valueGen.addItem(new SideNavItem("Incremental Key", IncrementalKeyView.class, VaadinIcon.CLOCK.create()));

        // Password Configurations
        SideNavItem passwordMenu = new SideNavItem("Password");
        passwordMenu.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordMenu.addItem(new SideNavItem("PEB Config", PEBConfigView.class, VaadinIcon.COG.create()));
        passwordMenu.addItem(new SideNavItem("Digest Config", DigestConfigView.class, VaadinIcon.DIPLOMA.create()));
        passwordMenu.addItem(new SideNavItem("Password Config", PasswordConfigView.class, VaadinIcon.USER.create()));

        // Tokenizer
        SideNavItem tokenizer = new SideNavItem("Tokenizer");
        tokenizer.setPrefixComponent(VaadinIcon.CODE.create());
        tokenizer.addItem(new SideNavItem("Token Configurations", TokenConfigView.class, VaadinIcon.TABLE.create()));
        tokenizer.addItem(new SideNavItem("Token Builder", TokenBuilderView.class, VaadinIcon.COG.create()));

        nav.addItem(dashboard, keyMgmt, cryptoOps, valueGen, passwordMenu, tokenizer);

        Scroller scroller = new Scroller(nav);
        VerticalLayout layout = new VerticalLayout(scroller);
        layout.setSizeFull();
        addToDrawer(layout);
    }
}