package eu.isygoit.ui.auth;

import com.google.zxing.WriterException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.request.AuthenticationContextRequest;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.dto.response.QrLoginCodePayload;
import eu.isygoit.dto.response.UserDataResponseDto;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.QrLoginStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.auth.AuthServiceFacade.Failure;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@UIScope
@Route(value = AuthRoutes.QR_LOGIN)
@PermitAll
public class QrCodeLoginView extends BaseLoginView {

    private final Image qrImage = new Image();
    private final Button refreshQrButton = new Button(
            I18n.t("auth.qrcode.button.refresh"), VaadinIcon.REFRESH.create());
    private final Div statusContainer = new Div();
    private final AuthServiceFacade authService;
    private final AuthSession authSession;
    private final ScheduledExecutorService pollScheduler;

    private String tenant;
    private String username;
    private String challengeId;
    private long challengeGeneration;
    private boolean generating;
    private boolean completing;
    private ScheduledFuture<?> scheduledPoll;

    public QrCodeLoginView(AuthServiceFacade authService, AuthSession authSession,
                           @Qualifier("authPollScheduler") ScheduledExecutorService pollScheduler) {
        super("auth.page.title.qrLogin");
        this.authService = authService;
        this.authSession = authSession;
        this.pollScheduler = pollScheduler;

        qrImage.setAlt(I18n.t("auth.qrcode.image.alt"));
        qrImage.setVisible(false);
        statusContainer.getElement().setAttribute("role", "status");
        statusContainer.getElement().setAttribute("aria-live", "polite");
        statusContainer.getElement().setAttribute("data-qr-status", "info");

        refreshQrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshQrButton.addClickListener(event -> generateQrCode());

        Anchor backToLogin = new Anchor(AuthRoutes.LOGIN,
                I18n.t("auth.common.link.backToSignIn"));
        addToCard(new BrandHeader(I18n.t("auth.qrcode.title"), null),
                qrImage, statusContainer, refreshQrButton, errorBanner,
                backToLogin, new AuthFooter());
        addDetachListener(event -> cancelPoll());
    }

    private void generateQrCode() {
        if (generating || completing || tenant == null || username == null) {
            return;
        }

        cancelPoll();
        long generation = ++challengeGeneration;
        clearError();
        challengeId = null;
        qrImage.setVisible(false);
        setQrStatus(I18n.t("auth.qrcode.status.awaitingApproval"), false);
        generating = true;
        setButtonLoading(refreshQrButton, true);

        AuthenticationContextRequest request = AuthenticationContextRequest.builder()
                .tenant(tenant)
                .userName(username)
                .authType(IEnumAuth.Types.QRC)
                .build();
        UI ui = UI.getCurrent();

        authService.resolveAuthContext(request).whenComplete((result, failure) ->
                ui.access(() -> {
                    if (!ui.isAttached() || generation != challengeGeneration) {
                        return;
                    }
                    generating = false;
                    setButtonLoading(refreshQrButton, false);
                    if (failure != null) {
                        reportUnexpectedFailure(failure);
                        showError(I18n.t("auth.qrcode.status.serviceError"), Failure.OFFLINE);
                        setQrStatus(I18n.t("auth.qrcode.status.serviceError"), true);
                        return;
                    }
                    if (!result.succeeded()) {
                        showError(failureMessage(result.failure(), "auth.qrcode.status.tokenFailed",
                                "auth.qrcode.status.serviceError"), result.failure());
                        setQrStatus(I18n.t("auth.qrcode.status.serviceError"), true);
                        return;
                    }
                    setChallenge(result.value(), ui);
                }));
    }

    private void setChallenge(UserContext context, UI ui) {
        if (context.getAuthTypeMode() != IEnumAuth.Types.QRC
                || context.getQrChallengeId() == null || context.getQrChallengeId().isBlank()) {
            showError(I18n.t("auth.qrcode.status.notAvailable"), Failure.SERVICE_ERROR);
            setQrStatus(I18n.t("auth.qrcode.status.notAvailable"), true);
            return;
        }

        challengeId = context.getQrChallengeId();
        String payload = QrLoginCodePayload.forChallenge(challengeId).toQrContent();
        try {
            qrImage.setSrc(QrCodeRenderer.toSvgDataUri(payload));
        } catch (WriterException exception) {
            reportUnexpectedFailure(exception);
            showError(I18n.t("auth.qrcode.status.serviceError"), Failure.SERVICE_ERROR);
            setQrStatus(I18n.t("auth.qrcode.status.serviceError"), true);
            return;
        }

        qrImage.setVisible(true);
        setQrStatus(I18n.t("auth.qrcode.status.scanHint"), false);
        schedulePoll(ui, challengeId);
    }

    private void schedulePoll(UI ui, String currentChallengeId) {
        if (!ui.isAttached() || !currentChallengeId.equals(challengeId)) {
            return;
        }
        scheduledPoll = pollScheduler.schedule(
                () -> pollStatus(ui, currentChallengeId), 2, TimeUnit.SECONDS);
    }

    private void pollStatus(UI ui, String currentChallengeId) {
        if (!ui.isAttached()) {
            return;
        }
        authService.getQrLoginStatus(currentChallengeId).whenComplete((result, failure) -> {
            if (!ui.isAttached()) {
                return;
            }
            ui.access(() -> {
                if (!currentChallengeId.equals(challengeId)) {
                    return;
                }
                scheduledPoll = null;
                if (failure != null) {
                    reportUnexpectedFailure(failure);
                    showError(I18n.t("auth.qrcode.status.serviceError"), Failure.OFFLINE);
                    setQrStatus(I18n.t("auth.qrcode.status.serviceError"), true);
                    return;
                }
                if (!result.succeeded()) {
                    showError(failureMessage(result.failure(), "auth.qrcode.status.authFailed",
                            "auth.qrcode.status.serviceError"), result.failure());
                    setQrStatus(I18n.t("auth.qrcode.status.serviceError"), true);
                    return;
                }

                QrLoginStatus status = result.value().getStatus();
                if (status == QrLoginStatus.PENDING) {
                    schedulePoll(ui, currentChallengeId);
                } else if (status == QrLoginStatus.APPROVED) {
                    setQrStatus(I18n.t("auth.qrcode.status.approved"), false);
                    completeQrLogin(ui, currentChallengeId);
                } else {
                    showError(I18n.t("auth.qrcode.status.expired"), Failure.EXPIRED);
                    setQrStatus(I18n.t("auth.qrcode.status.expired"), true);
                }
            });
        });
    }

    private void completeQrLogin(UI ui, String currentChallengeId) {
        if (completing) {
            return;
        }
        completing = true;
        setButtonLoading(refreshQrButton, true);

        authService.completeQrLogin(currentChallengeId).whenComplete((result, failure) ->
                ui.access(() -> {
                    if (!ui.isAttached() || !currentChallengeId.equals(challengeId)) {
                        return;
                    }
                    completing = false;
                    setButtonLoading(refreshQrButton, false);
                    if (failure != null) {
                        reportUnexpectedFailure(failure);
                        showError(I18n.t("auth.common.error.authenticationError"), Failure.OFFLINE);
                        setQrStatus(I18n.t("auth.common.error.authenticationError"), true);
                        return;
                    }
                    if (!result.succeeded()) {
                        showError(failureMessage(result.failure(), "auth.qrcode.status.authFailed",
                                "auth.qrcode.status.serviceError"), result.failure());
                        setQrStatus(I18n.t("auth.qrcode.status.authFailed"), true);
                        return;
                    }

                    AuthResponseDto response = result.value();
                    UserDataResponseDto userData = response.getUserDataResponseDto();
                    String authenticatedUser = userData == null || userData.getUserName() == null
                            ? username
                            : userData.getUserName();
                    try {
                        authSession.write(authenticatedUser, response.getAccessToken());
                        authSession.redirect(ui, redirectTarget);
                        showWelcome(authenticatedUser);
                    } catch (IllegalArgumentException | IllegalStateException exception) {
                        reportUnexpectedFailure(exception);
                        showError(I18n.t("auth.common.error.authenticationError"), Failure.SERVICE_ERROR);
                    }
                }));
    }

    private void setQrStatus(String message, boolean isError) {
        statusContainer.setText(message);
        statusContainer.getElement().setAttribute("data-qr-status", isError ? "error" : "info");
    }

    private void cancelPoll() {
        if (scheduledPoll != null) {
            scheduledPoll.cancel(false);
            scheduledPoll = null;
        }
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        clearError();
        cancelPoll();
        challengeGeneration++;
        generating = false;
        completing = false;
        Optional<String> tenantOpt = event.getLocation().getQueryParameters().getSingleParameter("tenant");
        Optional<String> usernameOpt = event.getLocation().getQueryParameters().getSingleParameter("username");
        if (tenantOpt.isEmpty() || usernameOpt.isEmpty()) {
            event.forwardTo(AuthRoutes.LOGIN);
            return;
        }

        tenant = tenantOpt.get();
        username = usernameOpt.get();
        generateQrCode();
    }
}
