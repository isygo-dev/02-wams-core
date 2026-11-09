package eu.isygoit.solver.cpsat.explain;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.CoreItem;
import eu.isygoit.dto.response.InfeasibilityCore;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.cpsat.AssumptionRef;
import eu.isygoit.solver.check.TestProblems;
import eu.isygoit.solver.diag.CoreItems;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link CpSatExplainer}. The fallback paths need no native library; the real assumption-core tests (G2b) are
 * skipped with {@code Assumptions.assumeTrue} when OR-Tools natives cannot be loaded (same seam spirit as
 * {@code CpSatLazyLoadingTest}).
 */
class CpSatExplainerTest {

    private static Constraint constraint(String id, IEnumEnforcement.Types enforcement) {
        return new Constraint(id, "fake", enforcement, ConstraintScope.all(), Map.of(), null,
                enforcement == IEnumEnforcement.Types.SOFT ? 1 : 0, null);
    }

    private static CompiledProblem compiled(Constraint... constraints) {
        PlanningProblem problem = TestProblems.problem(100, List.of(TestProblems.resource("R", 1)),
                List.of(TestProblems.task("A", 5).build()), List.of(constraints), List.of());
        return new CompiledProblem(problem, null, null, List.of(), List.of());
    }

    private static SolveContext context(Integer explainSeconds, Boolean explain) {
        SolverOptions options = new SolverOptions(null, 30, 1, null, null, null, null, null, null, null, explain,
                explainSeconds, null);
        return new SolveContext(options, new SolverCancellationToken(), SolveListener.NOOP, System.nanoTime());
    }

    private static boolean nativesAvailable() {
        try {
            Loader.loadNativeLibraries();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static List<String> ids(InfeasibilityCore core) {
        return core.items().stream().map(CoreItem::id).toList();
    }

    // ------------------------------------------------------------------------------------------ fallback (no natives)

    @Test
    void builderFailureFallsBackToTheLabelledV1List() {
        CompiledProblem problem = compiled(constraint("hard-1", IEnumEnforcement.Types.STRICTLY_REQUIRED),
                constraint("soft-1", IEnumEnforcement.Types.SOFT),
                constraint("relax-1", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION));
        AtomicInteger calls = new AtomicInteger();
        InfeasibilityCore core = new CpSatExplainer().explain(problem, context(2, true),
                (CpSatExplainer.ModelFactory) (p, c) -> {
                    calls.incrementAndGet();
                    throw new IllegalStateException("model too large");
                });
        assertEquals(1, calls.get());
        assertEquals(CoreItems.METHOD_NONE, core.method());
        assertFalse(core.minimal());
        assertEquals(List.of("hard-1", "relax-1"), ids(core), "SOFT constraints are never blamed");
        assertTrue(core.items().stream().allMatch(i -> CoreItems.CONSTRAINT.equals(i.kind())));
    }

    @Test
    void noAssumptionsMeansFallback() {
        CompiledProblem problem = compiled(constraint("hard-1", IEnumEnforcement.Types.STRICTLY_REQUIRED));
        InfeasibilityCore core = new CpSatExplainer().explain(problem, context(2, true),
                (CpSatExplainer.ModelFactory) (p, c) -> new CpSatExplainer.ExplainModel(null, List.of()));
        assertEquals(CoreItems.METHOD_NONE, core.method());
        assertEquals(List.of("hard-1"), ids(core));
    }

    @Test
    void disabledExplanationDoesNotEvenBuildTheModel() {
        CompiledProblem problem = compiled(constraint("hard-1", IEnumEnforcement.Types.STRICTLY_REQUIRED));
        AtomicInteger calls = new AtomicInteger();
        InfeasibilityCore core = new CpSatExplainer().explain(problem, context(2, false),
                (CpSatExplainer.ModelFactory) (p, c) -> {
                    calls.incrementAndGet();
                    return null;
                });
        assertEquals(0, calls.get());
        assertEquals(CoreItems.METHOD_NONE, core.method());
    }

    @Test
    void theFactoryReceivesAFreshSingleWorkerContextWithTheExplainBudget() {
        CompiledProblem problem = compiled(constraint("hard-1", IEnumEnforcement.Types.STRICTLY_REQUIRED));
        SolveContext original = context(7, true);
        SolveContext[] seen = new SolveContext[1];
        new CpSatExplainer().explain(problem, original, (CpSatExplainer.ModelFactory) (p, c) -> {
            seen[0] = c;
            return null;
        });
        assertEquals(1, seen[0].options().numWorkers());
        assertEquals(7, seen[0].options().maxTimeSeconds());
        assertTrue(seen[0].startNanos() >= original.startNanos());
        assertSame(original.cancellation(), seen[0].cancellation());
    }

    private static void assertSame(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual);
    }

    // ------------------------------------------------------------------------------------------ G2b (natives)

    /** x in [0,10]; a => x >= 5; b => x <= 3; c => x >= 0 (irrelevant). The minimal core is {a, b}. */
    private static CpSatExplainer.ExplainModel conflictingModel(boolean includeConflict) {
        CpModel model = new CpModel();
        IntVar x = model.newIntVar(0, 10, "x");
        BoolVar a = model.newBoolVar("a");
        BoolVar b = model.newBoolVar("b");
        BoolVar c = model.newBoolVar("c");
        model.addGreaterOrEqual(x, 5).onlyEnforceIf(a);
        model.addLessOrEqual(x, includeConflict ? 3 : 8).onlyEnforceIf(b);
        model.addGreaterOrEqual(x, 0).onlyEnforceIf(c);
        return new CpSatExplainer.ExplainModel(model, List.of(
                new AssumptionRef(a, "CONSTRAINT", "c-a", "CONSTRAINT"),
                new AssumptionRef(b, "TASK", "t-b", "WINDOW_EMPTY"),
                new AssumptionRef(c, "CONSTRAINT", "c-c", "CONSTRAINT")));
    }

    @Test
    void g2bAssumptionCoreIsShrunkToTheMinimalConflict() {
        Assumptions.assumeTrue(nativesAvailable(), "OR-Tools natives are not available");
        CompiledProblem problem = compiled(constraint("c-a", IEnumEnforcement.Types.STRICTLY_REQUIRED),
                constraint("c-c", IEnumEnforcement.Types.STRICTLY_REQUIRED));
        InfeasibilityCore core = new CpSatExplainer().explain(problem, context(5, true),
                (CpSatExplainer.ModelFactory) (p, c) -> conflictingModel(true));
        // T-EXPLAIN: if this fails with method NONE, the OR-Tools build returned an empty assumption core
        assertEquals(CoreItems.METHOD_ASSUMPTIONS_SHRUNK, core.method(), "core was " + core);
        assertTrue(core.minimal());
        assertEquals(List.of("c-a", "t-b"), ids(core));
        CoreItem task = core.items().stream().filter(i -> "t-b".equals(i.id())).findFirst().orElseThrow();
        assertEquals(CoreItems.TASK, task.kind());
        assertEquals(CoreItems.WINDOW_EMPTY, task.reasonCode());
    }

    @Test
    void satisfiableExplainModelFallsBack() {
        Assumptions.assumeTrue(nativesAvailable(), "OR-Tools natives are not available");
        CompiledProblem problem = compiled(constraint("c-a", IEnumEnforcement.Types.STRICTLY_REQUIRED));
        InfeasibilityCore core = new CpSatExplainer().explain(problem, context(5, true),
                (CpSatExplainer.ModelFactory) (p, c) -> conflictingModel(false));
        assertEquals(CoreItems.METHOD_NONE, core.method());
        assertFalse(core.minimal());
        assertEquals(List.of("c-a"), ids(core));
    }

    @Test
    void cancellationBeforeTheSolveFallsBackInsteadOfThrowing() {
        Assumptions.assumeTrue(nativesAvailable(), "OR-Tools natives are not available");
        CompiledProblem problem = compiled(constraint("c-a", IEnumEnforcement.Types.STRICTLY_REQUIRED));
        SolveContext ctx = context(5, true);
        ctx.cancellation().cancel();
        InfeasibilityCore core = new CpSatExplainer().explain(problem, ctx,
                (CpSatExplainer.ModelFactory) (p, c) -> conflictingModel(true));
        assertTrue(core.method().equals(CoreItems.METHOD_NONE)
                        || core.method().startsWith("ASSUMPTIONS"),
                "never an exception; a stopped solve yields the fallback");
    }
}
