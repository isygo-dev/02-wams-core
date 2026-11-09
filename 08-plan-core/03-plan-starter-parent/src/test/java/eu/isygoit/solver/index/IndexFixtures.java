package eu.isygoit.solver.index;

import eu.isygoit.dto.request.Calendar;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Pin;
import eu.isygoit.dto.request.PlanningMeta;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.PreprocessContext;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.ValidationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Programmatic builders shared by the WP2 tests (requests, problems, tasks, resources, a stub constraint family).
 * Horizon start is {@link #START}; tick {@code k} is {@code START + k * tickSeconds}.
 */
public final class IndexFixtures {

    /** Horizon start of every fixture. */
    public static final Instant START = Instant.parse("2026-09-07T08:00:00Z");

    private IndexFixtures() {
    }

    /** @return a horizon of {@code ticks} ticks of {@code tickSeconds} seconds starting at {@link #START} */
    public static Horizon horizon(int ticks, int tickSeconds) {
        return new Horizon(START, START.plusSeconds((long) ticks * tickSeconds), tickSeconds);
    }

    /** @return the instant of a tick */
    public static Instant at(int tick, int tickSeconds) {
        return START.plusSeconds((long) tick * tickSeconds);
    }

    /** @return a task builder */
    public static TaskB task(String id) {
        return new TaskB(id);
    }

    /** @return a resource builder */
    public static ResB resource(String id) {
        return new ResB(id);
    }

    /** @return a selector restricted to skills */
    public static ResourceSelector skills(String... skills) {
        return new ResourceSelector(null, null, Arrays.asList(skills), null, null, null);
    }

    /** @return a selector restricted to ids */
    public static ResourceSelector ids(String... ids) {
        return new ResourceSelector(Arrays.asList(ids), null, null, null, null, null);
    }

    /** @return a problem builder with a 1-minute tick horizon of {@code ticks} ticks */
    public static ProblemB problem(int ticks) {
        return new ProblemB(horizon(ticks, 60), 60);
    }

    /** @return a problem builder with a custom tick length */
    public static ProblemB problem(int ticks, int tickSeconds) {
        return new ProblemB(horizon(ticks, tickSeconds), tickSeconds);
    }

    /** Mutable task builder (all other fields default). */
    public static final class TaskB {
        private final String id;
        private String typeId;
        private Integer duration = 10;
        private Map<String, Integer> resourceDurations;
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
        private Set<String> tags;
        private List<eu.isygoit.dto.request.Preference> preferences;

        TaskB(String id) {
            this.id = id;
        }

        public TaskB type(String v) {
            typeId = v;
            return this;
        }

        public TaskB duration(Integer v) {
            duration = v;
            return this;
        }

        public TaskB resourceDurations(Map<String, Integer> v) {
            resourceDurations = v;
            return this;
        }

        public TaskB demand(int v) {
            demand = v;
            return this;
        }

        public TaskB priority(int v) {
            priority = v;
            return this;
        }

        public TaskB optional(long prize) {
            required = false;
            this.prize = prize;
            return this;
        }

        public TaskB requires(String role, ResourceSelector selector, Integer count, Integer amount) {
            requirements.add(new ResourceRequirement(role, selector, count, amount));
            return this;
        }

        public TaskB requires(ResourceSelector selector) {
            return requires("default", selector, 1, null);
        }

        public TaskB release(Instant v) {
            release = v;
            return this;
        }

        public TaskB deadline(Instant v) {
            deadline = v;
            return this;
        }

        public TaskB due(Instant v) {
            dueDate = v;
            return this;
        }

        public TaskB slots(String... v) {
            startSlots = Arrays.asList(v);
            return this;
        }

        public TaskB pin(Instant start, String... resourceIds) {
            pin = new Pin(start, Arrays.asList(resourceIds));
            return this;
        }

        public TaskB after(String predecessor) {
            predecessors.add(new PrecedenceRef(predecessor, null, null, null, null));
            return this;
        }

        public TaskB after(String predecessor, PrecedenceRef.Type type, int minLag) {
            predecessors.add(new PrecedenceRef(predecessor, type, minLag, null, null));
            return this;
        }

        public TaskB setupClass(String v) {
            setupClass = v;
            return this;
        }

        public TaskB tags(String... v) {
            tags = Set.of(v);
            return this;
        }

        public TaskB preference(eu.isygoit.dto.request.Preference p) {
            preferences = List.of(p);
            return this;
        }

        public Task build() {
            return new Task(id, typeId, null, duration, resourceDurations, demand, priority, required, prize,
                    requirements, release, deadline, dueDate, startSlots, pin, predecessors, setupClass, tags,
                    preferences, null);
        }
    }

    /** Mutable resource builder. */
    public static final class ResB {
        private final String id;
        private String typeId;
        private Integer capacity;
        private Set<String> skills;
        private Set<String> tags;
        private String calendarId;
        private List<TimeWindow> unavailable;
        private Double costPerTick;
        private Double costPerUse;
        private Integer maxLoadTicks;
        private String setupMatrixId;

        ResB(String id) {
            this.id = id;
        }

        public ResB type(String v) {
            typeId = v;
            return this;
        }

        public ResB capacity(int v) {
            capacity = v;
            return this;
        }

        public ResB skills(String... v) {
            skills = Set.of(v);
            return this;
        }

        public ResB tags(String... v) {
            tags = Set.of(v);
            return this;
        }

        public ResB calendar(String v) {
            calendarId = v;
            return this;
        }

        public ResB unavailable(Instant from, Instant to) {
            unavailable = List.of(new TimeWindow(from, to));
            return this;
        }

        public ResB costPerTick(double v) {
            costPerTick = v;
            return this;
        }

        public ResB costPerUse(double v) {
            costPerUse = v;
            return this;
        }

        public ResB maxLoad(int v) {
            maxLoadTicks = v;
            return this;
        }

        public ResB setupMatrix(String v) {
            setupMatrixId = v;
            return this;
        }

        public Resource build() {
            return new Resource(id, typeId, null, capacity, skills, tags, calendarId, unavailable, costPerTick,
                    costPerUse, maxLoadTicks, setupMatrixId, null);
        }
    }

    /** Mutable problem / request builder. */
    public static final class ProblemB {
        private final Horizon horizon;
        private final int tickSeconds;
        private final List<Slot> slots = new ArrayList<>();
        private final List<Calendar> calendars = new ArrayList<>();
        private final List<SetupMatrix> matrices = new ArrayList<>();
        private final List<Resource> resources = new ArrayList<>();
        private final List<Task> tasks = new ArrayList<>();
        private final List<Constraint> constraints = new ArrayList<>();
        private final List<Objective> objectives = new ArrayList<>();
        private SolverOptions options = SolverOptions.defaults();
        private InitialSolution initial = new InitialSolution(List.of());
        private PlanningMeta meta = new PlanningMeta("test", "2.0", "test", "en", "UTC");

        ProblemB(Horizon horizon, int tickSeconds) {
            this.horizon = horizon;
            this.tickSeconds = tickSeconds;
        }

        public int tickSeconds() {
            return tickSeconds;
        }

        public Instant at(int tick) {
            return IndexFixtures.at(tick, tickSeconds);
        }

        public ProblemB slot(String id, int fromTick, int toTick, double cost) {
            slots.add(new Slot(id, null, null, at(fromTick), at(toTick), cost, null));
            return this;
        }

        public ProblemB calendar(Calendar calendar) {
            calendars.add(calendar);
            return this;
        }

        public ProblemB matrix(SetupMatrix matrix) {
            matrices.add(matrix);
            return this;
        }

        public ProblemB resource(ResB r) {
            resources.add(r.build());
            return this;
        }

        public ProblemB resource(Resource r) {
            resources.add(r);
            return this;
        }

        public ProblemB task(TaskB t) {
            tasks.add(t.build());
            return this;
        }

        public ProblemB task(Task t) {
            tasks.add(t);
            return this;
        }

        public ProblemB constraint(Constraint c) {
            constraints.add(c);
            return this;
        }

        public ProblemB objective(Objective o) {
            objectives.add(o);
            return this;
        }

        public ProblemB options(SolverOptions o) {
            options = o;
            return this;
        }

        public ProblemB meta(PlanningMeta m) {
            meta = m;
            return this;
        }

        public ProblemB initial(InitialAssignment... assignments) {
            initial = new InitialSolution(Arrays.asList(assignments));
            return this;
        }

        public Horizon horizon() {
            return horizon;
        }

        public PlanningProblem problem() {
            return new PlanningProblem(null, "test", "UTC", horizon, slots, calendars, matrices, resources, tasks,
                    constraints, objectives, options, initial);
        }

        public PlanningRequest request() {
            return new PlanningRequest("tenant-a", meta, null, horizon, slots, calendars, matrices, resources, tasks,
                    constraints, objectives, options, initial);
        }
    }

    /** @return a HARD constraint of the given type with empty params */
    public static Constraint hard(String id, String type) {
        return new Constraint(id, type, IEnumEnforcement.Types.STRICTLY_REQUIRED, null, Map.of(), null, 0, null);
    }

    /** @return a compiled constraint of the stub family */
    public static CompiledConstraint compiled(int index, Constraint constraint, ConstraintFamily family) {
        return new CompiledConstraint(index, constraint, family, null, true);
    }

    /** Minimal {@link ConstraintFamily} whose {@code preprocess} runs a lambda and whose estimate is configurable. */
    public static final class StubFamily implements ConstraintFamily {
        private final String type;
        private final BiConsumer<PreprocessContext, Constraint> preprocess;
        private final ModelCost estimate;

        public StubFamily(String type, BiConsumer<PreprocessContext, Constraint> preprocess, ModelCost estimate) {
            this.type = type;
            this.preprocess = preprocess;
            this.estimate = estimate;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public Class<?> paramsType() {
            return Void.class;
        }

        @Override
        public Set<IEnumEnforcement.Types> supportedEnforcements() {
            return Set.of(IEnumEnforcement.Types.STRICTLY_REQUIRED, IEnumEnforcement.Types.CONFIGURABLE_HARD);
        }

        @Override
        public String descriptionKey() {
            return "plan.constraint." + type + ".description";
        }

        @Override
        public List<String> paramsHint() {
            return List.of();
        }

        @Override
        public ScopeKind scopeKind() {
            return ScopeKind.NONE;
        }

        @Override
        public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        }

        @Override
        public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
            return estimate;
        }

        @Override
        public void preprocess(PreprocessContext ctx, Constraint constraint, Object params) {
            if (preprocess != null) {
                preprocess.accept(ctx, constraint);
            }
        }

        @Override
        public ConstraintChecker checker() {
            return (ctx, constraint, params, sink) -> {
            };
        }
    }
}
