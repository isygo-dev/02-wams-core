package eu.isygoit.service.impl.planning;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.Metrics;
import eu.isygoit.dto.response.ModelEstimateDto;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.ValidationReportDto;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.exception.InfeasibleException;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.exception.RequestValidationException;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.SolveCancelledException;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolveProgress;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.SolverEngine;
import eu.isygoit.solver.SolverRegistry;
import eu.isygoit.solver.diag.PreSolveAnalyzer;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.index.CandidateIndexBuilder;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.spi.ModelCost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates the v2 planning pipeline (design 0.3 / 10.3):
 * {@code validate -> normalize -> verify -> candidate index + estimate -> pre-solve analysis -> concurrency guard ->
 * engine -> independent solution validation}.
 * <ul>
 *   <li>{@link #solve(Long, PlanningRequest, SolverCancellationToken, SolveListener)} runs the whole pipeline; the older
 *       overloads delegate with {@link SolveListener#NOOP}. The time budget starts at the beginning of the request (build
 *       time counts). A stop request while compiling or while waiting for a solver slot returns a result with status
 *       {@code UNKNOWN} (Phase 1 behaviour); an infeasible problem (exact pre-solve core or engine verdict) raises
 *       {@link InfeasibleException}.</li>
 *   <li>{@link #compile} stops after the estimate and returns the {@link CompiledProblem}.</li>
 *   <li>{@link #validate} is the {@code POST /validate} entry: ALL issues, the compatibility of the request with EVERY
 *       engine and the model estimate; it never throws for user errors.</li>
 * </ul>
 * {@code numWorkers} above {@code app.plan.limits.max-workers} is clamped (not rejected). Stateless; thread-safe (the
 * concurrency guard bounds simultaneous solves).
 */
@Slf4j
@Service
public class PlanningService {

    private final PlanningRequestValidator requestValidator;
    private final DomainNormalizer normalizer;
    private final ProblemVerifier verifier;
    private final ModelCostEstimator estimator;
    private final ConstraintFamilyRegistry constraintRegistry;
    private final ObjectiveFamilyRegistry objectiveRegistry;
    private final SolverRegistry solverRegistry;
    private final PreSolveAnalyzer preSolveAnalyzer;
    private final PlanningSolutionValidator solutionValidator;
    private final SolveConcurrencyGuard concurrencyGuard;
    private final PlanLimitsProperties limits;

    /**
     * Spring constructor.
     *
     * @param requestValidator   stage 1
     * @param normalizer         stage 2
     * @param verifier           stage 3
     * @param estimator          model size estimation
     * @param constraintRegistry constraint families (unknown type mapping)
     * @param objectiveRegistry  objective families (unknown type mapping)
     * @param solverRegistry     engines
     * @param solutionValidator  independent solution validation (WP6)
     * @param concurrencyGuard   bounded concurrent solves
     * @param limits             configured limits
     */
    @Autowired
    public PlanningService(
            PlanningRequestValidator requestValidator,
            DomainNormalizer normalizer,
            ProblemVerifier verifier,
            ModelCostEstimator estimator,
            ConstraintFamilyRegistry constraintRegistry,
            ObjectiveFamilyRegistry objectiveRegistry,
            SolverRegistry solverRegistry,
            PlanningSolutionValidator solutionValidator,
            SolveConcurrencyGuard concurrencyGuard,
            PlanLimitsProperties limits) {
        this(requestValidator, normalizer, verifier, estimator, constraintRegistry, objectiveRegistry, solverRegistry,
                new PreSolveAnalyzer(), solutionValidator, concurrencyGuard, limits);
    }

    /**
     * Constructor with an explicit pre-solve analyzer (tests).
     *
     * @param preSolveAnalyzer the exact structural infeasibility analysis
     * @see #PlanningService(PlanningRequestValidator, DomainNormalizer, ProblemVerifier, ModelCostEstimator,
     * ConstraintFamilyRegistry, ObjectiveFamilyRegistry, SolverRegistry, PlanningSolutionValidator,
     * SolveConcurrencyGuard, PlanLimitsProperties)
     */
    public PlanningService(
            PlanningRequestValidator requestValidator,
            DomainNormalizer normalizer,
            ProblemVerifier verifier,
            ModelCostEstimator estimator,
            ConstraintFamilyRegistry constraintRegistry,
            ObjectiveFamilyRegistry objectiveRegistry,
            SolverRegistry solverRegistry,
            PreSolveAnalyzer preSolveAnalyzer,
            PlanningSolutionValidator solutionValidator,
            SolveConcurrencyGuard concurrencyGuard,
            PlanLimitsProperties limits) {
        this.requestValidator = requestValidator;
        this.normalizer = normalizer;
        this.verifier = verifier;
        this.estimator = estimator;
        this.constraintRegistry = constraintRegistry;
        this.objectiveRegistry = objectiveRegistry;
        this.solverRegistry = solverRegistry;
        this.preSolveAnalyzer = preSolveAnalyzer;
        this.solutionValidator = solutionValidator;
        this.concurrencyGuard = concurrencyGuard;
        this.limits = limits;
    }

    // ==================================================================================================
    // solve
    // ==================================================================================================

    /** Solves a request that is not bound to a persisted job. */
    public SolveResult solve(PlanningRequest request) {
        return solve(request, new SolverCancellationToken());
    }

    /**
     * Validates, normalizes and solves a request. A global fair semaphore bounds the number of concurrent solves.
     *
     * @throws eu.isygoit.exception.SolverBusyException when no solver slot became free in time (transient)
     */
    public SolveResult solve(PlanningRequest request, SolverCancellationToken cancellationToken) {
        return solve(null, request, cancellationToken);
    }

    /**
     * Validates, normalizes and solves the request of a persisted job (no progress listener).
     *
     * @param jobId the id of the persisted plan job (carried by the problem), or {@code null} when there is none
     * @throws eu.isygoit.exception.SolverBusyException when no solver slot became free in time (transient)
     */
    public SolveResult solve(Long jobId, PlanningRequest request, SolverCancellationToken cancellationToken) {
        return solve(jobId, request, cancellationToken, SolveListener.NOOP);
    }

    /**
     * Validates, normalizes, compiles and solves the request of a persisted job.
     *
     * @param jobId             the id of the persisted plan job, or {@code null}
     * @param request           the request
     * @param cancellationToken cancellation token of the job
     * @param listener          progress and incumbent listener ({@code null} = none)
     * @return the validated, enriched result; status {@code UNKNOWN} when stopped before the engine produced anything
     * @throws InfeasibleException                       when the problem is infeasible (diagnostics attached)
     * @throws eu.isygoit.exception.SolverBusyException  when no solver slot became free in time (transient)
     */
    public SolveResult solve(Long jobId, PlanningRequest request, SolverCancellationToken cancellationToken,
                             SolveListener listener) {
        long startNanos = System.nanoTime();
        SolverCancellationToken token = cancellationToken == null ? new SolverCancellationToken() : cancellationToken;
        SolveListener progress = listener == null ? SolveListener.NOOP : listener;
        CompiledProblem compiled;
        try {
            compiled = compile(jobId, request, null, token);
            Optional<Diagnostics> core = preSolveAnalyzer.analyze(compiled);
            if (core.isPresent()) {
                throw new InfeasibleException(core.get());
            }
        } catch (SolveCancelledException exception) {
            return stopped(exception);
        }
        PlanningProblem problem = compiled.problem();
        SolverEngine engine = solverRegistry.get(problem.options().engine());
        SolverOptions options = problem.options();
        boolean fallbackEnabled = Boolean.TRUE.equals(options.fallbackToTimefold());
        long attemptStartNanos = System.nanoTime();
        long firstAttemptBudgetMillis = fallbackEnabled
                ? new SolveContext(options, token, progress, startNanos).remainingMillis(attemptStartNanos) / 2L
                : -1L;
        SolveContext engineContext = fallbackEnabled
                ? new SolveContext(options, token, progress, startNanos, attemptStartNanos, firstAttemptBudgetMillis)
                : new SolveContext(options, token, progress, startNanos);
        SolveResult result;
        try (SolveConcurrencyGuard.Permit ignored = concurrencyGuard.acquire(token)) {
            result = engine.solve(compiled, engineContext);
            if (fallbackEnabled
                    && result.status() == IEnumSolverStatus.Types.UNKNOWN
                    && result.solution() == null
                    && !token.isCancellationRequested()
                    && new SolveContext(options, token, progress, startNanos)
                    .remainingMillis(System.nanoTime()) > 0L) {
                log.info("CP-SAT returned UNKNOWN without a solution for planning job {}; retrying with Timefold",
                        jobId);
                progress.onProgress(new SolveProgress(
                        (System.nanoTime() - startNanos) / 1_000_000L, 0, null, null, null, 0,
                        "FALLBACK_TO_TIMEFOLD"));
                SolverEngine fallbackEngine = solverRegistry.get(SolverOptions.ENGINE_TIMEFOLD);
                SolverOptions fallbackOptions = options.withEngineAndFallback(
                        SolverOptions.ENGINE_TIMEFOLD, false);
                result = fallbackEngine.solve(compiled,
                        new SolveContext(fallbackOptions, token, progress, startNanos));
                result = withFallbackNotice(result);
            }
        } catch (SolveCancelledException exception) {
            return stopped(exception);
        }
        if (result.status() == IEnumSolverStatus.Types.INFEASIBLE) {
            throw new InfeasibleException(result.diagnostics());
        }
        if (result.solution() != null) {
            return solutionValidator.validateAndEnrich(compiled, result);
        }
        return result;
    }

    private static SolveResult stopped(SolveCancelledException exception) {
        return new SolveResult(IEnumSolverStatus.Types.UNKNOWN, null, List.of(),
                new Diagnostics(exception.getMessage(), List.of(), null, List.of(), Map.of()),
                new Metrics(0, 0, 0, null, 0, null, null, List.of()));
    }

    // ==================================================================================================
    // compile / normalize
    // ==================================================================================================

    /**
     * Validates, normalizes and verifies a request without building the index (cheap: used at submit time).
     *
     * @return the normalized problem
     * @throws LimitExceededException      when a size limit is crossed
     * @throws RequestValidationException  when validation found errors
     */
    public PlanningProblem normalizeAndValidate(PlanningRequest request) {
        return normalizeAndValidate(null, request);
    }

    /**
     * Validates, normalizes and verifies a request without building the index.
     *
     * @param jobId the id of the persisted plan job, or {@code null} when there is none
     */
    public PlanningProblem normalizeAndValidate(Long jobId, PlanningRequest request) {
        return prepare(jobId, request, null).problem();
    }

    /**
     * Validates, normalizes, verifies, builds the candidate index and estimates the model.
     *
     * @param jobId          the id of the persisted plan job, or {@code null}
     * @param request        the request
     * @param engineOverride engine id or alias replacing {@code solverOptions.engine}, or {@code null}
     * @return the compiled problem
     * @throws LimitExceededException      when a size limit is crossed
     * @throws RequestValidationException  when validation found errors
     * @throws SolveCancelledException     never with the default token
     */
    public CompiledProblem compile(Long jobId, PlanningRequest request, String engineOverride) {
        return compile(jobId, request, engineOverride, new SolverCancellationToken());
    }

    /**
     * Same as {@link #compile(Long, PlanningRequest, String)} with a cancellation token checked while building.
     *
     * @throws SolveCancelledException when stop was requested or the time budget is exhausted while building
     */
    public CompiledProblem compile(Long jobId, PlanningRequest request, String engineOverride,
                                   SolverCancellationToken token) {
        Prepared prepared = prepare(jobId, request, engineOverride);
        CandidateIndex index = CandidateIndexBuilder.build(prepared.problem(), prepared.verified().constraints(),
                limits, token);
        ModelCost cost = estimator.estimate(index, prepared.verified().constraints(), prepared.verified().objectives());
        estimator.enforce(cost, index);
        return new CompiledProblem(prepared.problem(), index, cost, prepared.verified().constraints(),
                prepared.verified().objectives());
    }

    private record Prepared(PlanningProblem problem, ProblemVerifier.Verified verified) {
    }

    private Prepared prepare(Long jobId, PlanningRequest request, String engineOverride) {
        throwIfErrors(requestValidator.collect(request));
        PlanningProblem problem = adjust(normalizer.normalize(jobId, request), engineOverride);
        SolverEngine engine = solverRegistry.get(problem.options().engine());
        ValidationResult result = new ValidationResult();
        ProblemVerifier.Verified verified = verifier.verifySemantics(problem, result);
        if (!result.hasErrors()) {
            verifier.checkCapabilities(problem, engine.capabilities(), result);
            if (Boolean.TRUE.equals(problem.options().fallbackToTimefold())) {
                SolverEngine fallbackEngine = solverRegistry.find(SolverOptions.ENGINE_TIMEFOLD).orElse(null);
                if (fallbackEngine == null) {
                    result.error("UNKNOWN_SOLVER", "solverOptions.fallbackToTimefold",
                            "Fallback solver 'timefold' is not supported",
                            Map.of("engine", SolverOptions.ENGINE_TIMEFOLD));
                } else {
                    verifier.checkCapabilities(problem, fallbackEngine.capabilities(), result);
                }
            }
        }
        throwIfErrors(result);
        if (!result.warnings().isEmpty()) {
            log.debug("Request validated with {} warning(s)", result.warnings().size());
        }
        return new Prepared(problem, verified);
    }

    /** Maps the first error to the Phase 1 exception types, otherwise raises {@link RequestValidationException}. */
    private void throwIfErrors(ValidationResult result) {
        if (!result.hasErrors()) {
            return;
        }
        ValidationResult.Issue first = result.firstError();
        Map<String, Object> args = first.args();
        switch (first.code()) {
            case LimitExceededException.CODE:
                if (args.containsKey("limit")) {
                    throw new LimitExceededException(String.valueOf(args.get("limit")),
                            ((Number) args.get("actual")).longValue(), ((Number) args.get("max")).longValue());
                }
                break;
            case "UNKNOWN_CONSTRAINT":
                constraintRegistry.get(String.valueOf(args.get("type"))); // throws UnknownConstraintException
                break;
            case "UNKNOWN_OBJECTIVE":
                objectiveRegistry.get(String.valueOf(args.get("type"))); // throws UnknownObjectiveException
                break;
            default:
                break;
        }
        throw new RequestValidationException(result);
    }

    private PlanningProblem adjust(PlanningProblem problem, String engineOverride) {
        SolverOptions options = problem.options();
        String engine = engineOverride == null || engineOverride.isBlank()
                ? options.engine() : SolverOptions.normalizeEngine(engineOverride);
        int maxWorkers = Math.max(1, limits.getMaxWorkers());
        int workers = options.numWorkers();
        if (workers > maxWorkers) {
            log.debug("Clamping solverOptions.numWorkers {} to app.plan.limits.max-workers {}", workers, maxWorkers);
            workers = maxWorkers;
        }
        if (engine.equals(options.engine()) && workers == options.numWorkers()) {
            return problem;
        }
        SolverOptions adjusted = new SolverOptions(engine, options.maxTimeSeconds(), workers, options.seed(),
                options.logSearch(), options.relaxationLevels(), options.deterministic(), options.objectiveScale(),
                options.gapTolerance(), options.lexicographicMode(), options.explainInfeasibility(),
                options.explainTimeSeconds(), options.keepIncumbentOnStop(), options.fallbackToTimefold());
        return new PlanningProblem(problem.jobId(), problem.domainName(), problem.timezone(), problem.horizon(),
                problem.slots(), problem.calendars(), problem.setupMatrices(), problem.resources(), problem.tasks(),
                problem.constraints(), problem.objectives(), adjusted, problem.initialSolution());
    }

    // ==================================================================================================
    // validate (POST /validate)
    // ==================================================================================================

    /**
     * Validates a request and reports every issue, the compatibility with every engine and the model estimate.
     *
     * @param request      the request
     * @param engineOrNull engine whose compatibility decides {@code valid} (default: {@code solverOptions.engine})
     * @return the report (never {@code null}; user errors are issues, not exceptions)
     */
    public ValidationReportDto validate(PlanningRequest request, String engineOrNull) {
        ValidationResult all = new ValidationResult();
        ValidationResult stage1 = requestValidator.collect(request);
        all.merge(stage1);
        if (stage1.hasErrors()) {
            return report(all, null, Map.of(), Map.of(), null);
        }
        PlanningProblem problem;
        try {
            problem = normalizer.normalize(null, request);
        } catch (InvalidRequestException e) {
            all.error(e.code(), "", e.getMessage());
            return report(all, null, Map.of(), Map.of(), null);
        }
        ValidationResult stage3 = new ValidationResult();
        ProblemVerifier.Verified verified = verifier.verifySemantics(problem, stage3);
        all.merge(stage3);

        String requested = SolverOptions.normalizeEngine(
                engineOrNull == null || engineOrNull.isBlank() ? problem.options().engine() : engineOrNull);
        Map<String, Boolean> compatible = new LinkedHashMap<>();
        Map<String, List<ValidationReportDto.Issue>> engineIssues = new LinkedHashMap<>();
        boolean requestedKnown = false;
        for (String id : solverRegistry.supportedEngines()) {
            ValidationResult capability = new ValidationResult();
            if (!stage3.hasErrors()) {
                verifier.checkCapabilities(problem, solverRegistry.get(id).capabilities(), capability);
            }
            compatible.put(id, !stage3.hasErrors() && !capability.hasErrors());
            List<ValidationReportDto.Issue> issues = new ArrayList<>();
            capability.issues().forEach(issue -> issues.add(issue.toDto()));
            engineIssues.put(id, issues);
            boolean isRequested = id.equals(SolverRegistry.canonicalId(requested));
            boolean isFallback = Boolean.TRUE.equals(problem.options().fallbackToTimefold())
                    && SolverOptions.ENGINE_TIMEFOLD.equals(id);
            if (isRequested || isFallback) {
                requestedKnown |= isRequested;
                all.merge(capability);
            }
        }
        if (Boolean.TRUE.equals(problem.options().fallbackToTimefold())
                && !solverRegistry.supportedEngines().contains(SolverOptions.ENGINE_TIMEFOLD)) {
            all.error("UNKNOWN_SOLVER", "solverOptions.fallbackToTimefold",
                    "Fallback solver 'timefold' is not supported",
                    Map.of("engine", SolverOptions.ENGINE_TIMEFOLD));
        }
        if (!requestedKnown) {
            all.error("UNKNOWN_SOLVER", "solverOptions.engine", "Solver '" + requested
                    + "' is not supported. Supported solvers: " + solverRegistry.supportedEngines(),
                    Map.of("engine", requested));
        }
        ModelEstimateDto estimate = null;
        if (!stage3.hasErrors()) {
            estimate = estimateQuietly(problem, verified, all);
        }
        return report(all, request, compatible, engineIssues, estimate, problem);
    }

    private static SolveResult withFallbackNotice(SolveResult result) {
        Diagnostics previous = result.diagnostics();
        List<DiagnosticIssue> warnings = new ArrayList<>(
                previous == null ? List.of() : previous.warnings());
        warnings.add(new DiagnosticIssue("ENGINE_FALLBACK", "WARNING", "solverOptions.fallbackToTimefold",
                "CP-SAT returned UNKNOWN without a solution; Timefold was tried."));
        Map<String, Object> details = new LinkedHashMap<>();
        if (previous != null) {
            details.putAll(previous.details());
        }
        details.put("fallbackFromEngine", SolverOptions.ENGINE_CP_SAT);
        details.put("fallbackToEngine", SolverOptions.ENGINE_TIMEFOLD);
        String message = previous == null || previous.message() == null
                ? "Timefold result after CP-SAT returned UNKNOWN without a solution"
                : previous.message() + " (retried with Timefold after CP-SAT returned UNKNOWN without a solution)";
        Diagnostics diagnostics = new Diagnostics(message,
                previous == null ? List.of() : previous.infeasibilityCore(),
                previous == null ? null : previous.core(), warnings, details);
        return new SolveResult(result.status(), result.solution(), result.relaxations(), diagnostics, result.metrics());
    }

    private ModelEstimateDto estimateQuietly(PlanningProblem problem, ProblemVerifier.Verified verified,
                                             ValidationResult all) {
        try {
            CandidateIndex index = CandidateIndexBuilder.build(problem, verified.constraints(), limits,
                    new SolverCancellationToken());
            ModelCost cost = estimator.estimate(index, verified.constraints(), verified.objectives());
            ModelEstimateDto dto = estimator.toDto(cost, index, verified.constraints(), verified.objectives());
            try {
                estimator.enforce(cost, index);
            } catch (LimitExceededException e) {
                limitIssue(all, e);
            }
            return dto;
        } catch (LimitExceededException e) {
            limitIssue(all, e);
        } catch (InvalidRequestException e) {
            all.error(e.code(), "", e.getMessage());
        } catch (SolveCancelledException e) {
            all.warning("ESTIMATE_INTERRUPTED", "", e.getMessage());
        }
        return null;
    }

    private static void limitIssue(ValidationResult all, LimitExceededException e) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("limit", e.limitName());
        args.put("max", e.limit());
        all.error(LimitExceededException.CODE, "", e.getMessage(), args);
    }

    private ValidationReportDto report(ValidationResult all, PlanningRequest request,
                                       Map<String, Boolean> compatible,
                                       Map<String, List<ValidationReportDto.Issue>> engineIssues,
                                       ModelEstimateDto estimate) {
        return report(all, request, compatible, engineIssues, estimate, null);
    }

    private ValidationReportDto report(ValidationResult all, PlanningRequest request,
                                       Map<String, Boolean> compatible,
                                       Map<String, List<ValidationReportDto.Issue>> engineIssues,
                                       ModelEstimateDto estimate, PlanningProblem problem) {
        List<ValidationReportDto.Issue> errors = new ArrayList<>();
        List<ValidationReportDto.Issue> warnings = new ArrayList<>();
        for (ValidationResult.Issue issue : all.issues()) {
            (issue.isError() ? errors : warnings).add(issue.toDto());
        }
        PlanningRequest normalized = request == null || problem == null ? null : new PlanningRequest(
                request.meta(), request.domain(), problem.horizon(), problem.slots(),
                problem.calendars(), problem.setupMatrices(), problem.resources(), problem.tasks(),
                problem.constraints(), problem.objectives(), problem.options(), problem.initialSolution());
        return ValidationReportDto.builder()
                .valid(!all.hasErrors())
                .normalizedRequest(normalized)
                .errors(errors)
                .warnings(warnings)
                .engineCompatible(new java.util.HashMap<>(compatible))
                .engineIssues(new java.util.HashMap<>(engineIssues))
                .estimate(estimate)
                .build();
    }
}
