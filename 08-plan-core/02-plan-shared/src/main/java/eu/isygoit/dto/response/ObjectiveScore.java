package eu.isygoit.dto.response;

/**
 * The independently evaluated value of one objective.
 *
 * @param objectiveId  the objective
 * @param type         objective family id
 * @param tier         lexicographic tier of the objective
 * @param weight       weight of the objective
 * @param rawValue     exact scaled integer value of the term (sign-normalised: maximisation objectives report the
 *                     positive reward)
 * @param score        {@code rawValue / scale} for decimal objectives, {@code rawValue} for integral ones (what a
 *                     human reads: 2.0, 190.0, 16.0)
 * @param contribution {@code weight * score}
 */
public record ObjectiveScore(
        String objectiveId,
        String type,
        int tier,
        int weight,
        long rawValue,
        double score,
        double contribution) {
}
