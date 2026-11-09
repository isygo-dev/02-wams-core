package eu.isygoit.dto.response;

/**
 * Size of the model the engine built.
 *
 * @param tasks             tasks in the model
 * @param resources         resources in the model
 * @param candidates        surviving (task, requirement, resource) candidates
 * @param boolVars          boolean variables
 * @param intVars           integer variables
 * @param constraints       constraints
 * @param intervals         interval variables
 * @param optionalIntervals optional interval variables
 * @param circuitArcs       setup circuit arcs
 * @param softLiterals      literals of soft penalties
 * @param relaxLiterals     literals of relaxable constraints
 */
public record ModelStats(
        int tasks,
        int resources,
        int candidates,
        int boolVars,
        int intVars,
        int constraints,
        int intervals,
        int optionalIntervals,
        int circuitArcs,
        int softLiterals,
        int relaxLiterals) {
}
