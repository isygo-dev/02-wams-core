package eu.isygoit.ui.common.layout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public abstract class BaseMainLayout extends AppLayout {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountImageService accountImageService;

    protected abstract String getTitle();
    protected abstract void createDrawer();

    public BaseMainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 title = new H1(getTitle());
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        // Left side: drawer toggle + title
        HorizontalLayout leftPart = new HorizontalLayout(new DrawerToggle(), title);
        leftPart.setAlignItems(FlexComponent.Alignment.CENTER);
        leftPart.setSpacing(true);

        // Right side: profile component
        com.vaadin.flow.component.Component profileComponent = createProfileComponent();
        headerLayout.add(leftPart, profileComponent);
        headerLayout.expand(leftPart);

        addToNavbar(headerLayout);
    }

    private com.vaadin.flow.component.Component createProfileComponent() {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        menuBar.setOpenOnHover(true);

        AccountDto currentAccount = getCurrentAccount();

        Avatar avatar = new Avatar();
        avatar.setThemeName("xsmall");
        if (currentAccount != null) {
            String fullName = currentAccount.getFullName();
            if (fullName != null && !fullName.isBlank()) {
                avatar.setName(fullName);
            } else {
                avatar.setName(currentAccount.getEmail());
            }
            loadProfileImage(avatar, currentAccount.getId());
        } else {
            avatar.setName("User");
        }

        MenuItem menuItem = menuBar.addItem(avatar);
        menuItem.getSubMenu().addItem("Profile", e -> UI.getCurrent().navigate("profile"));
        menuItem.getSubMenu().addItem("Settings", e -> UI.getCurrent().navigate("settings"));
        menuItem.getSubMenu().addItem("Logout", e -> logout());

        VerticalLayout profileContainer = new VerticalLayout(menuBar);
        profileContainer.setPadding(false);
        profileContainer.setSpacing(false);
        profileContainer.setAlignItems(FlexComponent.Alignment.END);
        profileContainer.getStyle().set("margin-right", "var(--lumo-space-m)");
        return profileContainer;
    }

    protected AccountDto getCurrentAccount() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof String) {
                String email = (String) principal;
                try {
                    // Replace with actual service call
                    // ResponseEntity<AccountDto> response = accountService.findByEmail(email);
                    // return response.getBody();
                    return null;
                } catch (Exception e) {
                    return null;
                }
            } else if (principal instanceof AccountDto) {
                return (AccountDto) principal;
            }
        }
        AccountDto account = VaadinSession.getCurrent().getAttribute(AccountDto.class);
        return account;
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
        VaadinSession.getCurrent().getSession().invalidate();
        VaadinSession.getCurrent().close();
        SecurityContextHolder.clearContext();
        UI.getCurrent().getPage().setLocation("login");
    }

    // Inner class for DrawerToggle
    protected static class DrawerToggle extends com.vaadin.flow.component.applayout.DrawerToggle {
        public DrawerToggle() {
            super();
        }
    }
}