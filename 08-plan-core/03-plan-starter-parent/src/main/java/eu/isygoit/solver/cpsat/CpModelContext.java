package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpModelProto;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.PeriodSpec;
import eu.isygoit.dto.response.ModelStats;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.solver.SolveCancelledException;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.index.CandidateIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a contributor may touch while one CP-SAT model is built (design 5.5 / 6.2): the model, the int-indexed
 * variable arrays created up front by {@link CpModelBuilder}, lazily created shared helpers (intervals, start choices,
 * buckets, load expressions, product literals, resource blocks, setup arcs) and the guards.
 * <p>
 * <b>Lifecycle.</b> One instance per build, created by {@code CpModelBuilder}. All accessors are O(1) array reads except the
 * lazy helpers, which create their variables on first use and cache them (so contributors may call them repeatedly).
 * <b>Thread-safety.</b> Not thread-safe: used by the single building thread only; after the build the context is read-only
 * (the solution extractor and hint applier only read it).
 * <b>Units.</b> Ticks (int) everywhere; literals and expressions belong to {@link #model()}.
 * <p>
 * <b>Variable semantics.</b>
 * <ul>
 *   <li>{@link #start(int)}: the start tick variable; its domain is the union of the allowed starts of the candidates (and
 *   the slot starts in slot mode); in EXPLAIN mode {@code [0, horizonTicks]} (restrictions are enforced by assumptions).</li>
 *   <li>{@link #presence(int)}: {@link #trueLit()} for required tasks (compare with {@code ==} to detect constants).</li>
 *   <li>{@link #cand(int)}: the literal "candidate {@code c} is used"; it may ALIAS {@link #presence(int)} or
 *   {@link #trueLit()} when the requirement leaves no alternative (design 6.2).</li>
 *   <li>{@link #end(int)}: the affine expression {@code start + duration} (no variable); with resource specific durations the
 *   duration is an IntVar and the expression is {@code start + dur}.</li>
 * </ul>
 * OR-TOOLS-VERIFY: every OR-Tools call was checked against the 9.15.6755 jar signatures (javap).
 */
public final class CpModelContext {

    private static final int CHECKPOINT_INTERVAL = 1024;

    // ------------------------------------------------------------------------------ package-private build state
    final CpModel model;
    final CompiledProblem compiled;
    final CandidateIndex index;
    final SolveContext solveContext;
    final BuildMode mode;
    final CpSatProperties properties;
    final PlanLimitsProperties limits;
    final AssumptionRegistry assumptions;
    final boolean named;
    final SolverCancellationToken token;
    final int maxSeconds;

    BoolVar trueLit;
    IntVar[] start;
    Literal[] presence;
    Literal[] cand;
    IntVar[] durVar;
    int[] commonDuration;
    LinearExpr[] endExpr;
    long[][] startDomain;
    long[][] unionRuns;
    long[][] slotValues;
    int[][] slotOfValue;
    Literal[][] startChoices;
    int softLiterals;
    int relaxLiterals;

    private int boolVars;
    private int intVars;
    private int constraints;
    private int intervals;
    private int circuitArcs;

    private IntervalVar[] masterIntervals;
    private IntervalVar[] candIntervals;
    private final Map<String, BucketGrid> grids = new HashMap<>();
    private final Map<String, Literal[][]> startBuckets = new HashMap<>();
    private LinearExpr[][] loads;
    private Literal[][] used;
    private final Map<Long, Literal> products = new HashMap<>();
    private final Map<Integer, List<Block>> blocks = new HashMap<>();
    private final Map<Integer, List<SetupArc>> setupArcs = new HashMap<>();

    /** A fixed interval of {@code demand} capacity that families add to a resource ({@link #blockResource}). */
    record Block(int lo, int hi, int demand, Literal presence) {
    }

    CpModelContext(CompiledProblem compiled, SolveContext solveContext, BuildMode mode, CpSatProperties properties) {
        this.compiled = compiled;
        this.index = compiled.index();
        this.solveContext = solveContext;
        this.mode = mode;
        this.properties = properties == null ? new CpSatProperties() : properties;
        this.limits = index.limits();
        this.model = new CpModel();
        this.assumptions = mode == BuildMode.EXPLAIN ? new AssumptionRegistry(model) : null;
        SolverOptions options = options();
        this.named = this.properties.isNamedVariables() || Boolean.TRUE.equals(options.logSearch());
        this.token = solveContext.cancellation() == null ? new SolverCancellationToken() : solveContext.cancellation();
        this.maxSeconds = options.maxTimeSeconds();
    }

    // ------------------------------------------------------------------------------------------ model + data

    /** @return the model under construction */
    public CpModel model() {
        return model;
    }

    /** @return the candidate index of the compiled problem */
    public CandidateIndex index() {
        return index;
    }

    /** @return the normalised problem */
    public PlanningProblem problem() {
        return compiled.problem();
    }

    /** @return the effective solver options (from the solve context, falling back to the problem's options) */
    public SolverOptions options() {
        SolverOptions options = solveContext.options();
        return options != null ? options : compiled.problem().options();
    }

    /** @return true when the model is built for the infeasibility explanation pass (design 6.10) */
    public boolean explainMode() {
        return mode == BuildMode.EXPLAIN;
    }

    /** @return the build mode */
    public BuildMode buildMode() {
        return mode;
    }

    /** @return the solve context of the request (cancellation token, listener, start time) */
    public SolveContext solveContext() {
        return solveContext;
    }

    /** @return the assumption registry in EXPLAIN mode, {@code null} otherwise */
    public AssumptionRegistry assumptions() {
        return assumptions;
    }

    // -------------------------------------------------------------------------------------- time variables

    /**
     * @param t task index
     * @return the start variable (domain: allowed start ticks)
     */
    public IntVar start(int t) {
        return start[t];
    }

    /**
     * @param t task index
     * @return the affine end expression {@code start + duration}
     */
    public LinearExpr end(int t) {
        return endExpr[t];
    }

    /**
     * @param t task index
     * @return {@link #trueLit()} for required tasks, otherwise the selection literal of the optional task
     */
    public Literal presence(int t) {
        return presence[t];
    }

    /**
     * @param c candidate index
     * @return the literal "candidate used" (may alias the presence literal or {@link #trueLit()})
     */
    public Literal cand(int c) {
        return cand[c];
    }

    /**
     * The first candidate of a task on a resource.
     *
     * @param t task index
     * @param r resource index
     * @return the candidate index, or -1 when the task cannot use the resource
     */
    public int candidate(int t, int r) {
        int end = index.candidateEnd(t);
        for (int c = index.candidateBegin(t); c < end; c++) {
            if (index.candidateResource(c) == r) {
                return c;
            }
        }
        return -1;
    }

    /**
     * @param c candidate index
     * @return the role name of the requirement the candidate belongs to
     */
    public String role(int c) {
        return index.roles[index.reqRole[index.candReq[c]]];
    }

    /**
     * The start domain of a task as flat inclusive runs {@code [lo0, hi0, lo1, hi1, ...]} in ticks. The returned array is
     * shared: callers MUST NOT modify it.
     *
     * @param t task index
     * @return the runs (empty when no start is possible)
     */
    public long[] startDomain(int t) {
        return startDomain[t];
    }

    /**
     * Master interval of a task (created lazily): optional when the task is optional. A required task with a constant
     * duration gets a plain fixed-size interval.
     *
     * @param t task index
     * @return the interval {@code [start(t), end(t))}
     */
    public IntervalVar masterInterval(int t) {
        if (masterIntervals == null) {
            masterIntervals = new IntervalVar[index.taskCount()];
        }
        IntervalVar interval = masterIntervals[t];
        if (interval != null) {
            return interval;
        }
        Literal p = presence[t];
        boolean required = p == trueLit;
        int duration = commonDuration[t];
        String label = named ? "master_" + t : "";
        if (duration >= 0) {
            interval = required
                    ? model.newFixedSizeIntervalVar(start[t], duration, label)
                    : model.newOptionalFixedSizeIntervalVar(start[t], duration, p, label);
        } else {
            interval = required
                    ? model.newIntervalVar(start[t], durVar[t], endExpr[t], label)
                    : model.newOptionalIntervalVar(start[t], durVar[t], endExpr[t], p, label);
        }
        masterIntervals[t] = interval;
        count(0, 0, 0, 1);
        return interval;
    }

    /**
     * Interval of a candidate (created lazily): sized with the candidate duration, sharing {@link #start(int)} with every
     * other candidate of the task, present when the candidate is used.
     *
     * @param c candidate index
     * @return the interval
     */
    public IntervalVar candInterval(int c) {
        if (candIntervals == null) {
            candIntervals = new IntervalVar[index.candidateCount()];
        }
        IntervalVar interval = candIntervals[c];
        if (interval != null) {
            return interval;
        }
        int t = index.candidateTask(c);
        Literal lit = cand[c];
        String label = named ? "cand_" + c : "";
        interval = lit == trueLit
                ? model.newFixedSizeIntervalVar(start[t], index.candidateDuration(c), label)
                : model.newOptionalFixedSizeIntervalVar(start[t], index.candidateDuration(c), lit, label);
        candIntervals[c] = interval;
        count(0, 0, 0, 1);
        return interval;
    }

    /** @return the single shared literal fixed to 1 (never rely on {@code CpModel.trueLiteral()}) */
    public Literal trueLit() {
        return trueLit;
    }

    // ------------------------------------------------------------------------------------- shared helpers

    /**
     * Slot-mode start choices: one literal per allowed start value, exactly one true when the task is present
     * ({@code sum == presence}), and {@code start == sum(value * literal)} when present.
     *
     * @param t task index
     * @return the literals, or {@code null} when the task is not in slot mode or its number of allowed starts exceeds
     * {@code app.plan.cpsat.max-start-choices} (then only {@link #startChoiceValues(int)} is available)
     */
    public Literal[] startChoice(int t) {
        return startChoices == null ? null : startChoices[t];
    }

    /**
     * @param t task index
     * @return the allowed start ticks in ascending order (parallel to {@link #startChoice}), or {@code null} when the task is
     * not in slot mode
     */
    public long[] startChoiceValues(int t) {
        return slotValues == null ? null : slotValues[t];
    }

    /**
     * @param t task index
     * @return the slot index of each value of {@link #startChoiceValues(int)}, or {@code null} when not in slot mode
     */
    public int[] startChoiceSlots(int t) {
        return slotOfValue == null ? null : slotOfValue[t];
    }

    /**
     * Literal per bucket "the task starts in bucket k", cached per (task, grid). Exactly one of them is true when the task is
     * present; a {@code null} entry marks an unreachable bucket. A task that could only start at the horizon end tick (a
     * zero-length task at {@code horizonTicks}) belongs to no bucket.
     *
     * @param t    task index
     * @param grid bucket grid from {@link #buckets(PeriodSpec)}
     * @return an array of {@code grid.size()} literals
     */
    public Literal[] startBucket(int t, BucketGrid grid) {
        Literal[][] perTask = startBuckets.computeIfAbsent(grid.id(), key -> new Literal[index.taskCount()][]);
        Literal[] cached = perTask[t];
        if (cached != null) {
            return cached;
        }
        int size = grid.size();
        Literal[] result = new Literal[size];
        long[] domain = startDomain[t];
        if (domain.length == 0) {
            perTask[t] = result;
            return result;
        }
        long domainMin = domain[0];
        long domainMax = domain[domain.length - 1];
        boolean[] reachable = new boolean[size];
        int reachableCount = 0;
        int only = -1;
        for (int k = 0; k < size; k++) {
            long lo = grid.lo()[k];
            long hi = (long) grid.hi()[k] - 1L;
            if (hi >= lo && intersects(domain, lo, hi)) {
                reachable[k] = true;
                reachableCount++;
                only = k;
            }
        }
        Literal p = presence[t];
        if (reachableCount == 1 && grid.lo()[only] <= domainMin && grid.hi()[only] - 1L >= domainMax) {
            result[only] = p;
            perTask[t] = result;
            return result;
        }
        List<Literal> literals = new ArrayList<>(reachableCount);
        for (int k = 0; k < size; k++) {
            if (!reachable[k]) {
                continue;
            }
            BoolVar bucket = model.newBoolVar(named ? "bucket_" + t + "_" + k : "");
            result[k] = bucket;
            literals.add(bucket);
            long lo = grid.lo()[k];
            long hi = (long) grid.hi()[k] - 1L;
            if (lo > domainMin) {
                model.addGreaterOrEqual(start[t], lo).onlyEnforceIf(bucket);
            }
            if (hi < domainMax) {
                model.addLessOrEqual(start[t], hi).onlyEnforceIf(bucket);
            }
        }
        if (literals.isEmpty()) {
            perTask[t] = result;
            return result;
        }
        if (p == trueLit) {
            model.addExactlyOne(literals.toArray(new Literal[0]));
        } else {
            LinearExprBuilder sum = LinearExpr.newBuilder();
            for (Literal literal : literals) {
                sum.add(literal);
            }
            sum.addTerm(p, -1);
            model.addEquality(sum.build(), 0);
        }
        count(literals.size(), 0, literals.size() * 2 + 1, 0);
        perTask[t] = result;
        return result;
    }

    /**
     * Tiles the horizon into periods (DAY / WEEK in {@code meta.timezone}, or fixed TICKS), cached per period spec.
     *
     * @param period the period spec
     * @return the grid (shared; do not modify)
     */
    public BucketGrid buckets(PeriodSpec period) {
        String key = period.unit() + ":" + period.ticks();
        BucketGrid grid = grids.get(key);
        if (grid == null) {
            grid = BucketGrids.of(compiled.problem(), period);
            grids.put(key, grid);
        }
        return grid;
    }

    /**
     * Literal "resource r is used by at least one task".
     *
     * @param r     resource index
     * @param exact {@code true}: {@code used <=> OR(cand)} (needed by symmetry breaking and cost-per-use minimisation);
     *              {@code false}: only {@code cand => used} (the solver may leave it true spuriously)
     * @return the literal; the constant false literal when the resource has no candidate
     */
    public Literal resourceUsed(int r, boolean exact) {
        if (used == null) {
            used = new Literal[index.resourceCount()][2];
        }
        int slot = exact ? 1 : 0;
        Literal cached = used[r][slot];
        if (cached != null) {
            return cached;
        }
        int[] candidates = candidatesOfResource(r);
        Literal result;
        if (candidates.length == 0) {
            result = trueLit.not();
        } else {
            BoolVar usedVar = model.newBoolVar(named ? "used_" + r + (exact ? "x" : "") : "");
            for (int c : candidates) {
                model.addImplication(cand[c], usedVar);
            }
            if (exact) {
                Literal[] clause = new Literal[candidates.length + 1];
                clause[0] = usedVar.not();
                for (int i = 0; i < candidates.length; i++) {
                    clause[i + 1] = cand[candidates[i]];
                }
                model.addBoolOr(clause);
            }
            count(1, 0, candidates.length + (exact ? 1 : 0), 0);
            result = usedVar;
        }
        used[r][slot] = result;
        return result;
    }

    /**
     * Load expression of a resource: {@code sum duration * cand} (TICKS) or {@code sum cand} (COUNT) over its candidates.
     *
     * @param r      resource index
     * @param metric TICKS or COUNT
     * @return the expression (constant 0 when the resource has no candidate); cached
     */
    public LinearExpr load(int r, LoadMetric metric) {
        if (loads == null) {
            loads = new LinearExpr[index.resourceCount()][2];
        }
        int slot = metric == LoadMetric.COUNT ? 1 : 0;
        LinearExpr cached = loads[r][slot];
        if (cached != null) {
            return cached;
        }
        LinearExprBuilder builder = LinearExpr.newBuilder();
        for (int c : candidatesOfResource(r)) {
            builder.addTerm(cand[c], metric == LoadMetric.COUNT ? 1L : index.candidateDuration(c));
        }
        LinearExpr result = builder.build();
        loads[r][slot] = result;
        return result;
    }

    /**
     * New integer variable for a measure or tier helper (counted against the size guard).
     *
     * @param name label (used only when variable naming is on)
     * @param lo   lower bound
     * @param hi   upper bound
     * @return the variable
     */
    public IntVar newTierVar(String name, long lo, long hi) {
        IntVar variable = model.newIntVar(lo, hi, named ? name : "");
        count(0, 1, 0, 0);
        return variable;
    }

    /**
     * Cached product literal {@code w = a AND b}: {@code w => a}, {@code w => b}, {@code (a AND b) => w}. These are
     * definitional constraints and must never be enforced by an {@link Enforcer} literal.
     *
     * @param a first literal
     * @param b second literal
     * @return the product literal ({@code a} or {@code b} itself when the other is constant true)
     */
    public Literal and(Literal a, Literal b) {
        if (a == trueLit || a == b) {
            return b;
        }
        if (b == trueLit) {
            return a;
        }
        int ia = a.getIndex();
        int ib = b.getIndex();
        long key = ((long) Math.min(ia, ib) << 32) | (Math.max(ia, ib) & 0xffffffffL);
        Literal cached = products.get(key);
        if (cached != null) {
            return cached;
        }
        BoolVar product = model.newBoolVar(named ? "and_" + ia + "_" + ib : "");
        model.addImplication(product, a);
        model.addImplication(product, b);
        model.addBoolOr(new Literal[]{a.not(), b.not(), product});
        count(1, 0, 3, 0);
        products.put(key, product);
        return product;
    }

    // ------------------------------------------------------------------------- resource blocks and setups

    /**
     * Adds a fixed interval of {@code demand} capacity units on a resource inside {@code [lo, hi)}; materialised in the
     * resource's NoOverlap / Cumulative at finalisation (design 6.4 step 11). With a non-null {@code enforcedBy} the block
     * is an OPTIONAL interval present exactly when the literal is true.
     *
     * @param r          resource index
     * @param lo         first blocked tick (clipped to 0)
     * @param hi         first tick after the block (clipped to the horizon)
     * @param demand     capacity units blocked, {@code >= 1}
     * @param enforcedBy presence literal, or {@code null} for an unconditional block
     */
    public void blockResource(int r, int lo, int hi, int demand, Literal enforcedBy) {
        int from = Math.max(0, lo);
        int to = Math.min(index.horizonTicks(), hi);
        if (to <= from || demand < 1) {
            return;
        }
        blocks.computeIfAbsent(r, key -> new ArrayList<>()).add(new Block(from, to, demand, enforcedBy));
    }

    /**
     * Records the setup circuit arcs of a resource (called by the setup circuit module).
     *
     * @param r    resource index
     * @param arcs the arcs with their literal and setup ticks
     */
    public void registerSetupArcs(int r, List<SetupArc> arcs) {
        circuitArcs += arcs.size();
        guard("max-setup-arcs", circuitArcs, limits.getMaxSetupArcs());
        setupArcs.computeIfAbsent(r, key -> new ArrayList<>()).addAll(arcs);
    }

    /**
     * @param r resource index
     * @return the setup arcs registered for the resource (empty when none); read by {@code minimize_setup}
     */
    public List<SetupArc> setupArcs(int r) {
        List<SetupArc> arcs = setupArcs.get(r);
        return arcs == null ? List.of() : Collections.unmodifiableList(arcs);
    }

    // ------------------------------------------------------------------------------ guards and bookkeeping

    /**
     * Cancellation / time check: throws {@link SolveCancelledException} when a stop was requested or the time budget
     * (which includes the build time) is exhausted. Call at least every 1024 iterations of a loop.
     */
    public void checkpoint() {
        SolveCancelledException.check(token, solveContext.startNanos(), maxSeconds);
    }

    /**
     * Loop helper: calls {@link #checkpoint()} every 1024th iteration.
     *
     * @param iteration the running loop counter
     */
    public void checkpoint(int iteration) {
        if ((iteration & (CHECKPOINT_INTERVAL - 1)) == 0) {
            checkpoint();
        }
    }

    /**
     * Records newly allocated model elements and checks the size guards.
     *
     * @param newBoolVars    new boolean variables
     * @param newIntVars     new integer variables
     * @param newConstraints new constraints (instances)
     * @param newIntervals   new interval variables
     * @throws LimitExceededException when {@code max-variables}, {@code max-constraint-instances} or {@code max-intervals}
     *                                is exceeded (design 6.13, model too large)
     */
    public void count(int newBoolVars, int newIntVars, int newConstraints, int newIntervals) {
        boolVars += newBoolVars;
        intVars += newIntVars;
        constraints += newConstraints;
        intervals += newIntervals;
        guard("max-variables", (long) boolVars + intVars, limits.getMaxVariables());
        guard("max-constraint-instances", constraints, limits.getMaxConstraintInstances());
        guard("max-intervals", intervals, limits.getMaxIntervals());
    }

    /**
     * Generic size guard for contributors (for example pair counts against {@code maxPairwise}).
     *
     * @param limitName the limit name (for example {@code max-pairwise})
     * @param actual    the actual value
     * @param limit     the configured limit
     * @throws LimitExceededException when {@code actual > limit}
     */
    public void guard(String limitName, long actual, long limit) {
        if (actual > limit) {
            throw new LimitExceededException(limitName, actual, limit);
        }
    }

    /**
     * Model statistics computed from the model proto (O(model size): call once, not in loops).
     *
     * @return the statistics
     */
    public ModelStats stats() {
        CpModelProto.Builder proto = model.getBuilder();
        int bools = 0;
        int ints = 0;
        for (int i = 0; i < proto.getVariablesCount(); i++) {
            var variable = proto.getVariables(i);
            if (variable.getDomainCount() == 2 && variable.getDomain(0) == 0L && variable.getDomain(1) == 1L) {
                bools++;
            } else {
                ints++;
            }
        }
        int plain = 0;
        int intervalCount = 0;
        int optional = 0;
        for (int i = 0; i < proto.getConstraintsCount(); i++) {
            var constraint = proto.getConstraints(i);
            if (constraint.hasInterval()) {
                intervalCount++;
                if (constraint.getEnforcementLiteralCount() > 0) {
                    optional++;
                }
            } else {
                plain++;
            }
        }
        return new ModelStats(index.taskCount(), index.resourceCount(), index.candidateCount(), bools, ints, plain,
                intervalCount, optional, circuitArcs, softLiterals, relaxLiterals);
    }

    // --------------------------------------------------------------------------------- package internals

    /** @return the blocks of a resource (never null) */
    List<Block> blocksOf(int r) {
        List<Block> list = blocks.get(r);
        return list == null ? List.of() : list;
    }

    /** @return the candidate ids of a resource in ascending order (a fresh array) */
    int[] candidatesOfResource(int r) {
        int from = index.resCandStart[r];
        int to = index.resCandStart[r + 1];
        int[] result = new int[to - from];
        System.arraycopy(index.resCand, from, result, 0, result.length);
        return result;
    }

    /**
     * Applies an optional enforcement to a constraint: no-op when the array is null or empty.
     *
     * @param constraint the constraint returned by a {@code CpModel.addXxx} call
     * @param literals   the enforcement literals (nulls are not allowed here; use {@link Enforcer#conditions})
     */
    public static void enforce(com.google.ortools.sat.Constraint constraint, Literal... literals) {
        if (literals != null && literals.length > 0) {
            constraint.onlyEnforceIf(literals);
        }
    }

    /** @return true when the flat inclusive runs contain a value of {@code [lo, hi]} */
    static boolean intersects(long[] runs, long lo, long hi) {
        for (int i = 0; i + 1 < runs.length; i += 2) {
            if (runs[i + 1] >= lo && runs[i] <= hi) {
                return true;
            }
            if (runs[i] > hi) {
                break;
            }
        }
        return false;
    }
}
