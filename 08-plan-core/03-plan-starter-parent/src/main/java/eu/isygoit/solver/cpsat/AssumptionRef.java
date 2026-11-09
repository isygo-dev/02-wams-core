package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.Literal;

/**
 * Metadata of one assumption literal of an EXPLAIN-mode model (design 6.10): which user-visible item the literal stands
 * for, so a sufficient-assumptions core can be translated into core items.
 * <p>
 * Created by the CP-SAT builder (WP3) in {@code BuiltModel.assumptions()}; consumed by the explainer (WP6). Immutable;
 * the literal belongs to one build.
 *
 * @param literal    the assumption literal
 * @param kind       CONSTRAINT | TASK | RESOURCE | EDGE
 * @param id         the id of the constraint / task / resource, or the edge description
 * @param reasonCode core reason code (for example CONSTRAINT, PRECEDENCE, CALENDAR, PIN_CONFLICT)
 */
public record AssumptionRef(Literal literal, String kind, String id, String reasonCode) {
}
