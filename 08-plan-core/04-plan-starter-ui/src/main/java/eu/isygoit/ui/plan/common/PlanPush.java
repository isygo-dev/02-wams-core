package eu.isygoit.ui.plan.common;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * Makes sure server push is on for the current UI: results of calls run off the UI thread
 * ({@code ContextAwareAsync} then {@code ui.access}) only reach the browser with push, and the plan
 * application shell does not enable it globally.
 */
public final class PlanPush {

    private PlanPush() {
    }

    /**
     * Enables automatic push on the UI when it is still disabled.
     */
    public static void ensure(UI ui) {
        if (ui != null && ui.getPushConfiguration().getPushMode() == PushMode.DISABLED) {
            ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        }
    }
}
