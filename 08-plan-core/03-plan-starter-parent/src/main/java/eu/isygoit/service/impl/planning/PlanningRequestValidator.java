package eu.isygoit.service.impl.planning;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Calendar;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.DomainDefinition;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningMeta;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.Preference;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.RecurringWindow;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.request.TypeDefinition;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.exception.RequestValidationException;
import eu.isygoit.solver.index.CalendarExpander;
import eu.isygoit.solver.index.PrecedenceGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 1 of the validation pipeline (design 7.1): structural, reference and unit checks that need the request only.
 * <p>
 * {@link #collect(PlanningRequest)} reports ALL issues (errors and warnings, with code, JSON path and i18n args) and is
 * what {@code POST /validate} uses; {@link #validate(PlanningRequest)} is the submit path and throws: a crossed size
 * limit raises {@link LimitExceededException} (Phase 1 behaviour: max-tasks, max-resources, max-slots, max-constraints,
 * max-request-bytes, max-time-seconds, max-variables, ...), every other error a {@link RequestValidationException}
 * whose {@code code()} is the code of the FIRST error and whose {@code issues()} holds all of them.
 * <p>
 * Cheap size limits run first and stop the validation, so nothing below ever loops over an oversized request. Checks that
 * need the constraint/objective families (params binding, scope kinds, enforcement, tiers) and the engine capability
 * checks belong to stage 3 ({@code ProblemVerifier}). Time points of tasks, slots and initial assignments MUST be tick
 * aligned and inside the horizon; calendar and unavailability bounds are rounded (warnings). {@code numWorkers} above
 * {@code max-workers} is NOT rejected: {@code PlanningService} clamps it.
 * <p>
 * Stateless apart from the limits; thread-safe.
 */
@Component
public class PlanningRequestValidator {

    /** Rough serialized size of one entity (JSON punctuation, field names) used by the request size estimate. */
    private static final int ENTITY_OVERHEAD_BYTES = 96;
    private static final int ATTRIBUTE_BYTES = 32;
    private static final String CODE_LIMIT = LimitExceededException.CODE;
    private static final String CODE_MISSING = "MISSING_REQUIRED_FIELD";
    private static final String CODE_INVALID = "INVALID_REQUEST";
    private static final Set<String> SCOPE_KINDS = Set.of(ConstraintScope.ALL, "task", "taskType", "resource",
            "resourceId", "resourceType", "tag");

    private final PlanLimitsProperties limits;

    /**
     * Creates the validator.
     *
     * @param limits the configured limits
     */
    @Autowired
    public PlanningRequestValidator(PlanLimitsProperties limits) {
        this.limits = limits;
    }

    /** Validator with the default {@link PlanLimitsProperties} (tests, programmatic use). */
    public PlanningRequestValidator() {
        this(new PlanLimitsProperties());
    }

    /**
     * Rejects a request whose actual serialized size is known by the caller (raw body available).
     *
     * @param actualBytes the serialized request size in bytes
     * @throws LimitExceededException ({@code max-request-bytes}) when above the limit
     */
    public void validateRequestBytes(long actualBytes) {
        if (actualBytes > limits.getMaxRequestBytes()) {
            throw new LimitExceededException("max-request-bytes", actualBytes, limits.getMaxRequestBytes());
        }
    }

    /**
     * Validates a request (submit path).
     *
     * @param request the request
     * @throws LimitExceededException      when a size limit is crossed
     * @throws RequestValidationException  when any other error was found
     */
    public void validate(PlanningRequest request) {
        ValidationResult result = collect(request);
        if (!result.hasErrors()) {
            return;
        }
        ValidationResult.Issue first = result.firstError();
        if (CODE_LIMIT.equals(first.code()) && first.args().containsKey("limit")) {
            throw new LimitExceededException(String.valueOf(first.args().get("limit")),
                    ((Number) first.args().get("actual")).longValue(), ((Number) first.args().get("max")).longValue());
        }
        throw new RequestValidationException(result);
    }

    /**
     * Collects every structural issue of a request.
     *
     * @param request the request ({@code null} is reported as an error)
     * @return the result with all errors and warnings (never {@code null})
     */
    public ValidationResult collect(PlanningRequest request) {
        ValidationResult result = new ValidationResult();
        if (request == null) {
            result.error(CODE_MISSING, "", "request is required");
            return result;
        }
        if (!checkSizeLimits(request, result)) {
            return result;
        }
        new Run(request, result).run();
        return result;
    }

    // ==================================================================================================
    // Size limits (cheap, first)
    // ==================================================================================================

    private boolean checkSizeLimits(PlanningRequest request, ValidationResult result) {
        limit(result, "max-tasks", request.tasks().size(), limits.getMaxTasks());
        limit(result, "max-resources", request.resources().size(), limits.getMaxResources());
        limit(result, "max-slots", request.slots().size(), limits.getMaxSlots());
        limit(result, "max-constraints", request.constraints().size(), limits.getMaxConstraints());
        if (request.solverOptions() != null && request.solverOptions().maxTimeSeconds() != null) {
            // Rejected (not clamped): silently shortening the requested time would change the result quality.
            limit(result, "max-time-seconds", request.solverOptions().maxTimeSeconds(), limits.getMaxTimeSeconds());
        }
        if (result.hasErrors()) {
            return false;
        }
        long precedences = 0;
        for (Task task : request.tasks()) {
            precedences += task.predecessors().size();
            limit(result, "max-requirements-per-task", task.requirements().size(),
                    limits.getMaxRequirementsPerTask());
        }
        limit(result, "max-precedences", precedences, limits.getMaxPrecedences());
        Set<Integer> tiers = new HashSet<>();
        for (Objective objective : request.objectives()) {
            tiers.add(objective.tier() == null ? 0 : objective.tier());
        }
        for (Constraint constraint : request.constraints()) {
            if (constraint.enforcement() == IEnumEnforcement.Types.SOFT) {
                tiers.add(constraint.tier() == null ? 0 : constraint.tier());
            }
        }
        limit(result, "max-tiers", tiers.size(), limits.getMaxTiers());
        limit(result, "max-request-bytes", estimateRequestBytes(request), limits.getMaxRequestBytes());
        return !result.hasErrors();
    }

    private static void limit(ValidationResult result, String name, long actual, long max) {
        if (actual > max) {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("limit", name);
            args.put("actual", actual);
            args.put("max", max);
            result.error(CODE_LIMIT, "", "Limit '" + name + "' exceeded: " + actual + " > " + max, args);
        }
    }

    /**
     * Approximation of the serialized request size used when the raw body is not available here (ids, type ids and a
     * fixed per-entity/per-attribute overhead). The exact check is {@link #validateRequestBytes(long)} and the
     * HTTP/Kafka payload limits of the caller.
     */
    private long estimateRequestBytes(PlanningRequest request) {
        long bytes = 0;
        for (Task task : request.tasks()) {
            bytes += ENTITY_OVERHEAD_BYTES + length(task.id()) + length(task.typeId())
                    + (long) task.attributes().size() * ATTRIBUTE_BYTES
                    + textBytes(task.startSlots()) + (long) task.predecessors().size() * ATTRIBUTE_BYTES
                    + (long) task.requirements().size() * ENTITY_OVERHEAD_BYTES;
            for (String tag : task.tags()) {
                bytes += length(tag) + 3;
            }
        }
        for (Resource resource : request.resources()) {
            bytes += ENTITY_OVERHEAD_BYTES + length(resource.id()) + length(resource.typeId())
                    + (long) resource.attributes().size() * ATTRIBUTE_BYTES
                    + (long) resource.unavailable().size() * ATTRIBUTE_BYTES;
            for (String skill : resource.skills()) {
                bytes += length(skill) + 3;
            }
        }
        for (Slot slot : request.slots()) {
            bytes += ENTITY_OVERHEAD_BYTES + 64 + length(slot.id()) + length(slot.typeId())
                    + (long) slot.attributes().size() * ATTRIBUTE_BYTES;
        }
        for (Calendar calendar : request.calendars()) {
            bytes += ENTITY_OVERHEAD_BYTES + length(calendar.id())
                    + (long) (calendar.windows().size() + calendar.exceptions().size()) * ATTRIBUTE_BYTES
                    + (long) (calendar.recurring().size() + calendar.breaks().size()) * ATTRIBUTE_BYTES;
        }
        for (SetupMatrix matrix : request.setupMatrices()) {
            bytes += ENTITY_OVERHEAD_BYTES + length(matrix.id()) + (long) matrix.entries().size() * ATTRIBUTE_BYTES;
        }
        for (Constraint constraint : request.constraints()) {
            bytes += ENTITY_OVERHEAD_BYTES + length(constraint.id()) + length(constraint.type())
                    + (long) constraint.params().size() * ATTRIBUTE_BYTES;
        }
        for (Objective objective : request.objectives()) {
            bytes += ENTITY_OVERHEAD_BYTES + length(objective.id()) + length(objective.type());
        }
        if (request.initialSolution() != null) {
            bytes += (long) request.initialSolution().assignments().size() * ENTITY_OVERHEAD_BYTES;
        }
        return bytes;
    }

    private static long length(String value) {
        return value == null ? 0 : value.length();
    }

    private static long textBytes(List<String> values) {
        long bytes = 0;
        for (String value : values) {
            bytes += length(value) + 3;
        }
        return bytes;
    }

    // ==================================================================================================
    // Structural run
    // ==================================================================================================

    private final class Run {

        private final PlanningRequest request;
        private final ValidationResult out;
        private Horizon horizon;
        private long h;
        private ZoneId zone = ZoneId.of("UTC");
        private final Set<String> taskIds = new HashSet<>();
        private final Set<String> resourceIds = new HashSet<>();
        private final Map<String, Slot> slotsById = new HashMap<>();
        private final Set<String> calendarIds = new HashSet<>();
        private final Set<String> matrixIds = new HashSet<>();
        private final Set<String> resourceTypes = new HashSet<>();
        private final Set<String> taskTypes = new HashSet<>();
        private final Set<String> slotTypes = new HashSet<>();
        private boolean anyResourceHasSetup;

        Run(PlanningRequest request, ValidationResult out) {
            this.request = request;
            this.out = out;
        }

        void run() {
            meta();
            horizon();
            domain();
            collectIds();
            for (int i = 0; i < request.slots().size(); i++) {
                slot(i, request.slots().get(i));
            }
            Map<String, int[]> calendarRuns = new HashMap<>();
            calendars(calendarRuns);
            setupMatrices();
            for (int i = 0; i < request.resources().size(); i++) {
                resource(i, request.resources().get(i), calendarRuns);
            }
            for (int i = 0; i < request.tasks().size(); i++) {
                task(i, request.tasks().get(i));
            }
            precedenceCycle();
            for (int i = 0; i < request.constraints().size(); i++) {
                constraint(i, request.constraints().get(i));
            }
            for (int i = 0; i < request.objectives().size(); i++) {
                objective(i, request.objectives().get(i));
            }
            initialSolution();
            if (request.tasks().isEmpty()) {
                out.error(CODE_MISSING, "tasks", "tasks must contain at least one task");
            }
            if (request.resources().isEmpty()) {
                out.error(CODE_MISSING, "resources", "resources must contain at least one resource");
            }
            if (!out.hasErrors()) {
                candidateEstimate();
            }
        }

        // ---------------------------------------------------------------- meta, horizon, domain, ids

        private void meta() {
            PlanningMeta meta = request.meta();
            if (meta == null) {
                error(CODE_MISSING, "meta", "meta is required");
                return;
            }
            if (!PlanningMeta.SCHEMA_VERSION.equals(meta.version())) {
                error("UNSUPPORTED_SCHEMA_VERSION", "meta.version",
                        "meta.version must be \"" + PlanningMeta.SCHEMA_VERSION + "\" but was \"" + meta.version() + "\"",
                        "version", meta.version());
            }
            try {
                zone = ZoneId.of(meta.timezone());
            } catch (DateTimeException e) {
                error("INVALID_TIMEZONE", "meta.timezone", "'" + meta.timezone() + "' is not a valid IANA time zone",
                        "timezone", meta.timezone());
            }
        }

        private void horizon() {
            horizon = request.horizon();
            if (horizon == null) {
                error("INVALID_HORIZON", "horizon", "horizon is required");
                return;
            }
            h = horizon.horizonTicks();
            if (h > limits.getMaxHorizonTicks()) {
                error("HORIZON_TOO_LARGE", "horizon", "horizon has " + h + " ticks, more than the limit "
                        + limits.getMaxHorizonTicks() + "; use a coarser tickSeconds",
                        "ticks", h, "max", limits.getMaxHorizonTicks());
                horizon = null;
            }
        }

        private void domain() {
            DomainDefinition domain = request.domain();
            typeIds(domain.resourceTypes(), "domain.resourceTypes", resourceTypes);
            typeIds(domain.taskTypes(), "domain.taskTypes", taskTypes);
            typeIds(domain.slotTypes(), "domain.slotTypes", slotTypes);
        }

        private void typeIds(List<TypeDefinition> definitions, String path, Set<String> target) {
            for (int i = 0; i < definitions.size(); i++) {
                String id = definitions.get(i).id();
                if (id == null || id.isBlank()) {
                    error(CODE_MISSING, path + "[" + i + "].id", "type id must not be blank");
                } else if (!target.add(id)) {
                    error("DUPLICATE_ID", path + "[" + i + "].id", "duplicate type id '" + id + "'", "id", id);
                }
            }
        }

        private void collectIds() {
            uniqueIds(request.tasks().stream().map(Task::id).toList(), "tasks", taskIds);
            uniqueIds(request.resources().stream().map(Resource::id).toList(), "resources", resourceIds);
            Set<String> slotIds = new HashSet<>();
            uniqueIds(request.slots().stream().map(Slot::id).toList(), "slots", slotIds);
            for (Slot slot : request.slots()) {
                if (slot.id() != null) {
                    slotsById.putIfAbsent(slot.id(), slot);
                }
            }
            uniqueIds(request.calendars().stream().map(Calendar::id).toList(), "calendars", calendarIds);
            uniqueIds(request.setupMatrices().stream().map(SetupMatrix::id).toList(), "setupMatrices", matrixIds);
            uniqueIds(request.constraints().stream().map(Constraint::id).toList(), "constraints", new HashSet<>());
            uniqueIds(request.objectives().stream().map(Objective::id).toList(), "objectives", new HashSet<>());
        }

        private void uniqueIds(List<String> ids, String path, Set<String> target) {
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                String at = path + "[" + i + "].id";
                if (id == null || id.isBlank()) {
                    error(CODE_MISSING, at, "id must not be blank");
                } else if (id.length() > Task.ID_MAX_LENGTH) {
                    error(CODE_INVALID, at, "id must not exceed " + Task.ID_MAX_LENGTH + " characters");
                } else if (!target.add(id)) {
                    error("DUPLICATE_ID", at, "duplicate id '" + id + "'", "id", id);
                }
            }
        }

        // ---------------------------------------------------------------- slots, calendars, setups, resources

        private void slot(int i, Slot slot) {
            String at = "slots[" + i + "]";
            typeRef(slot.typeId(), slotTypes, at + ".typeId");
            if (slot.start() == null || slot.end() == null) {
                error(CODE_MISSING, at, "slot " + slot.id() + " requires start and end");
                return;
            }
            if (!slot.start().isBefore(slot.end())) {
                error(CODE_INVALID, at, "slot " + slot.id() + " must have start before end");
                return;
            }
            instant(slot.start(), at + ".start", true);
            instant(slot.end(), at + ".end", true);
            if (slot.cost() < 0 || !Double.isFinite(slot.cost())) {
                error(CODE_INVALID, at + ".cost", "slot cost must be a finite number >= 0");
            }
        }

        private void calendars(Map<String, int[]> calendarRuns) {
            long totalRuns = 0;
            for (int i = 0; i < request.calendars().size(); i++) {
                Calendar calendar = request.calendars().get(i);
                String at = "calendars[" + i + "]";
                boolean valid = true;
                if (calendar.timezone() != null && !calendar.timezone().isBlank()) {
                    try {
                        ZoneId.of(calendar.timezone());
                    } catch (DateTimeException e) {
                        error("CALENDAR_INVALID", at + ".timezone", "'" + calendar.timezone() + "' is not a valid time zone");
                        valid = false;
                    }
                }
                valid &= windows(calendar.windows(), at + ".windows", true);
                valid &= windows(calendar.exceptions(), at + ".exceptions", false);
                valid &= recurring(calendar.recurring(), at + ".recurring");
                valid &= recurring(calendar.breaks(), at + ".breaks");
                if (!valid || horizon == null || calendar.id() == null) {
                    continue;
                }
                List<DiagnosticIssue> warnings = new ArrayList<>();
                try {
                    int[] runs = CalendarExpander.expand(calendar, horizon, zone, warnings,
                            limits.getMaxCalendarRuns());
                    calendarRuns.putIfAbsent(calendar.id(), runs);
                    totalRuns += runs.length / 2;
                    if (totalRuns > limits.getMaxCalendarRuns()) {
                        limitError("max-calendar-runs", totalRuns, limits.getMaxCalendarRuns());
                        return;
                    }
                } catch (LimitExceededException e) {
                    limitError(e.limitName(), e.limit() + 1, e.limit());
                    return;
                } catch (IllegalArgumentException e) {
                    error("CALENDAR_INVALID", at, e.getMessage());
                    continue;
                }
                for (DiagnosticIssue w : warnings) {
                    out.warning(w.code(), at + "." + w.path(), w.message());
                }
            }
        }

        private boolean windows(List<TimeWindow> windows, String path, boolean checkOverlap) {
            boolean valid = true;
            for (int i = 0; i < windows.size(); i++) {
                TimeWindow w = windows.get(i);
                if (w.start() == null || w.end() == null || !w.start().isBefore(w.end())) {
                    error("CALENDAR_INVALID", path + "[" + i + "]", "window start must be before end");
                    valid = false;
                }
            }
            if (valid && checkOverlap && windows.size() > 1) {
                List<TimeWindow> sorted = new ArrayList<>(windows);
                sorted.sort((a, b) -> a.start().compareTo(b.start()));
                for (int i = 1; i < sorted.size(); i++) {
                    if (sorted.get(i).start().isBefore(sorted.get(i - 1).end())) {
                        out.warning("CALENDAR_OVERLAP", path, "explicit windows overlap and are merged");
                        break;
                    }
                }
            }
            return valid;
        }

        private boolean recurring(List<RecurringWindow> windows, String path) {
            boolean valid = true;
            for (int i = 0; i < windows.size(); i++) {
                RecurringWindow w = windows.get(i);
                String at = path + "[" + i + "]";
                if (w.from() == null || w.to() == null) {
                    error("CALENDAR_INVALID", at, "recurring window requires from and to");
                    valid = false;
                }
                if (w.validFrom() != null && w.validTo() != null && w.validFrom().isAfter(w.validTo())) {
                    error("CALENDAR_INVALID", at, "validFrom must not be after validTo");
                    valid = false;
                }
            }
            return valid;
        }

        private void setupMatrices() {
            for (int i = 0; i < request.setupMatrices().size(); i++) {
                SetupMatrix matrix = request.setupMatrices().get(i);
                String at = "setupMatrices[" + i + "]";
                if (matrix.defaultTicks() < 0 || matrix.sameClassTicks() < 0) {
                    error("SETUP_MATRIX_INVALID", at, "setup ticks must be >= 0");
                }
                for (int e = 0; e < matrix.entries().size(); e++) {
                    SetupEntry entry = matrix.entries().get(e);
                    String ea = at + ".entries[" + e + "]";
                    if (entry.from() == null || entry.from().isBlank() || entry.to() == null || entry.to().isBlank()) {
                        error("SETUP_MATRIX_INVALID", ea, "setup entry requires from and to classes");
                    }
                    if (entry.ticks() < 0) {
                        error("SETUP_MATRIX_INVALID", ea + ".ticks", "setup ticks must be >= 0");
                    }
                }
            }
        }

        private void resource(int i, Resource resource, Map<String, int[]> calendarRuns) {
            String at = "resources[" + i + "]";
            typeRef(resource.typeId(), resourceTypes, at + ".typeId");
            int capacity = resource.capacity() == null ? 1 : resource.capacity();
            if (capacity < 1) {
                error("INVALID_CAPACITY", at + ".capacity", "capacity must be an integer >= 1", "capacity", capacity);
            }
            if (resource.costPerTick() != null && (resource.costPerTick() < 0 || !Double.isFinite(resource.costPerTick()))) {
                error(CODE_INVALID, at + ".costPerTick", "costPerTick must be a finite number >= 0");
            }
            if (resource.costPerUse() != null && (resource.costPerUse() < 0 || !Double.isFinite(resource.costPerUse()))) {
                error(CODE_INVALID, at + ".costPerUse", "costPerUse must be a finite number >= 0");
            }
            if (resource.maxLoadTicks() != null && resource.maxLoadTicks() < 0) {
                error(CODE_INVALID, at + ".maxLoadTicks", "maxLoadTicks must be >= 0");
            }
            if (resource.calendarId() != null && !calendarIds.contains(resource.calendarId())) {
                error("UNKNOWN_REFERENCE", at + ".calendarId", "unknown calendar '" + resource.calendarId() + "'",
                        "id", resource.calendarId());
            }
            if (resource.setupMatrixId() != null) {
                anyResourceHasSetup = true;
                if (!matrixIds.contains(resource.setupMatrixId())) {
                    error("UNKNOWN_REFERENCE", at + ".setupMatrixId",
                            "unknown setup matrix '" + resource.setupMatrixId() + "'", "id", resource.setupMatrixId());
                }
                if (capacity > 1) {
                    error("SETUP_RESOURCE_NOT_UNARY", at + ".setupMatrixId",
                            "resource " + resource.id() + " has a setup matrix and capacity " + capacity
                                    + "; setups need a unary resource");
                }
            }
            boolean validWindows = windows(resource.unavailable(), at + ".unavailable", false);
            if (validWindows && horizon != null && (!resource.unavailable().isEmpty() || resource.calendarId() != null)) {
                List<DiagnosticIssue> warnings = new ArrayList<>();
                int[] blocks = CalendarExpander.blockedRuns(resource.unavailable(), horizon, warnings,
                        at + ".unavailable");
                for (DiagnosticIssue w : warnings) {
                    out.warning(w.code(), w.path(), w.message());
                }
                int[] base = resource.calendarId() == null ? new int[]{0, (int) h} : calendarRuns.get(resource.calendarId());
                if (base != null && CalendarExpander.subtract(base, blocks).length == 0) {
                    out.warning("CALENDAR_EMPTY", at, "resource " + resource.id()
                            + " has no availability inside the horizon: all its candidates will be pruned");
                }
            }
        }

        // ---------------------------------------------------------------- tasks

        private void task(int i, Task task) {
            String at = "tasks[" + i + "]";
            typeRef(task.typeId(), taskTypes, at + ".typeId");
            // duration
            Integer minDuration = null;
            if (task.duration() != null) {
                if (task.duration() < 0 || (horizon != null && task.duration() > h)) {
                    error("INVALID_DURATION", at + ".duration", "duration must be between 0 and the horizon length ("
                            + h + " ticks)", "duration", task.duration());
                } else {
                    minDuration = task.duration();
                }
            } else {
                minDuration = slotDefaultDuration(at, task);
            }
            if (task.demand() != null && task.demand() < 1) {
                error("INVALID_DEMAND", at + ".demand", "demand must be an integer >= 1", "demand", task.demand());
            }
            if (task.priority() != null && task.priority() < 0) {
                error(CODE_INVALID, at + ".priority", "priority must be >= 0");
            }
            if (task.prize() != null && task.prize() < 0) {
                error(CODE_INVALID, at + ".prize", "prize must be >= 0");
            }
            requirements(at, task);
            Integer minWithResource = resourceDurations(at, task, minDuration);
            // time points
            int release = 0;
            int deadline = horizon == null ? Integer.MAX_VALUE : (int) h;
            Integer r = instant(task.release(), at + ".release", true);
            if (r != null) {
                release = r;
            }
            Integer d = instant(task.deadline(), at + ".deadline", true);
            if (d != null) {
                deadline = d;
            }
            instant(task.pin() == null ? null : task.pin().start(), at + ".pin.start", true);
            Integer due = instant(task.dueDate(), at + ".dueDate", false);
            if (horizon != null && minWithResource != null && (long) release + minWithResource > deadline) {
                error("WINDOW_IMPOSSIBLE", at, "release + duration (" + ((long) release + minWithResource)
                        + " ticks) is after the deadline (" + deadline + " ticks)");
            }
            if (due != null && minWithResource != null && due < (long) release + minWithResource) {
                out.warning("DUE_DATE_UNREACHABLE", at + ".dueDate",
                        "dueDate is before the earliest possible end of the task");
            }
            // slots
            for (int s = 0; s < task.startSlots().size(); s++) {
                String ref = task.startSlots().get(s);
                if (!"ALL".equalsIgnoreCase(ref) && !slotsById.containsKey(ref)) {
                    error("UNKNOWN_REFERENCE", at + ".startSlots[" + s + "]", "unknown slot '" + ref + "'", "id", ref);
                }
            }
            pin(at, task);
            predecessors(at, task);
            if (task.setupClass() != null && !anyResourceHasSetup) {
                error("UNKNOWN_REFERENCE", at + ".setupClass", "setupClass '" + task.setupClass()
                        + "' needs at least one resource with a setupMatrixId", "id", task.setupClass());
            }
            preferences(at, task);
        }

        /** Default duration in slot mode: the common slot length in ticks, or {@code null} after reporting. */
        private Integer slotDefaultDuration(String at, Task task) {
            if (task.startSlots().isEmpty()) {
                error("DURATION_REQUIRED", at + ".duration", "duration is required unless startSlots are given");
                return null;
            }
            if (horizon == null) {
                return null;
            }
            long length = -1;
            List<Slot> listed = new ArrayList<>();
            if (task.startSlots().stream().anyMatch("ALL"::equalsIgnoreCase)) {
                listed.addAll(request.slots());
            } else {
                for (String id : task.startSlots()) {
                    Slot slot = slotsById.get(id);
                    if (slot != null) {
                        listed.add(slot);
                    }
                }
            }
            for (Slot slot : listed) {
                if (slot.start() == null || slot.end() == null || !slot.start().isBefore(slot.end())) {
                    continue;
                }
                long seconds = java.time.Duration.between(slot.start(), slot.end()).getSeconds();
                long ticks = seconds / horizon.tickSeconds();
                if (length >= 0 && length != ticks) {
                    error("DURATION_REQUIRED", at + ".duration",
                            "the listed slots have different lengths: duration is required");
                    return null;
                }
                length = ticks;
            }
            return length < 0 ? null : (int) Math.min(length, Integer.MAX_VALUE);
        }

        private void requirements(String at, Task task) {
            Set<String> roles = new HashSet<>();
            for (int q = 0; q < task.requirements().size(); q++) {
                ResourceRequirement req = task.requirements().get(q);
                String rq = at + ".requirements[" + q + "]";
                if (!roles.add(req.role())) {
                    error("INVALID_REQUIREMENT", rq + ".role", "duplicate role '" + req.role() + "' in the task",
                            "role", req.role());
                }
                if (req.count() != null && req.count() < 1) {
                    error("INVALID_REQUIREMENT", rq + ".count", "count must be >= 1", "count", req.count());
                }
                if (req.amount() != null && req.amount() < 1) {
                    error("INVALID_REQUIREMENT", rq + ".amount", "amount must be >= 1", "amount", req.amount());
                }
                selector(rq + ".selector", req.selector());
            }
        }

        private void selector(String path, ResourceSelector selector) {
            for (int i = 0; i < selector.resourceIds().size(); i++) {
                String id = selector.resourceIds().get(i);
                if (!"ALL".equalsIgnoreCase(id) && !resourceIds.contains(id)) {
                    error("UNKNOWN_REFERENCE", path + ".resourceIds[" + i + "]", "unknown resource '" + id + "'",
                            "id", id);
                }
            }
            for (int i = 0; i < selector.excludeResourceIds().size(); i++) {
                String id = selector.excludeResourceIds().get(i);
                if (!resourceIds.contains(id)) {
                    error("UNKNOWN_REFERENCE", path + ".excludeResourceIds[" + i + "]", "unknown resource '" + id + "'",
                            "id", id);
                }
            }
            if (!resourceTypes.isEmpty()) {
                for (int i = 0; i < selector.resourceTypeIds().size(); i++) {
                    String id = selector.resourceTypeIds().get(i);
                    if (!resourceTypes.contains(id)) {
                        error("UNKNOWN_REFERENCE", path + ".resourceTypeIds[" + i + "]",
                                "unknown resource type '" + id + "'", "id", id);
                    }
                }
            }
        }

        /** Validates resourceDurations and returns the smallest duration the task can have (for the window check). */
        private Integer resourceDurations(String at, Task task, Integer base) {
            if (task.resourceDurations().isEmpty()) {
                return base;
            }
            List<ResourceRequirement> reqs = task.effectiveRequirements();
            if (reqs.size() != 1 || (reqs.get(0).count() != null && reqs.get(0).count() != 1)) {
                error("INVALID_REQUIREMENT", at + ".resourceDurations",
                        "resourceDurations need exactly one requirement of count 1");
            }
            Integer min = base;
            for (Map.Entry<String, Integer> e : task.resourceDurations().entrySet()) {
                String path = at + ".resourceDurations." + e.getKey();
                if (!resourceIds.contains(e.getKey())) {
                    error("UNKNOWN_REFERENCE", path, "unknown resource '" + e.getKey() + "'", "id", e.getKey());
                }
                Integer v = e.getValue();
                if (v == null || v < 0 || (horizon != null && v > h)) {
                    error("INVALID_DURATION", path, "duration must be between 0 and the horizon length");
                } else if (min == null || v < min) {
                    min = v;
                }
            }
            return min;
        }

        private void pin(String at, Task task) {
            if (task.pin() == null) {
                return;
            }
            int slots = 0;
            for (ResourceRequirement req : task.effectiveRequirements()) {
                slots += req.count() == null ? 1 : Math.max(1, req.count());
            }
            if (task.pin().resourceIds().size() > slots) {
                error("INVALID_REQUIREMENT", at + ".pin.resourceIds", "pin names " + task.pin().resourceIds().size()
                        + " resources but the task needs only " + slots);
            }
            for (int i = 0; i < task.pin().resourceIds().size(); i++) {
                String id = task.pin().resourceIds().get(i);
                if (!resourceIds.contains(id)) {
                    error("UNKNOWN_REFERENCE", at + ".pin.resourceIds[" + i + "]", "unknown resource '" + id + "'",
                            "id", id);
                }
            }
        }

        private void predecessors(String at, Task task) {
            for (int p = 0; p < task.predecessors().size(); p++) {
                PrecedenceRef ref = task.predecessors().get(p);
                String pa = at + ".predecessors[" + p + "]";
                if (ref.taskId() == null || ref.taskId().isBlank()) {
                    error(CODE_MISSING, pa + ".taskId", "predecessor taskId is required");
                    continue;
                }
                if (ref.taskId().equals(task.id())) {
                    error("PRECEDENCE_SELF_REFERENCE", pa, "task " + task.id() + " cannot precede itself",
                            "id", task.id());
                    continue;
                }
                if (!taskIds.contains(ref.taskId())) {
                    error("UNKNOWN_REFERENCE", pa + ".taskId", "unknown task '" + ref.taskId() + "'", "id", ref.taskId());
                }
                long min = ref.minLag() == null ? 0 : ref.minLag();
                if (horizon != null && (min < -h || min > h)) {
                    error("INVALID_LAG", pa + ".minLag", "minLag must be within the horizon length (+-" + h + " ticks)");
                }
                if (ref.maxLag() != null) {
                    if (ref.maxLag() < min) {
                        error("INVALID_LAG", pa + ".maxLag", "maxLag must not be smaller than minLag");
                    } else if (horizon != null && ref.maxLag() > h) {
                        error("INVALID_LAG", pa + ".maxLag", "maxLag must be within the horizon length");
                    }
                }
            }
        }

        private void preferences(String at, Task task) {
            for (int i = 0; i < task.preferences().size(); i++) {
                Preference preference = task.preferences().get(i);
                String pa = at + ".preferences[" + i + "]";
                if (preference.kind() == null) {
                    error(CODE_MISSING, pa + ".kind", "preference kind is required");
                    continue;
                }
                if (preference.penalty() < 0) {
                    error(CODE_INVALID, pa + ".penalty", "penalty must be >= 0");
                }
                if (preference.kind() == Preference.Kind.RESOURCE) {
                    if (preference.resourceId() == null || !resourceIds.contains(preference.resourceId())) {
                        error("UNKNOWN_REFERENCE", pa + ".resourceId",
                                "unknown resource '" + preference.resourceId() + "'", "id", preference.resourceId());
                    }
                } else if (preference.start() == null) {
                    error(CODE_MISSING, pa + ".start", "a START preference requires start");
                } else {
                    instant(preference.start(), pa + ".start", true);
                }
            }
        }

        private void precedenceCycle() {
            List<String> cycle = PrecedenceGraph.findCycle(request.tasks());
            if (!cycle.isEmpty()) {
                error("PRECEDENCE_CYCLE", "tasks", "precedence cycle: " + String.join(" -> ", cycle) + " -> "
                        + cycle.get(0), "cycle", String.join(" -> ", cycle) + " -> " + cycle.get(0));
            }
        }

        // ---------------------------------------------------------------- constraints, objectives, initial solution

        private void constraint(int i, Constraint constraint) {
            String at = "constraints[" + i + "]";
            if (constraint.type() == null || constraint.type().isBlank()) {
                error(CODE_MISSING, at + ".type", "constraint type is required");
            }
            scope(at + ".scope", constraint.scope());
        }

        private void objective(int i, Objective objective) {
            String at = "objectives[" + i + "]";
            if (objective.type() == null || objective.type().isBlank()) {
                error(CODE_MISSING, at + ".type", "objective type is required");
            }
            scope(at + ".scope", objective.scope());
        }

        private void scope(String path, ConstraintScope scope) {
            if (scope == null || ConstraintScope.ALL.equals(scope.kind())) {
                return;
            }
            if (!SCOPE_KINDS.contains(scope.kind())) {
                error("INVALID_SCOPE", path + ".kind", "unknown scope kind '" + scope.kind() + "'", "kind", scope.kind());
                return;
            }
            if (scope.value() == null || scope.value().isBlank()) {
                error("INVALID_SCOPE", path + ".value", "scope kind '" + scope.kind() + "' requires a value");
                return;
            }
            if ("task".equals(scope.kind()) && !taskIds.contains(scope.value())) {
                error("UNKNOWN_REFERENCE", path + ".value", "unknown task '" + scope.value() + "'", "id", scope.value());
            }
            if (("resource".equals(scope.kind()) || "resourceId".equals(scope.kind()))
                    && !resourceIds.contains(scope.value())) {
                error("UNKNOWN_REFERENCE", path + ".value", "unknown resource '" + scope.value() + "'",
                        "id", scope.value());
            }
        }

        private void initialSolution() {
            if (request.initialSolution() == null) {
                return;
            }
            Set<String> seen = new HashSet<>();
            List<InitialAssignment> assignments = request.initialSolution().assignments();
            for (int i = 0; i < assignments.size(); i++) {
                InitialAssignment a = assignments.get(i);
                String at = "initialSolution.assignments[" + i + "]";
                if (a.taskId() == null || !taskIds.contains(a.taskId())) {
                    error("INVALID_INITIAL_SOLUTION", at + ".taskId", "unknown task '" + a.taskId() + "'",
                            "id", a.taskId());
                } else if (!seen.add(a.taskId())) {
                    error("INVALID_INITIAL_SOLUTION", at + ".taskId", "task '" + a.taskId() + "' appears twice",
                            "id", a.taskId());
                }
                for (int r = 0; r < a.resourceIds().size(); r++) {
                    if (!resourceIds.contains(a.resourceIds().get(r))) {
                        error("INVALID_INITIAL_SOLUTION", at + ".resourceIds[" + r + "]",
                                "unknown resource '" + a.resourceIds().get(r) + "'", "id", a.resourceIds().get(r));
                    }
                }
                if (a.locked() && a.start() == null) {
                    error("INVALID_INITIAL_SOLUTION", at + ".start", "a locked assignment requires a start");
                }
                instant(a.start(), at + ".start", true);
            }
        }

        private void candidateEstimate() {
            long candidates = ModelCostEstimator.candidateUpperBound(request.tasks(), request.resources());
            long optional = request.tasks().stream().filter(t -> !t.requiredTask()).count();
            long variables = candidates + optional + request.tasks().size();
            if (candidates > limits.getMaxCandidates() * 4) {
                limitError("max-candidates", candidates, limits.getMaxCandidates() * 4);
            }
            if (variables > limits.getMaxVariables()) {
                limitError("max-variables", variables, limits.getMaxVariables());
            }
        }

        // ---------------------------------------------------------------- helpers

        private void typeRef(String typeId, Set<String> allowed, String path) {
            if (typeId != null && !allowed.isEmpty() && !allowed.contains(typeId)) {
                error("UNKNOWN_REFERENCE", path, "unknown type '" + typeId + "'", "id", typeId);
            }
        }

        /**
         * Checks alignment and horizon membership of a time point.
         *
         * @param strict {@code true}: outside the horizon is an error; {@code false}: it is clipped with a warning
         * @return the tick, or {@code null} when absent or invalid
         */
        private Integer instant(Instant t, String path, boolean strict) {
            if (t == null || horizon == null) {
                return null;
            }
            if (!horizon.isAligned(t)) {
                error("TIME_NOT_ALIGNED", path, t + " is not on a " + horizon.tickSeconds()
                        + "s tick boundary of the horizon", "instant", t.toString(), "tickSeconds", horizon.tickSeconds());
                return null;
            }
            if (t.isBefore(horizon.start()) || t.isAfter(horizon.end())) {
                if (strict) {
                    error("TIME_OUT_OF_HORIZON", path, t + " is outside the horizon [" + horizon.start() + ", "
                            + horizon.end() + "]", "instant", t.toString());
                    return null;
                }
                out.warning("OUT_OF_HORIZON_CLIPPED", path, t + " is outside the horizon and was clipped");
                return t.isBefore(horizon.start()) ? 0 : (int) h;
            }
            return horizon.toTick(t);
        }

        private void error(String code, String path, String message, Object... args) {
            out.error(code, path, message, argMap(args));
        }

        private void limitError(String name, long actual, long max) {
            out.error(CODE_LIMIT, "", "Limit '" + name + "' exceeded: " + actual + " > " + max,
                    argMap("limit", name, "actual", actual, "max", max));
        }
    }

    private static Map<String, Object> argMap(Object... kv) {
        if (kv == null || kv.length == 0) {
            return Map.of();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
