package eu.isygoit.dto.response;

/**
 * Value of one lexicographic tier. Tier -2 is the relaxation cost, tier -1 the automatic selection value, tiers
 * 0..7 the user tiers. {@code value} is the MINIMISED internal tier value (maximisation tiers are negative);
 * {@link ObjectiveScore} carries the human-facing values.
 *
 * @param tier   tier number
 * @param value  minimised internal value
 * @param bound  bound of the tier when known (sequential mode), else null
 * @param status OPTIMAL | FEASIBLE | NOT_SOLVED
 */
public record TierValue(int tier, long value, Double bound, String status) {
}
