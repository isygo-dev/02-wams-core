package eu.isygoit.solver;

import eu.isygoit.solver.cpsat.EnforcementSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What an engine supports (design 5.6 / 4.5): static features plus per-type constraint/objective support derived from
 * the contributor registries (family-supported AND contributor-supported, only when a contributor exists).
 * <p>
 * Created by the engines ({@code CpSatSolverEngine.capabilities()}, {@code TimefoldSolverEngine.capabilities()}); read
 * by {@code ProblemVerifier}, {@code FeatureScanner} and the capabilities endpoint. Immutable: all collections are
 * unmodifiable, maps/sets are sorted for deterministic output. Thread-safe.
 *
 * @param engineId         the canonical engine id ({@code cp_sat} | {@code timefold})
 * @param features         the supported features
 * @param constraints      constraint type to supported enforcements
 * @param objectives       objective types that have a contributor
 * @param provesOptimality {@code true} when the engine can report OPTIMAL / INFEASIBLE proofs (cp_sat), {@code false} for Timefold
 */
public record EngineCapabilities(
        String engineId,
        Set<Feature> features,
        Map<String, EnforcementSupport> constraints,
        Set<String> objectives,
        boolean provesOptimality) {

    /** Features a problem can require (design 4.5). */
    public enum Feature {
        FREE_START,
        SLOT_MODE,
        /** Non-slot duration. */
        DURATIONS_VARIABLE,
        RESOURCE_DURATIONS,
        /** Capacity greater than 1 together with demand greater than 1. */
        CUMULATIVE_DEMAND,
        OPTIONAL_TASKS,
        PRECEDENCES,
        TIME_WINDOWS,
        CALENDARS,
        SETUPS,
        /** Requirement count greater than 1, or more than one requirement. */
        MULTI_RESOURCE,
        PINS,
        /** Warm start hint. */
        INITIAL_SOLUTION,
        LOCKED,
        /** More than one tier. */
        LEXICOGRAPHIC,
        RELAXATION,
        EXPLAIN_ASSUMPTIONS,
        DETERMINISTIC,
        PROGRESS,
        COST_PER_USE,
        MAX_LOAD_TICKS
    }

    public EngineCapabilities {
        features = features == null || features.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(features));
        constraints = constraints == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new TreeMap<>(constraints));
        objectives = objectives == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new TreeSet<>(objectives));
    }

    /** @return {@code true} when the engine supports the feature */
    public boolean supports(Feature f) {
        return features.contains(f);
    }

    /**
     * @param allKnownTypes every constraint type of the catalogue
     * @return the types from {@code allKnownTypes} for which this engine has no contributor (sorted, unmodifiable)
     */
    public Set<String> unsupportedConstraintTypes(Collection<String> allKnownTypes) {
        return missing(allKnownTypes, constraints.keySet());
    }

    /**
     * @param allKnownTypes every objective type of the catalogue
     * @return the types from {@code allKnownTypes} for which this engine has no contributor (sorted, unmodifiable)
     */
    public Set<String> unsupportedObjectiveTypes(Collection<String> allKnownTypes) {
        return missing(allKnownTypes, objectives);
    }

    private static Set<String> missing(Collection<String> known, Set<String> supported) {
        Set<String> result = new TreeSet<>();
        if (known != null) {
            for (String type : known) {
                if (!supported.contains(type)) {
                    result.add(type);
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
