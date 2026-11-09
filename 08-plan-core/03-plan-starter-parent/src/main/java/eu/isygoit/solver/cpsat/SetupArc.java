package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.Literal;

/**
 * One arc literal of a resource's setup circuit: when {@code literal} is true the arc's two tasks are adjacent on the
 * resource and a setup of {@code ticks} separates them (design 6.6).
 * <p>
 * Registered by the setup circuit module through {@code CpModelContext.registerSetupArcs}, read by
 * {@code minimize_setup}. Immutable (the literal is a model object of one build; not thread-safe across builds).
 *
 * @param literal the arc literal
 * @param ticks   the setup duration in ticks
 */
public record SetupArc(Literal literal, int ticks) {
    // OR-TOOLS-VERIFY: com.google.ortools.sat.Literal is the public interface of BoolVar / NotBoolVar (OR-Tools 9.15 Java).
}
