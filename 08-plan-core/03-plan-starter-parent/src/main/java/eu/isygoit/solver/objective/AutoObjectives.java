package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Automatic objective injection (design 6.8, example 2.6e). When a problem has at least one OPTIONAL task and declares
 * neither a {@code maximize_selected_value} nor a {@code maximize_task_priority} objective, the built-in
 * {@code auto-selected-value} objective ({@code maximize_selected_value}, weight 1) is appended at the reserved tier
 * {@value #AUTO_TIER}, so selecting prize-bearing tasks is always optimised before any user tier. The user disables the
 * injection only by declaring one of the two selection objectives (weight 0 included).
 * <p>
 * Called by {@code DomainNormalizer}. Pure static function, thread-safe; inputs are never modified.
 */
public final class AutoObjectives {

    /** Id of the injected objective. */
    public static final String AUTO_ID = "auto-selected-value";
    /** Reserved tier of the injected objective. */
    public static final int AUTO_TIER = -1;

    private AutoObjectives() {
    }

    /**
     * Adds the built-in selection objective when needed.
     *
     * @param tasks      the tasks of the problem (request order); {@code null} is treated as empty
     * @param objectives the declared objectives (request order); {@code null} is treated as empty
     * @return an unmodifiable list: the declared objectives, plus {@code auto-selected-value} at tier -1 as LAST element
     * when the problem has optional tasks and no selection objective; its id is made unique when taken
     */
    public static List<Objective> inject(List<Task> tasks, List<Objective> objectives) {
        List<Objective> declared = objectives == null ? List.of() : objectives;
        if (!needsInjection(tasks, declared)) {
            return List.copyOf(declared);
        }
        Set<String> ids = new HashSet<>();
        for (Objective o : declared) {
            ids.add(o.id());
        }
        String id = AUTO_ID;
        for (int n = 2; ids.contains(id); n++) {
            id = AUTO_ID + "-" + n;
        }
        List<Objective> result = new ArrayList<>(declared.size() + 1);
        result.addAll(declared);
        result.add(new Objective(id, MaximizeSelectedValueObjective.TYPE, 1, AUTO_TIER, null, null, Map.of()));
        return List.copyOf(result);
    }

    private static boolean needsInjection(List<Task> tasks, List<Objective> declared) {
        boolean anyOptional = false;
        if (tasks != null) {
            for (Task t : tasks) {
                if (!t.requiredTask()) {
                    anyOptional = true;
                    break;
                }
            }
        }
        if (!anyOptional) {
            return false;
        }
        for (Objective o : declared) {
            if (MaximizeSelectedValueObjective.TYPE.equals(o.type())
                    || MaximizeTaskPriorityObjective.TYPE.equals(o.type())) {
                return false;
            }
        }
        return true;
    }
}
