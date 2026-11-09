package eu.isygoit.solver.check;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.response.ConstraintViolation;
import eu.isygoit.dto.response.Relaxation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Turns the violations a checker found on a DROPPED relaxable constraint into the real {@link Relaxation} impact
 * (design 6.9, 8.2 step 3).
 * <p>
 * A relaxation is reported ONLY when the family checker finds at least one violation ("relaxed but harmless" constraints
 * must not pollute the result). {@code violationCount} is the number of instances, {@code measure} the total in family
 * units, {@code impactOnScore = measure}, {@code violatedTaskIds} the first {@value #MAX_TASK_IDS} distinct task ids in
 * ascending order. The same violations are also exposed as hard-violation entries (relaxed hard constraints land in
 * {@code PlanningSolution.hardViolations}).
 * <p>
 * Stateless and thread-safe.
 */
public final class RelaxationReporter {

    /** Maximum number of task ids listed in a {@link Relaxation}. */
    public static final int MAX_TASK_IDS = 50;
    /** {@code relaxedValue} of every reported relaxation (kept from v1). */
    public static final String RELAXED = "RELAXED";

    /**
     * @param constraint the dropped constraint
     * @param violations what its checker reported on the solution
     * @return the relaxation, or empty when the checker found nothing
     */
    public Optional<Relaxation> report(Constraint constraint, ViolationCollector violations) {
        if (violations == null || violations.isEmpty()) {
            return Optional.empty();
        }
        TreeSet<String> taskIds = new TreeSet<>();
        for (ViolationCollector.Entry entry : violations.entries()) {
            if (entry.taskId() != null) {
                taskIds.add(entry.taskId());
            }
        }
        List<String> listed = new ArrayList<>(Math.min(taskIds.size(), MAX_TASK_IDS));
        for (String id : taskIds) {
            if (listed.size() == MAX_TASK_IDS) {
                break;
            }
            listed.add(id);
        }
        double measure = (double) violations.totalMeasure();
        return Optional.of(new Relaxation(constraint.id(), constraint.enforcement().name(), RELAXED,
                measure, true, violations.count(), listed, measure));
    }

    /**
     * @param constraint the dropped constraint
     * @param violations what its checker reported
     * @return one hard-violation entry per instance (impact = measure), sorted by (task, resource)
     */
    public List<ConstraintViolation> hardViolations(Constraint constraint, ViolationCollector violations) {
        List<ConstraintViolation> out = new ArrayList<>(violations.count());
        for (ViolationCollector.Entry entry : violations.entries()) {
            out.add(new ConstraintViolation(constraint.id(), entry.taskId(), entry.resourceId(), entry.code(),
                    entry.message(), (double) entry.measure()));
        }
        out.sort(Comparator.comparing(ConstraintViolation::taskId, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ConstraintViolation::resourceId, Comparator.nullsFirst(Comparator.naturalOrder())));
        return out;
    }
}
