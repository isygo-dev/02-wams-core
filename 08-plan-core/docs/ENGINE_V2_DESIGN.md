# isygoPlan - Phase 3: Engine v2 - Detailed Design Specification

Status: DESIGN ONLY (no code changed, nothing compiled or run). Audience: the implementation agents of Phase 3.
Authority: `README.md` and `IMPLEMENTATION_PLAN.md` of this module (`Authority: Edyra Pilot` wins for conventions:
package layout, DTO/mapper/service patterns, exception + i18n mechanism, tenant scoping). Phase 0/1 behaviours (limits,
solve guard, cancellation token, STOPPING flow, job state machine, tenant scoping, Kafka/DLQ) are NOT touched except where
section 9/10 says so explicitly.

Owner decisions applied (binding): no client data, compatibility is broken freely (JSON requests, tests and golden
examples are rewritten); IR = immutable Java records (Jackson); CP-SAT (OR-Tools 9.15) is the reference engine and
implements the full v2 model; Timefold 1.28 keeps a documented SUBSET and every v2 feature it lacks is REJECTED at
validation (no silent ignore); the engine is chosen by `solverOptions.engine`.

Terminology: "task" = unit of work (job, operation, shift, lesson, project activity); "resource" = anything a task
consumes or occupies (machine, person, room, crane pool); "candidate" = one possible (task, requirement, resource) triple;
"tick" = the integer time unit of the problem; "tier" = lexicographic objective level (lower number = more important).

## Table of contents

0. Design summary and decision log
1. Time model
2. Request IR (records, JSON, five complete examples)
3. Result IR
4. Constraint and objective catalogue v2 (+ engine capability matrix)
5. Engine SPI (extensibility), CandidateIndex, registries, capability API
6. CP-SAT model formulation (pseudocode)
7. Validation (structural, capability, cost estimation, error codes)
8. Results post-processing (independent solution validator, determinism, ordering)
9. Effects on other modules (API/DTO, export, UI Phase 4, PlanJob.engine, docs, golden scenarios G1-G11)
10. Implementation plan (work packages, file ownership, waves, acceptance, tests)
11. Risks and open questions

---

## 0. Design summary and decision log

### 0.1 What changes in one paragraph

v1 = "assign each task to one (resource, slot) from a pre-enumerated slot list" (generalised assignment). v2 = a
generic **interval scheduling + assignment** model: every task has a `start` (integer tick variable), a `duration`,
optional presence, 1..n resource requirements, windows, predecessors; resources have capacity, skills, calendars; the
solver models intervals with `NoOverlap` / `Cumulative`. The v1 "slot" survives as a *start-anchor list*
(`Task.startSlots`): a special case of a free start variable restricted to a finite set of start ticks. The constraint /
objective handler SPI (per-triple filter+penalty, edited in both adapters) is replaced by an engine-neutral
**family** (validate / preprocess / estimate / check) plus per-engine **contributors**; new constraint families are added
by adding beans, never by editing the core.

### 0.2 Decision log (D-n). The owner may veto any of these; the recommended default is what the spec implements.

| # | Decision | Alternative rejected | Why |
|---|----------|----------------------|-----|
| D-1 | Time = integer ticks on a `Horizon(start, end, tickSeconds)`; all durations/lags in ticks; time points are ISO instants in the IR and are converted by the normalizer. | Instants everywhere in the solver; double time. | CP-SAT needs integers; ticks make horizon/size limits checkable; instants stay user-friendly. |
| D-2 | Slot mode is KEPT as `Task.startSlots` (+ `Slot` entity with typed `cost`), a special case of a start domain. | Remove slots (clients generate start grids). | Timetabling/rostering users think in periods; slot cost objective; Timefold subset needs a finite option set. Cost: ~100 lines in the builder. |
| D-3 | Capacity = cumulative amount over overlapping intervals (`Cumulative`), unary resources use `NoOverlap`. Per-slot capacity keyed by id disappears (fixes double booking on overlapping slots). | Keep `resource|slot` capacity. | Correct with overlapping slots, durations, partial overlaps. |
| D-4 | Typed fields on `Task`/`Resource` (duration, demand, priority, prize, skills, capacity, costPerTick...) replace the free `attributes` keys; `attributes` stays only as an opaque extension map (never read by the engine). | Keep `attributes.capacity/capabilities/cost/priority`. | Validation, UI forms, no hidden magic keys. |
| D-5 | v2.0 calendars: a task is non-preemptive and must lie inside ONE working window of every resource it uses (`WITHIN_WINDOW`). "Spanning" tasks (duration counted in working time across breaks) is deferred to v2.1 (section 11, R-3). | Implement SPAN now. | Needs per-task step functions; large risk. Workaround: calendar-free resource. |
| D-6 | Constraint params stay `Map<String,Object>` in the IR but each family declares a typed params record (package `eu.isygoit.dto.request.params`), bound with strict Jackson (`FAIL_ON_UNKNOWN_PROPERTIES`). | One record subtype per constraint (polymorphic JSON). | New family = new bean + new params record; no change to `Constraint`. |
| D-7 | One contributor SPI per engine + one engine-neutral `ConstraintFamily`/`ObjectiveFamily`. `Enforcer` hides HARD/RELAXABLE/SOFT/EXPLAIN from contributors: they write instance-level `onlyEnforceIf(enforcer.instance(..))` constraints. | Each contributor handles 4 modes. | Soft + relaxation + infeasibility core come for free for every family. |
| D-8 | Relaxation in ONE solve (`active_k` literals, built-in tier -2). Infeasibility core via assumption pass (best effort, with deletion shrinking) + exact pre-solve structural analysis. | Rebuild-per-attempt loop. | Minimal relaxed set, one model build. |
| D-9 | Lexicographic tiers: AUTO = single weighted solve when all tiers fit in 2^53, else sequential solves with fixing; per-tier values reported either way. | Always weighted / always sequential. | Speed when safe, correctness when not. |
| D-10 | Engine ids stay `cp_sat` and `timefold` (aliases `cp-sat`, `cpsat` normalised in `SolverOptions`). Default engine `cp_sat`. | Rename to `cp-sat`. | Existing DB/UI/capabilities ids; the owner wrote "cp-sat" informally. |
| D-11 | `STOPPED_FEASIBLE` = stop requested while an incumbent existed; the incumbent is returned and persisted; the job still ends `CANCELLED` (state machine untouched). | Throw the incumbent away. | Users keep partial plans. |
| D-12 | Objective arithmetic: int64 but every tier and the weighted total are guarded to <= 2^53 (exact in JSON doubles and JS). Decimal inputs are multiplied by `objectiveScale` (default 100) and rounded half-up; integral measures (ticks, counts) are NOT scaled. | Rounded `long` without scale (v1). | Defined, overflow-safe, UI-exact. |
| D-13 | Setup/transition times use a per-resource `AddCircuit` (exact adjacency, supports `minimize_setup`), capped by `maxSetupArcs`. | Pairwise literals. | Needed for setup minimisation; no triangle assumption. |
| D-14 | Window/period constraints attribute a task to the period in which it STARTS (night shift belongs to the day it starts). | Pro-rata overlap per period. | Linear model, standard for rosters; pro-rata is a v2.1 option. |
| D-15 | Old classes `ConstraintHandler`, `ObjectiveHandler`, `ConstraintRegistry`, `ObjectiveRegistry`, `solver.handler.*`, `TaskCandidates`, `Resource.unavailableSlots` are DELETED. | Adapter layer. | No client data; clean break. |

### 0.3 Pipeline v2 (what `PlanningService.solve` does)

```
PlanningRequest (JSON)
  -> PlanningRequestValidator   structural validation, ids, references, units, limits, precedence cycle, calendars (collects ALL issues)
  -> DomainNormalizer           defaults, engine alias, requirement synthesis, constraint->task rewrites (preprocess families),
                                auto objective injection, instants -> ticks check, returns PlanningProblem
  -> ProblemVerifier            family.validate per constraint/objective, engine capability check, objective range guard
  -> CandidateIndexBuilder      int-indexed CandidateIndex (candidates, domains, CSR arrays, windows) + ModelCost estimate vs limits
  -> PreSolveAnalyzer           exact structural infeasibility (no candidate, window, CPM critical path, capacity) -> INFEASIBLE core
  -> SolveConcurrencyGuard      (unchanged)  -> SolverEngine.solve(CompiledProblem, SolveContext)
  -> PlanningSolutionValidator  independent re-check of every constraint family + objective re-evaluation
  -> SolveResult (ordered, deterministic)
```

---

## 1. Time model

### 1.1 Definition

```java
package eu.isygoit.dto.request;
public record Horizon(Instant start, Instant end, int tickSeconds) {
    public static final int MAX_TICK_SECONDS = 86_400;
    public Horizon {
        // start/end non-null, end > start; tickSeconds in [1, 86400] and 86400 % tickSeconds == 0
        // (so a day boundary is always a tick boundary); (end-start) % tickSeconds == 0;
        // horizonTicks() <= Integer.MAX_VALUE/2 (hard cap 2^30); the configurable cap is app.plan.limits.max-horizon-ticks
    }
    public long horizonTicks()                         // (end-start)/tickSeconds
    public boolean isAligned(Instant t)                // (t-start) % tickSeconds == 0
    public int toTick(Instant t)                       // exact; throws TimeNotAlignedException (unchecked) if !isAligned or outside [start,end]
    public int ceilTick(Instant t)                     // smallest tick >= t   (used for calendar availability start: shrinks availability)
    public int floorTick(Instant t)                    // largest tick <= t    (used for calendar availability end: shrinks availability)
    public Instant toInstant(long tick)                // start + tick*tickSeconds
    public int ticks(Duration d)                       // d must be a multiple of tickSeconds
    public Duration duration(long ticks)
}
```

* Tick 0 = `horizon.start`; ticks are `int` in the IR and in `CandidateIndex` (guard: `horizonTicks <= 2^30`; default
  limit `max-horizon-ticks = 2_000_000`, i.e. 23 days at 1 s, 3.8 years at 1 min, 228 years at 1 h).
* Granularity is `tickSeconds` (1, 5, 10, 15, 30, 60, 300, 900, 1800, 3600, 86400 ... any divisor of 86400): "minutes or
  seconds" is a value, not a mode. Recommended: the largest tick that represents all durations exactly (minimises
  domains and symmetry).
* All instants are UTC (`Instant`, ISO-8601 with `Z`); the JSON may carry offsets, Jackson normalises to `Instant`.
* **Time zone**: `meta.timezone` (IANA id, default `UTC`) is used ONLY (a) to expand recurring calendar rules and the
  `DAY`/`WEEK` periods of windowed constraints (calendar-local midnight/week start, DST-aware through
  `ZonedDateTime.of(LocalDate, LocalTime, zone)`; a nonexistent local time moves forward, an ambiguous one takes the
  earlier offset), and (b) by the UI for display. The solver never sees zones: it sees ticks.
* Rounding rules (strict by default, predictable):
  * Task/constraint time points (`release`, `deadline`, `dueDate`, `pin.start`, `initialSolution` starts, constraint
    params) MUST be tick aligned, otherwise the validation error `TIME_NOT_ALIGNED` (path of the offending field).
  * Calendar availability: window start rounded UP (`ceilTick`), window end rounded DOWN (`floorTick`) (availability only
    ever shrinks); unavailability/exception/break: start rounded DOWN, end rounded UP (unavailability only ever grows).
    A warning `CALENDAR_ROUNDED` is emitted when rounding changed a bound.
  * Anything outside `[start, end]` is clipped to the horizon (warning `OUT_OF_HORIZON_CLIPPED`); a task `release` or
    `deadline` outside it is an error `TIME_OUT_OF_HORIZON`.
* Result: `Assignment` carries `startTick/endTick` AND `start/end` instants (section 3). The solver output is ticks;
  conversion is `horizon.toInstant`.

### 1.2 Slot mode: keep as a special case (decision D-2)

A `Slot` is a named, typed, priced time interval:

```java
public record Slot(String id, String typeId, String label, Instant start, Instant end, double cost, Map<String,Object> attributes)
```

* `Task.startSlots` = list of slot ids (or the selector `"ALL"`, or empty = free start). Semantics: the task START must
  equal the start of one of the listed slots and the task interval `[start, start+duration)` must end `<= slot.end`
  (so a lesson longer than its slot is never offered that slot). Empty list = free start on the whole horizon.
* `Task.duration` may be omitted (null) in slot mode: it defaults to the slot length in ticks, provided all listed slots
  have the same length (else `DURATION_REQUIRED`).
* A slot is NOT a capacity container any more. Capacity is by resource over time (D-3), so overlapping or irregular
  slots (`mon-p1` 08:00-09:00 and a 08:30-09:30 slot) cannot double-book.
* `minimize_slot_cost` prices `slot.cost` for the slot chosen by the start (CP-SAT: `startChoice` literals, section 6.4).
* Timefold subset REQUIRES slot mode (every task has non-empty `startSlots`) and pairwise non-overlapping slots
  (error `ENGINE_UNSUPPORTED_FEATURE: FREE_START` / `OVERLAPPING_SLOTS`).

### 1.3 The v1 school timetable in v2

* `horizon`: Monday 06:00Z to Tuesday 08:15Z, `tickSeconds=900` (105 ticks); periods become slots (`mon-p1` = ticks
  [0,4), `mon-p2` = [5,9), `tue-p1` = [96,100), `tue-p2` = [101,105)).
* `resource_capacity` (capacity 1) disappears: `Resource.capacity` defaults to 1 and a unary resource gets an implicit
  `NoOverlap`.
* `task_eligibility`(resourceIds/slotIds) becomes `ResourceRequirement.selector` + `Task.startSlots`.
* `capabilityTags` become `ResourceSelector.skills` on the requirement; `resource.attributes.capabilities` becomes
  `Resource.skills`.
* New: a `no_overlap` constraint per class tag (a class cannot attend two lessons at the same time). Full JSON in 2.6(a).

---

## 2. Request IR (package `eu.isygoit.dto.request`, module `02-plan-shared`)

All records are immutable; compact constructors copy collections (`List.copyOf`, `ImmutableJsonMap.copyOf`), apply the
defaults listed in the comments and throw `IllegalArgumentException` for local (single-record) invariants. Cross-entity
rules (references, cycles, units, windows) are validated in the service layer (section 7), never in constructors.
Jackson: records are bound by component name; `FAIL_ON_UNKNOWN_PROPERTIES=true` for the IR mapper used by the validation
endpoint (typos must fail), lenient only for the stored-job re-read.

### 2.1 Core entities

```java
package eu.isygoit.dto.request;

/** Top-level request. */
public record PlanningRequest(
        String tenant,                       // overwritten by the controller from the caller's tenant
        PlanningMeta meta,                   // meta.version MUST be "2.0"
        DomainDefinition domain,             // OPTIONAL (UI hints: type ids + attribute names). null -> empty
        Horizon horizon,                     // REQUIRED
        List<Slot> slots,                    // default []
        List<Calendar> calendars,            // default []
        List<SetupMatrix> setupMatrices,     // default []
        List<Resource> resources,            // REQUIRED non-empty
        List<Task> tasks,                    // REQUIRED non-empty
        List<Constraint> constraints,        // default []
        List<Objective> objectives,          // default []
        SolverOptions solverOptions,         // default SolverOptions.defaults()
        InitialSolution initialSolution) {}  // OPTIONAL warm start / locked assignments

/** meta.version "2.0" is the only accepted value (error UNSUPPORTED_SCHEMA_VERSION). Other components unchanged. */
public record PlanningMeta(String clientRef, String version, String domainName, String locale, String timezone) {
    public static final String SCHEMA_VERSION = "2.0";
    public static final int CLIENT_REF_MAX_LENGTH = 128;
    // timezone default "UTC"; must be a valid ZoneId (error INVALID_TIMEZONE)
}

public record Task(
        String id,                           // unique, non-blank, <= 128 chars
        String typeId,                       // optional, validated against domain.taskTypes when a domain is given
        String name,                         // display only
        Integer duration,                    // ticks >= 0; null allowed only in slot mode (see 1.2); 0 = milestone
        Map<String,Integer> resourceDurations, // resourceId -> ticks overriding duration; only for tasks with exactly ONE requirement of count 1
        Integer demand,                      // amount consumed on each used resource, >= 1, default 1 (cumulative demand/size)
        Integer priority,                    // >= 0, default 0 (used by maximize_task_priority, tardiness weights, tie-breaks)
        Boolean required,                    // default TRUE; false = optional task (prize collecting)
        Long prize,                          // >= 0; reward when an OPTIONAL task is selected; null -> 1 (only read for optional tasks)
        List<ResourceRequirement> requirements, // default: ONE implicit requirement {role "default", count 1, selector ALL}
        Instant release,                     // earliest start (time window), null = horizon start
        Instant deadline,                    // HARD latest END (end <= deadline), null = horizon end
        Instant dueDate,                     // SOFT target end for tardiness objectives, null = none
        List<String> startSlots,             // slot ids / "ALL"; empty = free start
        Pin pin,                             // hard pinned start and/or resources, null = free
        List<PrecedenceRef> predecessors,    // HARD edges "predecessor -> this task", default []
        String setupClass,                   // key into SetupMatrix classes, null = none
        Set<String> tags,                    // free grouping labels ("class-7A", "order-17"); used by scopes, no_overlap, same_resource...
        List<Preference> preferences,        // soft preferences read by minimize_preference_penalty
        Map<String,Object> attributes) {     // opaque extension map; NEVER read by the engine
    public boolean requiredTask() { return !Boolean.FALSE.equals(required); }
}

/** One resource demand of a task. */
public record ResourceRequirement(
        String role,                         // label echoed in the result; matching resource type ids are inferred when selector has no type filter
        ResourceSelector selector,           // who may fulfil it
        Integer count,                       // number of distinct resources, >= 1, default 1
        Integer amount) {}                   // capacity amount consumed on each, >= 1, null -> task.demand

/** Pure data selector, evaluated once in the CandidateIndex (set algebra on bitsets, not per triple). */
public record ResourceSelector(
        List<String> resourceIds,            // non-empty -> restrict to these ids (also accepts the literal "ALL")
        List<String> resourceTypeIds,        // non-empty -> restrict to these types
        List<String> skills,                 // resource must have ALL of them
        List<String> anySkills,              // resource must have AT LEAST ONE (empty = ignored)
        List<String> tags,                   // resource must carry ALL of these tags
        List<String> excludeResourceIds) {}  // removed after the above

public record Pin(Instant start, List<String> resourceIds) {} // either may be null; resourceIds are matched to requirements in order of appearance (validation: one id per required slot of the task, or fewer for a partial pin)

public record PrecedenceRef(
        String taskId,                       // the PREDECESSOR (the owner of this ref is the successor)
        Type type,                           // default FINISH_START
        Integer minLag,                      // ticks, default 0, may be negative (lead)
        Integer maxLag,                      // ticks or null (no upper lag)
        Presence presence) {                 // default WHEN_BOTH
    public enum Type { FINISH_START, START_START, FINISH_FINISH, START_FINISH }
    public enum Presence { WHEN_BOTH, IMPLIES }   // IMPLIES: successor present => predecessor present
}

public record Preference(Kind kind, String resourceId, Instant start, int penalty) {
    public enum Kind { RESOURCE, START }     // RESOURCE: penalty if NOT assigned to resourceId; START: penalty per tick of |start - preferred start|
}

public record Resource(
        String id,
        String typeId,
        String name,
        Integer capacity,                    // cumulative capacity >= 1, default 1 (1 => unary NoOverlap)
        Set<String> skills,
        Set<String> tags,
        String calendarId,                   // null = always available
        List<TimeWindow> unavailable,        // extra blocked intervals (instants)
        Double costPerTick,                  // >= 0, default 0 (read by minimize_resource_cost)
        Double costPerUse,                   // >= 0, default 0, paid once if the resource is used at all
        Integer maxLoadTicks,                // optional horizon-wide cap on sum(duration*[assigned]); built-in hard cap
        String setupMatrixId,                // null = no setups on this resource
        Map<String,Object> attributes) {}

public record InitialSolution(List<InitialAssignment> assignments) {}
/** locked=false: warm-start hint only. locked=true: frozen (the task keeps exactly these values; infeasible if they violate hard constraints). */
public record InitialAssignment(String taskId, Instant start, List<String> resourceIds, boolean locked) {}
```

Notes: `Task.pin` is a definition-level hard fact ("this job is always Monday 08:00 on press-1"); `initialSolution` is
about a particular run (replanning). Roster shifts are tasks with `pin.start` (there is no separate Shift entity).

### 2.2 Calendars, windows, setups

```java
public record TimeWindow(Instant start, Instant end) {}              // start < end

public record Calendar(
        String id,
        String name,
        String timezone,                     // IANA, default meta.timezone
        List<TimeWindow> windows,            // explicit working windows (instants)
        List<RecurringWindow> recurring,     // recurring working windows
        List<TimeWindow> exceptions,         // explicit unavailable intervals (holidays, maintenance)
        List<RecurringWindow> breaks) {}     // recurring unavailable intervals (lunch)
// semantics: availability = (windows UNION expand(recurring)) MINUS (exceptions UNION expand(breaks)); if windows AND recurring
// are both empty the calendar is "always available" minus exceptions/breaks. Expanded by CalendarExpander in the horizon,
// merged and sorted into int[] [lo,hi) runs (tick units) shared by all resources using the calendar.

public record RecurringWindow(
        Set<DayOfWeek> days,                 // empty = every day
        LocalTime from, LocalTime to,        // calendar-local; to <= from means "crosses midnight" (to == from = 24 h)
        LocalDate validFrom, LocalDate validTo) {} // inclusive; null = unbounded

public record SetupMatrix(
        String id,
        int defaultTicks,                    // setup between two different classes with no explicit entry, default 0
        int sameClassTicks,                  // setup between two tasks of the same class, default 0
        List<SetupEntry> entries) {}         // sparse overrides
public record SetupEntry(String from, String to, int ticks) {}
// setup(i -> j) is the idle time the resource needs between the END of i and the START of j when j is the immediate
// successor of i on that resource. First task on a resource has no setup.
```

### 2.3 Constraints, objectives, options

```java
public record Constraint(
        String id,                           // unique
        String type,                         // family id, see section 4 (e.g. "min_rest")
        IEnumEnforcement.Types enforcement,  // default STRICTLY_REQUIRED (enum unchanged: STRICTLY_REQUIRED, CONFIGURABLE_HARD, SOFT, RELAXABLE_WITH_AUTHORIZATION)
        ConstraintScope scope,               // default {kind:"all"}
        Map<String,Object> params,           // bound to the family's typed record
        RelaxationPolicy relaxation,         // only read for RELAXABLE_WITH_AUTHORIZATION
        int weight,                          // SOFT: penalty per violation unit (>= 0); default 1 for SOFT when 0 is given
        Integer tier) {}                     // SOFT: lexicographic tier, default 0
// enforcement semantics: STRICTLY_REQUIRED and CONFIGURABLE_HARD are HARD (CONFIGURABLE_HARD only marks "the UI may
// turn it into SOFT"); RELAXABLE_WITH_AUTHORIZATION is HARD unless relaxation.authorized and the solver cannot satisfy it
// (then it is dropped at the cost described in 6.9); SOFT is penalised. STRICTLY_REQUIRED is never relaxed/softened.

public record ConstraintScope(String kind, String value) {}
// kind in: "all" (value ignored) | "task" (task id) | "taskType" | "resource" | "resourceId" alias of resource | "resourceType" | "tag" (task tag; for resource-oriented
// families "tag" selects RESOURCE tags, documented per family). Resolved ONCE per constraint by CandidateIndex.resolveTasks/resolveResources.

public record RelaxationPolicy(boolean authorized, int priority, double maxRelaxation) {}
// unchanged from v1 (priority >= 0, maxRelaxation 0|1). Lower priority number = dropped FIRST (cheapest to drop), exactly as in v1.

public record Objective(
        String id,                           // unique
        String type,                         // objective family id
        int weight,                          // >= 0 multiplier of the term inside its tier; 0 = disabled (warning)
        Integer tier,                        // lexicographic tier, 0..7, default 0 (lower = more important). Tiers -1 and -2 are reserved (6.8)
        Integer scale,                       // decimal -> long multiplier for THIS objective; default solverOptions.objectiveScale
        ConstraintScope scope,               // optional subset of tasks/resources the objective looks at; default all
        Map<String,Object> params) {         // bound to the family's typed record (usually empty)
    public Objective(String id, String type, int weight) { this(id, type, weight, null, null, null, Map.of()); }
}

public record SolverOptions(
        String engine,                       // default "cp_sat"; aliases "cp-sat","cpsat" -> "cp_sat"; "timefold"
        Integer maxTimeSeconds,              // default 30, >= 1, <= limits.max-time-seconds
        Integer numWorkers,                  // default 8, 1..32, clamped to limits.max-workers by PlanningService (unchanged)
        Long seed,                           // default 0, must fit int
        Boolean logSearch,                   // default false
        List<String> relaxationLevels,       // v1 allow-list of relaxable constraint ids, [] = all authorized ones
        Boolean deterministic,               // default false. true => reproducible: see 6.12
        Integer objectiveScale,              // default 100, 1..1_000_000
        Double gapTolerance,                 // relative gap at which the search stops, default 0.0 (prove optimality), 0..1
        LexicographicMode lexicographicMode, // default AUTO
        Boolean explainInfeasibility,        // default true: run the explanation pass when INFEASIBLE
        Integer explainTimeSeconds,          // default min(10, 20% of maxTimeSeconds), >= 1
        Boolean keepIncumbentOnStop) {       // default true => STOPPED_FEASIBLE result is returned (D-11)
    public enum LexicographicMode { AUTO, WEIGHTED, SEQUENTIAL }
    public static SolverOptions defaults() { /* all defaults above */ }
}
```

### 2.4 Normalized problem (what engines see)

```java
public record PlanningProblem(
        Long jobId, String domainName, String timezone, Horizon horizon,
        List<Slot> slots, List<Calendar> calendars, List<SetupMatrix> setupMatrices,
        List<Resource> resources, List<Task> tasks,
        List<Constraint> constraints, List<Objective> objectives,
        SolverOptions options, InitialSolution initialSolution) {}
```
Produced by `DomainNormalizer`: all defaults materialised (requirements, scope, tier, scale, durations from slot length),
engine alias resolved, preprocess-type constraints (`multi_resource_requirement`, hard `task_eligibility`) applied as task
rewrites, `auto-selected-value` objective injected (6.8). Lists keep REQUEST ORDER (no sorting, no hash iteration: this is
what makes model building deterministic).

### 2.5 Typed params records (package `eu.isygoit.dto.request.params`) - contract for the catalogue

```java
public record TaskEligibilityParams(List<String> resourceIds, List<String> excludeResourceIds, List<String> resourceTypeIds,
                                    List<String> skills, List<String> slotIds) {}
public record ResourceCapacityParams(Integer capacity, TimeWindow window) {}                 // lowers the capacity of the scoped resources (inside window, default horizon)
public record NoOverlapParams(String groupByTagPrefix, Integer minGapTicks) {}               // one group per task tag starting with the prefix; scope.kind="tag" -> one group
public record PrecedenceParams(ConstraintScope from, ConstraintScope to, PrecedenceRef.Type type,
                               Integer minLag, Integer maxLag, PairingMode pairing) {
    public enum PairingMode { ALL_PAIRS, BY_ORDER }                                          // ALL_PAIRS: every from x every to (guarded by maxPairwise); BY_ORDER: i-th from -> i-th to
}
public record TimeWindowParams(Instant earliestStart, Instant latestStart, Instant earliestEnd, Instant latestEnd) {}
public record ResourceAvailabilityParams(List<TimeWindow> unavailable, List<TimeWindow> availableOnly) {}
public record PeriodSpec(Unit unit, Integer ticks) { public enum Unit { TICKS, DAY, WEEK } } // TICKS needs ticks>=1; DAY/WEEK in meta.timezone (week starts Monday); buckets tile the horizon
public enum LoadMetric { TICKS, COUNT }
public record MaxConsecutiveParams(PeriodSpec period, int max) {}                            // at most `max` consecutive periods with work
public record MinRestParams(int minRestTicks, ConstraintScope afterTasks) {}                 // gap >= minRestTicks between consecutive tasks of a resource; afterTasks null = all
public record MaxLoadPerPeriodParams(PeriodSpec period, LoadMetric metric, Integer max, Integer min) {}
public record MultiResourceRequirementParams(String role, ResourceSelector selector, Integer count, Integer amount) {} // adds/replaces requirement `role` on every scoped task
public record GroupingParams(String groupByTagPrefix, String role) {}                        // same_resource / different_resource
public record FixedAssignmentParams(Instant start, List<String> resourceIds, String role) {}
public record ForbiddenPairParams(Kind kind, Relation relation, List<IdPair> pairs) {
    public enum Kind { TASK_TASK, RESOURCE_RESOURCE }
    public enum Relation { SAME_RESOURCE, TIME_OVERLAP }                                     // TASK_TASK; RESOURCE_RESOURCE ignores it (never together on one task)
    public record IdPair(String a, String b) {}
}
public record MaxTasksPerResourceParams(Integer max, Integer min, PeriodSpec period) {}      // period null = whole horizon
// objective params
public record TardinessParams(WeightBy weightBy) { public enum WeightBy { NONE, PRIORITY } }
public record LoadObjectiveParams(LoadMetric metric) {}
public record PlanChangeParams(Integer startDeviationWeight, Integer resourceChangeWeight) {}
```
Utility `eu.isygoit.util.ParamsBinder.bind(Map<String,Object>, Class<P>)` (strict Jackson `convertValue`; errors become
`PARAMS_INVALID` with the unknown/missing property path). `eu.isygoit.util.ScaledMath` (shared, used by engines, the
validator and Timefold so the same number is computed everywhere): `long scale(double v, int scale)` =
`Math.round(v*scale)` with `Math.multiplyExact` guard and `v` finite and `>= 0` (or `<= 0` only for explicit reward
inputs), `long addExact/mulExact` wrappers throwing `ObjectiveRangeException`.

### 2.6 Complete JSON examples

#### (a) School timetable (v1 equivalent)

```json
{
  "meta": { "clientRef": "school-timetable-v2", "version": "2.0", "domainName": "school-timetable",
            "locale": "en-US", "timezone": "Europe/Paris" },
  "domain": {
    "resourceTypes": [{ "id": "teacher", "attributes": ["name"] }],
    "taskTypes":     [{ "id": "lesson",  "attributes": ["classId", "subject"] }],
    "slotTypes":     [{ "id": "period",  "attributes": ["day"] }]
  },
  "horizon": { "start": "2026-09-07T06:00:00Z", "end": "2026-09-08T08:15:00Z", "tickSeconds": 900 },
  "slots": [
    { "id": "mon-p1", "typeId": "period", "label": "Mon 08:00", "start": "2026-09-07T06:00:00Z", "end": "2026-09-07T07:00:00Z", "cost": 0.0 },
    { "id": "mon-p2", "typeId": "period", "label": "Mon 09:15", "start": "2026-09-07T07:15:00Z", "end": "2026-09-07T08:15:00Z", "cost": 0.0 },
    { "id": "tue-p1", "typeId": "period", "label": "Tue 08:00", "start": "2026-09-08T06:00:00Z", "end": "2026-09-08T07:00:00Z", "cost": 1.0 },
    { "id": "tue-p2", "typeId": "period", "label": "Tue 09:15", "start": "2026-09-08T07:15:00Z", "end": "2026-09-08T08:15:00Z", "cost": 1.0 }
  ],
  "resources": [
    { "id": "teacher-alice", "typeId": "teacher", "name": "Alice Martin", "capacity": 1, "skills": ["math"] },
    { "id": "teacher-bob",   "typeId": "teacher", "name": "Bob Laurent",  "capacity": 1, "skills": ["science"] }
  ],
  "tasks": [
    { "id": "7a-math",    "typeId": "lesson", "tags": ["class-7A"], "startSlots": ["mon-p1", "tue-p1"],
      "requirements": [{ "role": "teacher", "selector": { "skills": ["math"] } }] },
    { "id": "7b-math",    "typeId": "lesson", "tags": ["class-7B"], "startSlots": ["mon-p1", "tue-p1"],
      "requirements": [{ "role": "teacher", "selector": { "skills": ["math"] } }] },
    { "id": "7a-science", "typeId": "lesson", "tags": ["class-7A"], "startSlots": ["mon-p2", "tue-p2"],
      "requirements": [{ "role": "teacher", "selector": { "skills": ["science"] } }] },
    { "id": "7b-science", "typeId": "lesson", "tags": ["class-7B"], "startSlots": ["mon-p2", "tue-p2"],
      "requirements": [{ "role": "teacher", "selector": { "skills": ["science"] } }] }
  ],
  "constraints": [
    { "id": "class-no-overlap", "type": "no_overlap", "enforcement": "STRICTLY_REQUIRED",
      "scope": { "kind": "all" }, "params": { "groupByTagPrefix": "class-" } }
  ],
  "objectives": [ { "id": "slot-cost", "type": "minimize_slot_cost", "weight": 1 } ],
  "solverOptions": { "engine": "cp_sat", "maxTimeSeconds": 10, "numWorkers": 2, "seed": 0 }
}
```
Duration omitted -> slot length (4 ticks). Expected: `OPTIMAL`, 2 lessons Monday + 2 Tuesday, tier-0 value = 2 slots
priced 1.0 -> `2 * 100 = 200` scaled (reported as 2.0). Also runs on Timefold (slot mode, disjoint slots).

#### (b) Job shop / workshop (durations, precedence, no-overlap, makespan)

```json
{
  "meta": { "clientRef": "jobshop-3x2", "version": "2.0", "domainName": "workshop", "timezone": "UTC" },
  "horizon": { "start": "2026-09-07T08:00:00Z", "end": "2026-09-07T16:00:00Z", "tickSeconds": 60 },
  "resources": [
    { "id": "M1", "typeId": "machine", "name": "Cutting", "capacity": 1 },
    { "id": "M2", "typeId": "machine", "name": "Assembly", "capacity": 1 }
  ],
  "tasks": [
    { "id": "A1", "duration": 30, "tags": ["job-A"], "requirements": [{ "role": "machine", "selector": { "resourceIds": ["M1"] } }] },
    { "id": "A2", "duration": 20, "tags": ["job-A"], "requirements": [{ "role": "machine", "selector": { "resourceIds": ["M2"] } }],
      "predecessors": [{ "taskId": "A1", "type": "FINISH_START", "minLag": 0 }] },
    { "id": "B1", "duration": 25, "tags": ["job-B"], "requirements": [{ "role": "machine", "selector": { "resourceIds": ["M2"] } }] },
    { "id": "B2", "duration": 15, "tags": ["job-B"], "requirements": [{ "role": "machine", "selector": { "resourceIds": ["M1"] } }],
      "predecessors": [{ "taskId": "B1" }] },
    { "id": "C1", "duration": 20, "tags": ["job-C"], "requirements": [{ "role": "machine", "selector": { "resourceIds": ["M1"] } }] },
    { "id": "C2", "duration": 30, "tags": ["job-C"], "requirements": [{ "role": "machine", "selector": { "resourceIds": ["M2"] } }],
      "predecessors": [{ "taskId": "C1" }] }
  ],
  "objectives": [ { "id": "makespan", "type": "minimize_makespan", "weight": 1 } ],
  "solverOptions": { "engine": "cp_sat", "maxTimeSeconds": 10, "numWorkers": 4, "deterministic": true }
}
```
No explicit `no_overlap` constraint is needed: capacity-1 resources are unary by definition. Expected: `OPTIMAL`,
makespan 75 ticks (M2 load = 25+20+30 = 75 with no idle: B1[0,25) C2[25,55) A2[55,75), M1: C1[0,20) A1[20,50) B2[50,65)).

#### (c) Shift roster (rest rules, skills, fairness)

```json
{
  "meta": { "clientRef": "roster-3d", "version": "2.0", "domainName": "roster", "timezone": "UTC" },
  "horizon": { "start": "2026-09-07T00:00:00Z", "end": "2026-09-10T00:00:00Z", "tickSeconds": 3600 },
  "resources": [
    { "id": "ana",  "typeId": "nurse", "skills": ["nurse", "icu"] },
    { "id": "ben",  "typeId": "nurse", "skills": ["nurse"] },
    { "id": "cleo", "typeId": "nurse", "skills": ["nurse"],
      "unavailable": [{ "start": "2026-09-07T08:00:00Z", "end": "2026-09-07T16:00:00Z" }] }
  ],
  "tasks": [
    { "id": "d0-day",   "duration": 8, "pin": { "start": "2026-09-07T08:00:00Z" }, "requirements": [{ "role": "nurse", "selector": { "skills": ["nurse"] } }] },
    { "id": "d0-night", "duration": 8, "pin": { "start": "2026-09-07T22:00:00Z" }, "requirements": [{ "role": "nurse", "selector": { "skills": ["nurse"] } }] },
    { "id": "d1-day",   "duration": 8, "pin": { "start": "2026-09-08T08:00:00Z" }, "requirements": [{ "role": "nurse", "selector": { "skills": ["nurse"] } }] },
    { "id": "d1-night", "duration": 8, "pin": { "start": "2026-09-08T22:00:00Z" }, "requirements": [{ "role": "nurse", "selector": { "skills": ["nurse", "icu"] } }] },
    { "id": "d2-day",   "duration": 8, "pin": { "start": "2026-09-09T08:00:00Z" }, "requirements": [{ "role": "nurse", "selector": { "skills": ["nurse"] } }] }
  ],
  "constraints": [
    { "id": "rest-11h", "type": "min_rest", "enforcement": "STRICTLY_REQUIRED", "scope": { "kind": "all" }, "params": { "minRestTicks": 11 } },
    { "id": "one-shift-per-day", "type": "max_load_per_period", "enforcement": "STRICTLY_REQUIRED", "scope": { "kind": "all" },
      "params": { "period": { "unit": "DAY" }, "metric": "TICKS", "max": 8 } },
    { "id": "max-2-days-in-a-row", "type": "max_consecutive", "enforcement": "SOFT", "weight": 10, "scope": { "kind": "all" },
      "params": { "period": { "unit": "DAY" }, "max": 2 } }
  ],
  "objectives": [ { "id": "fair", "type": "minimize_max_load", "weight": 1, "params": { "metric": "TICKS" } } ],
  "solverOptions": { "engine": "cp_sat", "maxTimeSeconds": 10, "numWorkers": 2, "deterministic": true }
}
```
Expected: `OPTIMAL`, tier-0 value 16 (five 8-tick shifts over three nurses, best max load = 2 shifts = 16), `d1-night` on `ana`,
no soft violation, rest and daily rules satisfied (assignment e.g. ana: d0-day + d1-night, ben: d0-night + d2-day, cleo: d1-day).

#### (d) Project plan (cumulative resource, precedence with lags, deadline)

```json
{
  "meta": { "clientRef": "project-5", "version": "2.0", "domainName": "project", "timezone": "UTC" },
  "horizon": { "start": "2026-09-01T00:00:00Z", "end": "2026-09-21T00:00:00Z", "tickSeconds": 86400 },
  "resources": [ { "id": "crew-pool", "typeId": "pool", "name": "Excavator crews", "capacity": 2 } ],
  "tasks": [
    { "id": "A", "name": "Survey north", "duration": 4, "demand": 1, "requirements": [{ "role": "crew", "selector": { "resourceIds": ["crew-pool"] } }] },
    { "id": "B", "name": "Survey south", "duration": 4, "demand": 1, "requirements": [{ "role": "crew", "selector": { "resourceIds": ["crew-pool"] } }] },
    { "id": "C", "name": "Heavy digging", "duration": 4, "demand": 2, "requirements": [{ "role": "crew", "selector": { "resourceIds": ["crew-pool"] } }] },
    { "id": "D", "name": "Backfill", "duration": 2, "demand": 1, "requirements": [{ "role": "crew", "selector": { "resourceIds": ["crew-pool"] } }],
      "predecessors": [{ "taskId": "A" }, { "taskId": "B" }] },
    { "id": "E", "name": "Inspection", "duration": 1, "demand": 1, "deadline": "2026-09-13T00:00:00Z",
      "requirements": [{ "role": "crew", "selector": { "resourceIds": ["crew-pool"] } }],
      "predecessors": [{ "taskId": "C", "minLag": 0 }, { "taskId": "D", "minLag": 0 }] }
  ],
  "objectives": [ { "id": "makespan", "type": "minimize_makespan", "weight": 1 } ],
  "solverOptions": { "engine": "cp_sat", "maxTimeSeconds": 10, "numWorkers": 2 }
}
```
Expected: `OPTIMAL`, makespan 11 ticks (days): pool work + the unavoidable one-unit idle during D gives 10 days for A-D, then E.
`deadline` day 12 is satisfied (E ends at 11).

#### (e) Optional-task selection (prize collecting), no explicit objective

```json
{
  "meta": { "clientRef": "prize-5", "version": "2.0", "domainName": "field-service", "timezone": "UTC" },
  "horizon": { "start": "2026-09-07T08:00:00Z", "end": "2026-09-07T16:00:00Z", "tickSeconds": 60 },
  "resources": [ { "id": "tech-1", "typeId": "technician", "capacity": 1 } ],
  "tasks": [
    { "id": "P1", "duration": 120, "required": false, "prize": 50 },
    { "id": "P2", "duration": 180, "required": false, "prize": 80 },
    { "id": "P3", "duration": 240, "required": false, "prize": 90 },
    { "id": "P4", "duration":  90, "required": false, "prize": 30 },
    { "id": "P5", "duration": 150, "required": false, "prize": 60 }
  ],
  "constraints": [],
  "objectives": [],
  "solverOptions": { "engine": "cp_sat", "maxTimeSeconds": 10, "numWorkers": 2 }
}
```
The normalizer injects `{id:"auto-selected-value", type:"maximize_selected_value", weight:1, tier:-1}` because the request has
optional tasks and no selection objective. Expected: `OPTIMAL`, selected P1+P2+P5 (450 of 480 ticks), value 190, P3 and P4 in
`unassignedTasks` (reason `NOT_SELECTED`, `forgonePrize` 90 and 30). No `objectives` entry is needed.

---

## 3. Result IR (package `eu.isygoit.dto.response`, module `02-plan-shared`)

`SolveResult` keeps its five components so the persisted JSON column and `PlanJobService.getResult` keep their shape;
everything inside is richer.

```java
public record SolveResult(
        IEnumSolverStatus.Types status,      // OPTIMAL | FEASIBLE | INFEASIBLE | UNKNOWN | STOPPED_FEASIBLE
        PlanningSolution solution,           // null for INFEASIBLE / UNKNOWN (and STOPPED without incumbent)
        List<Relaxation> relaxations,        // constraints dropped AND actually violated, with real impact
        Diagnostics diagnostics,             // message, infeasibility core, warnings, details
        Metrics metrics) {}

// enum IEnumSolverStatus.Types { OPTIMAL, FEASIBLE, INFEASIBLE, UNKNOWN, STOPPED_FEASIBLE };  STR_ENUM_SIZE 10 -> 20 (WP1; WP7 checks every column fed by it)
// OPTIMAL: proven optimal for every tier.  FEASIBLE: a solution exists, time/gap limit reached, bound known (gap in metrics).
// STOPPED_FEASIBLE: stop requested while an incumbent existed (D-11).  INFEASIBLE: proven.  UNKNOWN: nothing found, nothing proven.
// Timefold never returns OPTIMAL (heuristic): FEASIBLE at best; INFEASIBLE only from the pre-solve analysis (6.10).

public record PlanningSolution(
        List<Assignment> assignments,        // sorted: startTick, then task id (determinism, 8.3)
        List<UnassignedTask> unassignedTasks,// ONLY unselected optional tasks (a required task is never unassigned)
        List<ConstraintViolation> hardViolations, // always empty for a valid solution (kept for the validator report); relaxed hard constraints land here
        List<ConstraintViolation> softViolations, // instance-level, sorted by constraint id, task id
        List<ObjectiveScore> qualityDetails, // one per objective, evaluated INDEPENDENTLY by the validator (8.2)
        double qualityScore) {}              // 1.0 if OPTIMAL else 1/(1+gap) clamped [0.01,0.99]; 0.5 when gap unknown

public record Assignment(
        String taskId,
        boolean optional,                    // true = optional task that WAS selected
        int startTick, int endTick,          // endTick = startTick + duration actually used (resource-specific duration)
        Instant start, Instant end,          // horizon.toInstant(tick)
        String slotId,                       // slot-mode anchor, else null
        List<ResourceAssignment> resources) {}

public record ResourceAssignment(String resourceId, String role, int amount) {}  // amount = capacity consumed

public record UnassignedTask(
        String taskId,
        String reasonCode,                   // NO_ELIGIBLE_RESOURCE | PREDECESSOR_NOT_SELECTED | OUTSIDE_WINDOW | NOT_SELECTED
        String reason,                       // human text (English; the UI localises by reasonCode)
        String suggestion,
        long forgonePrize) {}                // effective prize of the task

public record ConstraintViolation(
        String constraintId, String taskId, String resourceId, String code, String message,
        double impact) {}                    // impact = weight * measure (scaled value / scale)

public record ObjectiveScore(
        String objectiveId, String type, int tier, int weight,
        long rawValue,                       // exact scaled integer value of the term (sign-normalised: maximisation objectives report the positive reward)
        double score,                        // rawValue / scale for decimal objectives, rawValue for integral ones (what a human reads: 2.0, 190.0, 16.0)
        double contribution) {}              // weight * score

public record Relaxation(
        String constraintId, Object originalValue, Object relaxedValue,   // enforcement names, kept from v1
        double impactOnScore,                // = measure (total violated family units); the real impact, never a guess
        boolean authorized,
        int violationCount,                  // number of instance violations found by the checker (REAL impact); 0 => not reported at all
        List<String> violatedTaskIds,        // first 50, sorted
        double measure) {}                   // family measure units violated (ticks over, tasks over, ...)

public record Diagnostics(
        String message,
        List<String> infeasibilityCore,      // legacy flat list of ids (constraint ids + task ids), kept for CSV/UI
        InfeasibilityCore core,              // structured IIS-style core, null when feasible
        List<DiagnosticIssue> warnings,
        Map<String,Object> details) {}

public record InfeasibilityCore(boolean minimal, String method, List<CoreItem> items) {}
// method: PRESOLVE_ANALYSIS (exact, no solver) | ASSUMPTIONS (CP-SAT sufficient assumptions) | ASSUMPTIONS_SHRUNK (deletion filter applied) | NONE
public record CoreItem(String kind, String id, String reasonCode, String message) {}
// kind: CONSTRAINT | TASK | RESOURCE | EDGE.  reasonCode: NO_ELIGIBLE_RESOURCE, WINDOW_EMPTY, CRITICAL_PATH_TOO_LONG, CAPACITY_EXCEEDED, PRECEDENCE,
//   CALENDAR, REQUIREMENT_COUNT, PIN_CONFLICT, LOCKED_CONFLICT, CONSTRAINT (a catalogue constraint is part of the core), HORIZON
public record DiagnosticIssue(String code, String severity, String path, String message) {}  // severity INFO | WARNING

public record Metrics(
        long durationMillis,                 // total wall time of the solve call (build + solve + post)
        long buildMillis,                    // index + model construction
        long solveMillis,                    // inside CpSolver.solve / Timefold solve
        Long timeToFirstSolutionMillis,      // from solve start; null if none
        int workersUsed,
        ModelStats model,
        SearchStats search,
        List<TierValue> tiers) {}

public record ModelStats(
        int tasks, int resources, int candidates, int boolVars, int intVars, int constraints,
        int intervals, int optionalIntervals, int circuitArcs, int softLiterals, int relaxLiterals) {}

public record SearchStats(
        long branches, long conflicts, int solutionsFound,
        Double objective,                    // last objective value of the LAST solved model (scaled units)
        Double bestBound, Double gap,        // relative gap |obj-bound|/max(1,|obj|); null when no bound (Timefold) or weighted multi-tier (see 6.8)
        int tiersSolved) {}

public record TierValue(int tier, long value, Double bound, String status) {}
// status per tier: OPTIMAL | FEASIBLE | NOT_SOLVED. tier -2 = relaxation cost, -1 = auto selection value, 0..7 user tiers.
// value is the minimised internal tier value (maximisation tiers are negative); ObjectiveScore is the human-facing one.
```

`CapabilitiesDto` and `ValidationReportDto` are defined in section 9.1.

---

## 4. Constraint and objective catalogue v2

### 4.1 Conventions

* `type` strings are the family ids below (`snake_case`). `scope` default `{kind:"all"}`.
* Enforcement support: **H** = hard (STRICTLY_REQUIRED/CONFIGURABLE_HARD), **S** = SOFT, **R** = RELAXABLE_WITH_AUTHORIZATION.
  A family declares what it supports (`ConstraintFamily.supportedEnforcements()`); each engine contributor declares what its
  formulation implements; the capability matrix is the intersection (5.6). An unsupported combination is rejected by the
  `ProblemVerifier` (`ENGINE_UNSUPPORTED_ENFORCEMENT`), never ignored.
* SOFT penalty = `constraint.weight` x violation units, in tier `constraint.tier` (default 0). The unit is family-specific
  (table). RELAXABLE = the whole constraint can be switched off (binary, as in v1) at cost `1000*(1+relaxation.priority)` in tier -2.
* "Preprocess" = the family prunes/rewrites in the engine-neutral compile step when it is HARD (both engines benefit).
* CP-SAT formulation details are in section 6 (6.x references below).

### 4.2 Structural semantics (built into the model, not constraints)

These hold without any `Constraint`: each required task gets all its requirements fulfilled (`count` distinct resources
per requirement, a resource used at most once per task); optional tasks are all-or-nothing; `release <= start`,
`end <= deadline`, within horizon; start in `startSlots` starts when given; resource calendars/`unavailable` (task lies in
one working window of every used resource); unary resources are `NoOverlap`, capacity > 1 resources are `Cumulative`
(demand = requirement amount); `Task.predecessors` (hard edges); `Task.pin`; `Resource.maxLoadTicks`; setup matrices
(circuit); locked `initialSolution` assignments.

### 4.3 Constraint families

| Type id | Semantics | Params record | Soft unit | CP-SAT (H/S/R) | Timefold (H/S/R) | Formulation |
|---------|-----------|---------------|-----------|----------------|------------------|-------------|
| `task_eligibility` | scoped tasks may only use resources / slots matching the params (skills, ids, types, excluded ids, slot ids) | `TaskEligibilityParams` | assigned (task,resource) outside the allowed set | H S R | H S - | 6.5.1 (H = preprocess prune) |
| `resource_capacity` | lowers capacity of scoped resources (optionally inside a window): `min(Resource.capacity, capacity)` | `ResourceCapacityParams` | - | H - R | H - - (window null or slot-aligned) | 6.5.2 fixed demand intervals |
| `no_overlap` | tasks of a group never overlap in time (class/patient conflicts, mutual exclusion regardless of resource); `minGapTicks` between them | `NoOverlapParams` | overlapping pair | H S R | - - - | 6.5.3 master intervals `NoOverlap`; soft = pair literals |
| `precedence` | bulk/soft/relaxable precedence between task sets: FS/SS/FF/SF with minLag/maxLag (hard edges of the definition go in `Task.predecessors`) | `PrecedenceParams` | violated pair | H S R | - - - | 6.5.4 |
| `time_window` | extra windows on scoped tasks: earliest/latest start/end | `TimeWindowParams` | ticks outside the window (measure) | H S R | H - - (prune) | 6.5.5 |
| `resource_availability` | extra unavailable intervals (or "available only" windows) for scoped resources, soft = overtime | `ResourceAvailabilityParams` | task candidate overlapping a blocked window | H S R | H - - (prune) | 6.5.6 |
| `max_consecutive` | a resource works at most `max` consecutive periods (DAY/WEEK/TICKS buckets, task belongs to the bucket of its start) | `MaxConsecutiveParams` | windows of max+1 periods all worked | H S R | - - - | 6.5.7 |
| `min_rest` | minimum gap between two tasks on the same unary resource | `MinRestParams` | pair closer than the gap | H S R | - - - | 6.5.8 |
| `max_load_per_period` | per resource and period: load (ticks or task count) `<= max` and/or `>= min` | `MaxLoadPerPeriodParams` | units above max / below min | H S R | - - - | 6.5.9 |
| `multi_resource_requirement` | adds/replaces requirement `role` (selector, count, amount) on every scoped task (bulk definition) | `MultiResourceRequirementParams` | - | H - - (preprocess) | - - - | 6.5.10 (task rewrite) |
| `same_resource` | tasks of a group (tag prefix or scope tag) use the same resource for `role` (all roles when null) | `GroupingParams` | - | H - R | - - - | 6.5.11 |
| `different_resource` | tasks of a group use pairwise different resources | `GroupingParams` | extra tasks sharing a resource | H S R | - - - | 6.5.12 |
| `fixed_assignment` | scoped tasks take given start and/or resources (bulk/soft/relaxable form of `Task.pin`) | `FixedAssignmentParams` | not at the pinned value | H S R | H - - (prune) | 6.5.13 |
| `forbidden_pair` | TASK_TASK: two tasks never on the same resource / never overlapping; RESOURCE_RESOURCE: two resources never together on one task | `ForbiddenPairParams` | pair violated | H S R | - - - | 6.5.14 |
| `max_tasks_per_resource` | number of tasks a resource takes (whole horizon or per period) `<= max` / `>= min` | `MaxTasksPerResourceParams` | tasks above max / below min | H S R | - - - | 6.5.15 |

Scope semantics per family: `task_eligibility`, `time_window`, `fixed_assignment`, `multi_resource_requirement`,
`precedence` scopes select TASKS (`kind` = all|task|taskType|tag); `resource_capacity`, `resource_availability`,
`max_consecutive`, `min_rest`, `max_load_per_period`, `max_tasks_per_resource` scopes select RESOURCES
(`kind` = all|resource|resourceType|tag-of-resource); `no_overlap`, `same_resource`, `different_resource`: `kind=tag`
(one group) or `kind=all` + `groupByTagPrefix` (one group per matching task tag); `forbidden_pair` ignores the scope.

### 4.4 Objective families

All objectives are MINIMISATION terms internally (maximisation = negated, reported sign-normalised). `scale` is applied only
to decimal inputs (`costPerTick`, `costPerUse`, `slot.cost`, `penalty` doubles); tick/count measures are exact integers.

| Type id | Term (per objective, before `weight`) | Params | Notes | CP-SAT | Timefold |
|---------|---------------------------------------|--------|-------|--------|----------|
| `minimize_makespan` | `M = max end over selected tasks in scope` | - | M >= every end | yes | no |
| `minimize_total_tardiness` | `sum max(0, end_t - dueDate_t)` over tasks with a dueDate | `TardinessParams(weightBy)` | `weightBy=PRIORITY` multiplies by `1+priority` | yes | no |
| `minimize_max_tardiness` | `max_t max(0, end_t - dueDate_t)` | - | | yes | no |
| `minimize_resource_cost` | `sum_c lit_c * scale(costPerTick_r) * duration_c  +  sum_r used_r * scale(costPerUse_r)` | - | `costPerUse` needs `used_r` | yes | yes (costPerTick only; costPerUse rejected `COST_PER_USE`) |
| `minimize_slot_cost` | `sum slot.cost of the slot chosen by the task start` (slot-mode tasks) | - | v1 objective | yes | yes |
| `maximize_task_priority` | `+ sum priority_t * selected_t` over OPTIONAL tasks (v1 semantic) | - | | yes | yes |
| `maximize_selected_value` | `+ sum effectivePrize_t * selected_t` over OPTIONAL tasks (`prize ?? 1`) | - | injected by default at tier -1 | yes | yes |
| `minimize_max_load` | `L >= load_r` for resources in scope | `LoadObjectiveParams(metric)` | fairness (min-max) | yes | no |
| `balance_load` | `Lmax - Lmin` over resources in scope that have candidates | `LoadObjectiveParams` | fairness (spread) | yes | no |
| `minimize_idle` | `sum_r (span_r - load_r)`, `span_r = lastEnd_r - firstStart_r` over tasks assigned to r | - | idle/gaps inside the working span | yes | no |
| `minimize_setup` | `sum` of setup ticks of adjacent task pairs (circuit arcs) | - | needs resources with a `setupMatrixId` | yes | no |
| `minimize_preference_penalty` | `sum` of `Task.preferences` penalties violated | - | RESOURCE kind: penalty if not on resource; START kind: penalty x ticks of deviation | yes | no |
| `minimize_plan_changes` | deviation from `initialSolution` (start moved: weight per tick; resource changed: weight per resource) | `PlanChangeParams` | replanning stability | yes | no |

Lexicographic tiers: each objective and each SOFT constraint has a tier. Tiers compare strictly (tier k is optimised
before tier k+1; k+1 never degrades k). Inside one tier terms are summed with their weights. Reserved: tier -2 =
relaxation cost, tier -1 = automatic selection value. `maxTiers` distinct tiers per request (default 8).
(Signed "lateness" is not offered: it is not monotone; use tardiness. Earliness cost is a v2.1 candidate.)

### 4.5 Engine capability matrix summary

Features (enum `EngineCapabilities.Feature`): `FREE_START`, `SLOT_MODE`, `DURATIONS_VARIABLE` (non-slot duration),
`RESOURCE_DURATIONS`, `CUMULATIVE_DEMAND` (capacity > 1 with demand > 1), `OPTIONAL_TASKS`, `PRECEDENCES`, `TIME_WINDOWS`,
`CALENDARS`, `SETUPS`, `MULTI_RESOURCE` (count > 1 or > 1 requirement), `PINS`, `INITIAL_SOLUTION` (hint), `LOCKED`,
`LEXICOGRAPHIC` (more than one tier), `RELAXATION`, `EXPLAIN_ASSUMPTIONS`, `DETERMINISTIC`, `PROGRESS`, `COST_PER_USE`,
`MAX_LOAD_TICKS`.

| Feature | cp_sat | timefold | Timefold rejection code |
|---------|:------:|:--------:|-------------------------|
| SLOT_MODE | yes | yes (only mode) | - |
| FREE_START | yes | **no** | `ENGINE_UNSUPPORTED_FEATURE:FREE_START` |
| DURATIONS_VARIABLE / RESOURCE_DURATIONS | yes | duration only used for slot fit; `resourceDurations` rejected | `...:RESOURCE_DURATIONS` |
| CUMULATIVE_DEMAND | yes | yes (per-slot sum of demand <= capacity, slots must be disjoint) | `OVERLAPPING_SLOTS` if slots overlap |
| OPTIONAL_TASKS | yes | yes | - |
| PRECEDENCES / SETUPS / MULTI_RESOURCE / LEXICOGRAPHIC / RELAXATION / LOCKED / INITIAL_SOLUTION / MAX_LOAD_TICKS / COST_PER_USE | yes | **no** | `ENGINE_UNSUPPORTED_FEATURE:<name>` |
| TIME_WINDOWS / CALENDARS / PINS | yes | yes, as neutral pruning of (task,resource,slot) options | - |
| EXPLAIN_ASSUMPTIONS | yes | no (the exact pre-solve analysis works for both) | warning only |
| DETERMINISTIC | yes (6.12) | yes (single move thread + seed) | - |
| PROGRESS (listener) | yes | yes (best-solution events) | - |

Optimality: cp_sat proves OPTIMAL/INFEASIBLE; Timefold reports FEASIBLE at best.

---

## 5. Engine SPI (the extensibility fix)

Goal: adding a constraint family = one params record + one `ConstraintFamily` bean + one contributor bean per engine that
supports it. `CpSatSolverEngine`, `CpModelBuilder`, `PlanningService`, validators and registries are never edited.

### 5.1 Layering

```
shared IR (records)                       eu.isygoit.dto.request.*, eu.isygoit.dto.request.params.*
engine-neutral SPI (spring beans)         eu.isygoit.solver.spi : ConstraintFamily, ObjectiveFamily, ConstraintChecker, ObjectiveEvaluator, ...
engine-neutral compile step               eu.isygoit.solver.index : CandidateIndex (+builder), CompiledProblem, scope resolution
per-engine SPI                            eu.isygoit.solver.cpsat   : CpSatConstraintContributor, CpSatObjectiveContributor, CpModelContext, Enforcer
                                          eu.isygoit.solver.timefold: TimefoldConstraintContributor, TimefoldObjectiveContributor
registries (auto-discover beans)          eu.isygoit.solver.registry: ConstraintFamilyRegistry, ObjectiveFamilyRegistry, CpSatContributorRegistry, TimefoldContributorRegistry
```
Spring wiring: each registry takes the collection of beans (constructor injection), builds an unmodifiable
`LinkedHashMap<String,X>` sorted by `@Order` then `type()`, and FAILS at startup on a duplicate `type()`. Registries are
created in `PlanningEngineConfiguration` (WP7) like the v1 `ConstraintRegistry`; tests construct them by hand with `List.of(...)`.

### 5.2 Engine-neutral SPI (package `eu.isygoit.solver.spi`) - exact signatures

```java
public interface ConstraintFamily {
    String type();                                                   // "min_rest"
    Class<?> paramsType();                                           // MinRestParams.class
    Set<IEnumEnforcement.Types> supportedEnforcements();             // semantic support, engine independent
    String descriptionKey();                                         // i18n key "plan.constraint.min_rest.description"
    List<String> paramsHint();                                       // ["minRestTicks","afterTasks"] for the UI
    ScopeKind scopeKind();                                           // TASKS | RESOURCES | GROUPS | NONE  (validates Constraint.scope.kind)
    /** Structural validation. Report through ctx; never throw for user errors. params is already bound. */
    void validate(ValidationContext ctx, Constraint constraint, Object params);
    /** Estimate the model cost BEFORE building (candidates/index are available). */
    ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params);
    /** Neutral compile-time step; called only when the constraint is HARD. Default: nothing. */
    default void preprocess(PreprocessContext ctx, Constraint constraint, Object params) {}
    /** Independent checker used by PlanningSolutionValidator (8.2). Must not use any solver. */
    ConstraintChecker checker();
}

public enum ScopeKind { TASKS, RESOURCES, GROUPS, NONE }

public interface ConstraintChecker {
    /** Reports every violated INSTANCE. measure = family units (ticks, count). Instances must match the unit the contributor penalises. */
    void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink);
}
public interface ViolationSink { void violation(String taskId, String resourceId, String code, String message, long measure); }

public interface ObjectiveFamily {
    String type();
    Class<?> paramsType();                                           // Void.class when none
    String descriptionKey();
    List<String> paramsHint();
    boolean reward();                                                // true = maximisation term (reported sign-normalised)
    void validate(ValidationContext ctx, Objective objective, Object params);
    ModelCost estimate(CandidateIndexView index, Objective objective, Object params);
    /** Exact value of the term (before weight) for a finished solution, same ScaledMath arithmetic as the engines. */
    ObjectiveEvaluator evaluator();
}
public interface ObjectiveEvaluator { long evaluate(CheckContext ctx, Objective objective, Object params); }

public record ModelCost(long boolVars, long intVars, long constraints, long intervals, long circuitArcs, long pairwise) {
    public static final ModelCost ZERO = new ModelCost(0,0,0,0,0,0);
    public ModelCost plus(ModelCost o) { /* component-wise saturating add */ }
}

public interface ValidationContext {
    PlanningProblem problem();
    CandidateIndexView index();                                      // may be a light view when validation runs before the index exists (ids -> idx only)
    <P> P params(Constraint c, Class<P> type);                       // strict bind; reports PARAMS_INVALID and returns null on failure
    void error(String code, String path, String message);
    void warning(String code, String path, String message);
    boolean hasTask(String id);  boolean hasResource(String id);  boolean hasSlot(String id);
    int tick(Instant t, String path);                                // aligned conversion, reports TIME_NOT_ALIGNED / TIME_OUT_OF_HORIZON, returns -1 on error
}

public interface PreprocessContext {
    CandidateIndexBuilder.Mutable candidates();                      // prune (task, requirement, resource) / start options; rewrite requirements
    int[] resolveTasks(ConstraintScope scope);   int[] resolveResources(ConstraintScope scope);
    Horizon horizon();
    void pruneCandidate(int candidate, String reasonCode, String constraintId);  // recorded for the infeasibility core
    void restrictStart(int task, int lo, int hi);                    // intersect the start domain
    void restrictStartDomain(int task, int[] runs /* [lo0,hi0,lo1,hi1,...] inclusive */);
}

/** Optional second interface of a family that REWRITES tasks before indexing (multi_resource_requirement). Called by DomainNormalizer for HARD constraints; the constraint is then `active=false`. */
public interface TaskRewriter { Task rewrite(Task task, Constraint constraint, Object params); }

public interface CheckContext {
    PlanningProblem problem();   CandidateIndexView index();   SolutionView solution();
    long scaled(double value, int scale);                            // ScaledMath
}
public interface SolutionView {                                      // int-indexed, built once per validation
    int taskCount();
    boolean selected(int task);
    int start(int task);  int end(int task);
    int resourceCount(int task);                                     // number of assigned resources
    int resourceAt(int task, int k);  String roleAt(int task, int k);  int amountAt(int task, int k);
    int[] tasksOfResource(int resource);                             // selected tasks assigned to the resource, ascending start (ties: task idx)
    int slotOf(int task);                                            // slot index or -1
}
```
`CandidateIndexView` is the read-only interface implemented by `CandidateIndex` (5.4). `PlanLimitsProperties` and the
`Horizon` are reachable from it (`index.horizon()`, `index.limits()`).

### 5.3 Engine contract

```java
package eu.isygoit.solver;

public interface SolverEngine {
    String name();                                                   // "cp_sat" | "timefold"
    EngineCapabilities capabilities();                               // static + registry derived (5.6)
    SolveResult solve(CompiledProblem problem, SolveContext context);// replaces solve(PlanningProblem, SolverOptions[, token])
}

public record SolveContext(
        SolverOptions options,
        SolverCancellationToken cancellation,                        // existing class, unchanged
        SolveListener listener,                                      // never null: SolveListener.NOOP
        long startNanos) {                                           // System.nanoTime() at the START of the whole request (build time counts)
    public long remainingMillis(long nowNanos) { /* maxTimeSeconds*1000 - elapsed */ }
}

public interface SolveListener {
    SolveListener NOOP = new SolveListener() {};
    default void onProgress(SolveProgress progress) {}               // throttled by the ENGINE to >= 1 per second
    default boolean wantsIncumbents() { return false; }
    default void onIncumbent(IncumbentSnapshot snapshot) {}          // only built when wantsIncumbents()
}
public record SolveProgress(long elapsedMillis, int solutionCount, Double objective, Double bestBound, Double gap, int tier, String phase) {}
// phase: BUILD | PRESOLVE | SEARCH | TIER_FIXING | EXPLAIN | POST
public record IncumbentSnapshot(long elapsedMillis, List<Assignment> assignments, double objective) {}

public record CompiledProblem(PlanningProblem problem, CandidateIndex index, ModelCost estimate, List<CompiledConstraint> constraints, List<CompiledObjective> objectives) {}
public record CompiledConstraint(int index, Constraint constraint, ConstraintFamily family, Object params, boolean active /* false = fully handled by preprocess */) {}
public record CompiledObjective(int index, Objective objective, ObjectiveFamily family, Object params, int tier, int scale) {}
```
`JobProcessor` (WP7) passes a listener that publishes a throttled `PROGRESS` `LogEntry` (existing log pipeline) and keeps
the cancellation token flow untouched. `SolveCancelledException.check(token, startNanos, budgetSeconds)` stays and is
called by `ctx.checkpoint()` and by the index builder.

### 5.4 CandidateIndex (built ONCE, int-indexed, no string keys in hot loops)

Package `eu.isygoit.solver.index`. All arrays are `int[]`/`long[]`/`boolean[]`; strings only in `taskIds[]`,
`resourceIds[]`, `slotIds[]` (idx -> id) and the build-time maps (`Map<String,Integer>`, used by validators and
`resolve*`, never inside model-building loops).

```java
public final class CandidateIndex implements CandidateIndexView {
    // ---- sizes
    public final int taskCount, resourceCount, slotCount, calendarCount, candidateCount, requirementCount, edgeCount;
    public final Horizon horizon;  public final int horizonTicks;
    // ---- ids
    public final String[] taskIds, resourceIds, slotIds, roles;   // by index (roles: distinct role names, requirement -> role idx in reqRole)
    // ---- tasks
    public final int[] duration;          // [t] base duration in ticks
    public final int[] demand, priority;  // [t]
    public final boolean[] required;      // [t]
    public final long[] prize;            // [t] effective prize (prize ?? 1), 0 for required tasks
    public final int[] release, latestEnd, due;  // [t] ticks; latestEnd = min(deadline, horizonTicks); due = NO_DUE (Integer.MIN_VALUE) if absent
    public final int[] estStart, lstStart;       // [t] domain after pruning: window, calendars, slots, CPM (5.4 step 5)
    public final int[] pinStart;          // [t] -1 or tick
    public final int[] setupClass;        // [t] class index inside the task's resource matrices, or -1
    // ---- requirements (CSR by task)
    public final int[] taskReqStart;      // [T+1]  requirement ids of task t = [taskReqStart[t], taskReqStart[t+1])
    public final int[] reqCount, reqAmount, reqRole;  // [Q]
    // ---- candidates (CSR by task, sorted by requirement then resource idx)
    public final int[] taskCandStart;     // [T+1]
    public final int[] candTask, candReq, candResource, candDuration, candAmount;   // [C]
    public final int[] candStartRunStart, candStartRuns;   // optional per-candidate start-domain override (runs [lo,hi] inclusive); empty range = same as task domain
    // ---- candidates (CSR by resource)
    public final int[] resCandStart;      // [R+1]
    public final int[] resCand;           // [C] candidate ids grouped by resource, ascending
    // ---- resources
    public final int[] capacity;          // [R]
    public final int[] resCalendar;       // [R] calendar idx or -1
    public final int[] resRunStart, resRuns;  // CSR: per-resource sorted merged AVAILABLE runs [lo,hi) (calendar + unavailable); empty range = always available
    public final int[] resSetupMatrix;    // [R] matrix idx or -1
    public final int[] maxLoadTicks;      // [R] or Integer.MAX_VALUE
    // ---- precedence edges (CSR by successor)
    public final int[] succEdgeStart, edgePred, edgeSucc, edgeType, edgeMinLag, edgeMaxLag, edgeImpliesPresence;   // [E]; maxLag = Integer.MAX_VALUE if none
    public final int[] topoOrder;         // [T] topological order (cycles were rejected before)
    // ---- slots
    public final int[] slotStart, slotEnd;   public final double[] slotCost;   // [S]
    public final int[] taskSlotStart, taskSlots;   // CSR [T+1],[..] allowed slot idx per task (empty range = free start)
    // ---- symmetry
    public final int[] taskSymmetryClass, resourceSymmetryClass;   // -1 = unique; equal values = interchangeable (6.7)

    public int[] resolveTasks(ConstraintScope scope);      // cached, sorted ascending
    public int[] resolveResources(ConstraintScope scope);  // cached, sorted ascending
    public int[][] groupTasksByTagPrefix(String prefix);   // groups for no_overlap / same_resource / different_resource (sorted by tag)
    public int taskIdx(String id);  public int resourceIdx(String id);  public int slotIdx(String id);   // build-time map lookups
}
```

Build algorithm (`CandidateIndexBuilder.build(PlanningProblem, List<CompiledConstraint>, PlanLimitsProperties, SolverCancellationToken)`),
complexity O(T + R + C + E + W) with C = number of surviving candidates (inherent):

1. Assign indices (request order). Convert instants to ticks with `Horizon` (alignment errors were reported earlier).
2. Calendars: expand once per calendar (`CalendarExpander`, DST-aware, merged sorted runs, O(windows)). Per-resource runs =
   calendar runs minus `Resource.unavailable` (O(runs)). Cache runs by `(calendarId, unavailable-hash)`.
3. Inverted indexes: `skill -> bitset(resources)`, `type -> bitset`, `tag -> bitset`, `id -> idx`. Selector evaluation =
   bitset intersections; results cached by the `ResourceSelector` record itself (`equals/hashCode`), so 1000 tasks sharing one
   selector evaluate it once. O(R/64) per distinct selector.
4. For each task and requirement: eligible resources = cached selector result; drop resources with `amount > capacity`;
   apply `Task.pin.resourceIds` and HARD preprocess families (task_eligibility, fixed_assignment, time_window,
   resource_availability) via `PreprocessContext`; drop a candidate when no feasible start exists in
   `[release, latestEnd-duration]` inside the resource runs (binary search, O(log runs)); for slot-mode tasks keep only slots
   with `slotEnd - slotStart >= duration` whose start lies in a run. Each drop is recorded `(candidate, reason, constraintId)` in a
   bounded list (10 000) for the infeasibility core.
5. Start domain per task: `estStart/lstStart` from release, `latestEnd - duration`, horizon, pin; then a forward/backward CPM pass over
   `topoOrder` (O(T+E)) tightening est/lst with durations and min lags; then calendars/slots (union over candidates of allowed
   runs; per-candidate override only when a candidate's allowed runs differ from the task union). `estStart > lstStart` is
   recorded for the pre-solve analyzer (not thrown).
6. Edges from `Task.predecessors` form the CSR; cycle detection happened in validation (7.1), `topoOrder` is Kahn's algorithm.
   `precedence` constraints are applied by the contributor, not the index.
7. Symmetry classes (6.7): hash of the canonical signature (duration, demand, priority, required, prize, candidate list,
   windows, slots; no pin/preds) for tasks NOT referenced by any constraint scope/pair/preference/tag-group/locked
   assignment; hash of (capacity, skills, tags, calendar, costs, type, setup matrix) for resources NOT referenced by id anywhere.
8. Estimate `ModelCost` (structural + families + objectives), compare with limits (7.3), throw `LimitExceededException`.

### 5.5 CP-SAT SPI (package `eu.isygoit.solver.cpsat`)

```java
public interface CpSatConstraintContributor {
    String type();                                                   // same id as the family
    EnforcementSupport support();                                    // which of H/S/R this FORMULATION implements
    /** Add the constraint to ctx.model(). Every constraint that expresses the RULE must be enforced by
     *  enforcer.instance(..) (null = unconditional). Definitional channeling constraints must NOT be enforced. */
    void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer);
}
public interface CpSatObjectiveContributor {
    String type();
    /** Contribute terms with sink.add(tier, expr, min, max, label). Maximisation => pass the negated expression. */
    void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink);
}
public interface ObjectiveSink {
    /** expr already includes the objective weight and scale; min/max are exact bounds of expr (overflow guards + tier multipliers). */
    void add(int tier, LinearExpr expr, long minValue, long maxValue, String label);
    void addConstantOffset(int tier, long value);
}
public record EnforcementSupport(boolean hard, boolean soft, boolean relaxable) {
    public static final EnforcementSupport HARD_ONLY = new EnforcementSupport(true, false, false);
    public static final EnforcementSupport ALL = new EnforcementSupport(true, true, true);
    public EnforcementSupport and(EnforcementSupport o) { /* component-wise AND */ }
}

/** One per constraint per build. Hides HARD / RELAXABLE / SOFT / EXPLAIN from contributors (D-7). */
public interface Enforcer {
    enum Mode { HARD, RELAXABLE, SOFT }
    Mode mode();
    /** Literal to pass to onlyEnforceIf for ONE violation instance of this constraint, or null when it must hold unconditionally.
     *  HARD(solve): null.  HARD(explain pass): the assumption literal of the constraint.  RELAXABLE: the shared active_k literal.
     *  SOFT: a fresh BoolVar "satisfied" whose negation is penalised weight x 1 in the constraint tier and recorded for reporting. */
    Literal instance(int taskIdx, int resourceIdx);                  // -1 when not applicable
    default Literal instance() { return instance(-1, -1); }
    /** SOFT only: penalise an integer measure instead of a unit literal: cost = weight x measure. HARD/RELAXABLE: returns false, adds nothing. */
    boolean penalizeMeasure(LinearExpr measure, long maxMeasure, int taskIdx);
    /** Combine instance literal with extra conditions into the array form of onlyEnforceIf; null literals are skipped. */
    Literal[] conditions(Literal instance, Literal... extra);
}
```
`CpModelContext` (one per build; created by `CpModelBuilder`; contributors only use these methods):

```java
public final class CpModelContext {
    // ---- model + data
    public CpModel model();
    public CandidateIndex index();
    public PlanningProblem problem();
    public SolverOptions options();
    public boolean explainMode();
    // ---- time variables (the builder creates all start/presence/cand variables up front; accessors are O(1) array reads)
    public IntVar start(int t);                       // domain [estStart, lstStart] (+ slot/calendar runs)
    public LinearExpr end(int t);                     // affine start+duration, or the end var when resource durations differ
    public Literal presence(int t);                   // trueLit() for required tasks, BoolVar for optional
    public Literal cand(int c);                       // BoolVar per candidate, or presence/trueLit when the task has exactly one candidate of count 1
    public IntervalVar masterInterval(int t);         // lazily created (optional when the task is optional)
    public IntervalVar candInterval(int c);           // optional interval sharing start(t)
    public Literal trueLit();                         // single shared BoolVar fixed to 1 (do not rely on CpModel.trueLiteral())
    // ---- shared helpers (cached)
    public Literal[] startChoice(int t);              // slot-mode: literal per allowed start value, exactly-one == presence(t); null if not slot mode
    public long[] startChoiceValues(int t);
    public Literal[] startBucket(int t, BucketGrid grid);  // literal per bucket "task starts in bucket k"; null entry = unreachable; exactly-one == presence
    public BucketGrid buckets(PeriodSpec period);    // tile the horizon (DAY/WEEK in meta.timezone, TICKS)
    public Literal resourceUsed(int r, boolean exact);
    public LinearExpr load(int r, LoadMetric metric); // sum duration*cand (TICKS) or sum cand (COUNT) over the resource's candidates
    public IntVar newTierVar(String name, long lo, long hi);
    public Literal and(Literal a, Literal b);          // cached product literal: w => a, w => b, (a and b) => w (definitional, never enforced)
    // ---- resource blocks and setups
    public void blockResource(int r, int lo, int hi, int demand, Literal enforcedBy);  // fixed (optional when enforcedBy != null) interval of `demand` capacity in [lo,hi); materialised in NoOverlap/Cumulative at finalisation
    public void registerSetupArcs(int r, List<SetupArc> arcs);   // used by the setup circuit module
    public List<SetupArc> setupArcs(int r);                       // read by minimize_setup
    // ---- guards and bookkeeping
    public void checkpoint();                         // cancellation/time check, call every 1024 iterations in loops
    public void count(int boolVars, int intVars, int constraints, int intervals);  // size guard vs PlanLimitsProperties
    public ModelStats stats();
}
public record BucketGrid(int[] lo, int[] hi, String id) {}   // bucket k = ticks [lo[k], hi[k])
public record SetupArc(Literal literal, int ticks) {}

/** Optional plug-in of the CP-SAT builder for structural model parts that are not catalogue constraints
 *  (setup circuits, symmetry breaking). Called in ascending order() after the structural model (6.1 steps 7-8). */
public interface CpSatStructuralModule { String name(); int order(); void apply(CpModelContext ctx); }
```
Every contributor method that allocates must call `ctx.count(..)` (the guard throws `LimitExceededException`, code
`MODEL_TOO_LARGE`) and loops call `ctx.checkpoint()`.

### 5.6 Timefold SPI and capability derivation

```java
public interface TimefoldConstraintContributor {
    String type();
    EnforcementSupport support();
    /** Contributes to option-level data only (no Timefold stream code in contributors). */
    void contribute(TimefoldBuildContext ctx, CompiledConstraint constraint);
}
public interface TimefoldObjectiveContributor {
    String type();
    /** adds the exact scaled cost of every (task,resource,slot) OPTION and/or the selection reward of a task. */
    void contribute(TimefoldBuildContext ctx, CompiledObjective objective);
}
public final class TimefoldBuildContext {
    public CandidateIndex index();
    public SlotOptions options();              // derived once: option = (candidate, slot) pair, CSR per task; neutral pruning already applied
    public long[] optionPenalty();             // [option] soft penalty
    public long[] selectionReward();           // [task] reward for selecting an optional task
    public void forbid(int option, String constraintId);
    public int[] slotCapacityOverride();       // [resource*slotCount + slot] or null
}
```
The v1 style model survives in Timefold: planning entity `TimefoldTask(taskIdx)` with ONE planning variable `option`
(nullable for optional tasks) whose `@ValueRangeProvider` returns a view over `SlotOptions` (a shared int slice; NO R*S map
per entity); constraint provider with: hard capacity per (resource, slot) = sum(demand) <= capacity, hard "option allowed",
soft per-option cost+penalty, soft selection reward. Score = `HardSoftLongScore` (64-bit; the v1 32-bit guard is deleted).

**Capability derivation** (`EngineCapabilities`):

```java
public record EngineCapabilities(
        String engineId,
        Set<Feature> features,
        Map<String, EnforcementSupport> constraints,   // type -> support (family.supported AND contributor.support, only when a contributor exists)
        Set<String> objectives,                        // objective types that have a contributor
        boolean provesOptimality) {
    public enum Feature { /* list in 4.5 */ }
    public boolean supports(Feature f);
    public Set<String> unsupportedConstraintTypes(Collection<String> allKnownTypes);
    public Set<String> unsupportedObjectiveTypes(Collection<String> allKnownTypes);
}
```
`CpSatSolverEngine.capabilities()` = every feature; constraints/objectives from `CpSatContributorRegistry`.
`TimefoldSolverEngine.capabilities()` = the fixed feature set of 4.5; constraints/objectives from `TimefoldContributorRegistry`.
`ProblemVerifier` (7.2) computes the required features of the problem (`FeatureScanner`) and checks
`required - engine.features == {}` plus per-constraint/objective support; every miss produces ONE issue with code, path
(`tasks[3].predecessors`, `constraints[2]`, `objectives[0]`), the feature/type name and the engine name. The same data feeds
`GET /capabilities` (9.1), so UIs can grey out unsupported options per engine.

### 5.7 Registries

```java
public final class ConstraintFamilyRegistry {
    public ConstraintFamilyRegistry(Collection<ConstraintFamily> families);       // duplicate type -> IllegalStateException at startup
    public ConstraintFamily get(String type);                                     // UnknownConstraintException (existing) when missing
    public Collection<String> supportedTypes();
}
public final class ObjectiveFamilyRegistry { /* same, UnknownObjectiveException */ }
public final class CpSatContributorRegistry {
    public CpSatContributorRegistry(Collection<CpSatConstraintContributor> c, Collection<CpSatObjectiveContributor> o);
    public Optional<CpSatConstraintContributor> constraint(String type);
    public Optional<CpSatObjectiveContributor> objective(String type);
}
public final class TimefoldContributorRegistry { /* same */ }
```
`SolverRegistry` stays (engine name -> engine, lower-cased; also resolves the aliases `cp-sat`, `cpsat`).

---

## 6. CP-SAT model formulation

Notation: T tasks, R resources, C candidates, E precedence edges, H horizon ticks. `p_t` = presence literal (constant true for
required tasks, no variable), `x_c` = candidate literal, `s_t` = start variable, `d_c` = candidate duration, `e_t = s_t + d_t`
(affine expression, no variable). All code below is pseudocode in the style of the OR-Tools Java API
(`model.addXxx(...).onlyEnforceIf(lits)`); implementers must check the exact 9.15 method names (for example
`newOptionalFixedSizeIntervalVar`, `addLinearExpressionInDomain`, `addCircuit`, `addCumulative`).

**Universal contributor rule (global constraints cannot be conditioned).** `NoOverlap`, `Cumulative` and `Circuit` have no
enforcement literal. A contributor that needs one of them checks `enforcer.instance()`: if it is `null` (HARD in the solve
pass) it uses the global constraint; if it is non-null (RELAXABLE, SOFT, or HARD in the explain pass) it uses the pairwise
fallback (order literals) guarded by `PlanLimitsProperties.maxPairwise`. Exceeding the guard in the explain pass skips that
family from the core (`diagnostics.details.explainSkipped`), exceeding it in the solve pass is a validation error
(`PAIRWISE_TOO_LARGE`).

### 6.1 Build order (`CpModelBuilder.build(CompiledProblem, SolveContext, BuildMode)`; `BuildMode` = `SOLVE` | `EXPLAIN`)

```
ctx = new CpModelContext(...)
 1  createTimeVariables()          // s_t, p_t, e_t, durations                       O(T)
 2  createCandidates()             // x_c, per-task requirement exactly-count        O(C)
 3  createResourceModels()         // intervals, NoOverlap/Cumulative collection     O(C)
 4  applyCandidateStartDomains()   // calendars / slots per candidate                O(C + runs)
 5  applyPrecedenceEdges()         // Task.predecessors                              O(E)
 6  applyPinsLocksMaxLoad()        // Task.pin, initialSolution.locked, maxLoadTicks O(T + R)
 7  for each CpSatStructuralModule (ascending order()):  module.apply(ctx)
                                   //   SetupCircuitModule (order 700, 6.6, O(arcs)), SymmetryBreakingModule (order 800, 6.7, O(T + C))
 9  for each active CompiledConstraint (registry order, then request order):
        enforcer = enforcerFor(constraint)            // HARD / RELAXABLE / SOFT / EXPLAIN
        contributor.apply(ctx, constraint, enforcer)
10  for each CompiledObjective: contributor.contribute(ctx, objective, sink)
11  finalizeResourceModels()       // NoOverlap/Cumulative creation with blocks added by families (ctx.blockResource)
12  buildObjective(sink, options)  // 6.8: tiers, relaxation tier, soft penalties
13  applyHints()                   // 6.11
return BuiltModel(model, ctx, tierExpressions, assumptions, softInstances, relaxLiterals, decisionVarRefs)
```
Every loop calls `ctx.checkpoint()` every 1024 iterations (existing `CHECKPOINT_INTERVAL` idiom).

### 6.2 Variables and counts

| Variable | Created for | Count | Notes |
|----------|-------------|-------|-------|
| `s_t` IntVar | every task | T | domain `[estStart, lstStart]` as runs (calendar/slot union); fixed by pin/lock; in EXPLAIN mode domain is `[0,H]` and restrictions become enforced constraints |
| `p_t` BoolVar | OPTIONAL tasks only | <= T | required tasks use `trueLit()` |
| `x_c` BoolVar | candidate with > 1 alternative in its (task, requirement) | <= C | if a requirement has a single candidate and count = 1, `x_c = p_t` (no variable); if count == number of candidates, `x_c = p_t` too |
| `dur_t` IntVar | tasks whose candidates have different durations (`resourceDurations`) | few | domain = distinct durations; `dur_t == d_c` enforced by `x_c` |
| master interval | lazily, only if a family asks (`no_overlap`, tardiness...) | <= T | optional (presence `p_t`) |
| candidate interval | every candidate of a resource that has >= 2 candidates | <= C | `newOptionalFixedSizeIntervalVar(s_t, d_c, x_c)`; ALL candidates of a task share the SAME start variable `s_t`; with resource-specific duration the interval is `newOptionalIntervalVar(s_t, d_c, s_t+d_c, x_c)` |
| startChoice literals | slot-mode tasks | sum of allowed starts | exactly-one over allowed start values, `s_t = sum value_k * y_k` |
| bucket literals | only for tasks touched by windowed families | on demand | cached per (task, grid) |
| tier / measure vars | objectives and soft measures | O(T) or O(R) | |

No names are given to variables unless `app.plan.cpsat.named-variables=true` (default false, true when `logSearch` or in tests):
names cost memory proportional to C. Variable identity is carried by the int-indexed arrays of `CpModelContext`
(`IntVar[] start`, `Literal[] cand`, `Literal[] presence`), never by a `Map<String,...>`.

### 6.3 Domain tightening and presolve data (all computed in the neutral `CandidateIndex`, step 5 of 5.4)

```
est[t] = max(release[t], pinStart[t] if pinned, 0)
lst[t] = min(latestEnd[t] - minCandDuration(t), H - minCandDuration(t))
forward pass in topoOrder:   est[j] = max(est[j], est[i] + minDur(i) + lag)     for FS edges (SS: est[i]+lag; FF/SF analogous on end)
backward pass in reverse:    lst[i] = min(lst[i], lst[j] - minDur(i) - lag)
calendar runs per candidate: allowed starts = { [max(a,est), min(b-d, lst)] : [a,b) in availableRuns(r), b-a >= d }
task start domain = union over candidates of allowed starts   (Domain.fromFlatIntervals)
slot mode: allowed starts additionally restricted to slotStart[k] of the task's slots (sorted, distinct)
candidate removed if its allowed starts are empty
```
Presolve data handed to CP-SAT: tight start domains (the biggest win), pruned candidates (fewer literals), `Domain` objects shared
by reference across tasks with equal `(runs, duration)` (cached in the index), constants folded (`x_c = p_t`), no big-M anywhere.
The horizon used for makespan-like variables is `[max(est+dur), min(H, max latestEnd)]`.

### 6.4 Structural constraints

**Requirements and presence** (step 2):

```
for each task t:
  for each requirement q of t (candidates K_q, count n_q):
      if required(t):   if n_q == 1: addExactlyOne(x_c for c in K_q)       else addLinearConstraint(sum x_c, n_q, n_q)
      else (optional):  addLinearExpressionEqual(sum x_c, n_q * p_t)        // for n_q == 1: p_t == OR(x_c) via sum == p_t
  for each resource r appearing in >= 2 requirements of t:  addAtMostOne(x_c for those c)        // a resource fills one requirement per task
  if required(t) and no candidate/insufficient candidates: handled by the pre-solve analyzer (never reaches the model)
```

**Resource models** (steps 3 and 11): each resource r with candidate set `K_r`:

```
if |K_r| >= 2 or r has blocks or setups:
   intervals_r = [ candInterval(c) for c in K_r if d_c > 0 ]
   if capacity[r] == 1 (or every amount == capacity):  noOverlap(intervals_r + blocks_r)        // blocks: fixed intervals added by families
   else:  cum = addCumulative(capacity[r]); for c in K_r: cum.addDemand(candInterval(c), amount_c); for b in blocks_r: cum.addDemand(fixedInterval(b), b.demand)
if maxLoadTicks[r] < INF:  addLinearConstraint(load(r, TICKS), 0, maxLoadTicks[r])               // structural hard cap
```
Candidates with duration 0 are excluded from `NoOverlap`/`Cumulative` (zero-size intervals occupy nothing). `blockResource(r, lo, hi, demand, lit)` is how
`resource_capacity` adds time-bounded capacity reductions; blocks with a non-null `lit` use an OPTIONAL fixed interval.

**Calendars (D-5)**. Availability is applied as START DOMAINS, not as blocking intervals inside `NoOverlap`
(a task must lie within ONE working window, so `start in [a, b-d]` for some run `[a,b)` is exactly equivalent and far
cheaper than T x blocks):

```
for each task t:
  union = task start domain (computed in 6.3)
  for each candidate c of t whose allowed starts differ from union (different calendar/duration):
      model.addLinearExpressionInDomain(s_t, Domain.fromFlatIntervals(runs(c))).onlyEnforceIf(x_c)     // cached Domain per (calendar, duration)
```
Resources without calendar and without `unavailable` produce no constraint at all.

**Slot mode** (`startChoice`): allowed start values `v_1..v_K` (K <= `app.plan.cpsat.max-start-choices`, default 256; larger K falls back to the plain domain
with no choice literals and `minimize_slot_cost` using `startAt` literals):

```
y_k = newBoolVar();  addLinearExpressionEqual(sum y_k, p_t) ;  addLinearConstraint(s_t - sum v_k*y_k, 0, 0)   // definitional, not enforced
```

**Precedence edges** (step 5, shared with the `precedence` family through `PrecedenceEmitter.emit`):

```
emit(i, j, type, minLag, maxLag, Literal[] cond):         // cond = [] for hard structural edges between required tasks
   lhs/rhs per type:  FS: s_j >= e_i + minLag     SS: s_j >= s_i + minLag     FF: e_j >= e_i + minLag     SF: e_j >= s_i + minLag
   maxLag (if any):   FS: s_j <= e_i + maxLag     SS: s_j <= s_i + maxLag     FF: e_j <= e_i + maxLag     SF: e_j <= s_i + maxLag
   cond += [p_i, p_j] for optional endpoints (skip constants)
   addGreaterOrEqual(lhs, rhs).onlyEnforceIf(cond)   // addLessOrEqual for the maxLag part
   presence == IMPLIES:  addImplication(p_j, p_i)
```
Both endpoints are `LinearExpr` affine expressions: no auxiliary variables. Edge count is limited by `maxPrecedences`.

**Pins, locks** (step 6): `Task.pin.start` -> start domain fixed (already in the index); `pin.resourceIds` -> candidates pruned in the index;
locked `initialSolution` entries -> `model.addEquality(s_t, tick)`, candidate literals fixed to 1 for the locked resources and 0 for the others of that
requirement. In EXPLAIN mode locks are guarded by a `LOCKED` assumption per task.

### 6.5 Constraint families (formulations and pseudocode)

Notation: `E = enforcer`, `inst(t, r) = E.instance(t, r)` (null = unconditional), `cond(...) = E.conditions(...)`.
Families that are HARD-only in preprocess are no-ops in `apply` when `E.mode()==HARD && !ctx.explainMode()`.

**6.5.1 `task_eligibility`** (apply is used for SOFT/RELAXABLE/explain; HARD is pruned by `preprocess`):
```
allowed(c) = resource matches params (ids/types/skills/exclusions) ; slotAllowed(k) = slotIds empty or contains slot k
for t in resolveTasks(scope):
  for c in cands(t) with !allowed(c):           addEquality(cand(c), 0).onlyEnforceIf(inst(t, resourceOf(c)))        // SOFT unit = one forbidden (task,resource) used
  if slot mode and slotIds given: for k with !slotAllowed(k): addEquality(startChoice(t)[k], 0).onlyEnforceIf(inst(t, -1))
```
**6.5.2 `resource_capacity`**:
```
for r in resolveResources(scope):
   newCap = min(capacity[r], params.capacity) ; if newCap >= capacity[r] continue
   lo,hi = window ticks (default 0,H)
   ctx.blockResource(r, lo, hi, demand = capacity[r] - newCap, enforcedBy = inst(-1, r))   // cumulative resources: demand; unary resources: newCap must be 0 => demand 1
```
(`params.capacity` is `>= 0`; for a unary resource only `0` is meaningful.)
**6.5.3 `no_overlap`**:
```
for group G in groups(scope, groupByTagPrefix):
   gap = params.minGapTicks (default 0)
   if inst() == null:  intervals = [ gap==0 ? masterInterval(t) : optionalInterval(s_t, d_t+gap, e_t+gap, p_t) for t in G if d_t > 0 ];  addNoOverlap(intervals)
   else: for each pair (i<j) in G that can conflict (est/lst test) and pairs <= maxPairwise:
            ei = inst(i, -1);  o1 = newBool; o2 = newBool
            addLessOrEqual(e_i + gap, s_j).onlyEnforceIf(o1);  addLessOrEqual(e_j + gap, s_i).onlyEnforceIf(o2)
            addBoolOr([o1, o2, !p_i, !p_j]).onlyEnforceIf(ei)                                  // SOFT unit = one overlapping pair
```
**6.5.4 `precedence`**: `from = resolveTasks(params.from)`, `to = resolveTasks(params.to)`; `ALL_PAIRS` -> `|from|*|to| <= maxPairwise`; `BY_ORDER` -> equal sizes;
`for each pair: emit(i, j, type, minLag, maxLag, cond(inst(j, -1)))` (SOFT unit = one violated pair).
**6.5.5 `time_window`** (HARD = preprocess `restrictStart`/latest end):
```
for t in tasks: for each given bound (ticks) L/U on start or end:
   if E.mode()==SOFT and E.penalizeMeasure(slack, maxSlack, t):    // slack >= L - s_t  (or e_t - U), slack >= 0, measure = ticks outside
        addGreaterOrEqual(slack + s_t, L)                           // definitional: slack = max(0, L - s_t) via minimisation
   else:  addGreaterOrEqual(s_t, L).onlyEnforceIf(cond(inst(t,-1), p_t))
```
**6.5.6 `resource_availability`**:
```
blocked = params.unavailable  (or complement of availableOnly inside the horizon)
HARD: preprocess (candidate start runs minus windows); apply is a no-op
else: for r in resolveResources(scope), for c in resCand(r), for each window w reachable by c:
        before = newBool; after = newBool
        addLessOrEqual(e_t(c), w.lo).onlyEnforceIf(before);  addGreaterOrEqual(s_t, w.hi).onlyEnforceIf(after)
        addBoolOr([before, after, !cand(c)]).onlyEnforceIf(inst(t(c), r))                    // SOFT unit = one candidate overlapping a blocked window (overtime)
```
**6.5.7 `max_consecutive`**:
```
grid = ctx.buckets(params.period) (K buckets); m = params.max
for r in resolveResources(scope):
   worked[k] = newBool for buckets reachable by some candidate of r
   for c in resCand(r), for k in reachableBuckets(t(c)):  w = ctx.and(cand(c), startBucket(t,grid)[k]);  addImplication(w, worked[k])
   for k in 0..K-m-1:  addLinearConstraint(sum worked[k..k+m], 0, m).onlyEnforceIf(inst(-1, r))     // SOFT unit = one window of m+1 consecutive worked periods
```
**6.5.8 `min_rest`** (unary resources only; `MIN_REST_RESOURCE_NOT_UNARY` otherwise):
```
for r in resolveResources(scope):
   K = candidates of r (apply afterTasks filter: only tasks in afterTasks are inflated by rest)
   if inst() == null:   intervals = [ optionalInterval(s_t, d_c + rest_if_inflated, e_t + rest_if_inflated, cand(c)) for c in K ];  addNoOverlap(intervals)   // separate NoOverlap, standard "inflated interval" trick
   else:  for pairs (i,j) of K with possible conflict (domains closer than rest) and total pairs <= maxPairwise:
              o1 => e_i + rest <= s_j ;  o2 => e_j + rest <= s_i ;  addBoolOr([o1, o2, !cand(i), !cand(j)]).onlyEnforceIf(inst(t(i), r))   // SOFT unit = one pair
   // static pairs (both tasks pinned/fixed times): decided at build time; conflicting pairs become addBoolOr([!cand(i), !cand(j)]) (roster case), O(conflicting pairs)
```
**6.5.9 `max_load_per_period`**:
```
grid = buckets(period); for r in scope, for bucket k reachable:
   terms = [ value_c * ctx.and(cand(c), startBucket(t(c), grid)[k])  for c in resCand(r) reaching k ]       // value_c = d_c (TICKS) or 1 (COUNT)
   load = sum(terms)
   if params.max != null:  if E.mode()==SOFT and E.penalizeMeasure(excess, max, -1):  excess >= load - max, excess >= 0
                           else addLessOrEqual(load, max).onlyEnforceIf(inst(-1, r))
   if params.min != null:  symmetric with deficit >= min - load  (deficit unit = ticks/count below min; only for resources having candidates)
```
**6.5.10 `multi_resource_requirement`**: not a model constraint: `DomainNormalizer` rewrites `Task.requirements` (replace the requirement of the same role or append) through
`TaskRewriter` (5.2); the resulting model is the structural one of 6.4. `apply` is a no-op. HARD only.
**6.5.11 `same_resource`**:
```
for group G (chain consecutive members a,b) and role filter:
   for r in candidateResources(a, role) UNION candidateResources(b, role):
        ca = cand(a, r) or none; cb = cand(b, r) or none
        both: addBoolOr([!p_a, !p_b, !ca,  cb]).onlyEnforceIf(inst(a,-1));  addBoolOr([!p_a, !p_b,  ca, !cb]).onlyEnforceIf(inst(a,-1))
        only ca: addBoolOr([!p_a, !p_b, !ca]).onlyEnforceIf(inst(a,-1))      only cb: symmetric
```
**6.5.12 `different_resource`**:
```
for r: lits = [ cand(c) for c in resCand(r) with t(c) in G and role matches ];  if |lits| < 2 continue
   if E.mode()==SOFT and E.penalizeMeasure(excess_r, |lits|-1, -1):  addLinearConstraint(sum lits - excess_r, -INF, 1)     // unit = extra tasks sharing r
   else: addLinearConstraint(sum lits, 0, 1).onlyEnforceIf(inst(-1, r))
```
**6.5.13 `fixed_assignment`** (HARD is neutral preprocess; apply for S/R/explain):
```
for t in scope:  cnd = cond(inst(t,-1), p_t)
   if start given:      addEquality(s_t, tick).onlyEnforceIf(cnd)
   for each requirement of `role` (or all): for c with resource not in resourceIds:  addEquality(cand(c), 0).onlyEnforceIf(cnd)
   a pinned resource that is not a candidate: addBoolOr([]) enforced => the instance literal is forced false (SOFT: pays; HARD: pre-solve analyzer reports PIN_CONFLICT)
```
**6.5.14 `forbidden_pair`**:
```
TASK_TASK SAME_RESOURCE: for pair (a,b): e = inst(a,-1) (ONE literal per pair); for r in cands(a) INTERSECT cands(b): addBoolOr([!cand(a,r), !cand(b,r)]).onlyEnforceIf(e)
TASK_TASK TIME_OVERLAP: same pairwise order-literal clause as no_overlap with gap 0
RESOURCE_RESOURCE (r1,r2): for each task t with candidates on both: addBoolOr([!cand(t,r1), !cand(t,r2)]).onlyEnforceIf(inst(t,-1))
```
**6.5.15 `max_tasks_per_resource`**: `count_r = ctx.load(r, COUNT)` (or per period with buckets as in 6.5.9 with value 1); `addLessOrEqual(count, max)` / `addGreaterOrEqual(count, min)` enforced by `inst(-1, r)`; SOFT via `penalizeMeasure(excess/deficit)`.

### 6.6 Setup / transition times (circuit, D-13)

Applies per resource with `setupMatrixId != null` (unary resources only: `SETUP_RESOURCE_NOT_UNARY`). Tasks with no `setupClass` have setup 0 to and from everything.
Precompute the class-pair table `setup[ci][cj]` (matrix defaults + entries) in the index; `s_ij = setup[class(i)][class(j)]`.

```
nodes: 0 = depot; 1..n = candidates of r (task nodes)
circuit = model.addCircuit()
circuit.addArc(0, 0, !used_r)                                   // resource unused: depot self-loop    (used_r = resourceUsed(r, exact=true))
for node i:  circuit.addArc(i, i, !cand(c_i))                   // task absent on r: self-loop
             first_i = newBool; circuit.addArc(0, i, first_i)   // i is first on r
             last_i  = newBool; circuit.addArc(i, 0, last_i)    // i is last
for ordered pair (i, j), i != j, only if  estEnd(i) + s_ij <= lst(j)  (else the arc is impossible, no literal):
             a_ij = newBool; circuit.addArc(i, j, a_ij)
             addGreaterOrEqual(s_{t(j)}, e_{t(i)} + s_ij).onlyEnforceIf(a_ij)
ctx.registerSetupArcs(r, [(a_ij, s_ij)])                        // for minimize_setup
```
The base `NoOverlap` of the resource stays (redundant, strengthens propagation). Arc count = sum over resources of (n^2 after window pruning); guard `maxSetupArcs`
(default 300 000, error `SETUP_TOO_LARGE`). If all `s_ij == 0` the matrix is dropped by the normalizer (no circuit). Optional tasks work naturally (absent task = self-loop).

### 6.7 Symmetry breaking (O(T + C), skipped for entities referenced anywhere by id)

* Interchangeable tasks (same `taskSymmetryClass`, ordered by index): `s_i <= s_j`; for optional tasks also `p_i >= p_j`.
* Interchangeable resources (same `resourceSymmetryClass`): `used_{r_k} >= used_{r_{k+1}}` with `exact=true` used literals.
* Disabled when the request has locked assignments or a hint touching a class member (index step 7) and when `solverOptions.deterministic` is false and `app.plan.cpsat.symmetry-breaking=false`.

### 6.8 Objective construction: tiers, weights, overflow (D-9, D-12)

Terms come from three sources, all through `ObjectiveSink` / `Enforcer`: user objectives (`CompiledObjective.tier`), SOFT penalties
(`weight x (1 - sat)` or `weight x measure`, tier = `constraint.tier`), relaxation (tier -2: `cost_k x (1 - active_k)`, 6.9), and the injected
`auto-selected-value` objective (tier -1). The normalizer injects the latter when the problem has an optional task and no objective of type
`maximize_selected_value` or `maximize_task_priority`; the user can disable the injection only by adding either objective.

```
tiers = sorted distinct tiers ascending (-2 < -1 < 0 < 1 ... 7); each tier has: expr_k (LinearExpr sum), lo_k = sum min bounds, hi_k = sum max bounds, R_k = hi_k - lo_k
guards: every added coefficient |c| <= 2^40 ; R_k <= 2^53 ; number of distinct tiers <= limits.maxTiers ;  violation => InvalidRequestException OBJECTIVE_SCORE_OUT_OF_RANGE (existing code)
mode = options.lexicographicMode
if AUTO: product P = prod_k (R_k + 1) computed in BigInteger;  mode = (P <= 2^53) ? WEIGHTED : SEQUENTIAL
WEIGHTED: M_last = 1; M_k = M_{k+1} * (R_{k+1} + 1);  minimize( sum_k M_k * expr_k )         // |objective| <= P <= 2^53 by construction
SEQUENTIAL: for k in tiers: model.minimize(expr_k) ; solve with time share ; fix  expr_k <= value_k ; hint = incumbent ; next tier
```
Sequential details: time share of tier k = `remaining / tiersLeft` (min 1 s, leftover rolls over); after an OPTIMAL tier add `expr_k == value_k`, after a FEASIBLE (time-limited) tier add
`expr_k <= value_k` and mark it FEASIBLE (the final status is then FEASIBLE, never OPTIMAL); an INFEASIBLE later tier (cannot happen with the hint) aborts with the previous incumbent.
`gapTolerance` applies only to the LAST tier (or to the weighted total) so earlier tiers keep their lexicographic guarantee. Tier values are read with `solver.value(expr_k)` and returned in
`Metrics.tiers` (+ `bound` for the sequential mode; `null` in weighted multi-tier mode, where only the combined bound exists, reported in `SearchStats.bestBound`).
No objective at all (no terms): no `minimize`; CP-SAT returns OPTIMAL once feasible, which is reported as `OPTIMAL`.
Decimal inputs: `ScaledMath.scale(x, scale)` = `Math.round(x * scale)` (half-up), `scale` = `objective.scale ?? options.objectiveScale`; max per term is verified with
`Math.multiplyExact` and BigInteger sums. The int64 range of CP-SAT is therefore never approached (<= 2^53).

### 6.9 Relaxation in one solve (D-8)

```
relaxable = constraints with enforcement RELAXABLE_WITH_AUTHORIZATION, relaxation.authorized, maxRelaxation > 0,
            and (options.relaxationLevels empty or contains id)          // anything else is treated as HARD (v1 semantics)
for k in relaxable:   active_k = newBoolVar();  hint active_k = 1
                      Enforcer(k).mode() = RELAXABLE;  Enforcer(k).instance(*) = active_k      // one shared literal for all instances
                      sink.add(tier = -2, cost_k * (1 - active_k)), cost_k = 1000 * (1 + relaxation.priority)    // lower priority number = cheaper to drop (v1 order)
```
`priority <= 1000` (validated) so `cost_k <= 1 001 000`. Tier -2 is optimised before everything else, so a constraint is dropped only when the problem
cannot be solved otherwise, and the cheapest set (smallest summed cost) is dropped. If there is no relaxable constraint nothing is reified (zero overhead).
After the solve: for each `active_k == 0`, the family `checker()` is run on the solution; a `Relaxation` is reported ONLY if it finds >= 1 violation
(`violationCount`, `violatedTaskIds`, `measure`, `impactOnScore = measure`), so "relaxed but harmless" constraints do not pollute the result.
The whole-model rebuild loop and the cumulative dropping of v1 are gone.

### 6.10 Infeasibility explanation (D-8)

Phase A - exact pre-solve analysis (`PreSolveAnalyzer`, engine neutral, runs on the `CandidateIndex` before any solver; also protects Timefold):
(1) required task without candidate for a requirement (`NO_ELIGIBLE_RESOURCE`, listing the constraint ids/reasons that pruned its candidates: `SKILL`, `CAPACITY_AMOUNT`, `CALENDAR`, `WINDOW`, `PIN`, `ELIGIBILITY:<id>`),
(2) `|candidates(requirement)| < count` (`REQUIREMENT_COUNT`), (3) `estStart > lstStart` (`WINDOW_EMPTY`), (4) critical path: CPM earliest end of a required task > its latest end, with the edge path back-traced (`CRITICAL_PATH_TOO_LONG`, `EDGE` items),
(5) single-resource energy: forced tasks of a resource with total duration > available ticks of its runs in their joint window (`CAPACITY_EXCEEDED`), (6) a locked/pinned assignment that is not a candidate (`LOCKED_CONFLICT`, `PIN_CONFLICT`).
Any finding returns `INFEASIBLE` with `method=PRESOLVE_ANALYSIS`, `minimal=true` per finding, without calling the solver.

Phase B - solver says INFEASIBLE and `explainInfeasibility`:

```
build again with BuildMode.EXPLAIN (same builder; soft constraints and objectives omitted; relaxation literals left free; numWorkers = 1; time = explainTimeSeconds):
   assumption groups (one literal each, added with model.addAssumptions):
     CONSTRAINT : every non-SOFT catalogue constraint  -> its Enforcer returns the assumption literal (so contributors need no change)
     TASK       : window/pin/lock facts of a task (start-domain restrictions emitted as enforced linear domains in explain mode)
     RESOURCE   : calendar / maxLoadTicks facts of a resource
     EDGE       : precedence edge (or per successor task when E > 5000)
   Facts that cannot be conditioned (NoOverlap, Cumulative, Circuit, requirement counts) stay unconditional and are never blamed.
solve -> INFEASIBLE -> core = solver.sufficientAssumptionsForInfeasibility()  (indices into the assumption list)
shrink (deletion filter, only if time remains): for each item i of the core (max 30): re-solve with assumptions core \ {i} under a short limit;
        still INFEASIBLE -> drop i (and replace the core by the returned subset); keep a flag minimal=true only if the loop completed
```
If the solver returns an empty core (assumption support depends on the single-worker/presolve combination of the OR-Tools build; the test T-EXPLAIN verifies it on 9.15),
the result falls back to `method=NONE`, `minimal=false` and the flat list of all non-soft constraint ids (the v1 behaviour, now explicitly labelled). The explain pass is extra time on top of
`maxTimeSeconds` bounded by `explainTimeSeconds` (<= 10 s default), inside the stale-job grace of `app.plan.execution.stale-job-grace-seconds` (240 s).

### 6.11 Hints, locked assignments, callback, progress

* Hints (`initialSolution` with `locked=false`; tier-to-tier incumbents; `active_k = 1`): `model.addHint(s_t, tick)`, `addHint(x_c, 0|1)`, `addHint(p_t, 1)`; parameters
  `repair_hint = true`, `hint_conflict_limit = 10`. Hints are advisory: invalid hints are ignored by the solver, but the validator reports `HINT_OUT_OF_DOMAIN` warnings for hints outside the pruned domains.
* Locked (`locked=true`): fixed variables (6.4). A locked assignment that violates a hard rule yields `INFEASIBLE` with `LOCKED_CONFLICT` items (pre-solve) or a `LOCKED` assumption in the core.
* `CpSolverSolutionCallback` subclass `ProgressCallback`: on each solution increments the counter, records the time to the first solution, reads `objectiveValue()` and
  `bestObjectiveBound()`, computes `gap = |obj - bound| / max(1, |obj|)`, and calls `listener.onProgress` at most once per second. If `listener.wantsIncumbents()` it also builds an
  `IncumbentSnapshot` (O(T + selected candidates)) at most once per 2 s. The final solution is read from the `CpSolver` after `solve` (the response keeps the best solution), so the callback never copies per-solution arrays by default.
* Cancellation: the existing `SolverCancellationToken.onCancellation(solver::stopSearch)` registration is kept. After `solve` returns with a solution and the token was cancelled, the status is `STOPPED_FEASIBLE`
  (if `keepIncumbentOnStop`), with no solution it is `UNKNOWN`; a cancellation during model construction still throws `SolveCancelledException` -> `UNKNOWN` as in v1.

### 6.12 Deterministic mode

`solverOptions.deterministic = true` sets: `random_seed = seed`; if `numWorkers == 1` single worker; if `numWorkers > 1`: `interleave_search = true` (reproducible multi-worker emulation, slower than the portfolio) with `num_workers` unchanged;
`max_deterministic_time = maxTimeSeconds * app.plan.cpsat.dtime-per-second` (default 1.0) AND a wall-clock `max_time_in_seconds` safety net (results are reproducible when the search ends by optimality or dtime;
`details.deterministicBroken=true` is added when the wall clock cut the search). Model construction is deterministic by design (request order, arrays, no hash iteration; `LinkedHashMap` only). Result ordering: 8.3.
Non-deterministic default: seed still passed, portfolio search, `num_workers = min(numWorkers, limits.maxWorkers)`.
Other parameters: `max_time_in_seconds` (remaining after build), `num_workers` (`setNumWorkers`; `setNumSearchWorkers` on older wrappers), `log_search_progress = logSearch`,
`relative_gap_limit = gapTolerance`, `symmetry_level` and presolve at defaults.

### 6.13 Guards, memory and complexity

Size guards: `ModelCost` from the families + structural estimate is checked BEFORE building (7.3); `ctx.count(..)` re-checks the actual counts during the build against `maxCandidates`,
`maxIntervals`, `maxPairwise`, `maxPrecedences`, `maxSetupArcs`, `maxVariables`, `maxConstraintInstances` and throws `LimitExceededException(MODEL_TOO_LARGE)`.
Complexity (build): index O(T + R/64 * distinctSelectors + C + E + runs), structural model O(C + E), families O(sum of their `ModelCost`), tiers O(terms): near-linear in candidates; the only super-linear pieces
are the explicit pairwise/circuit families, each with its own cap. Memory: arrays are primitive; no strings per candidate; the CP-SAT proto plus the native presolve copy cost roughly 1 KB per candidate
(guideline: 500 000 candidates ~ 500 MB native memory beyond the JVM heap; document in the Docker/compose resource sizing and keep `MaxRAMPercentage=60`).
Removed v1 costs: O(T*R*S) booleans, string-keyed maps in hot loops, O(T^2*R*S) pruning, per-attempt rebuilds.

### 6.14 Solution extraction

```
for t in selected tasks (p_t true):  start = value(s_t); for each requirement q: resources = [ resourceOf(c) for c in K_q with value(x_c) ] ;  endTick = start + candDuration(chosen c) ;
    slotId = slot-mode ? slot whose start == start : null ; amount = candAmount(c)
assignments sorted by (startTick, taskId) ; unassignedTasks = unselected optional tasks with reason (8.4)
```
Then the validator (8) re-evaluates everything independently; the engine reports `Metrics`, `TierValue`s and the status mapping:
`OPTIMAL` -> OPTIMAL (all tiers OPTIMAL), `FEASIBLE` -> FEASIBLE, `INFEASIBLE` -> explanation phase then INFEASIBLE, `MODEL_INVALID` -> `INVALID_SOLVER_MODEL` (unchanged error), `UNKNOWN` -> UNKNOWN.

---

## 7. Validation

Three stages, all collecting issues (never failing on the first problem when called from `POST /validate`):

| Stage | Class (WP) | Needs | Output |
|-------|------------|-------|--------|
| 1. structural | `PlanningRequestValidator` (WP2) | request only | `ValidationResult` (issues with code, severity ERROR/WARNING, JSON path) |
| 2. normalize | `DomainNormalizer` (WP2) | stage 1 passed | `PlanningProblem` |
| 3. semantic + capability + cost | `ProblemVerifier` (WP2), uses families (WP4) and `EngineCapabilities` (WP3/WP5) | problem, registries, engine | issues; then `CandidateIndexBuilder` + limits |

`PlanningRequestValidator.validate(request)` keeps its name and "throw on error" behaviour for the submit path: it throws the new
`RequestValidationException extends InvalidRequestException` (`code()` = code of the FIRST error, `issues()` = all) so
`PlanExceptionHandler` and the job `errorCode` keep working; `PlanningRequestValidator.collect(request)` returns the full
`ValidationResult` for `/validate`. `ValidationReportDto.errors` lists errors; warnings go to a new `warnings` list.

### 7.1 Structural checks (stage 1)

* meta: `version == "2.0"`, `clientRef` length, `timezone` valid.
* horizon: present, `end > start`, `tickSeconds | 86400`, `(end-start) % tickSeconds == 0`, `horizonTicks <= limits.maxHorizonTicks`.
* ids: tasks, resources, slots, calendars, setup matrices, constraints, objectives each unique and non-blank (`DUPLICATE_ID`, path `tasks[7].id`); type ids against `domain` when given.
* references (`UNKNOWN_REFERENCE`): `Task.predecessors[].taskId`, `startSlots`, `Task.pin.resourceIds`, `Task.setupClass` (must exist in the matrix of at least one candidate resource), `resourceDurations` keys, `ResourceSelector.resourceIds/excludeResourceIds`, `Resource.calendarId/setupMatrixId`, `Preference.resourceId`, `initialSolution` ids, `Constraint.scope`, constraint params ids.
* units and ranges: `duration >= 0` and `<= horizonTicks` (`INVALID_DURATION`); null duration only if `startSlots` non-empty with equal slot lengths (`DURATION_REQUIRED`); `demand >= 1`; `capacity >= 1`; requirement `count >= 1`, `amount >= 1`, unique roles, `<= maxRequirementsPerTask`; `resourceDurations` only when the task has exactly one requirement of count 1; `prize >= 0`; `costPerTick/costPerUse >= 0` finite; lags within `[-H, H]`; `maxLag >= minLag`; no self-edge; `Preference.penalty >= 0`.
* time points: aligned and inside the horizon (`TIME_NOT_ALIGNED`, `TIME_OUT_OF_HORIZON`); `release + minDuration <= deadline` (`WINDOW_IMPOSSIBLE`, error); `dueDate` inside horizon (warning if before `release + duration`).
* **precedence cycles**: Kahn topological sort over `Task.predecessors` (O(T+E)); on failure `PRECEDENCE_CYCLE` with the cycle path `A -> B -> C -> A` in the message (found by walking the residual graph); `PRECEDENCE_SELF_REFERENCE`.
* **calendars**: `windows[].start < end`; `recurring.from/to` valid; `validFrom <= validTo`; zone valid; warning `CALENDAR_OVERLAP` for overlapping explicit windows (they are merged), `CALENDAR_ROUNDED` when tick rounding moved a bound, `CALENDAR_EMPTY` when a resource has no availability inside the horizon (all its candidates will be pruned), `OUT_OF_HORIZON_CLIPPED`.
* setup matrices: classes referenced by tasks exist in some matrix; `ticks >= 0`; `SETUP_RESOURCE_NOT_UNARY` (resource with matrix and capacity > 1).
* size limits (cheap, first): `maxTasks`, `maxResources`, `maxSlots`, `maxConstraints`, `maxPrecedences`, `maxTiers`, request bytes.

### 7.2 Semantic and capability validation (stage 3, `ProblemVerifier`)

```
for each constraint:  family = constraintFamilyRegistry.get(type)            (UnknownConstraintException when absent)
                      params = ParamsBinder.bind(constraint.params, family.paramsType())      PARAMS_INVALID
                      enforcement in family.supportedEnforcements()                            INVALID_ENFORCEMENT
                      scope.kind compatible with family.scopeKind()                           INVALID_SCOPE
                      SOFT: weight >= 1 and tier in [0,7]  (INVALID_WEIGHT / INVALID_TIER);  RELAXABLE: relaxation != null, priority <= 1000
                      family.validate(ctx, constraint, params)
for each objective:   family lookup, params bind, tier in [0,7], weight >= 0, scale in [1, 1e6], family.validate
capability:           required = FeatureScanner.scan(problem)   // table below
                      miss = required - engine.capabilities().features  -> ENGINE_UNSUPPORTED_FEATURE (one issue per feature, path of the first offender)
                      each constraint type not in engine.constraints -> ENGINE_UNSUPPORTED_CONSTRAINT ; enforcement not supported -> ENGINE_UNSUPPORTED_ENFORCEMENT
                      each objective type not in engine.objectives -> ENGINE_UNSUPPORTED_OBJECTIVE
                      timefold extra: slots pairwise disjoint (OVERLAPPING_SLOTS)
```

`FeatureScanner` rules: `FREE_START` = any task with empty `startSlots`; `SLOT_MODE` = any non-empty; `RESOURCE_DURATIONS` = any non-empty `resourceDurations`; `CUMULATIVE_DEMAND` = any resource capacity > 1 or amount > 1; `OPTIONAL_TASKS` = any `required=false`;
`PRECEDENCES` = any `Task.predecessors` or a `precedence` constraint; `TIME_WINDOWS` = any release/deadline/`time_window`; `CALENDARS` = any `calendarId`/`unavailable`; `SETUPS` = any `setupMatrixId`; `MULTI_RESOURCE` = any requirement count > 1 or > 1 requirement or `multi_resource_requirement`;
`PINS` = any `pin` / `fixed_assignment`; `INITIAL_SOLUTION` = `initialSolution` non-empty; `LOCKED` = any `locked=true`; `LEXICOGRAPHIC` = more than one distinct tier among objectives and soft constraints; `RELAXATION` = any authorized RELAXABLE constraint;
`COST_PER_USE` = any `costPerUse > 0`; `MAX_LOAD_TICKS` = any `maxLoadTicks`; `DETERMINISTIC` = `deterministic=true`; `EXPLAIN_ASSUMPTIONS` is informational (warning for Timefold, not an error).
Because the scan is feature-based, a Timefold request that merely LISTS a v2 field with a default value (empty predecessors) is accepted; any real use is rejected with the exact path. The result of `POST /validate` carries the compatibility of the request with EVERY engine (9.1).

### 7.3 Cost estimation and limits

`PlanLimitsProperties` (prefix `app.plan.limits`, Phase 1 behaviour kept; new/changed keys):

| Key | Default | Meaning |
|-----|---------|---------|
| `max-tasks`, `max-resources`, `max-slots`, `max-constraints`, `max-request-bytes`, `max-time-seconds`, `max-workers`, `max-concurrent-solves`, `solve-queue-timeout-seconds`, `timefold-unimproved-seconds` | unchanged | unchanged |
| `max-horizon-ticks` (NEW) | 2 000 000 | ticks of the horizon (hard cap 2^30) |
| `max-candidates` (NEW) | 500 000 | surviving (task, requirement, resource) candidates; replaces the meaning of v1 `max-variables` as the main size guard |
| `max-variables` (CHANGED) | 2 000 000 | estimated Bool + Int variables of the whole model (candidates, optional presence, soft/relax literals, family helpers) |
| `max-intervals` (NEW) | 1 000 000 | estimated interval variables (candidate + master + fixed blocks) |
| `max-precedences` (NEW) | 500 000 | structural edges + expanded `precedence` pairs |
| `max-setup-arcs` (NEW) | 300 000 | circuit arcs after window pruning |
| `max-pairwise` (NEW) | 200 000 | pair literals of pairwise fallbacks/soft families |
| `max-requirements-per-task` (NEW) | 8 | |
| `max-tiers` (NEW) | 8 | distinct tiers |
| `max-calendar-runs` (NEW) | 100 000 | expanded availability runs of the whole request |
| `max-constraint-instances` (NEW) | 2 000 000 | estimated CP-SAT constraints |
| `app.plan.cpsat.max-start-choices` / `named-variables` / `symmetry-breaking` / `dtime-per-second` (NEW, `CpSatProperties`) | 256 / false / true / 1.0 | CP-SAT tuning, owned by WP3 |

Estimation (before any model is built), `ModelCostEstimator` (WP2) in this order so no oversized allocation ever happens:
1. `T, R, S, E` and requirement counts are known from the request (O(n)); cheap limits first.
2. Candidate upper bound = `sum over tasks and requirements of |selector bitset|` (cached per distinct selector, O(T)); if it exceeds `max-candidates * 4` fail fast; the index builder counts real candidates while enumerating and aborts when `max-candidates` is exceeded (`LimitExceededException`).
3. `ModelCost = structural + sum(family.estimate) + sum(objective.estimate)`; structural: `boolVars ~ C + optional T`, `intVars ~ T`, `intervals ~ C(resources with >= 2 cands) + masters`, `constraints ~ C + E + T`, `circuitArcs`, `pairwise` from families.
4. Compare each component with its limit; the error names the limit key and the actual value (existing `LimitExceededException` format) and `GET /validate` reports `estimate` (candidates, intervals, variables, constraints) in the report for UI feedback.

### 7.4 New error codes (existing `InvalidRequestException.code()` UPPER_SNAKE convention; i18n key = `plan.error.` + code lower-cased with dots)

Keys are added to every bundle `i18n/plan/messages*.properties` of the host (English text first; the UI bundle `04-plan-starter-ui` gets the UI-facing subset).

| Code | i18n key | Stage | Meaning |
|------|----------|-------|---------|
| `UNSUPPORTED_SCHEMA_VERSION` | `plan.error.unsupported.schema.version` | 1 | meta.version != 2.0 |
| `INVALID_TIMEZONE` | `plan.error.invalid.timezone` | 1 | |
| `INVALID_HORIZON` | `plan.error.invalid.horizon` | 1 | missing, end <= start, bad tickSeconds, not a multiple |
| `HORIZON_TOO_LARGE` | `plan.error.horizon.too.large` | 1 | > max-horizon-ticks |
| `TIME_NOT_ALIGNED` | `plan.error.time.not.aligned` | 1/3 | instant not on a tick boundary |
| `TIME_OUT_OF_HORIZON` | `plan.error.time.out.of.horizon` | 1/3 | |
| `DUPLICATE_ID` | `plan.error.duplicate.id` | 1 | |
| `MISSING_REQUIRED_FIELD` | `plan.error.missing.required.field` | 1 | |
| `UNKNOWN_REFERENCE` | `plan.error.unknown.reference` | 1 | |
| `INVALID_DURATION` / `DURATION_REQUIRED` | `plan.error.invalid.duration` / `plan.error.duration.required` | 1 | |
| `INVALID_CAPACITY` / `INVALID_DEMAND` | `plan.error.invalid.capacity` / `plan.error.invalid.demand` | 1 | replaces v1 `INVALID_RESOURCE_CAPACITY` |
| `INVALID_REQUIREMENT` | `plan.error.invalid.requirement` | 1 | count, role, amount, resourceDurations shape |
| `INVALID_LAG` | `plan.error.invalid.lag` | 1 | |
| `PRECEDENCE_CYCLE` / `PRECEDENCE_SELF_REFERENCE` | `plan.error.precedence.cycle` / `plan.error.precedence.self.reference` | 1 | cycle path in the message |
| `CALENDAR_INVALID` | `plan.error.calendar.invalid` | 1 | |
| `CALENDAR_OVERLAP`, `CALENDAR_ROUNDED`, `CALENDAR_EMPTY`, `OUT_OF_HORIZON_CLIPPED` | `plan.warning.calendar.overlap` ... | 1 | WARNINGS (key prefix `plan.warning.`) |
| `WINDOW_IMPOSSIBLE` | `plan.error.window.impossible` | 1 | release + duration > deadline |
| `SETUP_MATRIX_INVALID` / `SETUP_RESOURCE_NOT_UNARY` / `SETUP_TOO_LARGE` | `plan.error.setup.matrix.invalid` ... | 1/3 | |
| `PARAMS_INVALID` | `plan.error.params.invalid` | 3 | typed binding failed (unknown/missing property) |
| `INVALID_SCOPE` / `INVALID_ENFORCEMENT` / `INVALID_TIER` / `INVALID_WEIGHT` | `plan.error.invalid.scope` ... | 3 | |
| `MIN_REST_RESOURCE_NOT_UNARY` | `plan.error.min.rest.resource.not.unary` | 3 | |
| `ENGINE_UNSUPPORTED_FEATURE` / `_CONSTRAINT` / `_ENFORCEMENT` / `_OBJECTIVE` | `plan.error.engine.unsupported.feature` ... | 3 | args: feature/type, engine |
| `OVERLAPPING_SLOTS` | `plan.error.overlapping.slots` | 3 | Timefold needs disjoint slots |
| `PAIRWISE_TOO_LARGE` / `MODEL_TOO_LARGE` | `plan.error.pairwise.too.large` / `plan.error.model.too.large` | 3/build | |
| `INVALID_INITIAL_SOLUTION` | `plan.error.invalid.initial.solution` | 1/3 | unknown task/resource, locked outside domain; `HINT_OUT_OF_DOMAIN` is a warning |
| existing, unchanged | | | `LIMIT_EXCEEDED`, `OBJECTIVE_SCORE_OUT_OF_RANGE`, `INVALID_SOLVER_MODEL`, `SOLVER_BUSY`, `UNKNOWN_SOLVER`, `UNKNOWN_CONSTRAINT`, `UNKNOWN_OBJECTIVE` |

`ValidationReportDto.Issue` gains `severity` and `args` (map for i18n placeholders), see 9.1.

---

## 8. Results post-processing

### 8.1 Where it runs

`PlanningService.solve`: after `engine.solve(...)` returns a result with a solution, `PlanningSolutionValidator.validate(compiledProblem, result)` runs (existing hook), then returns an enriched `SolveResult` (it no longer only verifies: it
fills `softViolations`, `hardViolations` (for relaxations), `qualityDetails`, and the `unassignedTasks` reasons). Failure of verification throws the existing `InvalidSolverResultException` with a code (`SOLUTION_INVALID_*`) naming the family and task.

### 8.2 Independent validator (generic over families)

1. Build `SolutionView` once (int arrays from the result: selected flags, start/end ticks, resource lists, per-resource task lists sorted by start). No solver object is touched.
2. **Structural checker** (`StructuralSolutionChecker`, O(C log C)): every required task present exactly once; optional tasks selected or unassigned; each requirement fulfilled with `count` distinct eligible resources, `amount <= capacity`; start/end inside horizon, `release <= start`, `end <= deadline` (dueDate not hard), `end = start + duration of the chosen resource`; start in the task's slot starts (when slot mode) and `end <= slot.end`; start/end inside ONE availability run of every used resource; pins; unary resources: sorted sweep, consecutive intervals must satisfy `end_i + setup(i, j) <= start_j`; cumulative: event sweep, load never above capacity (plus blocks of `resource_capacity` constraints, via their checkers); `Task.predecessors` edges (type, minLag, maxLag, presence); `maxLoadTicks`; locked assignments equal to the lock.
3. **Catalogue families** (generic): for each `Constraint` (all enforcements) run `family.checker().check(ctx, constraint, params, sink)`:
   * HARD constraint (and RELAXABLE not listed in `result.relaxations`): any violation => `InvalidSolverResultException` (the engine produced an invalid plan).
   * RELAXABLE listed in `relaxations`: violations are allowed and reported there (and as `hardViolations`).
   * SOFT: violations become `softViolations` (instance level, `impact = weight x measure`) and add `weight x measure` to the constraint tier.
4. **Objectives**: every `ObjectiveFamily.evaluator()` computes its exact term; `ObjectiveScore` per objective (with `score = value/scale` for decimal objectives); per tier the validator computes `sum weight x term + soft penalties` and compares with the engine's `Metrics.tiers[k].value` for tiers >= -1 (exact integers, zero tolerance; tier -2 is not verifiable because harmless dropped constraints still cost). Mismatch => `InvalidSolverResultException(TIER_VALUE_MISMATCH)`: this is the safety net that keeps every contributor honest and is the main acceptance test of each family (the "model value == checker value" property test, section 10).
5. `qualityScore` is recomputed (3). Timefold results skip the tier comparison (no tiers) but not 2-4.

Adding a family means writing its checker once; the validator itself never changes.

### 8.3 Determinism and ordering of results

* Assignments: ascending `(startTick, taskId)`; `Assignment.resources`: requirement order, then resource id; `unassignedTasks`: by task id; `softViolations`/`hardViolations`: `(constraintId, taskId, resourceId)`; `relaxations`: constraint id; `qualityDetails`: objective request order; `core.items`: kind (`CONSTRAINT`, `TASK`, `RESOURCE`, `EDGE`) then id; `Metrics.tiers`: ascending tier.
* Property order in JSON follows record component order (Jackson default for records); the same `ObjectMapper` configuration (no unordered `Map` at the top level of results; `details` is a `LinkedHashMap`).
* Two runs of a request with `deterministic=true`, the same engine, the same OR-Tools version and a solve that ends by optimality (or dtime) return identical solutions; `Metrics` time fields are excluded from comparisons (`ResultComparator` in the test sources).
* Non-deterministic mode makes no promise about WHICH optimal plan is returned, only that the objective values are optimal.

### 8.4 Reasons for unselected optional tasks (`UnselectedReasoner`, O(T + E))

In order: `NO_ELIGIBLE_RESOURCE` (no candidate in the index, with the pruning constraint ids in the suggestion), `OUTSIDE_WINDOW` (`estStart > lstStart`), `PREDECESSOR_NOT_SELECTED` (an `IMPLIES` predecessor unselected),
else `NOT_SELECTED` ("not selected by the optimiser: the capacity or time it needs was worth more elsewhere") with `forgonePrize = effective prize`.

---

## 9. Effects on other modules

### 9.1 API / DTO consequences (`02-plan-shared`, `PlanJobService`, controller)

`CapabilitiesDto` becomes a per-engine matrix (the old fields stay as the UNION view for simple clients):

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CapabilitiesDto {
    private List<SolverCapability> solvers;            // + per-engine details below
    private List<ConstraintCapability> constraints;    // union of known types
    private List<ObjectiveCapability> objectives;
    private String schemaVersion;                       // "2.0"
    private LimitsView limits;                          // effective app.plan.limits (UI can pre-check sizes)

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SolverCapability {
        private String id;  private String label;  private boolean available;
        private boolean provesOptimality;
        private List<String> features;                  // supported EngineCapabilities.Feature names
        private List<String> unsupportedFeatures;       // everything else
        private List<String> unsupportedConstraintTypes;
        private List<String> unsupportedObjectiveTypes;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ConstraintCapability {
        private String type;  private String description;  private List<String> paramsHint;  private String scopeKind;
        private Map<String, EngineSupport> engines;     // engineId -> support
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EngineSupport { private boolean hardCapable; private boolean softCapable; private boolean relaxable; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ObjectiveCapability {
        private String type;  private String description;  private List<String> params;  private boolean reward;
        private List<String> engines;                   // engines supporting it
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LimitsView { private int maxTasks, maxResources, maxSlots, maxConstraints, maxHorizonTicks, maxTimeSeconds, maxWorkers; private long maxCandidates, maxRequestBytes; }
}
```
Example fragment: `{"type":"min_rest","engines":{"cp_sat":{"hardCapable":true,"softCapable":true,"relaxable":true},"timefold":{"hardCapable":false,"softCapable":false,"relaxable":false}}}`.
`PlanJobService.capabilities()` is rewritten to read `EngineCapabilities` of every engine in `SolverRegistry` and the family registries (WP7); the hard-coded `constraintDescription/constraintParams/objectiveDescription/objectiveParams` switch statements are DELETED (descriptions come from `family.descriptionKey()`/`paramsHint()`).

`ValidationReportDto`:

```java
public class ValidationReportDto {
    private boolean valid;                               // no errors for the REQUESTED engine
    private PlanningRequest normalizedRequest;           // as before (null when stage 1 failed)
    private List<Issue> errors;  private List<Issue> warnings;
    private Map<String, Boolean> engineCompatible;       // engineId -> request accepted by that engine (capability stage)
    private Map<String, List<Issue>> engineIssues;       // engineId -> capability issues
    private ModelEstimateDto estimate;                   // candidates, intervals, variables, constraints, tiers, horizonTicks
    public static class Issue { private String code; private String severity; private String message; private String path; private Map<String,Object> args; }
}
```
`POST /validate` accepts an optional `?engine=` (default: the request's `solverOptions.engine`).

Export (`GET /{id}/result/export?format=csv&section=assignments|unassigned|violations`, default `assignments`; `PlanJobServiceApi`, controller, `PlanFeignService`, `PlanJobService.exportResult` get the `section` parameter):
* `assignments`: `taskId,optional,role,resourceId,amount,slotId,startTick,endTick,start,end` (one row per task x resource; start/end ISO instants).
* `unassigned`: `taskId,reasonCode,forgonePrize,reason`.
* `violations`: `kind,constraintId,taskId,resourceId,code,impact,message` (kind = SOFT | RELAXED).
The old header `taskId,resourceId,slotId,start,end` is replaced (no compatibility needed).

### 9.2 PlanJob.engine

`PlanJobService.submit` stores the CANONICAL engine id (`cp_sat` | `timefold`, alias-normalised by `SolverOptions`) in `PlanJob.engine` / `PlanJobMinDto.engine` (unchanged column and filter); `SolverRegistry.get` resolves aliases for old stored requests; the stop/cancel flow, `JobProcessor` state machine, `SolveConcurrencyGuard`, tenant scoping and Kafka messages are unchanged.
`JobProcessor` changes (WP7): builds `SolveContext` with a throttled progress listener (`LogEntry` level INFO, phase `PROGRESS`, message with objective/bound/gap), persists a `STOPPED_FEASIBLE` result (the job still ends `CANCELLED`; `PlanJob.resultStatus` = `STOPPED_FEASIBLE`), maps `UNKNOWN` as before. `IEnumSolverStatus.STR_ENUM_SIZE` becomes 20: verify every column definition derived from it (the `result_status` value passed to `jobRepository.completeSolving`).

### 9.3 UI impact list for Phase 4 (no UI code in Phase 3)

* `PlanJobRequestDialog`: replace the free-JSON/`attributes` editing by typed editors: Horizon (start, end, tick granularity), Task editor (duration, demand, priority, optional + prize, requirements grid (role, count, amount, selector builder with skills/types/ids), window release/deadline/due, start slots, pin, predecessors grid with type/lags, setup class, tags, preferences), Resource editor (capacity, skills, tags, calendar picker, unavailable intervals, costs, maxLoad, setup matrix), Calendar editor (windows, recurring rules with day chips, breaks, exceptions, DST-aware preview), Setup matrix editor (class grid), Slot editor (cost), Constraint editor driven by `GET /capabilities` (`paramsHint`, per-engine support, enforcement choices disabled when the selected engine does not support them), Objective editor with tiers (drag to reorder tiers), Solver options (deterministic, scale, gap, lexicographic mode, explain), initial solution picker ("replan from job X").
* Result views: Gantt (lanes = resources, bars = assignments with `startTick/endTick` and instants, calendar/unavailable shading from the expanded runs, locked/pinned markers, precedence arrows, optional-task styling), unassigned list with reason codes, soft violations and relaxations grids, objective breakdown per tier (`qualityDetails`, `Metrics.tiers`), convergence chart from the progress logs (objective, bound, gap), infeasibility core panel (items with `reasonCode`, link to the offending entity), validation report with warnings and per-engine compatibility.
* Data needs the Phase 3 API must provide (all already in 9.1/3): per-engine capability matrix, expanded availability runs per resource (a `CalendarExpansionService` method reused by a `GET /{id}/timeline` endpoint added in Phase 4: lanes + blocks derived from request+result, no solver work), the progress log phase, `estimate` in the validation report, localized issue messages by `code`/`args`.
* i18n: UI bundles get the constraint/objective description keys (`plan.constraint.<type>.description`, `plan.objective.<type>.description`) and all `plan.error.*`/`plan.warning.*` keys of 7.4.

### 9.4 README and docs updates (WP8)

`README.md`: rewrite "Supported planning semantics" (time model, tasks/resources/requirements, calendars, setups, constraint and objective catalogue with engine matrix, tiers, relaxation, engines and their guarantees, determinism, limits) and "Roadmap" (Phase 3 -> design done/implemented); keep Flow/Messaging/Schema untouched. `IMPLEMENTATION_PLAN.md`: add "Phase 3 - Engine v2" section with the wave status. `examples/README.md`: new golden table (9.5), smoke test learns `g1..g14` plus an engine switch (`-Engine timefold` runs only the Timefold-compatible subset G1, G3, G12 and the rejection checks). Delete obsolete v1 statements (per-slot capacity, `attributes.capacity`, 32-bit Timefold guard, cumulative relaxation order).

### 9.5 Golden scenarios (files in `examples/`, requests rewritten to IR 2.0; every file carries a placeholder `meta.clientRef`)

Expected outcomes are asserted by `EngineV2GoldenTest` (service level, no Spring) and by the smoke test (job level). `value` = `ObjectiveScore.rawValue` of the stated tier, `score` = human value.

| Id | File | Scenario | Engine(s) | Expected status | Expected key facts |
|----|------|----------|-----------|-----------------|--------------------|
| G1 | `g1-capacity-feasible.json` | 3 one-hour sessions, 1 lab with capacity 2, slots `slot-early` (cost 0) and `slot-late` (cost 1.0), `minimize_slot_cost` | cp_sat, timefold | OPTIMAL (timefold: FEASIBLE) | 2 sessions in early, 1 in late; tier-0 value 100 (score 1.0); job SUCCEEDED |
| G2 | `g2-infeasible-capability.json` | required task needs skill `welding`, no resource has it | cp_sat, timefold | INFEASIBLE | no solution; `core.method=PRESOLVE_ANALYSIS`, items `[TASK weld-job NO_ELIGIBLE_RESOURCE]`; job INFEASIBLE |
| G2b | `g2b-infeasible-core.json` | two required tasks A,B pinned both at tick 0 (Task.pin) on two machines + hard `no_overlap` constraint `no-overlap-ab` over tag `ab` | cp_sat | INFEASIBLE | `core.method` in {ASSUMPTIONS, ASSUMPTIONS_SHRUNK}; items contain `CONSTRAINT no-overlap-ab`, `TASK A`, `TASK B`; if the solver returns no core, test accepts `method=NONE, minimal=false` only when `T-EXPLAIN-FALLBACK` flag is set (documented risk R-5) |
| G3 | `g3-optional-priorities.json` | 1 required + 3 optional (priority 9/5/1), 1 lab capacity 1, 2 one-hour slots, `maximize_task_priority` | cp_sat, timefold | OPTIMAL (timefold FEASIBLE) | `mandatory-session` + `optional-high` assigned, medium/low unassigned (`NOT_SELECTED`); tier-0 score 9.0 |
| G4 | `g4-precedence-chain.json` | t1(3) -> t2(4) -> t3(5) on one machine, FS, lag 0 then minLag 2, `minimize_makespan` | cp_sat (timefold must reject: `ENGINE_UNSUPPORTED_FEATURE PRECEDENCES` + `FREE_START`) | OPTIMAL | starts 0, 3, 9; ends 3, 7, 14; makespan 14 |
| G5 | `g5-relaxation.json` | A,B (4 ticks each) on one machine; `time_window` latestEnd 6 for both as RELAXABLE: `deadline-A` priority 5, `deadline-B` priority 0; `minimize_makespan` | cp_sat | OPTIMAL | exactly ONE relaxation: `deadline-B`, `violationCount=1`, `measure=2`; makespan 8; `Metrics.tiers[-2].value = 1000`; `deadline-A` not relaxed; single model build (metrics `tiersSolved`) |
| G6 | `g6-cost-minimisation.json` | 3 tasks of 60 ticks; `cheap` (costPerTick 1.0, unavailable after tick 120) and `expensive` (costPerTick 3.0, always); `minimize_resource_cost`; horizon 240 | cp_sat | OPTIMAL | 2 tasks on cheap, 1 on expensive; tier-0 value 30000, score 300.0; same ratio with `objectiveScale=1000` (value 300000) |
| G7 | generated in code (seeded 30 jobs x 15 machines job-shop, `maxTimeSeconds=60`, `minimize_makespan`) | cancellation: token cancelled after 2 s | cp_sat | STOPPED_FEASIBLE (UNKNOWN allowed only if no incumbent after 2 s) | returns <= 5 s after the cancel; `Metrics.search.solutionsFound >= 1` when STOPPED_FEASIBLE; job-level: stop flow STOPPING -> CANCELLED with the result persisted |
| G8 | `g8-jobshop.json` (= 2.6 b) | 3 jobs x 2 operations, 2 machines | cp_sat | OPTIMAL | makespan 75; precedence respected; machines never overlap |
| G9 | `g9-roster.json` (= 2.6 c) | 5 pinned shifts, 3 nurses, min rest 11, max 8 ticks per day, skills, fairness | cp_sat | OPTIMAL | `minimize_max_load` tier-0 value 16; `d1-night` on `ana`; no soft violation |
| G10 | `g10-project.json` (= 2.6 d) | pool of capacity 2, 5 activities, precedence, deadline | cp_sat | OPTIMAL | makespan 11; E ends 11 <= deadline 12; pool load never above 2 |
| G11 | `g11-prize-collecting.json` (= 2.6 e) | 5 optional tasks, one technician, 480 ticks, auto objective | cp_sat | OPTIMAL | selected P1,P2,P5; tier -1 score 190; P3,P4 unassigned with `forgonePrize` 90/30 |
| G12 | `school-timetable.json` (test resource, = 2.6 a) | 4 lessons, 2 teachers, class no-overlap | cp_sat, timefold | OPTIMAL (timefold FEASIBLE) | 2 lessons Monday, 2 Tuesday; tier-0 value 200 (score 2.0) |
| G13 | G8 with `deterministic=true`, run twice (also with `numWorkers=4`) | determinism | cp_sat | OPTIMAL | identical assignments (JSON equal ignoring `Metrics`) |
| G14 | `g14-setup-times.json` | 3 tasks (A,A,B classes) on one unary machine, setup 5 between different classes, `minimize_makespan` (tier 0) + `minimize_setup` (tier 1) | cp_sat | OPTIMAL | makespan 35; setup tier value 5; A tasks adjacent |

Rejection tests (validation level, `engine=timefold`): G4, G8, G9, G10, G11 (FREE_START or PRECEDENCES/MULTI...), each asserting the exact `ENGINE_UNSUPPORTED_*` code and path, and `engineCompatible = {cp_sat:true, timefold:false}`.

---

## 10. Implementation plan

Rules for every agent: write sources only; DO NOT compile or run anything (no `mvn`/`gradle`, do not search for Maven). The owner runs the build. Test command to give the owner after each wave
(from `02-wams-core`): `mvn -pl 08-plan-core/02-plan-shared,08-plan-core/03-plan-starter-parent -am test`. Phase 0/1 classes not listed in an ownership table are NOT to be touched.
Abbreviations: `SH/` = `02-wams-core/08-plan-core/02-plan-shared/src/main/java/eu/isygoit/`, `S/` = `02-wams-core/08-plan-core/03-plan-starter-parent/src/main/java/eu/isygoit/`,
`ST/` = `.../03-plan-starter-parent/src/test/java/eu/isygoit/`, `SHT/` = `.../02-plan-shared/src/test/java/eu/isygoit/`, `EX/` = `02-wams-core/08-plan-core/examples/`,
`HOST/` = `02-wams-core/10-starter-test/80-plan-management/04-plan-service/src/main/resources/`.

### 10.1 Work packages and file ownership (disjoint)

**WP1 - Shared IR, response IR, SPI contract (Wave 1, blocks everything)**
Owns: `SH/dto/request/*` (new/changed: `Horizon, Slot, Calendar, TimeWindow, RecurringWindow, SetupMatrix, SetupEntry, ResourceRequirement, ResourceSelector, Pin, PrecedenceRef, Preference, InitialSolution, InitialAssignment, Task, Resource, Constraint, ConstraintScope, Objective, RelaxationPolicy, SolverOptions, PlanningMeta, PlanningRequest, PlanningProblem`; DELETE `TaskCandidates`), `SH/dto/request/params/*` (every record of 2.5), `SH/dto/response/*` (every record of section 3 plus `CapabilitiesDto`, `ValidationReportDto` of 9.1; `Metrics`, `Diagnostics`, ...), `SH/enums/IEnumSolverStatus` (+STOPPED_FEASIBLE, size 20), `SH/util/ParamsBinder`, `SH/util/ScaledMath`, `SH/exception/{TimeNotAlignedException, ObjectiveRangeException}`;
in the starter: the CONTRACT package `S/solver/spi/*` (all interfaces/records of 5.2: `ConstraintFamily, ScopeKind, ConstraintChecker, ViolationSink, ObjectiveFamily, ObjectiveEvaluator, ModelCost, ValidationContext, PreprocessContext, CheckContext, SolutionView, TaskRewriter, CandidateIndexView`), `S/solver/{SolverEngine, EngineCapabilities, SolveContext, SolveListener, SolveProgress, IncumbentSnapshot, CompiledProblem, CompiledConstraint, CompiledObjective}`, `S/solver/cpsat/{CpSatConstraintContributor, CpSatObjectiveContributor, CpSatStructuralModule, ObjectiveSink, Enforcer, EnforcementSupport, BucketGrid, SetupArc, AssumptionRef, BuildMode}`, `S/solver/timefold/{TimefoldConstraintContributor, TimefoldObjectiveContributor}`, `S/solver/registry/*` (4 registries of 5.7), `S/solver/SolverRegistry` (aliases); DELETE `S/solver/{ConstraintHandler, ObjectiveHandler, ConstraintRegistry, ObjectiveRegistry}.java`, `S/solver/handler/*`.
Acceptance: all records compile-by-reading against section 2/3 signatures (field names exactly as written: they are the JSON contract), compact constructors apply the listed defaults, all five JSON examples of 2.6 deserialize (`FAIL_ON_UNKNOWN_PROPERTIES`) and round-trip. Tests: `SHT/dto/request/PlanningRequestJsonTest` (5 examples), `HorizonTest` (ceil/floor/aligned/toInstant), `ScaledMathTest` (rounding, overflow), `ParamsBinderTest` (strict binding), `ConstraintDefaultsTest`, `SolverOptionsAliasTest`; `ST/solver/registry/RegistriesTest` (duplicate type fails).

**WP2 - Validation, normalizer, CandidateIndex, limits, PlanningService (Wave 2)**
Owns: `S/service/impl/planning/{PlanningRequestValidator, DomainNormalizer, PlanningService, ProblemVerifier, FeatureScanner, ValidationResult, ModelCostEstimator}`, `S/exception/RequestValidationException`, `S/config/PlanLimitsProperties`, `S/solver/index/*` (`CandidateIndex, CandidateIndexBuilder, CalendarExpander, PrecedenceGraph, SelectorEvaluator, StartDomainCalculator, SymmetryDetector, SetupTable`).
Acceptance: section 5.4 algorithm and 7 rules implemented; the checks of 7.1/7.2 each have a test; index build is O(T+R+C+E) (no nested T x T loops; selector cache); `PlanningService` exposes the contract of 10.3 and keeps the Phase 1 guard/limits/cancellation behaviours (`SolveConcurrencyGuard` untouched).
Tests: `ST/service/impl/planning/{PlanningRequestValidatorTest, PlanningRequestValidatorLimitsTest (rewritten), DomainNormalizerTest, FeatureScannerTest, ModelCostEstimatorTest}`, `ST/solver/index/{CandidateIndexBuilderTest, CalendarExpanderTest (DST gap/overlap, overnight windows, breaks), PrecedenceGraphTest (cycle path, CPM), SelectorEvaluatorTest, SymmetryDetectorTest}`.

**WP3 - CP-SAT core: engine, builder, context, tiers, enforcers, hints, callbacks (Wave 2)**
Owns: `S/solver/cpsat/{CpSatSolverEngine, CpModelBuilder, CpModelContext, BuiltModel, CpSatEnforcerFactory, TierObjectiveBuilder, SolveRunner, ProgressCallback, SolutionExtractor, CpSatParameters, CpSatProperties, PrecedenceEmitter, ResourceModelBuilder, HintApplier, AssumptionRegistry, SymmetryBreakingModule}`.
Acceptance: the build order of 6.1 with `CpSatStructuralModule` plug-ins (order ascending; WP4b contributes the setup module, WP3 the symmetry module), structural model of 6.4, Enforcer semantics of 5.5 (HARD/RELAXABLE/SOFT/EXPLAIN), relaxation of 6.9, tier modes AUTO/WEIGHTED/SEQUENTIAL of 6.8 with the 2^53 guards, hints/locks 6.11, deterministic parameters 6.12, size guards 6.13, status mapping incl. `STOPPED_FEASIBLE`, native loading seam and lazy loading preserved.
Tests: `ST/solver/cpsat/{CpSatLazyLoadingTest (adapted), CpModelBuilderStructureTest (requirements, intervals, calendars, slot choice, precedence all types, optional tasks), TierObjectiveBuilderTest (mode decision, overflow, multipliers), EnforcerTest (soft literal accounting), RelaxationSingleSolveTest (G5), DeterminismTest (G13), HintAndLockTest, CancellationTest (G7 harness with fake long model), SymmetryBreakingTest}`.

**WP4a - Constraint families batch A (Wave 2)** - families (neutral) + CP-SAT contributors + checkers
Owns: `S/solver/family/{TaskEligibilityFamily, ResourceCapacityFamily, NoOverlapFamily, PrecedenceFamily, TimeWindowFamily, ResourceAvailabilityFamily}`, `S/solver/cpsat/contrib/{TaskEligibilityContributor, ResourceCapacityContributor, NoOverlapContributor, PrecedenceContributor, TimeWindowContributor, ResourceAvailabilityContributor}`.
Acceptance: each type implements `validate/estimate/preprocess(HARD)/checker` and a contributor honouring the Universal contributor rule; hard + soft + relaxable (as declared in 4.3) each covered; the model value equals the checker value (property).
Tests: `ST/solver/family/<Family>Test` and `ST/solver/cpsat/contrib/<Contributor>Test` for each of the six (hard, soft unit counts, relaxable, explain-mode assumption presence).

**WP4b - Constraint families batch B + setup circuit (Wave 2)**
Owns: `S/solver/family/{MaxConsecutiveFamily, MinRestFamily, MaxLoadPerPeriodFamily, MultiResourceRequirementFamily (implements TaskRewriter), SameResourceFamily, DifferentResourceFamily, FixedAssignmentFamily, ForbiddenPairFamily, MaxTasksPerResourceFamily}`, `S/solver/cpsat/contrib/{...Contributor for each}`, `S/solver/cpsat/SetupCircuitModule` (6.6, `CpSatStructuralModule` order 700), `S/solver/cpsat/BucketGrids` (DAY/WEEK/TICKS tiling in `meta.timezone`, DST-aware; also used by checkers through `S/solver/spi/PeriodBuckets` static helper owned here).
Acceptance and tests as WP4a plus: roster scenario (G9), setups (G14), `startBucket` correctness with DST days (23/25-hour days), static-pair shortcut of `min_rest`.

**WP4c - Objectives (Wave 2)**
Owns: `S/solver/objective/{MinimizeMakespanObjective, MinimizeTotalTardinessObjective, MinimizeMaxTardinessObjective, MinimizeResourceCostObjective, MinimizeSlotCostObjective, MaximizeTaskPriorityObjective, MaximizeSelectedValueObjective, MinimizeMaxLoadObjective, BalanceLoadObjective, MinimizeIdleObjective, MinimizeSetupObjective, MinimizePreferencePenaltyObjective, MinimizePlanChangesObjective, AutoObjectives}` (each = `ObjectiveFamily` with its evaluator) and `S/solver/cpsat/objective/<Name>Contributor` for each.
Acceptance: term definitions of 4.4 with exact bounds passed to the sink, `ScaledMath` for decimals, evaluator equals contributor value; `AutoObjectives.inject` per 6.8.
Tests: `ST/solver/objective/<Name>Test` (evaluator on hand-built `SolutionView`), `ST/solver/cpsat/objective/ObjectivesCpSatTest` (G4 makespan 14, G6 value 30000 and scale 1000, G11 value 190, G14 setup 5, tardiness, fairness).

**WP5 - Timefold subset (Wave 2)**
Owns: `S/solver/timefold/{TimefoldSolverEngine, TimefoldPlan, TimefoldTask, PlanConstraintProvider, SlotOptions, SlotOptionsBuilder, TimefoldBuildContext, TimefoldCapabilities}` (DELETE `AssignmentKey`, `TimefoldTaskAssignment`), `S/solver/timefold/contrib/{TaskEligibilityTimefoldContributor, ResourceCapacityTimefoldContributor, TimeWindowTimefoldContributor, ResourceAvailabilityTimefoldContributor, FixedAssignmentTimefoldContributor, MinimizeSlotCostTimefoldContributor, MinimizeResourceCostTimefoldContributor, MaximizeTaskPriorityTimefoldContributor, MaximizeSelectedValueTimefoldContributor}`.
Acceptance: 4.5 matrix exactly (declared features/constraints/objectives; nothing silently ignored), `HardSoftLongScore`, no R x S map in entities, never reports OPTIMAL, INFEASIBLE only via the pre-solve analyzer, single move thread + seed when `deterministic`, cancellation and `timefold-unimproved-seconds` kept, progress listener.
Tests: `ST/solver/timefold/{TimefoldSubsetTest (G1, G3, G12 same objective as CP-SAT), SlotOptionsBuilderTest, TimefoldCapabilitiesTest}`.

**WP6 - Post-processing: validator, checkers plumbing, pre-solve analysis, explanation, reporting (Wave 2)**
Owns: `S/service/impl/planning/PlanningSolutionValidator`, `S/solver/check/{SolutionViewImpl, CheckContextImpl, StructuralSolutionChecker, ObjectiveAggregator, UnselectedReasoner, RelaxationReporter}`, `S/solver/diag/{PreSolveAnalyzer, CoreItems}`, `S/solver/cpsat/explain/{CpSatExplainer, DeletionFilter}`.
Acceptance: 8.2 algorithm (structural + families + objectives + tier comparison), 8.3 ordering, 8.4 reasons, 6.10 phases A and B with graceful fallback; `InvalidSolverResultException` codes `SOLUTION_INVALID_*`, `TIER_VALUE_MISMATCH`.
Tests: `ST/service/impl/planning/PlanningSolutionValidatorTest`, `ST/solver/check/{StructuralSolutionCheckerTest (sweep line, cumulative, setup gaps), UnselectedReasonerTest, RelaxationReporterTest}`, `ST/solver/diag/PreSolveAnalyzerTest (G2 + each finding)`, `ST/solver/cpsat/explain/CpSatExplainerTest (G2b, fallback path)`.

**WP7 - API glue, job processing, configuration, i18n (Wave 2, finishes in Wave 3 when the registries exist)**
Owns: `S/service/impl/{PlanJobService, JobProcessor}`, `S/controller/PlanJobController`, `SH/api/PlanJobServiceApi` (export `section`), `S/exception/handler/PlanExceptionHandler` (issues rendering), `S/config/PlanningEngineConfiguration` (registry/contributor/family beans, `CpSatProperties`, `PlanLimitsProperties` binding), `04-plan-starter-ui/.../remote/plan/PlanFeignService` (signature only), `HOST/i18n/plan/messages*.properties` (all `plan.error.*`, `plan.warning.*`, `plan.constraint.*.description`, `plan.objective.*.description` keys, English text in every locale file), central config `10-edyra/09-docker/config-files/plan-service.yml` (`app.plan.limits.*` new keys and `app.plan.cpsat.*`).
Acceptance: 9.1-9.2 (capabilities per engine from `EngineCapabilities`, validate report with per-engine compatibility, export sections, progress logging, `STOPPED_FEASIBLE` persistence, canonical engine id on the job), all hard-coded description switches deleted, Phase 1/2 flows unchanged.
Tests: `ST/service/impl/{PlanJobServiceTest (capabilities/validate/export), JobProcessorReliabilityTest (adapted: SolveContext, STOPPED_FEASIBLE)}`, `ST/controller/PlanJobControllerTest`, `ST/exception/handler/PlanExceptionHandlerTest`.

**WP8 - Examples, fixtures, integration tests, docs (8a Wave 2, 8b Wave 3)**
Owns: `EX/*` (golden JSON `g1..g14`, `README.md`, `smoke-test.ps1/.sh`), `ST/../src/test/resources/requests/*.json` (school-timetable rewritten), `ST/testsupport/{EngineTestFactory (hand-wires registries + all families/contributors/objectives), ScenarioFixtures (programmatic builders, G7 generator), ResultComparator}`, `ST/service/impl/planning/{EngineV2GoldenTest, SchoolTimetableRequestTest (rewritten), PlanningServiceTest (rewritten), PlanningServiceGuardTest (adapted), TimefoldRejectionTest, ModelCheckerConsistencyTest (randomised: every family and objective: model tier value == validator value, 200 seeds)}`, `README.md`, `IMPLEMENTATION_PLAN.md`.
8a (Wave 2, only needs this document): JSON files, README tables, smoke tests, fixtures. 8b (Wave 3, needs all code): the integration tests and docs finalisation.

### 10.2 Dependencies and waves

```
Wave 1 :  WP1                                     (contract: records, SPI, registries; nothing else may start coding before the record signatures exist)
Wave 2 :  WP2  WP3  WP4a  WP4b  WP4c  WP5  WP6  WP7  WP8a      all in parallel, coding against WP1 + the cross-WP contracts of 10.3
          [owner builds + runs tests; compile errors are fixed by the owning WP agent]
Wave 3 :  WP8b (integration + golden + consistency tests)  +  integration fixes (each WP fixes its own compile/test failures)  +  WP7 final wiring
Wave 4 :  stabilisation: run golden scenarios against the real service (smoke test), performance pass (section 6.13 sizing), README polish
```
Hard order: WP1 -> {WP2..WP8a}; WP8b after all of Wave 2. Soft dependencies resolved by 10.3 (WP3/WP4 use `CandidateIndex` fields and `CpModelContext` methods exactly as written in 5.4/5.5; WP6 uses `CpModelBuilder.build`; WP7 uses the registries and `PlanningService` contract). Recommended: 4 waves (the build checkpoint between Wave 2 and 3 is the key risk control: nothing has ever been compiled in this module).

### 10.3 Cross-WP contracts (code against these before the other side lands)

```java
// WP2
public final class CandidateIndexBuilder {
    public static CandidateIndex build(PlanningProblem problem, List<CompiledConstraint> constraints,
                                       PlanLimitsProperties limits, SolverCancellationToken token);
    public interface Mutable { /* declared in spi.PreprocessContext usage: pruning and start restriction */ }
}
public final class CalendarExpander {
    /** sorted merged half-open runs [lo0,hi0,lo1,hi1,...) in ticks inside [0,horizonTicks); warnings appended */
    public static int[] expand(Calendar calendar, Horizon horizon, ZoneId defaultZone, List<DiagnosticIssue> warnings);
    public static int[] subtract(int[] runs, int[] blocks);
    public static int[] intersect(int[] a, int[] b);
}
public final class PrecedenceGraph {
    public static int[] topologicalOrder(int taskCount, int[] edgePred, int[] edgeSucc);       // null when cyclic
    public static List<String> findCycle(List<Task> tasks);                                    // ids in cycle order
}
public final class SetupTable { public static SetupTable build(CandidateIndex index, PlanningProblem p); public int ticks(int resource, int classFrom, int classTo); }
public class DomainNormalizer { public PlanningProblem normalize(Long jobId, PlanningRequest request); }   // calls AutoObjectives.inject and TaskRewriter families
public class PlanningService {
    public SolveResult solve(Long jobId, PlanningRequest request, SolverCancellationToken token, SolveListener listener);  // old overloads delegate (listener NOOP)
    public CompiledProblem compile(Long jobId, PlanningRequest request, String engineOverride);                               // validate + normalize + verify + index + estimate
    public ValidationReportDto validate(PlanningRequest request, String engineOrNull);                                        // /validate (all issues, per-engine compatibility, estimate)
}
// WP3
public final class CpModelBuilder { public BuiltModel build(CompiledProblem problem, SolveContext context, BuildMode mode); }
public final class BuiltModel { public CpModel model(); public CpModelContext context(); public List<AssumptionRef> assumptions(); /* + tier expressions, soft instances, relax literals */ }
// WP4c
public final class AutoObjectives { public static List<Objective> inject(List<Task> tasks, List<Objective> objectives); }   // adds auto-selected-value (tier -1) when needed
// WP6
public final class PreSolveAnalyzer { public Optional<Diagnostics> analyze(CompiledProblem problem); }                       // present = INFEASIBLE core (6.10 A)
public class PlanningSolutionValidator { public SolveResult validateAndEnrich(CompiledProblem problem, SolveResult result); } // throws InvalidSolverResultException
public final class CpSatExplainer { public InfeasibilityCore explain(CompiledProblem problem, SolveContext context, CpModelBuilder builder); }
// WP1 (spi) additions referenced above
public interface TaskRewriter { Task rewrite(Task task, Constraint constraint, Object params); }            // implemented by MultiResourceRequirementFamily
public interface CpSatStructuralModule { String name(); int order(); void apply(CpModelContext ctx); }        // SetupCircuitModule(700), SymmetryBreakingModule(800)
public record AssumptionRef(Literal literal, String kind, String id, String reasonCode) {}
public enum BuildMode { SOLVE, EXPLAIN }
```
`CpModelContext` additions needed by the families (also listed in 5.5): `void blockResource(int r, int lo, int hi, int demand, Literal enforcedBy)`, `Literal and(Literal a, Literal b)` (cached product literal with definitional clauses),
`void registerSetupArcs(int r, List<SetupArc> arcs)`, `List<SetupArc> setupArcs(int r)` with `record SetupArc(Literal literal, int ticks)`.
`PlanningService.solve` order: `compile` (validate, normalize, verify, index) -> `PreSolveAnalyzer` (INFEASIBLE core -> existing `InfeasibleException`) -> `SolveConcurrencyGuard.acquire` -> `engine.solve` -> INFEASIBLE => `InfeasibleException(diagnostics)` (existing flow stores the job as INFEASIBLE) -> `PlanningSolutionValidator.validateAndEnrich`.

### 10.4 Test inventory (summary; golden expectations in 9.5)

Unit tests per WP as listed; integration (WP8b): `EngineV2GoldenTest` (G1-G14 on the engines of 9.5), `TimefoldRejectionTest`, `ModelCheckerConsistencyTest` (the central safety property: for 200 random small instances per family/objective, solve with CP-SAT `numWorkers=1`, then `PlanningSolutionValidator` recomputes constraints and tier values and they must be equal),
`DeterminismTest`, `CancellationTest`, smoke test at job level (`examples/smoke-test.*` for G1, G2, G3, G8, G9, G10, G11).
Performance guard tests (WP8b, tagged `slow`, not run by default): 2 000 tasks x 20 resources (40 000 candidates) builds in < 3 s and the model has `candidates + O(T)` booleans; 20 x 20 job-shop reaches a feasible solution in 10 s.

---

## 11. Risks and open questions

Risks (with mitigation):

| # | Risk | Mitigation |
|---|------|------------|
| R-1 | OR-Tools 9.15 Java API names/semantics used in the pseudocode (enforcement on `addLinearExpressionInDomain`, `newOptionalFixedSizeIntervalVar`, `addCircuit`, `setNumWorkers`, `sufficientAssumptionsForInfeasibility`, `LinearExpr.affine`, `clearHints`) may differ slightly. Nothing has ever been compiled in this module. | The first Wave 3 build is the checkpoint; each pseudocode line is a semantic contract, not a signature; fall back to equivalent constructs (a fixed BoolVar instead of `trueLiteral`, `LinearExpr.newBuilder()` instead of `affine`). Keep OR-Tools calls inside `cpsat` package. |
| R-2 | Constraints without enforcement support (`NoOverlap`, `Cumulative`, `Circuit`, possibly `at_most_one`) | The Universal contributor rule (pairwise fallback + `maxPairwise`), `AddLinear` instead of `AddAtMostOne` where enforcement is needed. |
| R-3 | v2.0 tasks cannot span calendar breaks (D-5): long jobs over nights/weekends need a calendar-free resource | Documented limitation; v2.1 `calendarMode=SPAN` with the crossed-gap literal formulation (each gap k adds `gapLen_k * [lambda_k < start < gapStart_k]`) guarded by `maxSpanPairs`. Recommended default now: model long tasks on a calendar-free "machine" and keep people as short tasks. |
| R-4 | Weighted tier mode can be slower than sequential for hard tiers | `lexicographicMode` option; AUTO prefers weighted only when it fits 2^53; benchmark in Wave 4 and tune the AUTO rule (e.g. force SEQUENTIAL when tier-0 has > 10 000 terms). |
| R-5 | The assumption core may be empty or non-minimal depending on solver configuration | Explicit fallback label (`method=NONE`, `minimal=false`), exact pre-solve analysis covers the common causes, deletion filter with a time cap, test `CpSatExplainerTest` pins the behaviour on 9.15. |
| R-6 | Native memory: ~1 KB/candidate (model + presolve copy) outside the JVM heap | Default `max-candidates 500 000`, container sizing note, `ctx.count` guard, no variable names, arrays instead of objects; document `MaxRAMPercentage` interplay. |
| R-7 | Maintaining the Timefold subset costs effort for little gain | It is frozen to the 4.5 matrix; no new family gets a Timefold contributor unless the owner asks; the rejection mechanism keeps users safe. Recommendation: keep for G1/G3/G12-style slot problems only. |
| R-8 | DST and time zone corner cases in recurring calendars and DAY/WEEK buckets (23/25 hour days) | Tick arithmetic only after expansion; tests with `Europe/Paris` transition days; weeks start Monday. |
| R-9 | Soft pairwise families (soft `min_rest`, `no_overlap`, `precedence`) are quadratic | `maxPairwise` guard, domain-based pair pruning, static-pair shortcut; error `PAIRWISE_TOO_LARGE` with the family named. |
| R-10 | `IEnumSolverStatus.STR_ENUM_SIZE` 10 -> 20 may require a column resize (Hibernate `ddl-auto: update` does not shrink/grow reliably for existing tables) | No client data: drop/recreate the schema in dev; before production introduce Flyway (Phase 5) with the 20-wide column. |
| R-11 | Symmetry detection by hashing can group tasks that differ in a way the model cares about | The signature includes everything the index knows; tasks referenced by id in ANY constraint/pin/preference/lock are excluded; `app.plan.cpsat.symmetry-breaking=false` kill switch; G13/consistency tests run with it on and off. |
| R-12 | Users pick a tick far too fine (seconds over a month) | `max-horizon-ticks`, `HORIZON_TOO_LARGE` message suggests the coarsest tick that divides all durations; `/validate` reports the suggestion (`gcd` of durations, lags, window bounds). |
| R-13 | `STOPPED_FEASIBLE` result on a `CANCELLED` job could surprise API clients that treat CANCELLED as "no result" | `GET /{id}/result` returns it with status `STOPPED_FEASIBLE`; documented in README; UI shows a banner. |

Open questions (recommended defaults are what the spec implements; none blocks the implementation):

| # | Question | Recommended default |
|---|----------|---------------------|
| Q-1 | Engine id spelling `cp_sat` vs `cp-sat` | Keep `cp_sat`; accept `cp-sat`/`cpsat` as aliases (D-10). |
| Q-2 | Should `CONFIGURABLE_HARD` have a distinct solving effect? | No: HARD, only a UI hint that the user may soften it. |
| Q-3 | Pro-rata period attribution (a night shift split across two days) instead of "period of the start" (D-14) | v2.1 option `attribution: START | PRORATA` on `MaxLoadPerPeriodParams`. |
| Q-4 | Earliness / signed lateness objective | v2.1 `minimize_total_earliness`; not in v2.0. |
| Q-5 | Preemption (task split over several windows) | Out of scope for v2; model long tasks as several chained tasks. |
| Q-6 | Tasks whose candidates use different calendars with SPAN semantics | Rejected in v2.1 SPAN with `TASK_SPAN_MIXED_CALENDARS`. |
| Q-7 | Publish a JSON Schema for IR 2.0 | Phase 4 (generate from the records, e.g. victools) so the UI editor can validate offline; not needed for Phase 3. |
| Q-8 | Should the validator treat an `OPTIMAL` claim with a nonzero `gapTolerance` as FEASIBLE? | Yes: with `gapTolerance > 0` the engine reports FEASIBLE unless the gap is 0 (implemented in `SolveRunner`). |
| Q-9 | Is `tier -1` (auto selection) acceptable as the default for prize collecting, or should selection be weighted with user objectives? | Tier -1 (selection first, then user objectives); users who want a trade-off add their own `maximize_selected_value` objective in the same tier as their cost objective. |
| Q-10 | Keep the Timefold INFEASIBLE-by-heuristic behaviour? | No: only the pre-solve analyzer can declare INFEASIBLE; otherwise UNKNOWN (Phase 1 mitigation made permanent). |

---

## Appendix A - decisions most likely to be vetoed (summary)

1. D-2/D-3: slots become start-anchor lists and capacity is per-interval (`Cumulative`/`NoOverlap`), not per slot id.
2. D-5: v2.0 calendars are "task inside one working window"; spanning tasks deferred.
3. D-9/D-12: lexicographic tiers via weighted (2^53-guarded) or sequential solves; decimals scaled by `objectiveScale`.
4. D-7: SPI = neutral `ConstraintFamily` (validate/preprocess/estimate/check) + per-engine contributors with the `Enforcer` abstraction; old handler SPI deleted.
5. D-13: setup times through per-resource circuits (quadratic arcs, capped).
