package eu.isygoit.ui.plan.common;

import eu.isygoit.i18n.I18n;
import feign.FeignException;

import java.util.concurrent.CompletionException;

/**
 * Turns a failed call to the plan service into a message for the user: the HTTP statuses the service answers
 * with (400, 404, 409, 503) get their own translated text, anything else a generic one.
 */
public final class PlanErrors {

    private PlanErrors() {
    }

    /**
     * User-readable message of a failure (an async {@link CompletionException} is unwrapped first).
     */
    public static String describe(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof FeignException feign) {
            return switch (feign.status()) {
                case 400 -> I18n.t("plan.ui.error.bad.request");
                case 404 -> I18n.t("plan.ui.error.not.found");
                case 409 -> I18n.t("plan.ui.error.conflict");
                case 503 -> I18n.t("plan.ui.error.unavailable");
                default -> I18n.t("plan.ui.error.unexpected");
            };
        }
        return I18n.t("plan.ui.error.unexpected");
    }
}
