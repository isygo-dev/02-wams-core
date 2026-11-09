package eu.isygoit.solver.check;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.response.Metrics;
import eu.isygoit.dto.response.ObjectiveScore;
import eu.isygoit.dto.response.TierValue;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.util.ScaledMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Re-evaluates every objective of a finished solution INDEPENDENTLY of the engine and aggregates the per-tier values
 * (design 8.2 step 4).
 * <p>
 * Per objective the family evaluator returns the exact term (before weight), with the same {@link ScaledMath}
 * arithmetic the engines use. The evaluator reports the human-facing, sign-normalised magnitude: a maximisation term
 * (family {@code reward() == true}) returns the positive reward. The internal tier value, which is what the engines
 * minimise and report in {@code Metrics.tiers}, is {@code weight * term} for a minimisation family and
 * {@code -weight * term} for a reward family; SOFT constraint penalties ({@code weight * measure}) are added to their
 * constraint tier by {@link Aggregate#addSoftPenalty}. Tier -2 (relaxation cost) is never compared: harmless dropped
 * constraints still cost, so it cannot be recomputed from the plan.
 * <p>
 * Stateless; an {@link Aggregate} is single threaded (one per validation).
 */
public final class ObjectiveAggregator {

    /**
     * Objective types whose term is built from DECIMAL inputs (cost per tick/use, slot cost) and therefore carries the
     * objective scale; every other family reports exact integer measures (ticks, counts), see design 4.4.
     */
    public static final Set<String> DECIMAL_TYPES = Set.of("minimize_resource_cost", "minimize_slot_cost");

    /** Lowest tier compared with the engine (tier -2 is the relaxation cost). */
    public static final int FIRST_COMPARABLE_TIER = -1;

    /** One tier whose recomputed value differs from the value reported by the engine. */
    public record Mismatch(int tier, long computed, long reported) {
    }

    /** Result of one aggregation: per-objective scores and the internal tier totals. */
    public static final class Aggregate {
        private final List<ObjectiveScore> scores;
        private final SortedMap<Integer, Long> tiers = new TreeMap<>();

        Aggregate(List<ObjectiveScore> scores) {
            this.scores = scores;
        }

        /** @return one score per objective, in objective (request) order */
        public List<ObjectiveScore> scores() {
            return Collections.unmodifiableList(scores);
        }

        /** @return internal tier values (minimised sense), ascending by tier; unmodifiable view */
        public SortedMap<Integer, Long> tierTotals() {
            return Collections.unmodifiableSortedMap(tiers);
        }

        void add(int tier, long value) {
            tiers.merge(tier, value, ScaledMath::addExact);
        }

        /**
         * Adds the penalty of a SOFT constraint to its tier.
         *
         * @param tier    the constraint tier
         * @param weight  the constraint weight
         * @param measure the total measure reported by its checker
         */
        public void addSoftPenalty(int tier, long weight, long measure) {
            add(tier, ScaledMath.mulExact(weight, measure));
        }

        /**
         * Compares the recomputed tier values with the engine's {@code Metrics.tiers}: exact integers, zero tolerance,
         * tiers {@code >= -1} that the engine reports as solved. Tiers the engine did not report are skipped.
         *
         * @param metrics engine metrics (may be {@code null})
         * @return mismatches, ascending by tier (empty when everything agrees or there is nothing to compare)
         */
        public List<Mismatch> compareWith(Metrics metrics) {
            List<Mismatch> out = new ArrayList<>();
            if (metrics == null) {
                return out;
            }
            List<TierValue> reported = new ArrayList<>(metrics.tiers());
            reported.sort((a, b) -> Integer.compare(a.tier(), b.tier()));
            for (TierValue tv : reported) {
                if (tv.tier() < FIRST_COMPARABLE_TIER || "NOT_SOLVED".equals(tv.status())) {
                    continue;
                }
                long computed = tiers.getOrDefault(tv.tier(), 0L);
                if (computed != tv.value()) {
                    out.add(new Mismatch(tv.tier(), computed, tv.value()));
                }
            }
            return out;
        }
    }

    /**
     * Evaluates all objectives.
     *
     * @param ctx        check context (problem, index, solution)
     * @param objectives the compiled objectives (request order)
     * @return the aggregate (scores + tier totals, soft penalties still to be added)
     */
    public Aggregate aggregate(CheckContext ctx, List<CompiledObjective> objectives) {
        List<ObjectiveScore> scores = new ArrayList<>(objectives.size());
        Aggregate aggregate = new Aggregate(scores);
        for (CompiledObjective compiled : objectives) {
            Objective objective = compiled.objective();
            long raw = compiled.family().evaluator().evaluate(ctx, objective, compiled.params());
            boolean reward = compiled.family().reward();
            boolean decimal = DECIMAL_TYPES.contains(
                    String.valueOf(objective.type()).trim().toLowerCase(java.util.Locale.ROOT));
            double score = decimal ? ScaledMath.unscale(raw, compiled.scale()) : (double) raw;
            double contribution = objective.weight() * score;
            scores.add(new ObjectiveScore(objective.id(), objective.type(), compiled.tier(), objective.weight(),
                    raw, score, contribution));
            long internal = ScaledMath.mulExact(objective.weight(), raw);
            aggregate.add(compiled.tier(), reward ? -internal : internal);
        }
        return aggregate;
    }
}
