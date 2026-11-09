package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.solver.index.CandidateIndex;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Solution hints of the CP-SAT model (design 6.11). Hints are advisory: the solver repairs or ignores invalid ones
 * ({@code repair_hint}), so nothing here can make a model infeasible.
 * <ul>
 *   <li>{@link #applyInitial}: the NON-locked {@code initialSolution} assignments (start tick, selection, candidate literals
 *   of the listed resources) and {@code active_k = 1} for every relaxation literal. Locked assignments are fixed by the
 *   builder, not hinted.</li>
 *   <li>{@link #applyFromSolver}: the incumbent of a previous solve (tier to tier hand-over of the SEQUENTIAL mode): every
 *   start, selection and candidate literal.</li>
 * </ul>
 * A variable is hinted at most once (CP-SAT rejects duplicate hint variables); literals that alias the presence literal of
 * their task or the constant true literal are therefore hinted through the presence literal only.
 * <p>
 * Stateless utility; building / solving thread only. Units: ticks.
 */
public final class HintApplier {

    private HintApplier() {
    }

    /**
     * Applies the initial hints of the problem.
     *
     * @param ctx    the model context (variables are read from it)
     * @param relax  the relaxation literals to hint to "kept" (may be empty)
     * @return true when at least one hint was added
     */
    public static boolean applyInitial(CpModelContext ctx, List<CpSatEnforcerFactory.RelaxLiteral> relax) {
        CpModel model = ctx.model();
        CandidateIndex index = ctx.index();
        Set<Integer> hinted = new HashSet<>();
        boolean[] startHinted = new boolean[index.taskCount()];
        boolean any = false;
        InitialSolution initial = ctx.problem().initialSolution();
        if (initial != null) {
            for (InitialAssignment assignment : initial.assignments()) {
                if (assignment.locked()) {
                    continue;
                }
                int t = index.taskIdx(assignment.taskId());
                if (t < 0 || t >= index.taskCount()) {
                    continue;
                }
                if (assignment.start() != null && !startHinted[t]) {
                    int tick = tickOrMinusOne(index, assignment);
                    if (tick >= 0) {
                        model.addHint(ctx.start(t), tick);
                        startHinted[t] = true;
                        any = true;
                    }
                }
                any |= hint(ctx, hinted, ctx.presence(t), true);
                if (!assignment.resourceIds().isEmpty()) {
                    Set<Integer> chosen = new HashSet<>();
                    for (String resourceId : assignment.resourceIds()) {
                        int r = index.resourceIdx(resourceId);
                        if (r >= 0) {
                            chosen.add(r);
                        }
                    }
                    for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                        any |= hint(ctx, hinted, ctx.cand(c), chosen.contains(index.candidateResource(c)));
                    }
                }
            }
        }
        for (CpSatEnforcerFactory.RelaxLiteral literal : relax) {
            any |= hint(ctx, hinted, literal.active(), true);
        }
        return any;
    }

    /**
     * Replaces the hints by the incumbent of a solver (used between the tiers of the SEQUENTIAL mode).
     *
     * @param ctx    the model context
     * @param solver the solver that holds the incumbent
     * @param relax  the relaxation literals (hinted to their incumbent value)
     * @return true when at least one hint was added
     */
    public static boolean applyFromSolver(CpModelContext ctx, CpSolver solver,
                                          List<CpSatEnforcerFactory.RelaxLiteral> relax) {
        CpModel model = ctx.model();
        CandidateIndex index = ctx.index();
        model.clearHints();
        Set<Integer> hinted = new HashSet<>();
        boolean any = false;
        for (int t = 0; t < index.taskCount(); t++) {
            model.addHint(ctx.start(t), solver.value(ctx.start(t)));
            any = true;
            Literal presence = ctx.presence(t);
            any |= hint(ctx, hinted, presence, solver.booleanValue(presence));
            for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                Literal literal = ctx.cand(c);
                any |= hint(ctx, hinted, literal, solver.booleanValue(literal));
            }
        }
        for (CpSatEnforcerFactory.RelaxLiteral literal : relax) {
            any |= hint(ctx, hinted, literal.active(), solver.booleanValue(literal.active()));
        }
        return any;
    }

    private static int tickOrMinusOne(CandidateIndex index, InitialAssignment assignment) {
        try {
            return index.horizon().toTick(assignment.start());
        } catch (RuntimeException outsideOrUnaligned) {
            return -1;
        }
    }

    /** Adds one literal hint unless the literal is the constant true literal or its variable was already hinted. */
    private static boolean hint(CpModelContext ctx, Set<Integer> hinted, Literal literal, boolean value) {
        if (literal == ctx.trueLit()) {
            return false;
        }
        int rawIndex = literal.getIndex();
        boolean negated = rawIndex < 0;
        int variable = negated ? -rawIndex - 1 : rawIndex;
        if (!hinted.add(variable)) {
            return false;
        }
        if (negated) {
            ctx.model().addHint(literal.not(), !value);
        } else {
            ctx.model().addHint(literal, value);
        }
        return true;
    }
}
