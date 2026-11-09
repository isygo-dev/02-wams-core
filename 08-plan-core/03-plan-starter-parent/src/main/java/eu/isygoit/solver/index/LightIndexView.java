package eu.isygoit.solver.index;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.solver.spi.CandidateIndexView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The LIGHT {@link CandidateIndexView} used by validation before the real index exists (design 5.2): counts, ids,
 * id-to-index lookups, horizon, limits and scope resolution only; every other method throws
 * {@link UnsupportedOperationException} (the interface defaults). Immutable and thread-safe; O(T + R + S) to build.
 */
public final class LightIndexView implements CandidateIndexView {

    private final ScopeResolver scopes;
    private final List<Slot> slots;
    private final Map<String, Integer> slotIndex = new HashMap<>();
    private final String[] taskIds;
    private final String[] resourceIds;
    private final Horizon horizon;
    private final PlanLimitsProperties limits;

    /**
     * Creates the view.
     *
     * @param problem the (normalized or raw) problem
     * @param limits  the configured limits
     */
    public LightIndexView(PlanningProblem problem, PlanLimitsProperties limits) {
        this(problem, new ScopeResolver(problem.tasks(), problem.resources()), limits);
    }

    /**
     * Creates the view sharing an existing resolver.
     *
     * @param problem the problem
     * @param scopes  a resolver built from the problem tasks and resources
     * @param limits  the configured limits
     */
    public LightIndexView(PlanningProblem problem, ScopeResolver scopes, PlanLimitsProperties limits) {
        this.scopes = scopes;
        this.slots = problem.slots();
        this.horizon = problem.horizon();
        this.limits = limits;
        this.taskIds = new String[problem.tasks().size()];
        for (int t = 0; t < taskIds.length; t++) {
            taskIds[t] = problem.tasks().get(t).id();
        }
        this.resourceIds = new String[problem.resources().size()];
        for (int r = 0; r < resourceIds.length; r++) {
            resourceIds[r] = problem.resources().get(r).id();
        }
        for (int s = 0; s < slots.size(); s++) {
            if (slots.get(s).id() != null) {
                slotIndex.putIfAbsent(slots.get(s).id(), s);
            }
        }
    }

    @Override
    public int taskCount() {
        return taskIds.length;
    }

    @Override
    public int resourceCount() {
        return resourceIds.length;
    }

    @Override
    public int slotCount() {
        return slots.size();
    }

    @Override
    public Horizon horizon() {
        return horizon;
    }

    @Override
    public int horizonTicks() {
        return (int) horizon.horizonTicks();
    }

    @Override
    public PlanLimitsProperties limits() {
        return limits;
    }

    @Override
    public String taskId(int t) {
        return taskIds[t];
    }

    @Override
    public String resourceId(int r) {
        return resourceIds[r];
    }

    @Override
    public String slotId(int s) {
        return slots.get(s).id();
    }

    @Override
    public int taskIdx(String id) {
        return scopes.taskIdx(id);
    }

    @Override
    public int resourceIdx(String id) {
        return scopes.resourceIdx(id);
    }

    @Override
    public int slotIdx(String id) {
        Integer i = id == null ? null : slotIndex.get(id);
        return i == null ? -1 : i;
    }

    @Override
    public int[] resolveTasks(ConstraintScope scope) {
        return scopes.resolveTasks(scope);
    }

    @Override
    public int[] resolveResources(ConstraintScope scope) {
        return scopes.resolveResources(scope);
    }

    @Override
    public int[][] groupTasksByTagPrefix(String prefix) {
        return scopes.groupTasksByTagPrefix(prefix);
    }
}
