package eu.isygoit.ui.common.layout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
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

public abstract class BaseMainLayout extends AppLayout implements BeforeEnterObserver {

    private final AccountService accountService;
    private final AccountImageService accountImageService;
    private Div rightSlot; // container for profile

    public BaseMainLayout() {
        // Retrieve services statically (layout is not a Spring bean)
        this.accountService = SpringContextUtil.getBean(AccountService.class);
        this.accountImageService = SpringContextUtil.getBean(AccountImageService.class);

        createHeader();
        createDrawer();
    }

    protected abstract String getTitle();

    protected abstract void createDrawer();

    private void createHeader() {
        H1 title = new H1(getTitle());
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);
        title.getStyle().set("white-space", "nowrap");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(false);
        headerLayout.setSpacing(true);
        headerLayout.addClassName("main-header");

        HorizontalLayout leftPart = new HorizontalLayout(new DrawerToggle(), title);
        leftPart.setAlignItems(FlexComponent.Alignment.CENTER);
        leftPart.setSpacing(true);
        leftPart.getStyle().set("overflow", "hidden");

        // Right slot – will be filled in onAttach
        rightSlot = new Div();
        rightSlot.getStyle().set("margin-right", "var(--lumo-space-m)");
        // Put a simple placeholder text
        rightSlot.setText(I18n.t("layout.loading"));

        headerLayout.add(leftPart, rightSlot);
        headerLayout.expand(leftPart);

        addToNavbar(headerLayout);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Replace placeholder content with real components (language selector + profile)
        rightSlot.removeAll();
        rightSlot.add(createRightHeaderContent());
        injectResponsiveStyles();
    }

    private Component createRightHeaderContent() {
        HorizontalLayout rightContent = new HorizontalLayout();
        rightContent.setAlignItems(FlexComponent.Alignment.CENTER);
        rightContent.setSpacing(true);
        rightContent.setPadding(false);

        // Add language selector
        LanguageSelectorComponent languageSelector = new LanguageSelectorComponent();

        // Add profile component
        Component profileComponent = createProfileComponent();

        rightContent.add(languageSelector, profileComponent);
        return rightContent;
    }

    private Component createProfileComponent() {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        menuBar.setOpenOnHover(true);

        AccountDto currentAccount = getCurrentAccount();

        Avatar avatar = new Avatar();
        avatar.setThemeName("small");
        avatar.getStyle().set("border", "2px solid var(--lumo-contrast-20pct)");

        if (currentAccount != null) {
            String fullName = currentAccount.getFullName();
            if (fullName != null && !fullName.isBlank()) {
                avatar.setName(fullName);
            } else {
                avatar.setName(currentAccount.getEmail());
            }
            loadProfileImage(avatar, currentAccount.getId());
        } else {
            avatar.setName(I18n.t("layout.avatar.user.default.name"));
        }

        MenuItem menuItem = menuBar.addItem(avatar);
        menuItem.getSubMenu().addItem(I18n.t("layout.avatar.profile"), e -> UI.getCurrent().navigate("profile"));
        menuItem.getSubMenu().addItem(I18n.t("layout.avatar.settings"), e -> UI.getCurrent().navigate("settings"));
        menuItem.getSubMenu().addItem(I18n.t("layout.avatar.logout"), e -> logout());

        VerticalLayout profileContainer = new VerticalLayout(menuBar);
        profileContainer.setPadding(false);
        profileContainer.setSpacing(false);
        profileContainer.setAlignItems(FlexComponent.Alignment.END);
        return profileContainer;
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

    private void injectResponsiveStyles() {
        String css = """
                .main-header {
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                }
                @media (max-width: 640px) {
                    .main-header {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .main-header > :first-child {
                        justify-content: space-between;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    protected static class DrawerToggle extends com.vaadin.flow.component.applayout.DrawerToggle {
        public DrawerToggle() {
            super();
        }
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
}