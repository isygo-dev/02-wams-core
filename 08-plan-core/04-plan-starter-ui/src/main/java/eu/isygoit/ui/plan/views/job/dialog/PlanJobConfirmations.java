package eu.isygoit.ui.plan.views.job.dialog;

import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;
import eu.isygoit.ui.plan.common.PlanErrors;
import org.springframework.util.StringUtils;

/**
 * Texts and error mapping shared by the stop, cancel and delete confirmation dialogs of a planning job
 * (keys {@code plan.ui.job.<action>.title|confirm|button|success}).
 */
final class PlanJobConfirmations {

    private PlanJobConfirmations() {
    }

    static PinConfirmActionDialog.Texts texts(PlanJobMinDto job, String action) {
        String label = StringUtils.hasText(job.getCode()) ? job.getCode() : String.valueOf(job.getId());
        return new PinConfirmActionDialog.Texts(
                I18n.t("plan.ui.job." + action + ".title"),
                I18n.t("plan.ui.job." + action + ".confirm", label),
                I18n.t("plan.ui.job." + action + ".button"),
                I18n.t("plan.ui.job.pin.invalid"),
                I18n.t("plan.ui.job." + action + ".success"),
                detail -> I18n.t("plan.ui.job.action.error", detail));
    }

    /**
     * Wraps the remote call so that a Feign failure reaches the dialog already translated.
     */
    static PinConfirmActionDialog.Action guarded(PinConfirmActionDialog.Action call) {
        return () -> {
            try {
                return call.run();
            } catch (Exception e) {
                throw new IllegalStateException(PlanErrors.describe(e), e);
            }
        };
    }
}
