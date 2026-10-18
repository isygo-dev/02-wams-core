package eu.isygoit.ui.common.layout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.shared.Registration;
import eu.isygoit.dto.response.UserDataResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.component.AppSearchBar;
import eu.isygoit.ui.common.component.CollapsibleNavSection;
import eu.isygoit.ui.common.component.LanguageSelectorComponent;
import eu.isygoit.ui.common.component.ThemeToggleButton;
import eu.isygoit.ui.common.nav.NavRegistryProvider;
import eu.isygoit.ui.common.spring.SpringContextUtil;
import eu.isygoit.util.SecurityUtils;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@CssImport("./styles/scss/common.scss")
@CssImport("./themes/isygo/styles.css")
public abstract class BaseMainLayout extends AppLayout implements BeforeEnterObserver {

    private static final String SIDEBAR_COLLAPSED_SESSION_KEY = "wams-sidebar-collapsed";
    /**
     * Above this viewport width the drawer is a docked sidebar; at or below it, an overlay (see _app-shell.scss).
     */
    private static final int DESKTOP_MIN_WIDTH = 800;

    private final NavRegistryProvider navRegistryProvider;
    private final AccountService accountService;
    private final AccountImageService accountImageService;
    private Div rightSlot; // container for search/notifications/settings/language/profile
    private Button sidebarCollapseToggle;
    private boolean sidebarCollapsed;
    private Div drawerRoot;
    private boolean desktopViewport = true;
    private Registration resizeRegistration;

    public BaseMainLayout() {
        // Retrieve services statically (layout is not a Spring bean)
        this.accountService = SpringContextUtil.getBean(AccountService.class);
        this.accountImageService = SpringContextUtil.getBean(AccountImageService.class);
        this.navRegistryProvider = SpringContextUtil.getBean(NavRegistryProvider.class);

        if (!getModuleKey().isBlank()) {
            addClassName("wams-module-" + getModuleKey());
        }

        // The drawer docks on the inline-start side: set the direction on the layout itself too, so the sidebar is
        // on the right for Arabic from the first render, whatever the document-level attribute timing.
        getElement().setAttribute("dir", PageEnvironment.currentDirection());

        this.sidebarCollapsed = isSidebarCollapsedInSession();
        if (sidebarCollapsed) {
            addClassName("wams-sidebar-collapsed");
        }

        createHeader();
        createDrawer();
    }

    /**
     * Builds a leaf navigation item with its label also set as a Vaadin
     * tooltip, so it stays readable on hover when the sidebar is collapsed
     * to its icon-only rail (see {@code .wams-sidebar-collapsed} in
     * {@code styles/scss/layout/_app-shell.scss}).
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

    private static Component menuEntry(VaadinIcon icon, String label) {
        Icon ic = icon.create();
        ic.addClassName("wams-menu-entry__icon");
        Span text = new Span(label);
        Div row = new Div(ic, text);
        row.addClassName("wams-menu-entry");
        return row;
    }

    private static String displayNameOf(UserDataResponseDto user) {
        if (user != null) {
            String full = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                    + (user.getLastName() != null ? user.getLastName() : "")).trim();
            if (!full.isEmpty()) {
                return full;
            }
            if (user.getUserName() != null && !user.getUserName().isBlank()) {
                return user.getUserName();
            }
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                return user.getEmail();
            }
        }
        Object sessionUser = SecurityUtils.getCurrentUser();
        return sessionUser != null ? sessionUser.toString() : I18n.t("common.layout.avatar.user.default.name");
    }

    private static void forEachNavSection(Component root, java.util.function.Consumer<CollapsibleNavSection> action) {
        if (root instanceof CollapsibleNavSection section) {
            action.accept(section);
        }
        root.getChildren().forEach(child -> forEachNavSection(child, action));
    }

    private static String avatarCacheKey(Long accountId) {
        return "wams-avatar-" + accountId;
    }

    /**
     * Drops the cached avatar so the next layout shows the new image (call after an upload or removal).
     */
    public static void invalidateAvatarCache(Long accountId) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null && accountId != null) {
            session.setAttribute(avatarCacheKey(accountId), null);
        }
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

    protected String getProfileRoute() {
        return "profile";
    }

    protected String getSettingsRoute() {
        return "settings";
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

        // Direction, language and light/dark theme, re-applied on every layout so they survive full page loads and
        // module switches (see PageEnvironment).
        UI ui = attachEvent.getUI();
        PageEnvironment.apply(ui);

        // Track whether the drawer is the desktop sidebar (rail allowed) or the mobile overlay.
        ui.getPage().executeJs("return window.innerWidth").then(Integer.class, width -> {
            desktopViewport = width != null && width > DESKTOP_MIN_WIDTH;
            applyRailMode();
        });
        resizeRegistration = ui.getPage().addBrowserWindowResizeListener(event -> {
            desktopViewport = event.getWidth() > DESKTOP_MIN_WIDTH;
            applyRailMode();
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (resizeRegistration != null) {
            resizeRegistration.remove();
            resizeRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    /**
     * Two independently-styleable groups rather than one flat row of 5
     * disparate-width children: a search "slot" (whose width is styled by
     * {@code styles/scss/_app-search.scss} at each breakpoint) and a fixed-size
     * "icon cluster" (notifications,
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

        Div searchSlot = new Div(new AppSearchBar(navRegistryProvider.getNavRegistry()));
        searchSlot.addClassName("wams-header-search-slot");

        HorizontalLayout iconCluster = new HorizontalLayout();
        iconCluster.addClassName("wams-header-icon-cluster");
        iconCluster.setAlignItems(FlexComponent.Alignment.CENTER);
        iconCluster.setSpacing(true);
        iconCluster.setPadding(false);
        iconCluster.add(createSearchToggleButton(), createNotificationsButton(), createSettingsButton(),
                new ThemeToggleButton(), new LanguageSelectorComponent(), createProfileComponent());

        rightContent.add(searchSlot, iconCluster);
        return rightContent;
    }

    /**
     * Only visible on narrow viewports (see layout CSS): reveals the search
     * field as a full-width second row instead of squeezing it next to the
     * icon cluster.
     */
    private Component createSearchToggleButton() {
        Button toggle = new Button(VaadinIcon.SEARCH.create());
        toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        toggle.addClassNames("wams-header-icon-btn", "wams-header-search-toggle");
        String tooltip = I18n.t("common.layout.header.search.toggle.tooltip");
        toggle.setTooltipText(tooltip);
        toggle.setAriaLabel(tooltip);
        toggle.addClickListener(e -> {
            boolean open = !getElement().getClassList().contains("wams-search-open");
            getElement().getClassList().set("wams-search-open", open);
        });
        return toggle;
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
        settings.addClickListener(e -> UI.getCurrent().navigate(getSettingsRoute()));
        return settings;
    }

    private Component createProfileComponent() {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        menuBar.addClassName("wams-profile-menu");

        // Same source as the sidebar user card, so the header and the footer always show the same person.
        UserDataResponseDto currentUser = loadCurrentUser();

        Avatar avatar = new Avatar(displayNameOf(currentUser));
        avatar.setThemeName("small");
        avatar.addClassName("wams-avatar-bordered");
        if (currentUser != null) {
            loadProfileImage(avatar, currentUser.getId());
        }

        MenuItem menuItem = menuBar.addItem(avatar);
        menuItem.getElement().setAttribute("aria-label", I18n.t("common.layout.avatar.profile"));
        SubMenu subMenu = menuItem.getSubMenu();

        if (currentUser != null) {
            Span name = new Span(avatar.getName());
            name.addClassName("wams-profile-menu__name");
            Span email = new Span(currentUser.getEmail() != null ? currentUser.getEmail() : "");
            email.addClassName("wams-profile-menu__email");
            Div identity = new Div(name, email);
            identity.addClassName("wams-profile-menu__identity");
            MenuItem header = subMenu.addItem(identity);
            header.addClassName("wams-profile-menu__header");
            header.setEnabled(false);
            subMenu.add(new Hr());
        }

        subMenu.addItem(menuEntry(VaadinIcon.USER, I18n.t("common.layout.avatar.profile")),
                e -> UI.getCurrent().navigate(getProfileRoute()));
        subMenu.addItem(menuEntry(VaadinIcon.COG_O, I18n.t("common.layout.avatar.settings")),
                e -> UI.getCurrent().navigate(getSettingsRoute()));
        subMenu.add(new Hr());
        MenuItem logout = subMenu.addItem(menuEntry(VaadinIcon.SIGN_OUT, I18n.t("common.layout.avatar.logout")),
                e -> logout());
        logout.addClassName("wams-profile-menu__logout");

        return menuBar;
    }

    private void createDrawer() {
        Component content = createDrawerContent();
        if (content == null) {
            return;
        }

        Div scrollArea = new Div(content);
        scrollArea.addClassName("wams-drawer-scroll-area");

        Div footer = new Div(createSidebarUserCard(), createSidebarCollapseToggle());
        footer.addClassName("wams-drawer-footer");

        Div wrapper = new Div(scrollArea, footer);
        wrapper.addClassName("wams-drawer-wrapper");
        wrapper.setSizeFull();

        drawerRoot = wrapper;
        addToDrawer(wrapper);
        applyRailMode();
    }

    /**
     * User card pinned above the collapse toggle: avatar, name and role (avatar only on the icon rail). Opens the
     * profile page. The data comes from the identity service once per session.
     */
    private Component createSidebarUserCard() {
        UserDataResponseDto user = loadCurrentUser();

        String displayName = displayNameOf(user);
        Avatar avatar = new Avatar(displayName);
        avatar.setThemeName("small");
        avatar.addClassName("wams-avatar-bordered");
        if (user != null) {
            loadProfileImage(avatar, user.getId());
        }

        Span name = new Span(displayName);
        name.addClassName("wams-user-card__name");
        String roleText = user != null && user.getRole() != null ? user.getRole() : "";
        Span role = new Span(roleText);
        role.addClassName("wams-user-card__role");
        Div text = new Div(name, role);
        text.addClassName("wams-user-card__text");

        Div card = new Div(avatar, text);
        card.addClassName("wams-user-card");
        card.getElement().setAttribute("role", "link");
        card.getElement().setAttribute("tabindex", "0");
        card.getElement().setAttribute("aria-label", I18n.t("common.layout.avatar.profile"));
        Tooltip.forComponent(card).setText(displayName);
        card.addClickListener(e -> UI.getCurrent().navigate(getProfileRoute()));
        return card;
    }

    private UserDataResponseDto loadCurrentUser() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            UserDataResponseDto cached = session.getAttribute(UserDataResponseDto.class);
            if (cached != null) {
                return cached;
            }
        }
        try {
            ResponseEntity<UserDataResponseDto> response = accountService.connectedUser();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (session != null) {
                    session.setAttribute(UserDataResponseDto.class, response.getBody());
                }
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Could not load the signed-in user for the sidebar card", e);
        }
        return null;
    }

    /**
     * The group flyouts only make sense on the desktop icon rail; on the overlay drawer (below 800px) groups keep
     * their normal accordion behaviour even if the rail preference is stored in the session.
     */
    private void applyRailMode() {
        if (drawerRoot == null) {
            return;
        }
        boolean rail = sidebarCollapsed && desktopViewport;
        forEachNavSection(drawerRoot, section -> section.setRailMode(rail));
    }

    /**
     * Persistent affordance pinned at the bottom of every module's sidebar
     * that collapses it to an icon-only rail (~60px) or expands it back
     * (~220px) — see {@code .wams-sidebar-collapsed} in
     * {@code styles/scss/layout/_app-shell.scss}. The
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
        applyRailMode();
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

    /**
     * Gives the avatar a lazily-streamed image: the browser fetches it through Vaadin's own stream handler when
     * the avatar is rendered, so nothing has to be pushed from a background thread (the layouts do not enable
     * server push, which is why an image loaded on another thread never showed up). When there is no image or the
     * call fails, the stream is empty, the browser reports a load error and the avatar falls back to initials.
     */
    private void loadProfileImage(Avatar avatar, Long accountId) {
        if (accountId == null) {
            return;
        }
        avatar.setImageResource(new StreamResource("profile_" + accountId + ".jpg",
                () -> new ByteArrayInputStream(avatarBytes(accountId))));
    }

    private byte[] avatarBytes(Long accountId) {
        VaadinSession session = VaadinSession.getCurrent();
        String cacheKey = avatarCacheKey(accountId);
        if (session != null && session.getAttribute(cacheKey) instanceof byte[] cached) {
            return cached;
        }

        byte[] bytes = new byte[0];
        boolean cacheable = true;
        try {
            ResponseEntity<Resource> response = accountImageService.downloadImage(accountId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                bytes = response.getBody().getContentAsByteArray();
            }
        } catch (FeignException ex) {
            // 404 = the account has no image: remember that. Anything else may be temporary: try again later.
            if (ex.status() != 404) {
                cacheable = false;
                log.warn("Failed to load profile image for account {}", accountId, ex);
            }
        } catch (IOException | RuntimeException ex) {
            cacheable = false;
            log.warn("Failed to load profile image for account {}", accountId, ex);
        }
        if (cacheable && session != null) {
            session.setAttribute(cacheKey, bytes);
        }
        return bytes;
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
