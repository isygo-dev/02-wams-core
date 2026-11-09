package eu.isygoit.solver.timefold;

import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.EngineCapabilities;
import eu.isygoit.solver.EngineCapabilities.Feature;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.registry.TimefoldContributorRegistry;
import eu.isygoit.solver.spi.ConstraintFamily;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The documented capability matrix of the Timefold subset (design 4.5) and its derivation from the registries (5.6).
 * <p>
 * {@link #FEATURES} is the EXACT list of what the Timefold formulation supports; every {@link Feature} not in it makes
 * {@code ProblemVerifier} reject a request that needs it ({@code ENGINE_UNSUPPORTED_FEATURE}); nothing is silently
 * ignored. Supported: slot mode (the only mode), variable durations (used for slot fit only), cumulative demand (per
 * (resource, slot) bucket, slots must be disjoint), optional tasks, time windows / calendars / pins (neutral pruning of
 * options), deterministic mode (single move thread + seed) and progress events. NOT supported: free start, resource
 * durations, precedences, setups, multi-resource, initial solution hint, locked assignments, lexicographic tiers,
 * relaxation, assumption explanation, cost per use, max load ticks.
 * <p>
 * Utility class; the derived value is immutable. Thread-safe.
 */
public final class TimefoldCapabilities {

    /** The engine id. */
    public static final String ENGINE_ID = "timefold";

    /** The exact supported feature set (design 4.5). */
    public static final Set<Feature> FEATURES = Collections.unmodifiableSet(EnumSet.of(
            Feature.SLOT_MODE,
            Feature.DURATIONS_VARIABLE,
            Feature.CUMULATIVE_DEMAND,
            Feature.OPTIONAL_TASKS,
            Feature.TIME_WINDOWS,
            Feature.CALENDARS,
            Feature.PINS,
            Feature.DETERMINISTIC,
            Feature.PROGRESS));

    private TimefoldCapabilities() {
    }

    /**
     * Derives the capabilities: constraints = {@code family.supportedEnforcements() AND contributor.support()} for every
     * contributor that has a family (or, when {@code families} is {@code null}, the contributor support alone);
     * objectives = the contributor types that have an objective family (or all contributor types when
     * {@code objectiveFamilies} is {@code null}). {@code provesOptimality} is always {@code false}.
     *
     * @param contributors     the Timefold contributor registry (required)
     * @param families         the constraint family registry, may be {@code null}
     * @param objectiveFamilies the objective family registry, may be {@code null}
     * @return the capabilities of engine {@code timefold}
     */
    public static EngineCapabilities of(TimefoldContributorRegistry contributors, ConstraintFamilyRegistry families,
                                        ObjectiveFamilyRegistry objectiveFamilies) {
        Map<String, EnforcementSupport> constraints = new TreeMap<>();
        for (String type : contributors.constraintTypes()) {
            EnforcementSupport support = contributors.constraint(type).map(TimefoldConstraintContributor::support)
                    .orElse(EnforcementSupport.NONE);
            if (families != null) {
                ConstraintFamily family = families.find(type).orElse(null);
                if (family == null) {
                    continue;
                }
                support = support.and(familySupport(family));
            }
            if (support.hard() || support.soft() || support.relaxable()) {
                constraints.put(type, support);
            }
        }
        Set<String> objectives = new TreeSet<>();
        for (String type : contributors.objectiveTypes()) {
            if (objectiveFamilies == null || objectiveFamilies.contains(type)) {
                objectives.add(type);
            }
        }
        return new EngineCapabilities(ENGINE_ID, FEATURES, constraints, objectives, false);
    }

    private static EnforcementSupport familySupport(ConstraintFamily family) {
        Set<IEnumEnforcement.Types> supported = family.supportedEnforcements();
        boolean hard = supported.contains(IEnumEnforcement.Types.STRICTLY_REQUIRED)
                || supported.contains(IEnumEnforcement.Types.CONFIGURABLE_HARD);
        return new EnforcementSupport(hard, supported.contains(IEnumEnforcement.Types.SOFT),
                supported.contains(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION));
    }
}
