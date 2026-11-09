package eu.isygoit.testsupport;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Pin;
import eu.isygoit.dto.request.PlanningMeta;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.RelaxationPolicy;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.SolverCancellationToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Programmatic builders for the golden scenarios G1-G14 of design 9.5 (IR 2.0) and the G7 cancellation model, plus loaders for
 * the JSON files and small request-copy helpers.
 *
 * <p>Contract: every {@code gN...()} method returns a request that is the SAME scenario as the corresponding JSON file in
 * {@code examples/} (or in {@code src/test/resources/requests/} for G12): same ids, durations, slots, constraints, objectives
 * and options; display-only fields (names, labels, domain hints, clientRef, tenant text) may differ. WP8b asserts that the
 * file and the builder give the same result. All builders are deterministic (no clock, no unseeded randomness) and return new immutable
 * records on every call, so they are safe to share between threads. Constraint params are built with ISO strings for instants
 * and plain maps, exactly as Jackson would deliver them from JSON.
 *
 * <p>Units: horizon ticks and durations are ticks; the tick length is stated per scenario ({@code tickSeconds}); time points
 * are {@link Instant}s aligned on that grid.
 *
 * <p>Expected outcomes of every scenario are documented on its builder and in {@code examples/README.md}; the assertions
 * live in {@code EngineV2GoldenTest} (WP8b).
 */
public final class ScenarioFixtures {

    /** Monday 2026-09-07 08:00Z, the common start of the workshop scenarios. */
    public static final Instant WORKSHOP_START = Instant.parse("2026-09-07T08:00:00Z");

    private ScenarioFixtures() {
    }

    // ================================================================== loaders

    /** Strict mapper (unknown properties fail), as used by the validation endpoint. */
    public static ObjectMapper strictMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    /** Loads {@code /requests/<name>} from the test classpath (G12: {@code school-timetable.json}). */
    public static PlanningRequest loadResource(String name) {
        String path = "/requests/" + name;
        try (InputStream in = ScenarioFixtures.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Test resource not found: " + path);
            }
            return strictMapper().readValue(in, PlanningRequest.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read test resource " + path, e);
        }
    }

    /**
     * Loads a golden file of {@code examples/} (for example {@code g8-jobshop.json}). The directory is taken from the system
     * property {@code plan.examples.dir}, else searched upwards from the working directory ({@code examples},
     * {@code ../examples}, {@code 08-plan-core/examples}).
     */
    public static PlanningRequest loadExample(String fileName) {
        Path file = examplesDir().resolve(fileName);
        try {
            return strictMapper().readValue(Files.readAllBytes(file), PlanningRequest.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read example " + file, e);
        }
    }

    private static Path examplesDir() {
        String configured = System.getProperty("plan.examples.dir");
        List<Path> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(Path.of(configured));
        }
        Path cwd = Path.of("").toAbsolutePath();
        candidates.add(cwd.resolve("examples"));
        candidates.add(cwd.resolve("../examples").normalize());
        candidates.add(cwd.resolve("08-plan-core/examples"));
        candidates.add(cwd.resolve("../08-plan-core/examples").normalize());
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("g1-capacity-feasible.json"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("examples/ directory not found (set -Dplan.examples.dir): tried " + candidates);
    }

    // ================================================================== request copy helpers

    /** Same request with another engine ({@code cp_sat} or {@code timefold}). */
    public static PlanningRequest withEngine(PlanningRequest r, String engine) {
        SolverOptions o = r.solverOptions();
        return withOptions(r, new SolverOptions(engine, o.maxTimeSeconds(), o.numWorkers(), o.seed(), o.logSearch(),
                o.relaxationLevels(), o.deterministic(), o.objectiveScale(), o.gapTolerance(), o.lexicographicMode(),
                o.explainInfeasibility(), o.explainTimeSeconds(), o.keepIncumbentOnStop()));
    }

    /** Same request with {@code deterministic} and {@code numWorkers} replaced (G13). */
    public static PlanningRequest withDeterminism(PlanningRequest r, boolean deterministic, int numWorkers) {
        SolverOptions o = r.solverOptions();
        return withOptions(r, new SolverOptions(o.engine(), o.maxTimeSeconds(), numWorkers, o.seed(), o.logSearch(),
                o.relaxationLevels(), deterministic, o.objectiveScale(), o.gapTolerance(), o.lexicographicMode(),
                o.explainInfeasibility(), o.explainTimeSeconds(), o.keepIncumbentOnStop()));
    }

    /** Same request with {@code objectiveScale} replaced (G6 with 1000). */
    public static PlanningRequest withObjectiveScale(PlanningRequest r, int objectiveScale) {
        SolverOptions o = r.solverOptions();
        return withOptions(r, new SolverOptions(o.engine(), o.maxTimeSeconds(), o.numWorkers(), o.seed(), o.logSearch(),
                o.relaxationLevels(), o.deterministic(), objectiveScale, o.gapTolerance(), o.lexicographicMode(),
                o.explainInfeasibility(), o.explainTimeSeconds(), o.keepIncumbentOnStop()));
    }

    /** Same request with {@code maxTimeSeconds} replaced. */
    public static PlanningRequest withMaxTime(PlanningRequest r, int maxTimeSeconds) {
        SolverOptions o = r.solverOptions();
        return withOptions(r, new SolverOptions(o.engine(), maxTimeSeconds, o.numWorkers(), o.seed(), o.logSearch(),
                o.relaxationLevels(), o.deterministic(), o.objectiveScale(), o.gapTolerance(), o.lexicographicMode(),
                o.explainInfeasibility(), null, o.keepIncumbentOnStop()));
    }

    public static PlanningRequest withOptions(PlanningRequest r, SolverOptions options) {
        return new PlanningRequest(r.meta(), r.domain(), r.horizon(), r.slots(), r.calendars(),
                r.setupMatrices(), r.resources(), r.tasks(), r.constraints(), r.objectives(), options,
                r.initialSolution());
    }

    /** Same request with the constraints replaced. */
    public static PlanningRequest withConstraints(PlanningRequest r, List<Constraint> constraints) {
        return new PlanningRequest(r.meta(), r.domain(), r.horizon(), r.slots(), r.calendars(),
                r.setupMatrices(), r.resources(), r.tasks(), constraints, r.objectives(), r.solverOptions(),
                r.initialSolution());
    }

    /** Same request with the objectives replaced. */
    public static PlanningRequest withObjectives(PlanningRequest r, List<Objective> objectives) {
        return new PlanningRequest(r.meta(), r.domain(), r.horizon(), r.slots(), r.calendars(),
                r.setupMatrices(), r.resources(), r.tasks(), r.constraints(), objectives, r.solverOptions(),
                r.initialSolution());
    }

    /** Same request with a different {@code meta.clientRef} (idempotency key at job level). */
    public static PlanningRequest withClientRef(PlanningRequest r, String clientRef) {
        PlanningMeta m = r.meta();
        return new PlanningRequest(new PlanningMeta(clientRef, m.version(), m.domainName(), m.locale(),
                m.timezone()), r.domain(), r.horizon(), r.slots(), r.calendars(), r.setupMatrices(), r.resources(),
                r.tasks(), r.constraints(), r.objectives(), r.solverOptions(), r.initialSolution());
    }

    // ================================================================== small builders

    /** Fluent builder for {@link Task} (the record has 20 components). */
    public static final class TaskBuilder {
        private final String id;
        private Integer duration;
        private Integer demand;
        private Integer priority;
        private Boolean required;
        private Long prize;
        private final List<ResourceRequirement> requirements = new ArrayList<>();
        private Instant release;
        private Instant deadline;
        private Instant dueDate;
        private List<String> startSlots;
        private Pin pin;
        private final List<PrecedenceRef> predecessors = new ArrayList<>();
        private String setupClass;
        private final Set<String> tags = new LinkedHashSet<>();

        private TaskBuilder(String id) {
            this.id = id;
        }

        public TaskBuilder duration(Integer ticks) { this.duration = ticks; return this; }
        public TaskBuilder demand(int demand) { this.demand = demand; return this; }
        public TaskBuilder priority(int priority) { this.priority = priority; return this; }
        public TaskBuilder optional() { this.required = Boolean.FALSE; return this; }
        public TaskBuilder prize(long prize) { this.prize = prize; return this; }
        public TaskBuilder release(Instant release) { this.release = release; return this; }
        public TaskBuilder deadline(Instant deadline) { this.deadline = deadline; return this; }
        public TaskBuilder dueDate(Instant dueDate) { this.dueDate = dueDate; return this; }
        public TaskBuilder startSlots(String... slotIds) { this.startSlots = List.of(slotIds); return this; }
        public TaskBuilder setupClass(String setupClass) { this.setupClass = setupClass; return this; }
        public TaskBuilder tags(String... tags) { Collections.addAll(this.tags, tags); return this; }

        public TaskBuilder requires(String role, ResourceSelector selector) {
            requirements.add(new ResourceRequirement(role, selector, null, null));
            return this;
        }

        public TaskBuilder requires(String role, ResourceSelector selector, int count, Integer amount) {
            requirements.add(new ResourceRequirement(role, selector, count, amount));
            return this;
        }

        public TaskBuilder pin(Instant start, String... resourceIds) {
            this.pin = new Pin(start, resourceIds.length == 0 ? null : List.of(resourceIds));
            return this;
        }

        /** FINISH_START predecessor with lag 0. */
        public TaskBuilder after(String taskId) {
            predecessors.add(new PrecedenceRef(taskId, null, null, null, null));
            return this;
        }

        public TaskBuilder after(String taskId, PrecedenceRef.Type type, int minLag) {
            predecessors.add(new PrecedenceRef(taskId, type, minLag, null, null));
            return this;
        }

        public Task build() {
            return new Task(id, null, null, duration, null, demand, priority, required, prize,
                    requirements, release, deadline, dueDate, startSlots, pin, predecessors, setupClass,
                    tags, null, null);
        }
    }

    public static TaskBuilder task(String id) {
        return new TaskBuilder(id);
    }

    /** Selector restricted to the given resource ids. */
    public static ResourceSelector onResources(String... resourceIds) {
        return new ResourceSelector(List.of(resourceIds), null, null, null, null, null);
    }

    /** Selector requiring ALL the given skills. */
    public static ResourceSelector withSkills(String... skills) {
        return new ResourceSelector(null, null, List.of(skills), null, null, null);
    }

    /** Unary resource (capacity 1) of the given type. */
    public static Resource resource(String id, String typeId) {
        return new Resource(id, typeId, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** Resource with explicit capacity and skills. */
    public static Resource resource(String id, String typeId, int capacity, String... skills) {
        return new Resource(id, typeId, null, capacity, skills.length == 0 ? null : Set.of(skills), null, null,
                null, null, null, null, null, null);
    }

    /** Resource with a per-tick cost and optional unavailable windows. */
    public static Resource costedResource(String id, String typeId, double costPerTick, TimeWindow... unavailable) {
        return new Resource(id, typeId, null, 1, null, null, null,
                unavailable.length == 0 ? null : List.of(unavailable), costPerTick, null, null, null, null);
    }

    public static Slot slot(String id, Instant start, Instant end, double cost) {
        return new Slot(id, "period", null, start, end, cost, null);
    }

    public static Objective objective(String id, String type, int weight, Integer tier, Map<String, Object> params) {
        return new Objective(id, type, weight, tier, null, null, params == null ? Map.of() : params);
    }

    public static Constraint hard(String id, String type, ConstraintScope scope, Map<String, Object> params) {
        return new Constraint(id, type, IEnumEnforcement.Types.STRICTLY_REQUIRED, scope, params, null, 0, null);
    }

    public static SolverOptions options(int maxTimeSeconds, int numWorkers, boolean deterministic) {
        return new SolverOptions("cp_sat", maxTimeSeconds, numWorkers, 0L, null, null, deterministic, null, null,
                null, null, null, null);
    }

    private static PlanningRequest request(String clientRef, String domainName, String timezone, Horizon horizon,
                                           List<Slot> slots, List<SetupMatrix> setups,
                                           List<Resource> resources, List<Task> tasks, List<Constraint> constraints,
                                           List<Objective> objectives, SolverOptions options) {
        return new PlanningRequest(new PlanningMeta(clientRef, PlanningMeta.SCHEMA_VERSION, domainName, "en-US", timezone),
                null, horizon, slots, null, setups, resources, tasks, constraints, objectives, options, null);
    }

    private static Horizon workshopHorizon(int ticks) {
        return new Horizon(WORKSHOP_START, WORKSHOP_START.plusSeconds(ticks * 60L), 60);
    }

    private static Instant minute(int minutesAfterStart) {
        return WORKSHOP_START.plusSeconds(minutesAfterStart * 60L);
    }

    // ================================================================== G1, G2, G2b, G3 (slot mode and infeasibility)

    private static final Instant LAB_START = Instant.parse("2026-09-07T06:00:00Z");

    private static Horizon labHorizon() {
        return new Horizon(LAB_START, LAB_START.plusSeconds(2 * 3600L), 3600);
    }

    /**
     * G1: 3 one-hour sessions, 1 lab with capacity 2, slots {@code slot-early} (cost 0) and {@code slot-late} (cost 1.0),
     * {@code minimize_slot_cost}. Expected: OPTIMAL (timefold FEASIBLE), 2 sessions in early, 1 in late, tier-0 value 100.
     */
    public static PlanningRequest g1CapacityFeasible() {
        List<Slot> slots = List.of(
                slot("slot-early", LAB_START, LAB_START.plusSeconds(3600), 0.0),
                slot("slot-late", LAB_START.plusSeconds(3600), LAB_START.plusSeconds(7200), 1.0));
        List<Task> tasks = List.of(
                task("session-a").startSlots("slot-early", "slot-late").requires("lab", withSkills("lab")).build(),
                task("session-b").startSlots("slot-early", "slot-late").requires("lab", withSkills("lab")).build(),
                task("session-c").startSlots("slot-early", "slot-late").requires("lab", withSkills("lab")).build());
        return request("golden-g1", "lab-sessions", "Europe/Paris", labHorizon(), slots, null,
                List.of(resource("lab-1", "lab", 2, "lab")), tasks, List.of(),
                List.of(new Objective("prefer-cheap-slots", "minimize_slot_cost", 1)), options(10, 2, false));
    }

    /**
     * G2: a required task needs skill {@code welding}, no resource has it. Expected: INFEASIBLE, no solution,
     * {@code core.method=PRESOLVE_ANALYSIS}, items {@code [TASK weld-job NO_ELIGIBLE_RESOURCE]}.
     */
    public static PlanningRequest g2InfeasibleCapability() {
        List<Slot> slots = List.of(slot("slot-early", LAB_START, LAB_START.plusSeconds(3600), 0.0));
        List<Task> tasks = List.of(
                task("weld-job").startSlots("slot-early").requires("welder", withSkills("welding")).build());
        return request("golden-g2", "lab-sessions", "Europe/Paris", labHorizon(), slots, null,
                List.of(resource("lab-1", "lab", 1, "chemistry")), tasks, List.of(),
                List.of(new Objective("prefer-cheap-slots", "minimize_slot_cost", 1)), options(10, 2, false));
    }

    /**
     * G2b: A and B pinned at tick 0 on two different machines plus a hard {@code no_overlap} {@code no-overlap-ab} over tag
     * {@code ab}. Expected: INFEASIBLE, core method ASSUMPTIONS or ASSUMPTIONS_SHRUNK with CONSTRAINT no-overlap-ab,
     * TASK A, TASK B (not found by the pre-solve analysis).
     */
    public static PlanningRequest g2bInfeasibleCore() {
        List<Task> tasks = List.of(
                task("A").duration(10).tags("ab").pin(WORKSHOP_START, "M1")
                        .requires("machine", onResources("M1", "M2")).build(),
                task("B").duration(10).tags("ab").pin(WORKSHOP_START, "M2")
                        .requires("machine", onResources("M1", "M2")).build());
        return request("golden-g2b", "workshop", "UTC", workshopHorizon(60), null, null,
                List.of(resource("M1", "machine"), resource("M2", "machine")), tasks,
                List.of(hard("no-overlap-ab", "no_overlap", new ConstraintScope("tag", "ab"), Map.of())),
                List.of(new Objective("makespan", "minimize_makespan", 1)), options(10, 1, false));
    }

    /**
     * G3: 1 required + 3 optional tasks (priority 9/5/1), one lab of capacity 1, two one-hour slots,
     * {@code maximize_task_priority}. Expected: OPTIMAL (timefold FEASIBLE), {@code mandatory-session} and
     * {@code optional-high} assigned, medium/low unassigned (NOT_SELECTED), tier-0 score 9.0.
     */
    public static PlanningRequest g3OptionalPriorities() {
        List<Slot> slots = List.of(
                slot("slot-1", LAB_START, LAB_START.plusSeconds(3600), 0.0),
                slot("slot-2", LAB_START.plusSeconds(3600), LAB_START.plusSeconds(7200), 0.0));
        List<Task> tasks = List.of(
                task("mandatory-session").startSlots("slot-1", "slot-2").requires("lab", withSkills("lab")).build(),
                task("optional-high").optional().priority(9).startSlots("slot-1", "slot-2")
                        .requires("lab", withSkills("lab")).build(),
                task("optional-medium").optional().priority(5).startSlots("slot-1", "slot-2")
                        .requires("lab", withSkills("lab")).build(),
                task("optional-low").optional().priority(1).startSlots("slot-1", "slot-2")
                        .requires("lab", withSkills("lab")).build());
        return request("golden-g3", "lab-sessions", "Europe/Paris", labHorizon(), slots, null,
                List.of(resource("lab-1", "lab", 1, "lab")), tasks, List.of(),
                List.of(new Objective("prefer-high-priority", "maximize_task_priority", 1)), options(10, 2, false));
    }

    // ================================================================== G4, G5, G6 (free start)

    /**
     * G4: t1(3) -> t2(4) -> t3(5) on one machine, FS with lag 0 then minLag 2, {@code minimize_makespan}.
     * Expected (cp_sat): OPTIMAL, starts 0, 3, 9, ends 3, 7, 14, makespan 14. Timefold must reject PRECEDENCES + FREE_START.
     */
    public static PlanningRequest g4PrecedenceChain() {
        List<Task> tasks = List.of(
                task("t1").duration(3).requires("machine", onResources("M1")).build(),
                task("t2").duration(4).requires("machine", onResources("M1"))
                        .after("t1", PrecedenceRef.Type.FINISH_START, 0).build(),
                task("t3").duration(5).requires("machine", onResources("M1"))
                        .after("t2", PrecedenceRef.Type.FINISH_START, 2).build());
        return request("golden-g4", "workshop", "UTC", workshopHorizon(60), null, null,
                List.of(resource("M1", "machine")), tasks, List.of(),
                List.of(new Objective("makespan", "minimize_makespan", 1)), options(10, 2, true));
    }

    /**
     * G5: A, B (4 ticks) on one machine, {@code time_window} latestEnd tick 6 for both as RELAXABLE ({@code deadline-A}
     * priority 5, {@code deadline-B} priority 0), {@code minimize_makespan}. Expected: OPTIMAL, exactly ONE relaxation
     * {@code deadline-B} (violationCount 1, measure 2), makespan 8, {@code Metrics.tiers[-2].value = 1000}.
     */
    public static PlanningRequest g5Relaxation() {
        List<Task> tasks = List.of(
                task("A").duration(4).requires("machine", onResources("M1")).build(),
                task("B").duration(4).requires("machine", onResources("M1")).build());
        Map<String, Object> latestEnd6 = Map.of("latestEnd", minute(6).toString());
        List<Constraint> constraints = List.of(
                new Constraint("deadline-A", "time_window", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION,
                        new ConstraintScope("task", "A"), latestEnd6, new RelaxationPolicy(true, 5, 1.0), 0, null),
                new Constraint("deadline-B", "time_window", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION,
                        new ConstraintScope("task", "B"), latestEnd6, new RelaxationPolicy(true, 0, 1.0), 0, null));
        return request("golden-g5", "workshop", "UTC", workshopHorizon(60), null, null,
                List.of(resource("M1", "machine")), tasks, constraints,
                List.of(new Objective("makespan", "minimize_makespan", 1)), options(10, 2, true));
    }

    /**
     * G6: 3 tasks of 60 ticks; {@code cheap} (costPerTick 1.0, unavailable from tick 120) and {@code expensive}
     * (costPerTick 3.0); {@code minimize_resource_cost}; horizon 240. Expected: OPTIMAL, 2 tasks on cheap, 1 on expensive,
     * tier-0 value 30000 (score 300.0); with objectiveScale 1000 value 300000 ({@link #withObjectiveScale}).
     */
    public static PlanningRequest g6CostMinimisation() {
        ResourceSelector both = onResources("cheap", "expensive");
        List<Task> tasks = List.of(
                task("job-1").duration(60).requires("technician", both).build(),
                task("job-2").duration(60).requires("technician", both).build(),
                task("job-3").duration(60).requires("technician", both).build());
        List<Resource> resources = List.of(
                costedResource("cheap", "technician", 1.0, new TimeWindow(minute(120), minute(240))),
                costedResource("expensive", "technician", 3.0));
        return request("golden-g6", "field-service", "UTC", workshopHorizon(240), null, null, resources, tasks,
                List.of(), List.of(new Objective("cost", "minimize_resource_cost", 1)), options(10, 2, false));
    }

    // ================================================================== G7 (cancellation model)

    /**
     * G7 default instance: seeded 30 jobs x 15 machines job-shop, {@code maxTimeSeconds=60}, {@code minimize_makespan}, 4
     * workers. Expected when the token is cancelled after 2 s: STOPPED_FEASIBLE (UNKNOWN only without incumbent), returned
     * at most 5 s after the cancel, {@code search.solutionsFound >= 1} when STOPPED_FEASIBLE.
     */
    public static PlanningRequest g7BigJobShop() {
        return bigJobShop(30, 15, 7L, 60, 4);
    }

    /**
     * Seeded job-shop generator: {@code jobs} jobs, each visiting every one of the {@code machines} machines once in a
     * seeded random order, durations 1..20 ticks of 60 s, chained FINISH_START. The horizon is the sum of all durations plus
     * one tick, so the serial schedule is always feasible (no infeasibility, only a hard optimisation). Task ids are
     * {@code j<job>-o<operation>}, resources {@code M<machine>}.
     */
    public static PlanningRequest bigJobShop(int jobs, int machines, long seed, int maxTimeSeconds, int numWorkers) {
        Random random = new Random(seed);
        List<Resource> resources = new ArrayList<>(machines);
        for (int m = 0; m < machines; m++) {
            resources.add(resource("M" + m, "machine"));
        }
        List<Task> tasks = new ArrayList<>(jobs * machines);
        long totalTicks = 0;
        for (int j = 0; j < jobs; j++) {
            List<Integer> order = new ArrayList<>(machines);
            for (int m = 0; m < machines; m++) {
                order.add(m);
            }
            Collections.shuffle(order, random);
            String previous = null;
            for (int o = 0; o < machines; o++) {
                int duration = 1 + random.nextInt(20);
                totalTicks += duration;
                String id = "j" + j + "-o" + o;
                TaskBuilder builder = task(id).duration(duration).tags("job-" + j)
                        .requires("machine", onResources("M" + order.get(o)));
                if (previous != null) {
                    builder.after(previous);
                }
                tasks.add(builder.build());
                previous = id;
            }
        }
        return request("golden-g7-" + jobs + "x" + machines, "workshop", "UTC",
                workshopHorizon(Math.toIntExact(totalTicks + 1)), null, null, resources, tasks, List.of(),
                List.of(new Objective("makespan", "minimize_makespan", 1)),
                new SolverOptions("cp_sat", maxTimeSeconds, numWorkers, 0L, null, null, null, null, null, null, null,
                        null, null));
    }

    /**
     * Starts a daemon thread that cancels {@code token} after {@code millis} ms (G7 harness). Returns the thread so the
     * test can join it.
     */
    public static Thread cancelAfter(SolverCancellationToken token, long millis) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            token.cancel();
        }, "g7-cancel-after-" + millis);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    // ================================================================== G8, G9, G10, G11, G13, G14

    /**
     * G8 (design 2.6 b): 3 jobs x 2 operations on 2 machines. Expected: OPTIMAL, makespan 75, precedence respected,
     * machines never overlap. G13 = this request with {@code deterministic=true} (already set) run twice, also with
     * {@code numWorkers=4} ({@link #withDeterminism}).
     */
    public static PlanningRequest g8JobShop() {
        List<Task> tasks = List.of(
                task("A1").duration(30).tags("job-A").requires("machine", onResources("M1")).build(),
                task("A2").duration(20).tags("job-A").requires("machine", onResources("M2")).after("A1").build(),
                task("B1").duration(25).tags("job-B").requires("machine", onResources("M2")).build(),
                task("B2").duration(15).tags("job-B").requires("machine", onResources("M1")).after("B1").build(),
                task("C1").duration(20).tags("job-C").requires("machine", onResources("M1")).build(),
                task("C2").duration(30).tags("job-C").requires("machine", onResources("M2")).after("C1").build());
        return request("golden-g8", "workshop", "UTC", workshopHorizon(480), null, null,
                List.of(resource("M1", "machine"), resource("M2", "machine")), tasks, List.of(),
                List.of(new Objective("makespan", "minimize_makespan", 1)), options(10, 4, true));
    }

    /**
     * G9 (design 2.6 c): 5 pinned shifts, 3 nurses, min rest 11, max 8 ticks per day, skills, fairness. Expected: OPTIMAL,
     * {@code minimize_max_load} tier-0 value 16, {@code d1-night} on {@code ana}, no soft violation.
     */
    public static PlanningRequest g9Roster() {
        Instant start = Instant.parse("2026-09-07T00:00:00Z");
        Horizon horizon = new Horizon(start, Instant.parse("2026-09-10T00:00:00Z"), 3600);
        ResourceSelector nurse = withSkills("nurse");
        ResourceSelector icuNurse = withSkills("nurse", "icu");
        List<Resource> resources = List.of(
                resource("ana", "nurse", 1, "nurse", "icu"),
                resource("ben", "nurse", 1, "nurse"),
                new Resource("cleo", "nurse", null, null, Set.of("nurse"), null, null,
                        List.of(new TimeWindow(Instant.parse("2026-09-07T08:00:00Z"),
                                Instant.parse("2026-09-07T16:00:00Z"))), null, null, null, null, null));
        List<Task> tasks = List.of(
                task("d0-day").duration(8).pin(Instant.parse("2026-09-07T08:00:00Z")).requires("nurse", nurse).build(),
                task("d0-night").duration(8).pin(Instant.parse("2026-09-07T22:00:00Z")).requires("nurse", nurse).build(),
                task("d1-day").duration(8).pin(Instant.parse("2026-09-08T08:00:00Z")).requires("nurse", nurse).build(),
                task("d1-night").duration(8).pin(Instant.parse("2026-09-08T22:00:00Z")).requires("nurse", icuNurse).build(),
                task("d2-day").duration(8).pin(Instant.parse("2026-09-09T08:00:00Z")).requires("nurse", nurse).build());
        List<Constraint> constraints = List.of(
                hard("rest-11h", "min_rest", ConstraintScope.all(), Map.of("minRestTicks", 11)),
                hard("one-shift-per-day", "max_load_per_period", ConstraintScope.all(),
                        Map.of("period", Map.of("unit", "DAY"), "metric", "TICKS", "max", 8)),
                new Constraint("max-2-days-in-a-row", "max_consecutive", IEnumEnforcement.Types.SOFT,
                        ConstraintScope.all(), Map.of("period", Map.of("unit", "DAY"), "max", 2), null, 10, null));
        return request("golden-g9", "roster", "UTC", horizon, null, null, resources, tasks,
                constraints, List.of(objective("fair", "minimize_max_load", 1, null, Map.of("metric", "TICKS"))),
                options(10, 2, true));
    }

    /**
     * G10 (design 2.6 d): crew pool of capacity 2, 5 activities, precedence, deadline day 12. Expected: OPTIMAL, makespan 11
     * (days), E ends at 11, pool load never above 2.
     */
    public static PlanningRequest g10Project() {
        Instant start = Instant.parse("2026-09-01T00:00:00Z");
        Horizon horizon = new Horizon(start, Instant.parse("2026-09-21T00:00:00Z"), 86_400);
        ResourceSelector pool = onResources("crew-pool");
        List<Task> tasks = List.of(
                task("A").duration(4).demand(1).requires("crew", pool).build(),
                task("B").duration(4).demand(1).requires("crew", pool).build(),
                task("C").duration(4).demand(2).requires("crew", pool).build(),
                task("D").duration(2).demand(1).requires("crew", pool).after("A").after("B").build(),
                task("E").duration(1).demand(1).deadline(Instant.parse("2026-09-13T00:00:00Z"))
                        .requires("crew", pool).after("C").after("D").build());
        return request("golden-g10", "project", "UTC", horizon, null, null,
                List.of(resource("crew-pool", "pool", 2)), tasks, List.of(),
                List.of(new Objective("makespan", "minimize_makespan", 1)), options(10, 2, false));
    }

    /**
     * G11 (design 2.6 e): 5 optional tasks, one technician, 480 ticks, no objective (the normalizer injects
     * {@code auto-selected-value} at tier -1). Expected: OPTIMAL, P1, P2, P5 selected, tier -1 score 190, P3 and P4
     * unassigned with forgonePrize 90 / 30.
     */
    public static PlanningRequest g11PrizeCollecting() {
        List<Task> tasks = List.of(
                task("P1").duration(120).optional().prize(50).build(),
                task("P2").duration(180).optional().prize(80).build(),
                task("P3").duration(240).optional().prize(90).build(),
                task("P4").duration(90).optional().prize(30).build(),
                task("P5").duration(150).optional().prize(60).build());
        return request("golden-g11", "field-service", "UTC", workshopHorizon(480), null, null,
                List.of(resource("tech-1", "technician")), tasks, List.of(), List.of(), options(10, 2, false));
    }

    /**
     * G14: 3 tasks (classes A, A, B) on one unary machine with setup 5 between different classes, {@code minimize_makespan}
     * (tier 0) + {@code minimize_setup} (tier 1). Expected: OPTIMAL, makespan 35, setup tier value 5, the A tasks adjacent.
     */
    public static PlanningRequest g14SetupTimes() {
        Resource press = new Resource("press-1", "machine", null, 1, null, null, null, null, null, null, null,
                "paint-setup", null);
        ResourceSelector onPress = onResources("press-1");
        List<Task> tasks = List.of(
                task("A1").duration(10).setupClass("A").requires("machine", onPress).build(),
                task("A2").duration(10).setupClass("A").requires("machine", onPress).build(),
                task("B1").duration(10).setupClass("B").requires("machine", onPress).build());
        return request("golden-g14", "workshop", "UTC", workshopHorizon(120), null,
                List.of(new SetupMatrix("paint-setup", 5, 0, List.of())), List.of(press), tasks, List.of(),
                List.of(objective("makespan", "minimize_makespan", 1, 0, null),
                        objective("setup", "minimize_setup", 1, 1, null)),
                options(10, 2, true));
    }

    // ================================================================== G12 (school timetable)

    /**
     * G12 (design 2.6 a): the school timetable as stored in {@code src/test/resources/requests/school-timetable.json}
     * (loaded, so the file is the single source). Expected: OPTIMAL (timefold FEASIBLE only without the class constraint,
     * see {@link #schoolTimetableWithoutNoOverlap()}), 2 lessons Monday, 2 Tuesday, tier-0 value 200 (score 2.0).
     */
    public static PlanningRequest schoolTimetable() {
        return loadResource("school-timetable.json");
    }

    /**
     * G12 for Timefold: the same request without the {@code class-no-overlap} constraint (design 4.3 gives {@code no_overlap}
     * no Timefold contributor; the lessons of one class never share a slot pair, so the optimum is unchanged).
     */
    public static PlanningRequest schoolTimetableWithoutNoOverlap() {
        return withConstraints(schoolTimetable(), List.of());
    }

    // ================================================================== catalogue of the file-backed scenarios

    /** Scenario id -> JSON file in {@code examples/} (G12 lives in the test resources, G7/G13 are generated). */
    public static Map<String, String> exampleFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("G1", "g1-capacity-feasible.json");
        files.put("G2", "g2-infeasible-capability.json");
        files.put("G2b", "g2b-infeasible-core.json");
        files.put("G3", "g3-optional-priorities.json");
        files.put("G4", "g4-precedence-chain.json");
        files.put("G5", "g5-relaxation.json");
        files.put("G6", "g6-cost-minimisation.json");
        files.put("G8", "g8-jobshop.json");
        files.put("G9", "g9-roster.json");
        files.put("G10", "g10-project.json");
        files.put("G11", "g11-prize-collecting.json");
        files.put("G14", "g14-setup-times.json");
        return Collections.unmodifiableMap(files);
    }

    /** Scenario id -> programmatic builder result, same keys as {@link #exampleFiles()} (programmatic twin of the files). */
    public static Map<String, PlanningRequest> programmaticScenarios() {
        Map<String, PlanningRequest> scenarios = new LinkedHashMap<>();
        scenarios.put("G1", g1CapacityFeasible());
        scenarios.put("G2", g2InfeasibleCapability());
        scenarios.put("G2b", g2bInfeasibleCore());
        scenarios.put("G3", g3OptionalPriorities());
        scenarios.put("G4", g4PrecedenceChain());
        scenarios.put("G5", g5Relaxation());
        scenarios.put("G6", g6CostMinimisation());
        scenarios.put("G8", g8JobShop());
        scenarios.put("G9", g9Roster());
        scenarios.put("G10", g10Project());
        scenarios.put("G11", g11PrizeCollecting());
        scenarios.put("G14", g14SetupTimes());
        return Collections.unmodifiableMap(scenarios);
    }
}
