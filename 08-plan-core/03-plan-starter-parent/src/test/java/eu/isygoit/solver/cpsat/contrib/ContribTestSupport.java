package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.ConstraintProto;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.spi.ConstraintFamily;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared scaffolding of the contributor tests (test sources of WP4a only).
 * <p>
 * No native OR-Tools library is needed: the contributors only BUILD a {@link CpModel} (protobuf), which these tests inspect
 * through {@code model.model()} (OR-TOOLS-VERIFY: {@code CpModel.model()} returns the {@code CpModelProto}; the generated
 * {@code ConstraintProto} lives in {@code com.google.ortools.sat}). {@link CpModelContext} and {@link CandidateIndex} are final
 * classes of other work packages and are mocked (Mockito inline mock maker, the default of Mockito 5). The solver itself
 * (model value == checker value) is exercised by the integration tests of WP8b.
 * <p>
 * Model: tasks are added with {@link #task}; the index mock reports one candidate list per task; the horizon is 100
 * one-minute ticks.
 */
final class ContribTestSupport {

    static final Instant T0 = Instant.parse("2026-01-05T00:00:00Z");
    static final Horizon H = new Horizon(T0, T0.plusSeconds(6000), 60);

    final CpModel model = new CpModel();
    final BoolVar one = model.newBoolVar("one");
    final CpModelContext ctx = mock(CpModelContext.class);
    final CandidateIndex index = mock(CandidateIndex.class);
    final PlanLimitsProperties limits = new PlanLimitsProperties();
    final List<Resource> resources = new ArrayList<>();
    final List<Slot> slots = new ArrayList<>();

    private final List<IntVar> starts = new ArrayList<>();
    private final List<Literal> presences = new ArrayList<>();
    private final List<int[]> taskWindows = new ArrayList<>(); // {est, lst, firstCandidate, endCandidate}
    private final List<int[]> candidates = new ArrayList<>(); // {task, resource, duration}
    private final List<BoolVar> candidateLiterals = new ArrayList<>();
    private boolean explain;

    ContribTestSupport(int resourceCount) {
        for (int r = 0; r < resourceCount; r++) {
            resources.add(new Resource("r" + r, "crew", "r" + r, 1, null, null, null, null, null, null, null, null, null));
        }
        when(index.horizon()).thenReturn(H);
        when(index.horizonTicks()).thenReturn(100);
        when(index.limits()).thenReturn(limits);
        when(index.taskCount()).thenAnswer(i -> starts.size());
        when(index.resourceCount()).thenReturn(resourceCount);
        when(index.estStart(anyInt())).thenAnswer(i -> taskWindows.get(i.getArgument(0, Integer.class))[0]);
        when(index.lstStart(anyInt())).thenAnswer(i -> taskWindows.get(i.getArgument(0, Integer.class))[1]);
        when(index.candidateBegin(anyInt())).thenAnswer(i -> taskWindows.get(i.getArgument(0, Integer.class))[2]);
        when(index.candidateEnd(anyInt())).thenAnswer(i -> taskWindows.get(i.getArgument(0, Integer.class))[3]);
        when(index.candidateResource(anyInt())).thenAnswer(i -> candidates.get(i.getArgument(0, Integer.class))[1]);
        when(index.candidateDuration(anyInt())).thenAnswer(i -> candidates.get(i.getArgument(0, Integer.class))[2]);
        when(index.duration(anyInt())).thenAnswer(i -> {
            int[] w = taskWindows.get(i.getArgument(0, Integer.class));
            return w[2] < w[3] ? candidates.get(w[2])[2] : 0;
        });
        when(index.taskId(anyInt())).thenAnswer(i -> "t" + i.getArgument(0));
        when(index.resourceId(anyInt())).thenAnswer(i -> "r" + i.getArgument(0));
        when(ctx.model()).thenReturn(model);
        when(ctx.index()).thenReturn(index);
        when(ctx.problem()).thenAnswer(i -> problem(List.of()));
        when(ctx.explainMode()).thenAnswer(i -> explain);
        when(ctx.trueLit()).thenReturn(one);
        when(ctx.start(anyInt())).thenAnswer(i -> starts.get(i.getArgument(0, Integer.class)));
        when(ctx.presence(anyInt())).thenAnswer(i -> presences.get(i.getArgument(0, Integer.class)));
        when(ctx.cand(anyInt())).thenAnswer(i -> candidateLiterals.get(i.getArgument(0, Integer.class)));
        when(ctx.end(anyInt())).thenAnswer(i -> {
            int t = i.getArgument(0, Integer.class);
            int[] w = taskWindows.get(t);
            long duration = w[2] < w[3] ? candidates.get(w[2])[2] : 0;
            return LinearExpr.newBuilder().add(starts.get(t)).add(duration).build();
        });
    }

    /**
     * Adds a task.
     *
     * @param est       earliest start after pruning
     * @param lst       latest start after pruning
     * @param optional  {@code true} for an optional task (own presence literal)
     * @param resourceAndDuration pairs {@code resource, duration} (one candidate per pair)
     * @return the task index
     */
    int task(int est, int lst, boolean optional, int... resourceAndDuration) {
        int t = starts.size();
        starts.add(model.newIntVar(est, lst, "s" + t));
        presences.add(optional ? model.newBoolVar("p" + t) : one);
        int first = candidates.size();
        for (int k = 0; k < resourceAndDuration.length; k += 2) {
            candidates.add(new int[]{t, resourceAndDuration[k], resourceAndDuration[k + 1]});
            candidateLiterals.add(model.newBoolVar("x" + candidates.size()));
        }
        taskWindows.add(new int[]{est, lst, first, candidates.size()});
        return t;
    }

    BoolVar candidateLiteral(int candidate) {
        return candidateLiterals.get(candidate);
    }

    void explain(boolean explain) {
        this.explain = explain;
    }

    PlanningProblem problem(List<Constraint> constraints) {
        return new PlanningProblem(1L, "t", "UTC", H, slots, List.of(), List.of(), resources, List.of(), constraints, List.of(), null, null);
    }

    void maxPairwise(int max) {
        ReflectionTestUtils.setField(limits, "maxPairwise", (long) max);
    }

    static Constraint constraint(String type, IEnumEnforcement.Types enforcement, eu.isygoit.dto.request.ConstraintScope scope) {
        return new Constraint("c1", type, enforcement, scope, Map.of(), null, 3, 0);
    }

    static CompiledConstraint compiled(Constraint constraint, ConstraintFamily family, Object params) {
        return new CompiledConstraint(0, constraint, family, params, true);
    }

    /** @return the constraints added to the model so far */
    List<ConstraintProto> constraints() {
        return model.model().getConstraintsList();
    }

    List<ConstraintProto> constraints(ConstraintProto.ConstraintCase type) {
        return constraints().stream().filter(c -> c.getConstraintCase() == type).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ enforcer fake

    /** Mirrors the contract of {@code Enforcer}: HARD solve null; SOFT fresh literal per instance; RELAXABLE / explain a shared literal. */
    static final class FakeEnforcer implements Enforcer {
        private final Mode mode;
        private final CpModel model;
        private final Literal shared;
        int instanceCalls;
        final List<LinearExpr> penalized = new ArrayList<>();
        final List<Literal> issued = new ArrayList<>();

        FakeEnforcer(Mode mode, CpModel model, Literal shared) {
            this.mode = mode;
            this.model = model;
            this.shared = shared;
        }

        @Override
        public Mode mode() {
            return mode;
        }

        @Override
        public Literal instance(int taskIdx, int resourceIdx) {
            instanceCalls++;
            Literal literal = mode == Mode.SOFT ? model.newBoolVar("sat") : shared;
            if (literal != null) {
                issued.add(literal);
            }
            return literal;
        }

        @Override
        public boolean penalizeMeasure(LinearExpr measure, long maxMeasure, int taskIdx) {
            if (mode != Mode.SOFT) {
                return false;
            }
            penalized.add(measure);
            return true;
        }

        @Override
        public Literal[] conditions(Literal instance, Literal... extra) {
            List<Literal> list = new ArrayList<>();
            if (instance != null) {
                list.add(instance);
            }
            for (Literal e : extra) {
                if (e != null) {
                    list.add(e);
                }
            }
            return list.toArray(new Literal[0]);
        }
    }

    FakeEnforcer hard() {
        return new FakeEnforcer(Enforcer.Mode.HARD, model, null);
    }

    /** HARD enforcer of the explain pass: the assumption literal is returned as instance literal. */
    FakeEnforcer assumption(Literal assumption) {
        return new FakeEnforcer(Enforcer.Mode.HARD, model, assumption);
    }

    FakeEnforcer relaxable(Literal active) {
        return new FakeEnforcer(Enforcer.Mode.RELAXABLE, model, active);
    }

    FakeEnforcer soft() {
        return new FakeEnforcer(Enforcer.Mode.SOFT, model, null);
    }
}
