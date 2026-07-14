package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.request.AccountAuthTypeRequest;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.PublicAuthService;
import eu.isygoit.util.SecurityUtils;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@UIScope
@Route(value = "login/qr")
@PageTitle("QR Code Login")
@PermitAll
public class QrCodeLoginView extends BaseLoginView {

    private final Image qrImage = new Image();
    private final Button refreshQrButton = new Button(I18n.t("auth.qrcode.button.refresh"), VaadinIcon.REFRESH.create());
    private final Div statusContainer = new Div();

    private String tenant;
    private String username;
    private String qrCodeToken;

    @Autowired
    private PublicAuthService authService;

    public QrCodeLoginView() {
        configureAsAuthPage("qr-login-view");

        qrImage.setWidth("200px");
        qrImage.setHeight("200px");
        qrImage.addClassName("wams-qr-image");

        statusContainer.addClassName("wams-qr-status");
        statusContainer.addClassName("wams-qr-status--info");

        refreshQrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshQrButton.addClickListener(e -> generateQrCode());

        Anchor backToLogin = createLink("login", I18n.t("auth.common.link.backToSignIn"));

        var card = createCard();
        card.add(createBrand(I18n.t("auth.qrcode.title"), null),
                qrImage, statusContainer, refreshQrButton, backToLogin, createFooter());

        add(card);

        addAttachListener(event -> {
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
                    statusContainer.setText(I18n.t("auth.qrcode.status.notAvailable"));
                    return;
                }
                String encoded = URLEncoder.encode(qrCodeToken, StandardCharsets.UTF_8);
                String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encoded;
                qrImage.setSrc(qrUrl);
                statusContainer.setText(I18n.t("auth.qrcode.status.scanHint"));
                statusContainer.removeClassName("wams-qr-status--error");
                statusContainer.addClassName("wams-qr-status--info");
            } else {
                statusContainer.setText(I18n.t("auth.qrcode.status.tokenFailed"));
            }
        } catch (Exception ex) {
            statusContainer.setText(I18n.t("auth.qrcode.status.serviceError"));
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

                VaadinSession vaadinSession = VaadinSession.getCurrent();
                vaadinSession.setAttribute("user", username);
                vaadinSession.setAttribute("accessToken", authResponse.getAccessToken());

                // Also store in plain HTTP session as a fallback
                HttpSession httpSession = VaadinServletRequest.getCurrent().getSession(true);
                httpSession.setAttribute("user", username);
                httpSession.setAttribute("accessToken", authResponse.getAccessToken());

                log.info("User logged in: {} (session id: {})", username, vaadinSession.getSession().getId());

                String target = (redirectTarget != null && SecurityUtils.isSafeInternalPath(redirectTarget))
                        ? redirectTarget
                        : "landing";
                UI.getCurrent().navigate(target);

                Notification.show(I18n.t("auth.qrcode.notification.loggedIn"), 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                statusContainer.setText(I18n.t("auth.qrcode.status.authFailed"));
                statusContainer.removeClassName("wams-qr-status--info");
                statusContainer.addClassName("wams-qr-status--error");
            }
        } catch (Exception ex) {
            statusContainer.setText(I18n.t("auth.common.error.authenticationError"));
            statusContainer.removeClassName("wams-qr-status--info");
            statusContainer.addClassName("wams-qr-status--error");
        }
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        Optional<String> tenantOpt = event.getLocation().getQueryParameters().getSingleParameter("tenant");
        Optional<String> usernameOpt = event.getLocation().getQueryParameters().getSingleParameter("username");

        if (tenantOpt.isEmpty() || usernameOpt.isEmpty()) {
            event.forwardTo("login");
            return;
        }

        tenant = tenantOpt.get();
        username = usernameOpt.get();
        generateQrCode();
    }
}
