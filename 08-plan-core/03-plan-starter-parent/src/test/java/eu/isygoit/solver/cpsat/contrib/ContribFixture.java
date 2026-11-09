package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.PeriodSpec;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.BucketGrid;
import eu.isygoit.solver.cpsat.BucketGrids;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.cpsat.SetupArc;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.spi.ConstraintFamily;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test harness of the CP-SAT contributor tests: a real OR-Tools {@link CpModel} with the variables the model builder (WP3)
 * would have created (start, presence, candidate literals, requirement exactly-one, start-bucket and product literals),
 * wrapped in Mockito mocks of the final {@code CpModelContext} / {@code CandidateIndex} that follow the design 5.4 / 5.5
 * method names. The index mock delegates every view method to a {@link FamilyKit.View}; the three public array fields the
 * contributors read ({@code roles}, {@code reqRole}, {@code candReq}) are set by reflection.
 * <p>
 * Everything that needs the OR-Tools natives is guarded by {@link #nativeAvailable()}; tests call
 * {@code Assumptions.assumeTrue(ContribFixture.nativeAvailable())}.
 */
public final class ContribFixture {

    private static Boolean available;

    public final FamilyKit.View view;
    public final PlanningProblem problem;
    public final CpModel model = new CpModel();
    public final BoolVar trueLit;
    public final IntVar[] start;
    public final Literal[] presence;
    public final Literal[] cand;
    public final CandidateIndex index;
    public final CpModelContext ctx;
    public boolean explain;

    private final Map<String, Literal[]> bucketCache = new HashMap<>();
    private final Map<String, Literal> andCache = new HashMap<>();
    private final Map<Integer, Literal> usedCache = new HashMap<>();
    private final Map<Integer, List<SetupArc>> setupArcs = new HashMap<>();

    public static synchronized boolean nativeAvailable() {
        if (available == null) {
            try {
                Loader.loadNativeLibraries();
                available = true;
            } catch (Throwable t) {
                available = false;
            }
        }
        return available;
    }

    public static ContribFixture of(FamilyKit.View view, String... optionalTasks) {
        return new ContribFixture(view, Set.of(optionalTasks));
    }

    private ContribFixture(FamilyKit.View view, Set<String> optional) {
        this.view = view;
        this.problem = view.problem();
        trueLit = model.newBoolVar("true");
        model.addEquality(trueLit, 1);
        int tasks = view.taskCount();
        start = new IntVar[tasks];
        presence = new Literal[tasks];
        cand = new Literal[view.candidateCount()];
        for (int t = 0; t < tasks; t++) {
            start[t] = model.newIntVar(view.estStart(t), view.lstStart(t), "s" + t);
            presence[t] = optional.contains(view.taskId(t)) ? model.newBoolVar("p" + t) : trueLit;
            Map<String, List<Integer>> byRole = new LinkedHashMap<>();
            for (int c = view.candidateBegin(t); c < view.candidateEnd(t); c++) {
                byRole.computeIfAbsent(view.roleOf(c), k -> new ArrayList<>()).add(c);
            }
            for (List<Integer> group : byRole.values()) {
                if (group.size() == 1) {
                    cand[group.get(0)] = presence[t];
                } else {
                    LinearExprBuilder sum = LinearExpr.newBuilder();
                    for (int c : group) {
                        cand[c] = model.newBoolVar("x" + c);
                        sum.add(cand[c]);
                    }
                    model.addEquality(sum.build(), presence[t]);
                }
            }
        }
        index = Mockito.mock(CandidateIndex.class, invocation -> {
            Method m = invocation.getMethod();
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "equals":
                        return invocation.getMock() == invocation.getArgument(0);
                    case "hashCode":
                        return System.identityHashCode(invocation.getMock());
                    default:
                        return "indexMock";
                }
            }
            try {
                Method target = FamilyKit.View.class.getMethod(m.getName(), m.getParameterTypes());
                return target.invoke(view, invocation.getArguments());
            } catch (NoSuchMethodException e) {
                return Mockito.RETURNS_DEFAULTS.answer(invocation);
            }
        });
        setField(index, "roles", view.roles());
        setField(index, "reqRole", view.reqRole());
        setField(index, "candReq", view.candReq());
        ctx = Mockito.mock(CpModelContext.class, invocation -> answer(invocation.getMethod().getName(),
                invocation.getArguments()));
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = CandidateIndex.class.getField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("CandidateIndex field " + name + " not settable: " + e, e);
        }
    }

    private Object answer(String name, Object[] args) {
        switch (name) {
            case "model":
                return model;
            case "index":
                return index;
            case "problem":
                return problem;
            case "options":
                return SolverOptions.defaults();
            case "explainMode":
                return explain;
            case "start":
                return start[(Integer) args[0]];
            case "end":
                return LinearExpr.affine(start[(Integer) args[0]], 1, view.duration((Integer) args[0]));
            case "presence":
                return presence[(Integer) args[0]];
            case "cand":
                return cand[(Integer) args[0]];
            case "trueLit":
                return trueLit;
            case "checkpoint":
            case "count":
                return null;
            case "buckets":
                return BucketGrids.of(problem, (PeriodSpec) args[0]);
            case "startBucket":
                return startBucket((Integer) args[0], (BucketGrid) args[1]);
            case "and":
                return and((Literal) args[0], (Literal) args[1]);
            case "resourceUsed":
                return resourceUsed((Integer) args[0]);
            case "load":
                return load((Integer) args[0], (LoadMetric) args[1]);
            case "newTierVar":
                return model.newIntVar((Long) args[1], (Long) args[2], (String) args[0]);
            case "registerSetupArcs":
                setupArcs.put((Integer) args[0], new ArrayList<>((List<SetupArc>) args[1]));
                return null;
            case "setupArcs":
                return setupArcs.getOrDefault((Integer) args[0], List.of());
            default:
                throw new UnsupportedOperationException("CpModelContext." + name + " is not faked");
        }
    }

    public List<SetupArc> setupArcsOf(int resource) {
        return setupArcs.getOrDefault(resource, List.of());
    }

    private Literal[] startBucket(int t, BucketGrid grid) {
        String key = t + "|" + grid.id();
        Literal[] cached = bucketCache.get(key);
        if (cached != null) {
            return cached;
        }
        int lo = view.estStart(t);
        int hi = view.lstStart(t);
        BoolVar[] choice = new BoolVar[hi - lo + 1];
        LinearExprBuilder sum = LinearExpr.newBuilder();
        LinearExprBuilder value = LinearExpr.newBuilder();
        for (int v = lo; v <= hi; v++) {
            choice[v - lo] = model.newBoolVar("y" + t + "_" + v);
            sum.add(choice[v - lo]);
            value.addTerm(choice[v - lo], v);
        }
        model.addEquality(sum.build(), presence[t]);
        model.addEquality(start[t], value.build());
        Literal[] out = new Literal[grid.size()];
        for (int k = 0; k < grid.size(); k++) {
            LinearExprBuilder inBucket = LinearExpr.newBuilder();
            boolean any = false;
            for (int v = Math.max(lo, grid.lo()[k]); v <= Math.min(hi, grid.hi()[k] - 1); v++) {
                inBucket.add(choice[v - lo]);
                any = true;
            }
            if (any) {
                BoolVar b = model.newBoolVar("b" + t + "_" + k);
                model.addEquality(b, inBucket.build());
                out[k] = b;
            }
        }
        bucketCache.put(key, out);
        return out;
    }

    private Literal and(Literal a, Literal b) {
        String key = a.getIndex() + "&" + b.getIndex();
        Literal cached = andCache.get(key);
        if (cached != null) {
            return cached;
        }
        BoolVar w = model.newBoolVar("and" + key);
        model.addImplication(w, a);
        model.addImplication(w, b);
        model.addBoolOr(new Literal[]{a.not(), b.not(), w});
        andCache.put(key, w);
        return w;
    }

    private Literal resourceUsed(int r) {
        Literal cached = usedCache.get(r);
        if (cached != null) {
            return cached;
        }
        BoolVar used = model.newBoolVar("used" + r);
        List<Literal> lits = new ArrayList<>();
        lits.add(used.not());
        for (int c = 0; c < view.candidateCount(); c++) {
            if (view.candidateResource(c) == r) {
                model.addImplication(cand[c], used);
                lits.add(cand[c]);
            }
        }
        model.addBoolOr(lits);
        usedCache.put(r, used);
        return used;
    }

    private LinearExpr load(int r, LoadMetric metric) {
        LinearExprBuilder b = LinearExpr.newBuilder();
        for (int c = 0; c < view.candidateCount(); c++) {
            if (view.candidateResource(c) == r) {
                b.addTerm(cand[c], metric == LoadMetric.COUNT ? 1 : view.candidateDuration(c));
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------ helpers for tests

    public CompiledConstraint compile(ConstraintFamily family, Constraint constraint) {
        return new CompiledConstraint(0, constraint, family, FamilyKit.bind(family, constraint), true);
    }

    /** Adds {@code NoOverlap} over the candidate intervals of every unary resource (the structural model of WP3). */
    public void addStructuralNoOverlap() {
        for (int r = 0; r < view.resourceCount(); r++) {
            List<com.google.ortools.sat.IntervalVar> intervals = new ArrayList<>();
            for (int c = 0; c < view.candidateCount(); c++) {
                if (view.candidateResource(c) == r && view.candidateDuration(c) > 0) {
                    intervals.add(model.newOptionalFixedSizeIntervalVar(start[view.candidateTask(c)],
                            view.candidateDuration(c), cand[c], ""));
                }
            }
            model.addNoOverlap(intervals);
        }
    }

    /** Result of a solve with accessors on the solved model. */
    public final class Solved {
        public final CpSolver solver = new CpSolver();
        public final CpSolverStatus status;

        Solved(LinearExpr objective) {
            if (objective != null) {
                model.minimize(objective);
            }
            status = solver.solve(model);
        }

        public boolean solved() {
            return status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE;
        }

        public boolean infeasible() {
            return status == CpSolverStatus.INFEASIBLE;
        }

        public long startOf(String task) {
            return solver.value(start[view.taskIdx(task)]);
        }

        public boolean on(String task, String resource) {
            int t = view.taskIdx(task);
            int r = view.resourceIdx(resource);
            for (int c = view.candidateBegin(t); c < view.candidateEnd(t); c++) {
                if (view.candidateResource(c) == r && solver.booleanValue(cand[c])) {
                    return true;
                }
            }
            return false;
        }

        public boolean selected(String task) {
            return solver.booleanValue(presence[view.taskIdx(task)]);
        }

        /** Rebuilds the solution as the independent checkers see it. */
        public FamilyKit.Solution toSolution() {
            FamilyKit.Solution s = new FamilyKit.Solution(view);
            for (int t = 0; t < view.taskCount(); t++) {
                if (!solver.booleanValue(presence[t])) {
                    continue;
                }
                int st = (int) solver.value(start[t]);
                s.select(view.taskId(t), st, st + view.duration(t));
                for (int c = view.candidateBegin(t); c < view.candidateEnd(t); c++) {
                    if (solver.booleanValue(cand[c])) {
                        s.addResource(view.taskId(t), view.roleOf(c), view.resourceId(view.candidateResource(c)));
                    }
                }
            }
            return s;
        }

        public double objective() {
            return solver.objectiveValue();
        }
    }

    public Solved solve(LinearExpr objective) {
        return new Solved(objective);
    }

    // ------------------------------------------------------------------ enforcer

    /** Fake {@link Enforcer} of the four modes with the accounting of the real one (weight 1). */
    public static final class FakeEnforcer implements Enforcer {
        private final Mode mode;
        private final CpModel model;
        private final boolean explain;
        private BoolVar shared;
        public final List<BoolVar> satisfied = new ArrayList<>();
        public final List<LinearExpr> measures = new ArrayList<>();

        public FakeEnforcer(Mode mode, ContribFixture fixture) {
            this.mode = mode;
            this.model = fixture.model;
            this.explain = fixture.explain;
        }

        @Override
        public Mode mode() {
            return mode;
        }

        @Override
        public Literal instance(int taskIdx, int resourceIdx) {
            switch (mode) {
                case HARD:
                    return explain ? shared() : null;
                case RELAXABLE:
                    return shared();
                default:
                    BoolVar sat = model.newBoolVar("sat");
                    satisfied.add(sat);
                    return sat;
            }
        }

        /** The assumption / activation literal shared by all instances. */
        public BoolVar shared() {
            if (shared == null) {
                shared = model.newBoolVar("shared");
            }
            return shared;
        }

        @Override
        public boolean penalizeMeasure(LinearExpr measure, long maxMeasure, int taskIdx) {
            if (mode != Mode.SOFT) {
                return false;
            }
            measures.add(measure);
            return true;
        }

        @Override
        public Literal[] conditions(Literal instance, Literal... extra) {
            List<Literal> out = new ArrayList<>();
            if (instance != null) {
                out.add(instance);
            }
            for (Literal l : extra) {
                if (l != null) {
                    out.add(l);
                }
            }
            return out.toArray(new Literal[0]);
        }

        /** Soft penalty: one per unsatisfied instance plus every measure. */
        public LinearExpr penalty() {
            LinearExprBuilder b = LinearExpr.newBuilder();
            for (BoolVar sat : satisfied) {
                b.add(1);
                b.addTerm(sat, -1);
            }
            for (LinearExpr m : measures) {
                b.add(m);
            }
            return b.build();
        }
    }
}
