package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;

/**
 * One per constraint per build. Hides HARD / RELAXABLE / SOFT / EXPLAIN from contributors (D-7): contributors write
 * instance-level {@code onlyEnforceIf(enforcer.instance(..))} constraints and soft, relaxation and infeasibility cores
 * come for free for every family.
 * <p>
 * Implemented by {@code CpSatEnforcerFactory} (WP3); used on the building thread only. Literals belong to the build's
 * model. OR-TOOLS-VERIFY: {@code com.google.ortools.sat.Literal} and {@code LinearExpr} (OR-Tools 9.15 Java).
 */
public interface Enforcer {

    /** The enforcement mode as seen by the contributor. */
    enum Mode { HARD, RELAXABLE, SOFT }

    /** @return the effective mode */
    Mode mode();

    /**
     * Literal to pass to {@code onlyEnforceIf} for ONE violation instance of this constraint, or {@code null} when it must
     * hold unconditionally.
     * <ul>
     *   <li>HARD (solve): {@code null}.</li>
     *   <li>HARD (explain pass): the assumption literal of the constraint.</li>
     *   <li>RELAXABLE: the shared {@code active_k} literal.</li>
     *   <li>SOFT: a fresh BoolVar "satisfied" whose negation is penalised weight x 1 in the constraint tier and recorded
     *   for reporting.</li>
     * </ul>
     *
     * @param taskIdx     task index of the instance, or -1 when not applicable
     * @param resourceIdx resource index of the instance, or -1 when not applicable
     */
    Literal instance(int taskIdx, int resourceIdx);

    /** Shorthand for {@code instance(-1, -1)}. */
    default Literal instance() {
        return instance(-1, -1);
    }

    /**
     * SOFT only: penalise an integer measure instead of a unit literal: cost = weight x measure. HARD / RELAXABLE: returns
     * {@code false} and adds nothing.
     *
     * @param measure    the violation measure expression (family units: ticks or counts)
     * @param maxMeasure exact upper bound of the measure (overflow guard)
     * @param taskIdx    task index for reporting, or -1
     * @return {@code true} when the measure was penalised
     */
    boolean penalizeMeasure(LinearExpr measure, long maxMeasure, int taskIdx);

    /**
     * Combines the instance literal with extra conditions into the array form of {@code onlyEnforceIf}; {@code null}
     * literals are skipped (so the result may be empty, meaning unconditional).
     */
    Literal[] conditions(Literal instance, Literal... extra);
}
