package eu.isygoit.solver.check;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.RelaxationPolicy;
import eu.isygoit.dto.response.ConstraintViolation;
import eu.isygoit.dto.response.Relaxation;
import eu.isygoit.enums.IEnumEnforcement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelaxationReporterTest {

    private static final Constraint RELAXABLE = new Constraint("rest-1", "min_rest",
            IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, ConstraintScope.all(), Map.of(),
            new RelaxationPolicy(true, 1, 1.0), 0, null);

    private final RelaxationReporter reporter = new RelaxationReporter();

    @Test
    void harmlessRelaxationIsNotReported() {
        assertEquals(Optional.empty(), reporter.report(RELAXABLE, new ViolationCollector()));
    }

    @Test
    void realImpactIsCountedAndTaskIdsAreSortedAndDistinct() {
        ViolationCollector violations = new ViolationCollector();
        violations.violation("t2", "R", "REST", "too close", 3);
        violations.violation("t1", "R", "REST", "too close", 4);
        violations.violation("t2", "S", "REST", "too close", 5);
        violations.violation(null, "S", "REST", "no task", 1);
        Relaxation relaxation = reporter.report(RELAXABLE, violations).orElseThrow();
        assertEquals("rest-1", relaxation.constraintId());
        assertEquals("RELAXABLE_WITH_AUTHORIZATION", relaxation.originalValue());
        assertEquals("RELAXED", relaxation.relaxedValue());
        assertEquals(4, relaxation.violationCount());
        assertEquals(13.0, relaxation.measure());
        assertEquals(13.0, relaxation.impactOnScore());
        assertTrue(relaxation.authorized());
        assertEquals(List.of("t1", "t2"), relaxation.violatedTaskIds());
    }

    @Test
    void taskIdListIsCappedAtFifty() {
        ViolationCollector violations = new ViolationCollector();
        for (int i = 0; i < 80; i++) {
            violations.violation(String.format("t%03d", i), null, "X", "m", 1);
        }
        Relaxation relaxation = reporter.report(RELAXABLE, violations).orElseThrow();
        assertEquals(80, relaxation.violationCount());
        assertEquals(RelaxationReporter.MAX_TASK_IDS, relaxation.violatedTaskIds().size());
        assertEquals("t000", relaxation.violatedTaskIds().get(0));
        assertEquals("t049", relaxation.violatedTaskIds().get(49));
    }

    @Test
    void hardViolationEntriesCarryTheMeasureAsImpactAndAreOrdered() {
        ViolationCollector violations = new ViolationCollector();
        violations.violation("t2", "R", "REST", "b", 2);
        violations.violation("t1", "S", "REST", "a", 7);
        violations.violation(null, null, "REST", "c", 1);
        List<ConstraintViolation> out = reporter.hardViolations(RELAXABLE, violations);
        assertEquals(3, out.size());
        assertEquals(null, out.get(0).taskId());
        assertEquals("t1", out.get(1).taskId());
        assertEquals(7.0, out.get(1).impact());
        assertEquals("t2", out.get(2).taskId());
        assertEquals("rest-1", out.get(2).constraintId());
    }

    @Test
    void collectorSumsMeasures() {
        ViolationCollector violations = new ViolationCollector();
        violations.violation("a", null, "X", "m", 5);
        violations.violation("b", null, "X", "m", 6);
        assertEquals(11L, violations.totalMeasure());
        assertEquals(2, violations.count());
        assertTrue(!violations.isEmpty());
    }
}
