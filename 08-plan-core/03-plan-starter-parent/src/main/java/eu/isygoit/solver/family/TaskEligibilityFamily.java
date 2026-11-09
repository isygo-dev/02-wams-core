package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.params.TaskEligibilityParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.PreprocessContext;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.spi.ViolationSink;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Family {@code task_eligibility} (design 4.3 / 6.5.1): scoped tasks may only use resources and start slots that match
 * the params (resource ids, excluded ids, resource types, skills, slot ids).
 * <p>
 * Semantics (shared by preprocess, the CP-SAT contributor and the checker, so that model value == checker value):
 * <ul>
 *   <li>A resource is <b>allowed</b> when it matches ALL non-empty criteria: id in {@code resourceIds} (the literal
 *   {@code "ALL"} matches everything), type in {@code resourceTypeIds}, owns every skill of {@code skills}, and is not
 *   in {@code excludeResourceIds}.</li>
 *   <li>When {@code slotIds} is non-empty (and has no {@code "ALL"}), the task START tick must equal the start tick of
 *   one of the listed slots.</li>
 *   <li>SOFT unit: one assigned (task, resource) pair with a not-allowed resource (measure 1), plus one per task whose
 *   start is not an allowed slot start (measure 1).</li>
 * </ul>
 * HARD constraints are fully handled by {@link #preprocess} (candidate pruning and start-domain restriction). Stateless
 * and thread-safe; scope = tasks; units are ticks.
 */
@Component
public class TaskEligibilityFamily implements ConstraintFamily {

    /** The family id. */
    public static final String TYPE = "task_eligibility";
    /** Literal accepted in {@code resourceIds} / {@code slotIds} meaning "no restriction". */
    public static final String ALL = "ALL";

    private static final Set<IEnumEnforcement.Types> SUPPORTED =
            Collections.unmodifiableSet(EnumSet.allOf(IEnumEnforcement.Types.class));
    private static final ConstraintChecker CHECKER = new Checker();

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return TaskEligibilityParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return SUPPORTED;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.task_eligibility.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("resourceIds", "excludeResourceIds", "resourceTypeIds", "skills", "slotIds");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.TASKS;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof TaskEligibilityParams p)) {
            return;
        }
        boolean any = !p.resourceIds().isEmpty() || !p.excludeResourceIds().isEmpty() || !p.resourceTypeIds().isEmpty()
                || !p.skills().isEmpty() || !p.slotIds().isEmpty();
        if (!any) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, null),
                    "task_eligibility needs at least one of resourceIds, excludeResourceIds, resourceTypeIds, skills, slotIds");
        }
        for (int k = 0; k < p.resourceIds().size(); k++) {
            String id = p.resourceIds().get(k);
            if (!ALL.equals(id) && !ctx.hasResource(id)) {
                ctx.error("UNKNOWN_REFERENCE", path(ctx, constraint, "resourceIds[" + k + "]"), "Unknown resource id '" + id + "'");
            }
        }
        for (int k = 0; k < p.excludeResourceIds().size(); k++) {
            String id = p.excludeResourceIds().get(k);
            if (!ctx.hasResource(id)) {
                ctx.error("UNKNOWN_REFERENCE", path(ctx, constraint, "excludeResourceIds[" + k + "]"),
                        "Unknown resource id '" + id + "'");
            }
        }
        for (int k = 0; k < p.slotIds().size(); k++) {
            String id = p.slotIds().get(k);
            if (!ALL.equals(id) && !ctx.hasSlot(id)) {
                ctx.error("UNKNOWN_REFERENCE", path(ctx, constraint, "slotIds[" + k + "]"), "Unknown slot id '" + id + "'");
            }
        }
        if (!p.resourceTypeIds().isEmpty()) {
            Set<String> known = new HashSet<>();
            for (Resource r : ctx.problem().resources()) {
                known.add(r.typeId());
            }
            for (int k = 0; k < p.resourceTypeIds().size(); k++) {
                if (!known.contains(p.resourceTypeIds().get(k))) {
                    ctx.warning("UNKNOWN_REFERENCE", path(ctx, constraint, "resourceTypeIds[" + k + "]"),
                            "No resource has type '" + p.resourceTypeIds().get(k) + "'");
                }
            }
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        if (!(params instanceof TaskEligibilityParams) || isHard(constraint)) {
            return ModelCost.ZERO; // HARD is a pure preprocess prune
        }
        long candidates = 0;
        int[] tasks = index.resolveTasks(constraint.scope());
        for (int t : tasks) {
            candidates += index.candidateEnd(t) - index.candidateBegin(t);
        }
        long instances = candidates + tasks.length; // upper bound: every candidate / one slot restriction per task
        long soft = constraint.enforcement() == IEnumEnforcement.Types.SOFT ? instances : 0;
        return new ModelCost(soft + tasks.length, 0, instances, 0, 0, 0);
    }

    @Override
    public void preprocess(PreprocessContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof TaskEligibilityParams p)) {
            return;
        }
        PlanningProblem problem = ctx.candidates().problem(); // needs from WP2: Mutable.problem()
        boolean[] allowed = allowedResources(problem, p);
        int[] starts = allowedSlotStarts(problem, p);
        int[] runs = null;
        if (starts != null) {
            runs = new int[starts.length * 2];
            for (int k = 0; k < starts.length; k++) {
                runs[2 * k] = starts[k];
                runs[2 * k + 1] = starts[k];
            }
        }
        for (int t : ctx.resolveTasks(constraint.scope())) {
            // needs from WP2: Mutable.candidateBegin/candidateEnd/candidateResource
            for (int c = ctx.candidates().candidateBegin(t); c < ctx.candidates().candidateEnd(t); c++) {
                if (!allowed[ctx.candidates().candidateResource(c)]) {
                    ctx.pruneCandidate(c, "ELIGIBILITY", constraint.id());
                }
            }
            if (runs != null) {
                ctx.restrictStartDomain(t, runs);
            }
        }
    }

    @Override
    public ConstraintChecker checker() {
        return CHECKER;
    }

    // ------------------------------------------------------------------ shared semantics

    /**
     * Evaluates the resource criteria once.
     *
     * @param problem the problem (resources in index order)
     * @param p       the params
     * @return {@code allowed[r]} for every resource index
     */
    public static boolean[] allowedResources(PlanningProblem problem, TaskEligibilityParams p) {
        List<Resource> resources = problem.resources();
        boolean[] allowed = new boolean[resources.size()];
        Set<String> ids = new HashSet<>(p.resourceIds());
        boolean anyId = ids.isEmpty() || ids.contains(ALL);
        Set<String> types = new HashSet<>(p.resourceTypeIds());
        Set<String> excluded = new HashSet<>(p.excludeResourceIds());
        for (int r = 0; r < allowed.length; r++) {
            Resource res = resources.get(r);
            boolean ok = anyId || ids.contains(res.id());
            if (ok && !types.isEmpty()) {
                ok = types.contains(res.typeId());
            }
            if (ok && !p.skills().isEmpty()) {
                ok = res.skills().containsAll(p.skills());
            }
            if (ok && excluded.contains(res.id())) {
                ok = false;
            }
            allowed[r] = ok;
        }
        return allowed;
    }

    /**
     * Start ticks of the allowed slots.
     *
     * @return ascending distinct start ticks, or {@code null} when {@code slotIds} imposes no restriction
     */
    public static int[] allowedSlotStarts(PlanningProblem problem, TaskEligibilityParams p) {
        if (p.slotIds().isEmpty() || p.slotIds().contains(ALL)) {
            return null;
        }
        Set<String> ids = new HashSet<>(p.slotIds());
        TreeSet<Integer> starts = new TreeSet<>();
        for (Slot slot : problem.slots()) {
            if (ids.contains(slot.id())) {
                starts.add(problem.horizon().floorTick(slot.start()));
            }
        }
        int[] result = new int[starts.size()];
        int k = 0;
        for (int s : starts) {
            result[k++] = s;
        }
        return result;
    }

    private static boolean isHard(Constraint constraint) {
        return constraint.enforcement() == IEnumEnforcement.Types.STRICTLY_REQUIRED
                || constraint.enforcement() == IEnumEnforcement.Types.CONFIGURABLE_HARD;
    }

    private static String path(ValidationContext ctx, Constraint constraint, String field) {
        int i = ctx.problem().constraints().indexOf(constraint);
        String base = "constraints[" + (i < 0 ? "?" : String.valueOf(i)) + "].params";
        return field == null ? base : base + "." + field;
    }

    // ------------------------------------------------------------------ checker

    private static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            if (!(params instanceof TaskEligibilityParams p)) {
                return;
            }
            CandidateIndexView idx = ctx.index();
            SolutionView sol = ctx.solution();
            boolean[] allowed = allowedResources(ctx.problem(), p);
            int[] starts = allowedSlotStarts(ctx.problem(), p);
            for (int t : idx.resolveTasks(constraint.scope())) {
                if (!sol.selected(t)) {
                    continue;
                }
                for (int k = 0; k < sol.resourceCount(t); k++) {
                    int r = sol.resourceAt(t, k);
                    if (!allowed[r]) {
                        sink.violation(idx.taskId(t), idx.resourceId(r), "RESOURCE_NOT_ELIGIBLE",
                                "Task " + idx.taskId(t) + " uses resource " + idx.resourceId(r) + " which is not eligible", 1);
                    }
                }
                if (starts != null && Arrays.binarySearch(starts, sol.start(t)) < 0) {
                    sink.violation(idx.taskId(t), null, "SLOT_NOT_ALLOWED",
                            "Task " + idx.taskId(t) + " starts at tick " + sol.start(t) + " which is not an allowed slot start", 1);
                }
            }
        }
    }
}
