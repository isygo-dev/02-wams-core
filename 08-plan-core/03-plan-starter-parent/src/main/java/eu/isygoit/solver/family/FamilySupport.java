package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.params.PeriodSpec;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.PeriodBuckets;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Shared, stateless helpers of the WP4b constraint families and their CP-SAT contributors: enforcement sets, validation
 * paths, scope grouping, candidate reachability per period bucket and the pair enumeration of {@code min_rest}.
 * <p>
 * Everything the model builder and the independent checker must agree on lives here (one implementation, two callers), so
 * that "model value == checker value" holds by construction. Pure functions, thread-safe. Units: ticks (tick 0 = horizon
 * start), start domains are the inclusive {@code [estStart, lstStart]} of {@link CandidateIndexView}.
 */
public final class FamilySupport {

    /** HARD only (STRICTLY_REQUIRED, CONFIGURABLE_HARD). */
    public static final Set<IEnumEnforcement.Types> HARD_ONLY = Set.of(
            IEnumEnforcement.Types.STRICTLY_REQUIRED, IEnumEnforcement.Types.CONFIGURABLE_HARD);

    /** HARD and RELAXABLE_WITH_AUTHORIZATION (no soft form). */
    public static final Set<IEnumEnforcement.Types> HARD_RELAXABLE = Set.of(
            IEnumEnforcement.Types.STRICTLY_REQUIRED, IEnumEnforcement.Types.CONFIGURABLE_HARD,
            IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION);

    /** HARD, SOFT and RELAXABLE_WITH_AUTHORIZATION. */
    public static final Set<IEnumEnforcement.Types> ALL_ENFORCEMENTS = Set.of(
            IEnumEnforcement.Types.STRICTLY_REQUIRED, IEnumEnforcement.Types.CONFIGURABLE_HARD,
            IEnumEnforcement.Types.SOFT, IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION);

    private FamilySupport() {
    }

    // ------------------------------------------------------------------ validation helpers

    /**
     * @param ctx        validation context
     * @param constraint the constraint
     * @param suffix     path below the constraint, for example {@code "params.period"}; may be null
     * @return a JSON path such as {@code constraints[2].params.period}
     */
    public static String path(ValidationContext ctx, Constraint constraint, String suffix) {
        List<Constraint> all = ctx.problem().constraints();
        int found = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) == constraint) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            for (int i = 0; i < all.size(); i++) {
                if (java.util.Objects.equals(all.get(i).id(), constraint.id())) {
                    found = i;
                    break;
                }
            }
        }
        String base = found >= 0 ? "constraints[" + found + "]" : "constraints[" + constraint.id() + "]";
        return suffix == null || suffix.isEmpty() ? base : base + "." + suffix;
    }

    /**
     * Validates a {@link PeriodSpec}; reports MISSING_REQUIRED_FIELD / PARAMS_INVALID.
     *
     * @param required report an error when the spec is null
     * @return true when the spec is usable
     */
    public static boolean validatePeriod(ValidationContext ctx, Constraint constraint, PeriodSpec period, boolean required) {
        if (period == null) {
            if (required) {
                ctx.error("MISSING_REQUIRED_FIELD", path(ctx, constraint, "params.period"),
                        "params.period is required");
            }
            return !required;
        }
        if (period.unit() == null) {
            ctx.error("MISSING_REQUIRED_FIELD", path(ctx, constraint, "params.period.unit"),
                    "params.period.unit is required (TICKS, DAY or WEEK)");
            return false;
        }
        if (period.unit() == PeriodSpec.Unit.TICKS && (period.ticks() == null || period.ticks() < 1)) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, "params.period.ticks"),
                    "params.period.ticks must be >= 1 when unit is TICKS");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ zone and scope

    /**
     * @param problem the problem
     * @return the zone of {@code meta.timezone}; UTC when blank or invalid
     */
    public static ZoneId zone(PlanningProblem problem) {
        String tz = problem.timezone();
        if (tz == null || tz.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(tz);
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }

    /**
     * Task groups of a grouping family ({@code same_resource}, {@code different_resource}): one group per task tag starting
     * with the prefix, or ONE group = the scope's tasks when no prefix is given. Groups have at least two members, ascending.
     *
     * @param index  index view
     * @param scope  the constraint scope (restricts the members)
     * @param prefix {@code groupByTagPrefix}, may be null
     * @return the groups
     */
    public static int[][] groups(CandidateIndexView index, ConstraintScope scope, String prefix) {
        List<int[]> out = new ArrayList<>();
        if (prefix != null && !prefix.isBlank()) {
            int[][] raw = index.groupTasksByTagPrefix(prefix);
            boolean restrict = scope != null && !ConstraintScope.ALL.equals(scope.kind());
            boolean[] allowed = restrict ? flags(index.taskCount(), index.resolveTasks(scope)) : null;
            for (int[] g : raw) {
                int[] members = Arrays.stream(g).filter(t -> allowed == null || allowed[t]).sorted().distinct().toArray();
                if (members.length >= 2) {
                    out.add(members);
                }
            }
        } else {
            int[] members = Arrays.stream(index.resolveTasks(scope == null ? ConstraintScope.all() : scope))
                    .sorted().distinct().toArray();
            if (members.length >= 2) {
                out.add(members);
            }
        }
        return out.toArray(new int[0][]);
    }

    /**
     * @param n       size
     * @param members indices to flag
     * @return a boolean array of length n, true at the members
     */
    public static boolean[] flags(int n, int[] members) {
        boolean[] f = new boolean[n];
        for (int m : members) {
            if (m >= 0 && m < n) {
                f[m] = true;
            }
        }
        return f;
    }

    /**
     * Whether a task is selected by a TASKS scope, evaluated on the raw task (used by the task rewriter, which runs before
     * any index exists). {@code tag} matches task tags, {@code taskType} the type id, {@code task} the id.
     */
    public static boolean scopeIncludes(ConstraintScope scope, Task task) {
        if (scope == null || ConstraintScope.ALL.equals(scope.kind())) {
            return true;
        }
        String v = scope.value();
        return switch (scope.kind()) {
            case "task" -> v != null && v.equals(task.id());
            case "taskType" -> v != null && v.equals(task.typeId());
            case "tag" -> v != null && task.tags().contains(v);
            default -> false;
        };
    }

    // ------------------------------------------------------------------ solution helpers

    /**
     * @param solution solution view
     * @param task     task index
     * @param role     role filter, null = all roles
     * @return the resource indices assigned to the task for that role, in assignment order
     */
    public static int[] resourcesOf(SolutionView solution, int task, String role) {
        int n = solution.resourceCount(task);
        int[] tmp = new int[n];
        int size = 0;
        for (int k = 0; k < n; k++) {
            if (role == null || role.equals(solution.roleAt(task, k))) {
                tmp[size++] = solution.resourceAt(task, k);
            }
        }
        return Arrays.copyOf(tmp, size);
    }

    /** @return true when the sorted-or-not array contains the value */
    public static boolean contains(int[] values, int value) {
        for (int v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ candidates and buckets

    /**
     * @param index index view
     * @return candidate ids per resource (ascending candidate id), built in O(C)
     */
    public static int[][] candidatesByResource(CandidateIndexView index) {
        int resources = index.resourceCount();
        int total = index.candidateCount();
        int[] count = new int[resources];
        for (int c = 0; c < total; c++) {
            count[index.candidateResource(c)]++;
        }
        int[][] out = new int[resources][];
        for (int r = 0; r < resources; r++) {
            out[r] = new int[count[r]];
            count[r] = 0;
        }
        for (int c = 0; c < total; c++) {
            int r = index.candidateResource(c);
            out[r][count[r]++] = c;
        }
        return out;
    }

    /**
     * Buckets reachable per resource: bucket k is reachable by resource r when some candidate of r belongs to a task whose
     * start domain {@code [estStart, lstStart]} intersects the bucket. Model and checker both define "applicable bucket" (for
     * {@code min} bounds) with this function.
     *
     * @param index     index view
     * @param resources the scoped resources
     * @param lo        bucket lower bounds
     * @param hi        bucket upper bounds
     * @return resource index to reachable buckets (resources without any reachable bucket are absent)
     */
    public static Map<Integer, BitSet> reachableBuckets(CandidateIndexView index, int[] resources, int[] lo, int[] hi) {
        boolean[] inScope = flags(index.resourceCount(), resources);
        Map<Integer, BitSet> out = new TreeMap<>();
        int total = index.candidateCount();
        for (int c = 0; c < total; c++) {
            int r = index.candidateResource(c);
            if (!inScope[r]) {
                continue;
            }
            int t = index.candidateTask(c);
            int[] range = PeriodBuckets.bucketRange(lo, hi, index.estStart(t), index.lstStart(t));
            if (range == null) {
                continue;
            }
            out.computeIfAbsent(r, k -> new BitSet()).set(range[0], range[1] + 1);
        }
        return out;
    }

    /**
     * Size of the windowed model of a family, for {@code estimate}: how many (candidate, bucket) product literals and how many
     * applicable (resource, bucket) pairs the contributor creates.
     *
     * @param index     index view
     * @param resources the scoped resources
     * @param lo        bucket lower bounds
     * @param hi        bucket upper bounds
     * @return {@code {productLiterals, resourceBucketPairs}}
     */
    public static long[] windowedSize(CandidateIndexView index, int[] resources, int[] lo, int[] hi) {
        boolean[] inScope = flags(index.resourceCount(), resources);
        long products = 0;
        int total = index.candidateCount();
        for (int c = 0; c < total; c++) {
            if (!inScope[index.candidateResource(c)]) {
                continue;
            }
            int t = index.candidateTask(c);
            int[] range = PeriodBuckets.bucketRange(lo, hi, index.estStart(t), index.lstStart(t));
            if (range != null) {
                products += range[1] - range[0] + 1L;
            }
        }
        long pairs = 0;
        for (BitSet b : reachableBuckets(index, resources, lo, hi).values()) {
            pairs += b.cardinality();
        }
        return new long[]{products, pairs};
    }

    /**
     * Bucket count of a period for estimates (DAY / WEEK counted in UTC, which is within one bucket of the real count).
     *
     * @return the bucket count, 0 when the spec is unusable
     */
    public static int bucketCount(CandidateIndexView index, PeriodSpec period) {
        try {
            return PeriodBuckets.tile(index.horizon(), ZoneId.of("UTC"), period)[0].length;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ min_rest pair enumeration

    /**
     * Pairs of candidates of ONE resource that can violate the minimum rest: sizes are the inflated sizes
     * {@code candidateDuration + rest (only for tasks flagged in afterTask)}, a candidate of size 0 never conflicts, and two
     * candidates of the same task never conflict. A pair is possible iff the start domains allow
     * {@code s_i + size_i > s_j} and {@code s_j + size_j > s_i}. Enumeration is sorted by earliest start and stops early, so
     * it costs O(n log n + examined pairs).
     *
     * @param index     index view
     * @param cands     candidate ids of the resource
     * @param afterTask per task flag "followed by rest"; null = every task
     * @param rest      minimum rest in ticks
     * @param cap       stop after this many pairs (the result then has more than {@code cap} pairs)
     * @return flat array {@code [ci0, cj0, ci1, cj1, ...]}, {@code ci} is the candidate with the earlier earliest start
     */
    public static int[] conflictPairs(CandidateIndexView index, int[] cands, boolean[] afterTask, int rest, long cap) {
        int n = 0;
        int[] order = new int[cands.length];
        int[] size = new int[cands.length];
        int[] est = new int[cands.length];
        int[] lst = new int[cands.length];
        Integer[] boxed = new Integer[cands.length];
        for (int k = 0; k < cands.length; k++) {
            int c = cands[k];
            int t = index.candidateTask(c);
            int s = index.candidateDuration(c) + (afterTask == null || afterTask[t] ? rest : 0);
            if (s > 0 && index.estStart(t) <= index.lstStart(t)) {
                boxed[n++] = k;
            }
            size[k] = s;
            est[k] = index.estStart(t);
            lst[k] = index.lstStart(t);
        }
        Integer[] active = Arrays.copyOf(boxed, n);
        Arrays.sort(active, (a, b) -> {
            int d = Integer.compare(est[a], est[b]);
            return d != 0 ? d : Integer.compare(cands[a], cands[b]);
        });
        for (int k = 0; k < n; k++) {
            order[k] = active[k];
        }
        int[] out = new int[16];
        int len = 0;
        long pairs = 0;
        for (int a = 0; a < n; a++) {
            int i = order[a];
            for (int b = a + 1; b < n; b++) {
                int j = order[b];
                if ((long) est[j] - lst[i] >= size[i]) {
                    break;
                }
                if (index.candidateTask(cands[i]) == index.candidateTask(cands[j])) {
                    continue;
                }
                if (len + 2 > out.length) {
                    out = Arrays.copyOf(out, out.length * 2);
                }
                out[len++] = cands[i];
                out[len++] = cands[j];
                pairs++;
                if (pairs > cap) {
                    return Arrays.copyOf(out, len);
                }
            }
        }
        return Arrays.copyOf(out, len);
    }
}
