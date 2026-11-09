package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.LinearExpr;

/**
 * Receiver of the objective terms of {@link CpSatObjectiveContributor}s (and of SOFT constraint penalties). All terms are
 * MINIMISATION terms; the tier machinery (WP3 {@code TierObjectiveBuilder}) combines them with tier multipliers and
 * guards the 2^53 range (D-9 / D-12).
 * <p>
 * Implemented by the CP-SAT builder; used on the building thread only. Units: scaled integers for decimal inputs, exact
 * ticks/counts otherwise.
 */
public interface ObjectiveSink {

    /**
     * Adds a term to a tier.
     * <p>
     * OR-TOOLS-VERIFY: {@code com.google.ortools.sat.LinearExpr} (OR-Tools 9.15 Java).
     *
     * @param tier     lexicographic tier (-2 relaxation, -1 auto selection, 0..7 user tiers)
     * @param expr     the term; ALREADY includes the objective weight and scale. Maximisation => pass the negated expression
     * @param minValue exact lower bound of {@code expr} (overflow guards and tier multipliers)
     * @param maxValue exact upper bound of {@code expr}
     * @param label    short label for diagnostics
     */
    void add(int tier, LinearExpr expr, long minValue, long maxValue, String label);

    /** Adds a constant offset to a tier (for example the sum of all prizes of a reward term). */
    void addConstantOffset(int tier, long value);
}
