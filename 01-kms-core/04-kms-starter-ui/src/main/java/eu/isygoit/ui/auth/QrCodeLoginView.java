package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.request.AccountAuthTypeRequest;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.remote.ims.PublicAuthService;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@UIScope
@Route(value = "login/qr")
@PageTitle("QR Code Login")
@PermitAll
public class QrCodeLoginView extends VerticalLayout implements BeforeEnterObserver {

    private final Image qrImage = new Image();
    private final Button refreshQrButton = new Button("Refresh QR", VaadinIcon.REFRESH.create());
    private final Div statusContainer = new Div();
    private boolean stylesInjected = false;

    private String tenant;
    private String username;
    private String qrCodeToken;
    private String redirectTarget;

    @Autowired
    private PublicAuthService authService;

    public QrCodeLoginView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("qr-login-view");

        // Brand
        Div brand = new Div();
        brand.addClassName("brand");
        Avatar logo = new Avatar("KMS/IMS");
        logo.setColorIndex(3);
        logo.setWidth("56px");
        logo.setHeight("56px");
        H2 title = new H2("QR Code Login");
        title.addClassName(LumoUtility.FontWeight.BOLD);
        title.addClassName(LumoUtility.Margin.NONE);
        brand.add(logo, title);

        // QR Code Image
        qrImage.setWidth("200px");
        qrImage.setHeight("200px");
        qrImage.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

        // Status text
        statusContainer.addClassName("status-text");
        statusContainer.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("margin", "var(--lumo-space-s) 0");

        // Refresh button
        refreshQrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshQrButton.addClickListener(e -> generateQrCode());

        // Back link
        Anchor backToLogin = new Anchor("login", "← Back to sign in");
        backToLogin.addClassName("back-link");

        // Footer
        Paragraph footer = new Paragraph("© 2026 KMS/IMS Platform");
        footer.addClassName(LumoUtility.TextColor.TERTIARY);
        footer.addClassName(LumoUtility.FontSize.XXSMALL);
        footer.addClassName(LumoUtility.Margin.Top.MEDIUM);

        VerticalLayout wrapper = new VerticalLayout(brand, qrImage, statusContainer, refreshQrButton,
                backToLogin, footer);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setMaxWidth("400px");
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(true);
        wrapper.addClassName("qr-wrapper");

        add(wrapper);

        addAttachListener(event -> {
            if (!stylesInjected) {
                injectResponsiveStyles();
                stylesInjected = true;
            }
            // Simulate QR scan after 5 seconds (demo)
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => { $0.dispatchEvent(new Event('qr-scanned')); }, 5000);",
                    getElement()
            );
            getElement().addEventListener("qr-scanned", e -> handleQrScan());
        });
    }

    private void generateQrCode() {
        AccountAuthTypeRequest request = AccountAuthTypeRequest.builder()
                .tenant(tenant)
                .userName(username)
                .build();

        try {
            ResponseEntity<UserContext> response = authService.getAuthenticationType(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                UserContext userContext = response.getBody();
                qrCodeToken = userContext.getQrCodeToken();
                if (qrCodeToken == null || qrCodeToken.isEmpty()) {
                    statusContainer.setText("QR code not available. Please use another method.");
                    return;
                }
                String encoded = URLEncoder.encode(qrCodeToken, StandardCharsets.UTF_8);
                String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encoded;
                qrImage.setSrc(qrUrl);
                statusContainer.setText("Scan the QR code with your authenticator app");
                statusContainer.getStyle().set("color", "var(--lumo-secondary-text-color)");
            } else {
                statusContainer.setText("Failed to retrieve QR token.");
            }
        } catch (Exception ex) {
            statusContainer.setText("Service error. Please try again.");
        }
    }

    private void handleQrScan() {
        AuthenticationRequestDto authRequest = AuthenticationRequestDto.builder()
                .tenant(tenant)
                .application("default")
                .userName(username)
                .password(qrCodeToken != null ? qrCodeToken : "")
                .authType(IEnumAuth.Types.QRC)
                .build();

        try {
            ResponseEntity<AuthResponseDto> response = authService.authenticate(authRequest);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AuthResponseDto authResponse = response.getBody();
                VaadinSession.getCurrent().setAttribute("user", username);
                VaadinSession.getCurrent().setAttribute("accessToken", authResponse.getAccessToken());

                String target = (redirectTarget != null) ? redirectTarget : "kms";
                UI.getCurrent().navigate(target);

                Notification.show("Logged in via QR code!", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                statusContainer.setText("QR authentication failed. Please try again.");
                statusContainer.getStyle().set("color", "var(--lumo-error-text-color)");
            }
        } catch (Exception ex) {
            statusContainer.setText("Authentication error. Please try again.");
            statusContainer.getStyle().set("color", "var(--lumo-error-text-color)");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // If already authenticated, redirect to target or default
        if (VaadinSession.getCurrent().getAttribute("user") != null) {
            String target = event.getLocation().getQueryParameters()
                    .getSingleParameter("redirect")
                    .filter(this::isSafeInternalPath)
                    .orElse("kms");
            event.forwardTo(target);
            return;
        }

        Optional<String> tenantOpt = event.getLocation().getQueryParameters().getSingleParameter("tenant");
        Optional<String> usernameOpt = event.getLocation().getQueryParameters().getSingleParameter("username");
        redirectTarget = event.getLocation().getQueryParameters()
                .getSingleParameter("redirect")
                .filter(this::isSafeInternalPath)
                .orElse(null);

        if (tenantOpt.isEmpty() || usernameOpt.isEmpty()) {
            event.forwardTo("login");
            return;
        }

        tenant = tenantOpt.get();
        username = usernameOpt.get();
        generateQrCode();
    }

    private boolean isSafeInternalPath(String path) {
        return StringUtils.hasText(path) && path.startsWith("/") && !path.contains("..") && !path.contains("//");
    }

    private void injectResponsiveStyles() {
        String css = """
                .qr-login-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .qr-wrapper {
                    background: var(--lumo-base-color);
                    border-radius: var(--lumo-border-radius-xl);
                    box-shadow: var(--lumo-box-shadow-m);
                    padding: var(--lumo-space-l);
                }
                .qr-login-view .brand {
                    text-align: center;
                    margin-bottom: var(--lumo-space-m);
                }
                .qr-login-view .brand h2 {
                    font-size: var(--lumo-font-size-xl);
                    letter-spacing: -0.5px;
                }
                .qr-login-view .back-link {
                    color: var(--lumo-primary-text-color);
                    font-size: var(--lumo-font-size-s);
                    margin-top: var(--lumo-space-m);
                }
                .qr-login-view .status-text {
                    min-height: 3em;
                }
                .qr-login-view img {
                    border-radius: var(--lumo-border-radius-m);
                    box-shadow: var(--lumo-box-shadow-xs);
                }
                @media (max-width: 480px) {
                    .qr-wrapper {
                        padding: var(--lumo-space-m);
                        border-radius: var(--lumo-border-radius-l);
                        margin: var(--lumo-space-m);
                    }
                    .qr-login-view .brand h2 {
                        font-size: var(--lumo-font-size-l);
                    }
                    .qr-login-view img {
                        width: 160px !important;
                        height: 160px !important;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}