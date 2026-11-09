package eu.isygoit.ui.plan.nav;

import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.ui.common.component.INavRegistry;

import java.util.List;

/**
 * Navigation targets of the planning module, used by the global search and the landing page.
 */
public final class PlanNavRegistry implements INavRegistry {
    private static final List<NavTarget> ALL = List.of(
            new NavTarget("plan.ui.nav.dashboard", "plan", VaadinIcon.HOME, "plan"),
            new NavTarget("plan.ui.nav.jobs", "plan/jobs", VaadinIcon.LIST, "plan"),
            new NavTarget("plan.ui.nav.logs", "plan/logs", VaadinIcon.LIST, "plan")
    );

    @Override
    public String moduleLabelKey(String moduleKey) {
        return "common.nav.module." + moduleKey;
    }

    @Override
    public List<NavTarget> getAll() {
        return ALL;
    }
}
