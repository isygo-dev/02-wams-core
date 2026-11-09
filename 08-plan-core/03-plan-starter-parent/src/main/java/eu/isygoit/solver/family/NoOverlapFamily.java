package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.NoOverlapParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.spi.ViolationSink;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Family {@code no_overlap} (design 4.3 / 6.5.3): the tasks of a group never overlap in time, whatever their
 * resources (class / patient conflicts, mutual exclusion), with an optional {@code minGapTicks} between them.
 * <p>
 * Groups: {@code scope.kind = tag} gives ONE group (the tasks carrying the tag, {@code groupByTagPrefix} must then be
 * absent); {@code scope.kind = all} together with {@code groupByTagPrefix} gives one group per task tag starting with the
 * prefix (a task with two matching tags belongs to two groups and its pairs are counted per group).
 * <p>
 * Semantics: for two selected tasks i, j of a group, {@code end_i + gap <= start_j} or {@code end_j + gap <= start_i}.
 * A task whose size is zero and whose gap is zero occupies nothing and is ignored (same rule in the global constraint,
 * the pairwise fallback and the checker). SOFT unit: one overlapping pair per group (measure 1). Stateless and
 * thread-safe; units are ticks.
 */
@Component
public class NoOverlapFamily implements ConstraintFamily {

    /** The family id. */
    public static final String TYPE = "no_overlap";

    private static final Set<IEnumEnforcement.Types> SUPPORTED =
            Collections.unmodifiableSet(EnumSet.allOf(IEnumEnforcement.Types.class));
    private static final ConstraintChecker CHECKER = new Checker();

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return NoOverlapParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return SUPPORTED;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.no_overlap.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("groupByTagPrefix", "minGapTicks");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.GROUPS;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof NoOverlapParams p)) {
            return;
        }
        int gap = p.minGapTicks();
        if (gap < 0 || gap > ctx.problem().horizon().horizonTicks()) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, "minGapTicks"), "minGapTicks must be within [0, horizon ticks]");
        }
        String kind = constraint.scope().kind();
        boolean hasPrefix = p.groupByTagPrefix() != null && !p.groupByTagPrefix().isBlank();
        if ("tag".equals(kind)) {
            if (constraint.scope().value() == null || constraint.scope().value().isBlank()) {
                ctx.error("INVALID_SCOPE", "constraints[" + indexOf(ctx, constraint) + "].scope", "scope.value (the tag) is required");
            }
            if (hasPrefix) {
                ctx.error("PARAMS_INVALID", path(ctx, constraint, "groupByTagPrefix"),
                        "groupByTagPrefix must be omitted when scope.kind is 'tag' (the scope tag defines the single group)");
            }
        } else if ("all".equals(kind)) {
            if (!hasPrefix) {
                ctx.error("PARAMS_INVALID", path(ctx, constraint, "groupByTagPrefix"),
                        "groupByTagPrefix is required when scope.kind is 'all'");
            }
        } else {
            ctx.error("INVALID_SCOPE", "constraints[" + indexOf(ctx, constraint) + "].scope.kind",
                    "no_overlap accepts scope.kind 'tag' or 'all' only");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        if (!(params instanceof NoOverlapParams p)) {
            return ModelCost.ZERO;
        }
        boolean hard = constraint.enforcement() == IEnumEnforcement.Types.STRICTLY_REQUIRED
                || constraint.enforcement() == IEnumEnforcement.Types.CONFIGURABLE_HARD;
        int gap = p.minGapTicks();
        long groupsCount = 0;
        long members = 0;
        long pairs = 0;
        for (int[] g : groups(index, constraint, p)) {
            groupsCount++;
            members += g.length;
            if (!hard) {
                pairs += countConflictPairs(index, g, gap);
            }
        }
        if (hard) {
            // global NoOverlap per group; the optional inflated intervals exist only when gap > 0
            return new ModelCost(0, 0, groupsCount, gap > 0 ? members : 0, 0, 0);
        }
        long soft = constraint.enforcement() == IEnumEnforcement.Types.SOFT ? pairs : 0;
        return new ModelCost(2 * pairs + soft, 0, 3 * pairs, 0, 0, pairs);
    }

    @Override
    public ConstraintChecker checker() {
        return CHECKER;
    }

    // ------------------------------------------------------------------ shared semantics

    /**
     * Resolves the task groups of the constraint (groups with fewer than two members are dropped).
     *
     * @param index      the index view (scope resolution only)
     * @param constraint the constraint
     * @param p          its params
     * @return groups of task indices, ascending inside a group
     */
    public static int[][] groups(CandidateIndexView index, Constraint constraint, NoOverlapParams p) {
        List<int[]> result = new ArrayList<>();
        if ("tag".equals(constraint.scope().kind())) {
            int[] g = index.resolveTasks(constraint.scope());
            if (g.length >= 2) {
                result.add(g);
            }
        } else if (p.groupByTagPrefix() != null && !p.groupByTagPrefix().isBlank()) {
            for (int[] g : index.groupTasksByTagPrefix(p.groupByTagPrefix())) {
                if (g.length >= 2) {
                    result.add(g);
                }
            }
        }
        return result.toArray(new int[0][]);
    }

    /** @return the largest duration the task can have over its candidates (the base duration when it has none) */
    public static long maxDuration(CandidateIndexView index, int task) {
        long max = -1;
        for (int c = index.candidateBegin(task); c < index.candidateEnd(task); c++) {
            max = Math.max(max, index.candidateDuration(c));
        }
        return max < 0 ? index.duration(task) : max;
    }

    /** @return {@code true} when the task can never occupy time: gap 0 and every candidate duration 0 */
    public static boolean zeroSize(CandidateIndexView index, int task, int gap) {
        return gap == 0 && maxDuration(index, task) == 0;
    }

    /**
     * Cheap static test: {@code false} when the domains guarantee the two tasks are separated whatever the solver
     * decides, so that no pair literal is needed (and the checker can never find a violation).
     */
    public static boolean mayConflict(CandidateIndexView index, int i, int j, int gap, long maxDurI, long maxDurJ) {
        return !((long) index.lstStart(i) + maxDurI + gap <= index.estStart(j)
                || (long) index.lstStart(j) + maxDurJ + gap <= index.estStart(i));
    }

    /** @return the number of pairs of the group for which {@link #mayConflict} holds (zero-size tasks excluded) */
    public static long countConflictPairs(CandidateIndexView index, int[] group, int gap) {
        long[] maxDur = new long[group.length];
        boolean[] skip = new boolean[group.length];
        for (int a = 0; a < group.length; a++) {
            maxDur[a] = maxDuration(index, group[a]);
            skip[a] = gap == 0 && maxDur[a] == 0;
        }
        long pairs = 0;
        for (int a = 0; a < group.length; a++) {
            if (skip[a]) {
                continue;
            }
            for (int b = a + 1; b < group.length; b++) {
                if (!skip[b] && mayConflict(index, group[a], group[b], gap, maxDur[a], maxDur[b])) {
                    pairs++;
                }
            }
        }
        return pairs;
    }

    private static int indexOf(ValidationContext ctx, Constraint constraint) {
        return ctx.problem().constraints().indexOf(constraint);
    }

    private static String path(ValidationContext ctx, Constraint constraint, String field) {
        int i = indexOf(ctx, constraint);
        return "constraints[" + (i < 0 ? "?" : String.valueOf(i)) + "].params." + field;
    }

    // ------------------------------------------------------------------ checker

    private static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            if (!(params instanceof NoOverlapParams p)) {
                return;
            }
            CandidateIndexView idx = ctx.index();
            SolutionView sol = ctx.solution();
            int gap = p.minGapTicks();
            for (int[] group : groups(idx, constraint, p)) {
                List<Integer> members = new ArrayList<>();
                for (int t : group) {
                    if (sol.selected(t) && (gap > 0 || sol.end(t) > sol.start(t))) {
                        members.add(t);
                    }
                }
                members.sort((x, y) -> {
                    int c = Integer.compare(sol.start(x), sol.start(y));
                    return c != 0 ? c : Integer.compare(x, y);
                });
                for (int a = 0; a < members.size(); a++) {
                    int ta = members.get(a);
                    long limit = (long) sol.end(ta) + gap;
                    for (int b = a + 1; b < members.size() && sol.start(members.get(b)) < limit; b++) {
                        int tb = members.get(b);
                        sink.violation(idx.taskId(ta), null, "TASKS_OVERLAP",
                                "Tasks " + idx.taskId(ta) + " and " + idx.taskId(tb) + " overlap (minimum gap " + gap + " ticks)", 1);
                    }
                }
            }
        }
    }
}
