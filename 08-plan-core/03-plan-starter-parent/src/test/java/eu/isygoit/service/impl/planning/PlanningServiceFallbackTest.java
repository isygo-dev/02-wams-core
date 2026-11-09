package eu.isygoit.service.impl.planning;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.Metrics;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.exception.InfeasibleException;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.SolverEngine;
import eu.isygoit.solver.SolverRegistry;
import eu.isygoit.solver.diag.PreSolveAnalyzer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanningServiceFallbackTest {

    @Test
    void retriesWithTimefoldWhenCpSatReturnsUnknownWithoutSolution() {
        CompiledProblem compiled = compiledProblem();
        SolverEngine cpSat = engine(SolverOptions.ENGINE_CP_SAT);
        SolverEngine timefold = engine(SolverOptions.ENGINE_TIMEFOLD);
        when(cpSat.solve(eq(compiled), any())).thenReturn(result(IEnumSolverStatus.Types.UNKNOWN));
        when(timefold.solve(eq(compiled), any())).thenReturn(result(IEnumSolverStatus.Types.UNKNOWN));

        PlanningService service = service(compiled, cpSat, timefold);
        SolveResult actual = service.solve(1L, null, new SolverCancellationToken(), SolveListener.NOOP);

        ArgumentCaptor<SolveContext> fallbackContext = ArgumentCaptor.forClass(SolveContext.class);
        verify(timefold).solve(eq(compiled), fallbackContext.capture());
        assertEquals(SolverOptions.ENGINE_TIMEFOLD, fallbackContext.getValue().options().engine());
        assertFalse(fallbackContext.getValue().options().fallbackToTimefold());
        assertEquals(SolverOptions.ENGINE_TIMEFOLD,
                actual.diagnostics().details().get("fallbackToEngine"));
        assertTrue(actual.diagnostics().warnings().stream()
                .anyMatch(warning -> "ENGINE_FALLBACK".equals(warning.code())));
    }

    @Test
    void doesNotRetryForInfeasibleOrCancelledSolves() {
        CompiledProblem compiled = compiledProblem();
        SolverEngine cpSat = engine(SolverOptions.ENGINE_CP_SAT);
        SolverEngine timefold = engine(SolverOptions.ENGINE_TIMEFOLD);
        when(cpSat.solve(eq(compiled), any())).thenReturn(result(IEnumSolverStatus.Types.INFEASIBLE));
        PlanningService service = service(compiled, cpSat, timefold);

        assertThrows(InfeasibleException.class,
                () -> service.solve(1L, null, new SolverCancellationToken(), SolveListener.NOOP));
        verify(timefold, never()).solve(any(), any());

        SolverCancellationToken token = new SolverCancellationToken();
        when(cpSat.solve(eq(compiled), any())).thenAnswer(invocation -> {
            ((SolveContext) invocation.getArgument(1)).cancellation().cancel();
            return result(IEnumSolverStatus.Types.UNKNOWN);
        });
        SolveResult cancelled = service.solve(1L, null, token, SolveListener.NOOP);

        assertEquals(IEnumSolverStatus.Types.UNKNOWN, cancelled.status());
        verify(timefold, never()).solve(any(), any());
    }

    private static PlanningService service(CompiledProblem compiled, SolverEngine cpSat, SolverEngine timefold) {
        SolverRegistry registry = new SolverRegistry(List.of(cpSat, timefold));
        PreSolveAnalyzer preSolveAnalyzer = mock(PreSolveAnalyzer.class);
        when(preSolveAnalyzer.analyze(compiled)).thenReturn(Optional.empty());
        return new PlanningService(null, null, null, null, null, null, registry, preSolveAnalyzer, null,
                new SolveConcurrencyGuard(1, Duration.ofSeconds(1)), null) {
            @Override
            public CompiledProblem compile(Long jobId, PlanningRequest request, String engineOverride,
                                           SolverCancellationToken token) {
                return compiled;
            }
        };
    }

    private static CompiledProblem compiledProblem() {
        SolverOptions options = SolverOptions.defaults().withEngineAndFallback(SolverOptions.ENGINE_CP_SAT, true);
        PlanningProblem problem = new PlanningProblem(1L, null, null, null, null, null, null, null, null,
                null, null, options, null);
        return new CompiledProblem(problem, null, null, null, null);
    }

    private static SolverEngine engine(String name) {
        SolverEngine engine = mock(SolverEngine.class);
        when(engine.name()).thenReturn(name);
        return engine;
    }

    private static SolveResult result(IEnumSolverStatus.Types status) {
        return new SolveResult(status, null, List.of(),
                new Diagnostics(status.name(), List.of(), null, List.of(), Map.of()),
                new Metrics(0, 0, 0, null, 0, null, null, List.of()));
    }
}
