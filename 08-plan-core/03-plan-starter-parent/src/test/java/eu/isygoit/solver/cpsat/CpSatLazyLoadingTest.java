package eu.isygoit.solver.cpsat;

import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.exception.SolverUnavailableException;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.EngineCapabilities;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.cpsat.explain.CpSatExplainer;
import eu.isygoit.solver.registry.CpSatContributorRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Native loading stays lazy and cached behind the {@code NativeLoader} seam (no OR-Tools library needed here). */
class CpSatLazyLoadingTest {

    @Test
    void constructionDoesNotLoadNativeLibraries() {
        AtomicInteger loads = new AtomicInteger();
        engine(() -> loads.incrementAndGet());
        assertEquals(0, loads.get());
    }

    @Test
    void loadFailureIsReportedAsSolverUnavailableAndCached() {
        AtomicInteger loads = new AtomicInteger();
        CpSatSolverEngine engine = engine(() -> {
            loads.incrementAndGet();
            throw new UnsatisfiedLinkError("msvcp140.dll missing");
        });

        assertFalse(engine.isAvailable());
        SolverUnavailableException exception = assertThrows(SolverUnavailableException.class,
                () -> engine.solve(compiled(), context()));
        assertEquals(SolverUnavailableException.CODE, exception.code());
        assertTrue(exception.getMessage().contains("cp_sat"));
        assertEquals(1, loads.get(), "a failed load must be attempted once and cached");
    }

    @Test
    void successfulLoadIsProbedOnce() {
        AtomicInteger loads = new AtomicInteger();
        CpSatSolverEngine engine = engine(loads::incrementAndGet);
        assertTrue(engine.isAvailable());
        assertTrue(engine.isAvailable());
        assertEquals(1, loads.get());
    }

    @Test
    void capabilitiesDeclareEveryFeatureAndOptimalityWithoutLoadingNatives() {
        AtomicInteger loads = new AtomicInteger();
        CpSatSolverEngine engine = engine(loads::incrementAndGet);
        EngineCapabilities capabilities = engine.capabilities();
        assertEquals("cp_sat", engine.name());
        assertEquals("cp_sat", capabilities.engineId());
        assertTrue(capabilities.provesOptimality());
        for (EngineCapabilities.Feature feature : EngineCapabilities.Feature.values()) {
            assertTrue(capabilities.supports(feature), "cp_sat must support " + feature);
        }
        assertEquals(0, loads.get(), "capabilities() must not load the native library");
    }

    private CpSatSolverEngine engine(CpSatSolverEngine.NativeLoader loader) {
        CpSatProperties properties = new CpSatProperties();
        CpModelBuilder builder = new CpModelBuilder(new CpSatContributorRegistry(List.of(), List.of()), List.of(),
                properties);
        return new CpSatSolverEngine(builder, new CpSatExplainer(), properties, loader);
    }

    private SolveContext context() {
        return new SolveContext(SolverOptions.defaults(), new SolverCancellationToken(), SolveListener.NOOP,
                System.nanoTime());
    }

    private CompiledProblem compiled() {
        Instant start = Instant.parse("2026-09-07T08:00:00Z");
        PlanningProblem problem = new PlanningProblem(1L, "generic", "UTC", new Horizon(start, start.plusSeconds(3600), 60),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), SolverOptions.defaults(),
                null);
        return new CompiledProblem(problem, null, null, List.of(), List.of());
    }
}
