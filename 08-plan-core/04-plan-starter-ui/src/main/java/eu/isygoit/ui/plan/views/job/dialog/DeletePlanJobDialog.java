package eu.isygoit.ui.plan.views.job.dialog;

import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;

import java.util.function.Consumer;

/**
 * Deletes a finished planning job: shared {@link DeleteActionDialog} (red button, the user retypes the generated
 * 9-digit code before the action is enabled).
 */
public class DeletePlanJobDialog extends DeleteActionDialog {

    public DeletePlanJobDialog(PlanFeignService service, PlanJobMinDto job, Runnable onSuccess,
                               Consumer<Boolean> loading) {
        super(PlanJobConfirmations.texts(job, "delete"),
                PlanJobConfirmations.guarded(() -> service.delete(job.getId())),
                onSuccess, loading, null);
    }
}
