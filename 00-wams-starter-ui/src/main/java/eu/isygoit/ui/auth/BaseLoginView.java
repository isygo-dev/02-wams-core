package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.component.UIDetachedException;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.auth.AuthServiceFacade.Failure;
import eu.isygoit.ui.common.layout.PageEnvironment;
import eu.isygoit.util.SecurityUtils;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;

@CssImport("./themes/isygo/auth/auth.scss")
public abstract class BaseLoginView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(BaseLoginView.class);

    private static final ScheduledExecutorService COUNTDOWN_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "auth-rate-limit-countdown");
                thread.setDaemon(true);
                return thread;
            });

    protected final AuthErrorBanner errorBanner = new AuthErrorBanner();
    private final String pageTitleKey;
    private final AuthLayout authLayout = new AuthLayout();
    protected String redirectTarget;
    private volatile ScheduledFuture<?> rateLimitCountdown;
    private volatile ScheduledFuture<?> buttonCooldown;

    protected BaseLoginView(String pageTitleKey) {
        this.pageTitleKey = pageTitleKey;
        errorBanner.setId(AuthErrorBanner.ID);
        getElement().appendChild(authLayout.getElement());
        addDetachListener(event -> {
            cancelRateLimitCountdown();
            cancelButtonCooldown();
        });
    }

    @Override
    public String getPageTitle() {
        return I18n.t(pageTitleKey);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getUI().getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        PageEnvironment.apply(event.getUI());
        redirectTarget = SecurityUtils.consumeRedirect();
        if (redirectTarget == null) {
            redirectTarget = event.getLocation()
                    .getQueryParameters()
                    .getSingleParameter("redirect")
                    .filter(SecurityUtils::isSafeInternalPath)
                    .orElse(null);
        }

        if (SecurityUtils.isUserLoggedIn()) {
            String destination = redirectTarget != null
                    && SecurityUtils.isSafeInternalPath(redirectTarget)
                    ? redirectTarget
                    : AuthRoutes.LANDING;
            event.forwardTo(destination);
            return;
        }

        onBeforeEnter(event);
        getElement().executeJs(
                "this.querySelector('auth-brand h1')?.focus({preventScroll: true})");
    }

    protected void onBeforeEnter(BeforeEnterEvent event) {
    }

    protected void showError(String message, Failure failure) {
        errorBanner.showError(message, failure);
    }

    protected void showError(String message, Failure failure, Component focusTarget) {
        showError(message, failure);
        if (focusTarget != null) {
            focusTarget.getElement().executeJs("this.focus()");
        }
    }

    protected void clearError() {
        errorBanner.clear();
    }

    protected void describeErrors(Component field) {
        field.getElement().setAttribute("aria-describedby", AuthErrorBanner.ID);
    }

    protected final void setButtonLoading(Button button, boolean loading) {
        button.setEnabled(!loading);
        button.getElement().setAttribute("aria-busy", Boolean.toString(loading));
        if (loading) {
            button.addClassName("is-loading");
        } else {
            button.removeClassName("is-loading");
        }
    }

    protected void addToCard(Component... components) {
        authLayout.addToCard(components);
    }

    protected void setWideLayout(boolean wide) {
        authLayout.setWideLayout(wide);
    }

    /**
     * Shows a failed authentication call. For RATE_LIMITED with a known wait time this starts a visible
     * "try again in m:ss" countdown that locks the given buttons until it ends; otherwise it behaves like
     * {@link #showError(String, Failure, Component)}.
     */
    protected void showFailure(AuthServiceFacade.Result<?> result, String invalidCredentialsKey,
                               String fallbackKey, Component focusTarget, Button... lockedButtons) {
        if (result.failure() == Failure.RATE_LIMITED && result.retryAfterSeconds() > 0) {
            startRateLimitCountdown(result.retryAfterSeconds(), lockedButtons);
            return;
        }
        showError(failureMessage(result.failure(), invalidCredentialsKey, fallbackKey),
                result.failure(), focusTarget);
    }

    /**
     * Called on the UI thread when a rate-limit countdown ends and its buttons were re-enabled.
     * Views whose buttons depend on other state should re-apply it here.
     */
    protected void onRateLimitEnded() {
    }

    private void startRateLimitCountdown(long seconds, Button... buttons) {
        cancelRateLimitCountdown();
        UI ui = UI.getCurrent();
        if (ui == null) {
            showError(I18n.t("auth.common.error.rateLimited"), Failure.RATE_LIMITED);
            return;
        }

        long endNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        for (Button button : buttons) {
            button.setEnabled(false);
        }
        showError(rateLimitMessage(seconds), Failure.RATE_LIMITED);

        rateLimitCountdown = COUNTDOWN_SCHEDULER.scheduleAtFixedRate(() -> {
            long remaining = (long) Math.ceil((endNanos - System.nanoTime()) / 1_000_000_000.0);
            try {
                ui.access(() -> {
                    if (rateLimitCountdown == null) {
                        return;
                    }
                    if (remaining <= 0) {
                        cancelRateLimitCountdown();
                        clearError();
                        for (Button button : buttons) {
                            button.setEnabled(true);
                        }
                        onRateLimitEnded();
                    } else {
                        errorBanner.showCountdown(rateLimitMessage(remaining));
                    }
                });
            } catch (UIDetachedException exception) {
                cancelRateLimitCountdown();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Disables a button for the given time and shows the remaining time on it ("Resend code in 0:45"), then
     * restores its label and re-enables it. Used for resend cooldowns that mirror a server-side rule.
     */
    protected void startButtonCooldown(Button button, int seconds, LongFunction<String> remainingLabel,
                                       String restingLabel, Runnable onDone) {
        cancelButtonCooldown();
        UI ui = UI.getCurrent();
        if (ui == null || seconds <= 0) {
            return;
        }

        long endNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        button.setEnabled(false);
        button.setText(remainingLabel.apply(seconds));
        buttonCooldown = COUNTDOWN_SCHEDULER.scheduleAtFixedRate(() -> {
            long remaining = (long) Math.ceil((endNanos - System.nanoTime()) / 1_000_000_000.0);
            try {
                ui.access(() -> {
                    if (buttonCooldown == null) {
                        return;
                    }
                    if (remaining <= 0) {
                        cancelButtonCooldown();
                        button.setText(restingLabel);
                        onDone.run();
                    } else {
                        button.setText(remainingLabel.apply(remaining));
                    }
                });
            } catch (UIDetachedException exception) {
                cancelButtonCooldown();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Ends a running button cooldown early and restores the button's label (the caller decides whether it is
     * enabled).
     */
    protected void stopButtonCooldown(Button button, String restingLabel) {
        if (buttonCooldown != null) {
            cancelButtonCooldown();
            button.setText(restingLabel);
        }
    }

    protected boolean isButtonCoolingDown() {
        return buttonCooldown != null;
    }

    /**
     * Formats seconds as m:ss.
     */
    protected static String formatClock(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void cancelButtonCooldown() {
        ScheduledFuture<?> cooldown = buttonCooldown;
        buttonCooldown = null;
        if (cooldown != null) {
            cooldown.cancel(false);
        }
    }

    private void cancelRateLimitCountdown() {
        ScheduledFuture<?> countdown = rateLimitCountdown;
        rateLimitCountdown = null;
        if (countdown != null) {
            countdown.cancel(false);
        }
    }

    private String rateLimitMessage(long seconds) {
        return I18n.t("auth.common.error.rateLimitedRetry", formatClock(seconds));
    }

    protected String failureMessage(Failure failure, String invalidCredentialsKey, String fallbackKey) {
        if (failure == null) {
            return I18n.t(fallbackKey);
        }
        return switch (failure) {
            case INVALID_CREDENTIALS -> I18n.t(invalidCredentialsKey);
            case INVALID_TOKEN -> I18n.t("auth.qrcode.status.authFailed");
            case RATE_LIMITED -> I18n.t("auth.common.error.rateLimited");
            case LOCKED -> I18n.t("auth.common.error.locked");
            case EXPIRED -> I18n.t("auth.common.error.expired");
            case OFFLINE -> I18n.t("auth.common.error.offline");
            case SERVICE_ERROR -> I18n.t(fallbackKey);
            case REGISTRATION_REJECTED -> I18n.t(fallbackKey);
        };
    }

    protected void showWelcome(String username) {
        Notification.show(I18n.t("auth.common.notification.welcome", username), 2000,
                        Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    protected void reportUnexpectedFailure(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        LOG.warn("Authentication service request failed ({})",
                cause.getClass().getSimpleName(), cause);
    }
}
