package eu.isygoit.ui.plan.views.job.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;

import java.util.function.Consumer;

/**
 * Cancels a queued (CREATED) planning job: shared {@link PinConfirmActionDialog} with the 9-digit code
 * confirmation and the red action button.
 */
public class CancelPlanJobDialog extends PinConfirmActionDialog {

    public CancelPlanJobDialog(PlanFeignService service, PlanJobMinDto job, Runnable onSuccess,
                               Consumer<Boolean> loading) {
        super(PlanJobConfirmations.texts(job, "cancel"),
                PlanJobConfirmations.guarded(() -> service.cancel(job.getId())),
                onSuccess, loading, null, true, ButtonVariant.LUMO_ERROR);
    }
}
