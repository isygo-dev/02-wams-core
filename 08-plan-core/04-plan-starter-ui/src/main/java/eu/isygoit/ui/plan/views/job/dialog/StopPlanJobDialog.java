package eu.isygoit.ui.plan.views.job.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;

import java.util.function.Consumer;

/**
 * Asks a running planning job to stop (the best solution found so far is kept): shared
 * {@link PinConfirmActionDialog} as a message-only confirmation (no PIN), primary action button.
 */
public class StopPlanJobDialog extends PinConfirmActionDialog {

    public StopPlanJobDialog(PlanFeignService service, PlanJobMinDto job, Runnable onSuccess,
                             Consumer<Boolean> loading) {
        super(PlanJobConfirmations.texts(job, "stop"),
                PlanJobConfirmations.guarded(() -> service.stop(job.getId())),
                onSuccess, loading, null, false, ButtonVariant.LUMO_PRIMARY);
    }
}
