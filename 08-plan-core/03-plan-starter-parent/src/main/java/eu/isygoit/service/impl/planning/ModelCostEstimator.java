package eu.isygoit.service.impl.planning;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.response.ModelEstimateDto;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.index.SelectorEvaluator;
import eu.isygoit.solver.spi.ModelCost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Model size estimation BEFORE any solver model is built (design 7.3).
 * <p>
 * Order, so that no oversized allocation ever happens: (1) {@link #candidateUpperBound} from the request and the cached
 * selector bitsets (O(T + R)); (2) the index builder counts real candidates while enumerating and aborts at
 * {@code max-candidates}; (3) {@link #estimate} adds the structural cost of the index, the family estimates of every
 * active constraint and the objective estimates; (4) {@link #enforce} compares each component with its limit and throws
 * {@link LimitExceededException} naming the limit key and the actual value.
 * <p>
 * Stateless apart from the (immutable) limits, thread-safe.
 */
@Component
public class ModelCostEstimator {

    private final PlanLimitsProperties limits;

    /**
     * Creates the estimator.
     *
     * @param limits the configured limits
     */
    @Autowired
    public ModelCostEstimator(PlanLimitsProperties limits) {
        this.limits = limits;
    }

    /** Estimator with the default limits (tests, programmatic use). */
    public ModelCostEstimator() {
        this(new PlanLimitsProperties());
    }

    /**
     * Upper bound of the candidates: the sum over tasks and requirements of the number of resources the selector
     * selects. It ignores capacity, calendar and constraint pruning, so it never under-estimates.
     *
     * @param tasks     the tasks
     * @param resources the resources
     * @return the bound, O(T + R) with one evaluation per distinct selector
     */
    public static long candidateUpperBound(List<Task> tasks, List<Resource> resources) {
        SelectorEvaluator selectors = new SelectorEvaluator(resources);
        long total = 0;
        for (Task task : tasks) {
            for (ResourceRequirement requirement : task.effectiveRequirements()) {
                total += selectors.cardinality(requirement.selector());
            }
        }
        return total;
    }

    /**
     * Structural cost of the index (before families and objectives): booleans = candidates + optional presence + slot
     * start choices, integers = one start per task, intervals = candidate intervals of resources with at least two
     * candidates + one master interval per task, constraints = candidates + edges + tasks + resources, circuit arcs of
     * resources with a setup matrix (n squared bound).
     *
     * @param index the built index
     * @return the cost
     */
    public ModelCost structural(CandidateIndex index) {
        long optional = 0;
        for (int t = 0; t < index.taskCount; t++) {
            if (!index.required[t]) {
                optional++;
            }
        }
        long intervals = index.taskCount;
        long arcs = 0;
        for (int r = 0; r < index.resourceCount; r++) {
            long n = index.resCandStart[r + 1] - index.resCandStart[r];
            if (n >= 2) {
                intervals += n;
            }
            if (index.resSetupMatrix[r] >= 0) {
                arcs = saturatingAdd(arcs, n * n + 3 * n);
            }
        }
        long boolVars = (long) index.candidateCount + optional + index.taskSlots.length;
        long constraints = (long) index.candidateCount + index.edgeCount + index.taskCount + index.resourceCount;
        return new ModelCost(boolVars, index.taskCount, constraints, intervals, arcs, 0);
    }

    /**
     * Total cost: structural + sum of the family estimates of the active constraints + sum of the objective estimates.
     *
     * @param index       the built index
     * @param constraints compiled constraints (inactive, i.e. rewritten, ones cost nothing)
     * @param objectives  compiled objectives
     * @return the total cost
     */
    public ModelCost estimate(CandidateIndex index, List<CompiledConstraint> constraints,
                              List<CompiledObjective> objectives) {
        ModelCost total = structural(index);
        for (CompiledConstraint cc : constraints) {
            if (cc.active()) {
                total = total.plus(cc.family().estimate(index, cc.constraint(), cc.params()));
            }
        }
        for (CompiledObjective co : objectives) {
            total = total.plus(co.family().estimate(index, co.objective(), co.params()));
        }
        return total;
    }

    /**
     * Compares the estimate with the limits.
     *
     * @param cost  the total cost
     * @param index the built index (candidates and edges are exact)
     * @throws LimitExceededException naming the first exceeded limit key ({@code max-candidates}, {@code max-variables},
     *                                {@code max-intervals}, {@code max-constraint-instances}, {@code max-setup-arcs},
     *                                {@code max-pairwise}, {@code max-precedences})
     */
    public void enforce(ModelCost cost, CandidateIndex index) {
        check("max-candidates", index.candidateCount, limits.getMaxCandidates());
        check("max-precedences", index.edgeCount, limits.getMaxPrecedences());
        check("max-variables", saturatingAdd(cost.boolVars(), cost.intVars()), limits.getMaxVariables());
        check("max-intervals", cost.intervals(), limits.getMaxIntervals());
        check("max-constraint-instances", cost.constraints(), limits.getMaxConstraintInstances());
        check("max-setup-arcs", cost.circuitArcs(), limits.getMaxSetupArcs());
        check("max-pairwise", cost.pairwise(), limits.getMaxPairwise());
    }

    /**
     * Report form for {@code POST /validate}.
     *
     * @param cost        the total cost
     * @param index       the built index
     * @param constraints compiled constraints
     * @param objectives  compiled objectives
     * @return the estimate DTO (candidates, intervals, variables, constraints, distinct tiers, horizon ticks)
     */
    public ModelEstimateDto toDto(ModelCost cost, CandidateIndex index, List<CompiledConstraint> constraints,
                                  List<CompiledObjective> objectives) {
        return ModelEstimateDto.builder()
                .candidates(index.candidateCount)
                .intervals(cost.intervals())
                .variables(saturatingAdd(cost.boolVars(), cost.intVars()))
                .constraints(cost.constraints())
                .tiers(distinctTiers(constraints, objectives).size())
                .horizonTicks(index.horizonTicks)
                .build();
    }

    /**
     * Distinct lexicographic tiers in use: objectives with weight above zero, SOFT constraints, and the reserved
     * relaxation tier -2 when a relaxable constraint is authorized.
     *
     * @param constraints compiled constraints
     * @param objectives  compiled objectives
     * @return the distinct tiers
     */
    public static Set<Integer> distinctTiers(List<CompiledConstraint> constraints, List<CompiledObjective> objectives) {
        Set<Integer> tiers = new HashSet<>();
        for (CompiledObjective o : objectives) {
            if (o.objective().weight() > 0) {
                tiers.add(o.tier());
            }
        }
        for (CompiledConstraint c : constraints) {
            if (!c.active()) {
                continue;
            }
            IEnumEnforcement.Types e = c.constraint().enforcement();
            if (e == IEnumEnforcement.Types.SOFT) {
                tiers.add(c.constraint().tier() == null ? 0 : c.constraint().tier());
            } else if (e == IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION
                    && c.constraint().relaxation() != null && c.constraint().relaxation().authorized()) {
                tiers.add(-2);
            }
        }
        return tiers;
    }

    private static void check(String name, long actual, long max) {
        if (actual > max) {
            throw new LimitExceededException(name, actual, max);
        }
    }

    private static long saturatingAdd(long a, long b) {
        long sum = a + b;
        return ((a ^ sum) & (b ^ sum)) < 0 ? Long.MAX_VALUE : sum;
    }
}
