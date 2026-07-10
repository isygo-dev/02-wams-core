package eu.isygoit.ui.common.layout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.component.AppSearchBar;
import eu.isygoit.ui.common.component.LanguageSelectorComponent;
import eu.isygoit.ui.common.spring.SpringContextUtil;
import eu.isygoit.util.SecurityUtils;
import feign.FeignException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * All shared CSS for the application is imported once here, since every view
 * sits inside a subclass of this layout (including {@code LandingView}).
 *
 * <p>Header: brand (click → landing), per-module title, quick-navigation
 * search, notifications, settings, language selector, and the user's profile
 * menu. Sidebar: whatever {@link #createDrawerContent()} returns, plus a
 * persistent icon-rail collapse toggle pinned at the bottom (state kept in
 * the {@link VaadinSession} so it survives navigation between modules).
 */
@CssImport("./styles/tokens.css")
@CssImport("./styles/card.css")
@CssImport("./styles/dialog.css")
@CssImport("./styles/layout.css")
@CssImport("./styles/modules.css")
@CssImport("./styles/landing.css")
public abstract class BaseMainLayout extends AppLayout implements BeforeEnterObserver {

    private static final String SIDEBAR_COLLAPSED_SESSION_KEY = "wams-sidebar-collapsed";

    private final AccountService accountService;
    private final AccountImageService accountImageService;
    private Div rightSlot; // container for search/notifications/settings/language/profile
    private Button sidebarCollapseToggle;
    private boolean sidebarCollapsed;

    public BaseMainLayout() {
        // Retrieve services statically (layout is not a Spring bean)
        this.accountService = SpringContextUtil.getBean(AccountService.class);
        this.accountImageService = SpringContextUtil.getBean(AccountImageService.class);

        if (!getModuleKey().isBlank()) {
            addClassName("wams-module-" + getModuleKey());
        }

        this.sidebarCollapsed = isSidebarCollapsedInSession();
        if (sidebarCollapsed) {
            addClassName("wams-sidebar-collapsed");
        }

        createHeader();
        createDrawer();
    }

    protected abstract String getTitle();

    /**
     * Builds this module's navigation content (typically one or more
     * {@code SideNav} instances wrapped in a {@code Scroller}). The base
     * layout wraps the result together with the sidebar's icon-rail collapse
     * toggle, so subclasses should NOT call {@code addToDrawer(...)}
     * themselves. Return {@code null} for layouts with no sidebar at all
     * (e.g. {@code LandingView}) — no drawer content or collapse toggle is
     * added in that case.
     */
    protected abstract Component createDrawerContent();

    protected String getModuleKey() {
        return "";
    }

    /**
     * Builds a leaf navigation item with its label also set as a Vaadin
     * tooltip, so it stays readable on hover when the sidebar is collapsed
     * to its icon-only rail (see {@code .wams-sidebar-collapsed} in layout.css).
     *
     * <p>The icon is given an explicit pixel size (not em-based) so it stays
     * fully visible even when collapsed mode zeroes out the link's font-size
     * to hide the label — {@code SideNavItem}'s label is rendered as a plain
     * text node in its default slot, with no wrapping element to hide via a
     * CSS class, so shrinking the inherited font-size to 0 is the reliable
     * way to make it disappear without affecting the icon.
     */
    protected static SideNavItem navItem(String label, String route, VaadinIcon icon) {
        Icon iconComponent = icon.create();
        iconComponent.setSize("20px");
        SideNavItem item = new SideNavItem(label, route, iconComponent);
        item.setTooltipText(label);
        return item;
    }

    private void createHeader() {
        HorizontalLayout brand = new HorizontalLayout();
        brand.addClassName("wams-brand");
        brand.setAlignItems(FlexComponent.Alignment.CENTER);
        brand.setSpacing(false);
        Icon brandIcon = VaadinIcon.CUBES.create();
        brandIcon.addClassName("wams-brand__icon");
        Span brandName = new Span(I18n.t("common.layout.header.brand"));
        brandName.addClassName("wams-brand__name");
        brand.add(brandIcon, brandName);
        Tooltip.forComponent(brand).setText(I18n.t("common.layout.header.landing.tooltip"));
        brand.addClickListener(e -> UI.getCurrent().navigate("landing"));

        Span moduleTitle = new Span(getTitle());
        moduleTitle.addClassName("wams-header-title");

        DrawerToggle drawerToggle = new DrawerToggle();
        String menuToggleTooltip = I18n.t("common.layout.header.menu.toggle.tooltip");
        drawerToggle.setAriaLabel(menuToggleTooltip);
        Tooltip.forComponent(drawerToggle).setText(menuToggleTooltip);

        HorizontalLayout leftPart = new HorizontalLayout(drawerToggle, brand, moduleTitle);
        leftPart.setAlignItems(FlexComponent.Alignment.CENTER);
        leftPart.setSpacing(true);
        leftPart.addClassName("wams-main-header__left");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(false);
        headerLayout.setSpacing(true);
        headerLayout.addClassName("wams-main-header");

        // Right slot – will be filled in onAttach
        rightSlot = new Div();
        rightSlot.addClassName("wams-main-header__right");
        rightSlot.setText(I18n.t("common.layout.loading"));

        headerLayout.add(leftPart, rightSlot);
        headerLayout.expand(leftPart);

        addToNavbar(headerLayout);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Replace placeholder content with real components (search, notifications,
        // settings, language selector, profile)
        rightSlot.removeAll();
        rightSlot.add(createRightHeaderContent());
    }

    /**
     * Two independently-styleable groups rather than one flat row of 5
     * disparate-width children: a search "slot" (whose width layout.css
     * controls per breakpoint) and a fixed-size "icon cluster" (notifications,
     * settings, language, profile — always kept together, never wrapped
     * internally). This gives {@code .wams-app-search} a stable container to
     * shrink/grow within at each breakpoint instead of fighting flex-wrap
     * order across 5 mixed-size siblings.
     */
    private Component createRightHeaderContent() {
        HorizontalLayout rightContent = new HorizontalLayout();
        rightContent.addClassName("wams-header-right-content");
        rightContent.setAlignItems(FlexComponent.Alignment.CENTER);
        rightContent.setSpacing(true);
        rightContent.setPadding(false);

        Div searchSlot = new Div(new AppSearchBar());
        searchSlot.addClassName("wams-header-search-slot");

        HorizontalLayout iconCluster = new HorizontalLayout();
        iconCluster.addClassName("wams-header-icon-cluster");
        iconCluster.setAlignItems(FlexComponent.Alignment.CENTER);
        iconCluster.setSpacing(true);
        iconCluster.setPadding(false);
        iconCluster.add(createNotificationsButton(), createSettingsButton(),
                new LanguageSelectorComponent(), createProfileComponent());

        rightContent.add(searchSlot, iconCluster);
        return rightContent;
    }

    private Component createNotificationsButton() {
        Button bell = new Button(VaadinIcon.BELL.create());
        bell.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        bell.addClassName("wams-header-icon-btn");
        String tooltip = I18n.t("common.layout.header.notifications.tooltip");
        bell.setTooltipText(tooltip);
        // setTooltipText only wires aria-describedby (a description) — icon-only
        // buttons still need an explicit accessible *name* for screen readers.
        bell.setAriaLabel(tooltip);

        Popover popover = new Popover();
        popover.setTarget(bell);
        popover.addClassName("wams-header-popover");
        Span empty = new Span(I18n.t("common.layout.header.notifications.empty"));
        empty.addClassName(LumoUtility.TextColor.SECONDARY);
        empty.addClassName(LumoUtility.FontSize.SMALL);
        popover.add(empty);

        return bell;
    }

    private Component createSettingsButton() {
        Button settings = new Button(VaadinIcon.COG_O.create());
        settings.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        settings.addClassName("wams-header-icon-btn");
        String tooltip = I18n.t("common.layout.header.settings.tooltip");
        settings.setTooltipText(tooltip);
        settings.setAriaLabel(tooltip);
        settings.addClickListener(e -> UI.getCurrent().navigate("settings"));
        return settings;
    }

    private Component createProfileComponent() {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        menuBar.setOpenOnHover(true);

        AccountDto currentAccount = getCurrentAccount();

        Avatar avatar = new Avatar();
        avatar.setThemeName("small");
        avatar.addClassName("wams-avatar-bordered");

        if (currentAccount != null) {
            String fullName = currentAccount.getFullName();
            if (fullName != null && !fullName.isBlank()) {
                avatar.setName(fullName);
            } else {
                avatar.setName(currentAccount.getEmail());
            }
            loadProfileImage(avatar, currentAccount.getId());
        } else {
            avatar.setName(I18n.t("common.layout.avatar.user.default.name"));
        }

        MenuItem menuItem = menuBar.addItem(avatar);
        menuItem.getSubMenu().addItem(I18n.t("common.layout.avatar.profile"), e -> UI.getCurrent().navigate("profile"));
        menuItem.getSubMenu().addItem(I18n.t("common.layout.avatar.settings"), e -> UI.getCurrent().navigate("settings"));
        menuItem.getSubMenu().addItem(I18n.t("common.layout.avatar.logout"), e -> logout());

        VerticalLayout profileContainer = new VerticalLayout(menuBar);
        profileContainer.setPadding(false);
        profileContainer.setSpacing(false);
        profileContainer.setAlignItems(FlexComponent.Alignment.END);
        return profileContainer;
    }

    private void createDrawer() {
        Component content = createDrawerContent();
        if (content == null) {
            return;
        }

        Div scrollArea = new Div(content);
        scrollArea.addClassName("wams-drawer-scroll-area");

        Div wrapper = new Div(scrollArea, createSidebarCollapseToggle());
        wrapper.addClassName("wams-drawer-wrapper");
        wrapper.setSizeFull();

        addToDrawer(wrapper);
    }

    /**
     * Persistent affordance pinned at the bottom of every module's sidebar
     * that collapses it to an icon-only rail (~60px) or expands it back
     * (~220px) — see {@code .wams-sidebar-collapsed} in layout.css. The
     * choice is stored in the {@link VaadinSession} (not just this layout
     * instance) so it survives navigating between modules, each of which
     * instantiates a fresh {@code <Module>MainLayout}.
     */
    private Component createSidebarCollapseToggle() {
        sidebarCollapseToggle = new Button(collapseToggleIcon());
        sidebarCollapseToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        sidebarCollapseToggle.addClassName("wams-sidebar-collapse-toggle");
        sidebarCollapseToggle.setTooltipText(collapseToggleTooltip());
        sidebarCollapseToggle.addClickListener(e -> toggleSidebarCollapsed());
        return sidebarCollapseToggle;
    }

    private void toggleSidebarCollapsed() {
        sidebarCollapsed = !sidebarCollapsed;
        getElement().getClassList().set("wams-sidebar-collapsed", sidebarCollapsed);
        sidebarCollapseToggle.setIcon(collapseToggleIcon());
        sidebarCollapseToggle.setTooltipText(collapseToggleTooltip());
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().setAttribute(SIDEBAR_COLLAPSED_SESSION_KEY, sidebarCollapsed);
        }
    }

    private Icon collapseToggleIcon() {
        return (sidebarCollapsed ? VaadinIcon.CHEVRON_RIGHT_SMALL : VaadinIcon.CHEVRON_LEFT_SMALL).create();
    }

    private String collapseToggleTooltip() {
        return sidebarCollapsed
                ? I18n.t("common.layout.sidebar.expand.tooltip")
                : I18n.t("common.layout.sidebar.collapse.tooltip");
    }

    private boolean isSidebarCollapsedInSession() {
        if (VaadinSession.getCurrent() == null) {
            return false;
        }
        return Boolean.TRUE.equals(VaadinSession.getCurrent().getAttribute(SIDEBAR_COLLAPSED_SESSION_KEY));
    }

    private AccountDto getCurrentAccount() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof AccountDto) {
                    return (AccountDto) principal;
                }
            }
            if (VaadinSession.getCurrent() != null) {
                return VaadinSession.getCurrent().getAttribute(AccountDto.class);
            }
        } catch (Exception e) {
            // ignore
        }
        // For development without auth, return a fake account
        AccountDto fake = new AccountDto();
        fake.setId(1L);
        fake.setEmail("demo@isygoit.eu");
        fake.setFullName("Demo User");
        return fake;
    }

    private void loadProfileImage(Avatar avatar, Long accountId) {
        if (accountId == null) return;
        new Thread(() -> {
            try {
                ResponseEntity<Resource> response = accountImageService.downloadImage(accountId);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    byte[] imageBytes = response.getBody().getContentAsByteArray();
                    UI.getCurrent().access(() -> {
                        StreamResource resource = new StreamResource("profile_" + accountId + ".jpg",
                                () -> new ByteArrayInputStream(imageBytes));
                        avatar.setImageResource(resource);
                    });
                }
            } catch (FeignException ex) {
                if (ex.status() != 404) {
                    ex.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void logout() {
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().getSession().invalidate();
            VaadinSession.getCurrent().close();
        }
        SecurityContextHolder.clearContext();
        UI.getCurrent().getPage().setLocation("login");
    }

    @Override
    public final void beforeEnter(BeforeEnterEvent event) {
        String currentPath = event.getLocation().getPathWithQueryParameters();
        if (!SecurityUtils.isUserLoggedIn()) {
            UI.getCurrent().getPage().setLocation("login?redirect=" + URLEncoder.encode(currentPath, StandardCharsets.UTF_8));
        } else {
            SecurityUtils.storeRedirect(currentPath);
        }
    }

    protected static class DrawerToggle extends com.vaadin.flow.component.applayout.DrawerToggle {
        public DrawerToggle() {
            super();
        }
    }
}
