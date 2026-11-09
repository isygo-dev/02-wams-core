package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import com.google.ortools.util.Domain;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.registry.CpSatContributorRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Builds the CP-SAT model of a compiled problem in the order of design 6.1 (the structural model of 6.4, the
 * {@link CpSatStructuralModule} plug-ins, the catalogue constraints and objectives through their contributors, the resource
 * models, the lexicographic objective and the hints).
 * <pre>
 *  1 createTimeVariables       start, presence, start domains (from the index union runs; slot starts)
 *  2 createCandidates          candidate literals, requirement counts, durations, end expressions
 *  3 (resource models)         intervals are created lazily and collected at step 11
 *  4 applyCandidateStartDomains per-candidate calendar domains, slot start choices
 *  5 applyPrecedenceEdges      Task.predecessors through PrecedenceEmitter
 *  6 applyPinsLocksMaxLoad     partial pins, locked assignments, maxLoadTicks
 *  7 structural modules        ascending order()
 *  9 constraints               one Enforcer per constraint (HARD / RELAXABLE / SOFT / explain assumption)
 * 10 objectives                contributors feed the tier sink
 * 11 resource models           NoOverlap / Cumulative incl. the blocks added by families
 * 12 objective                 tiers: WEIGHTED minimize, or SEQUENTIAL handled by the SolveRunner
 * 13 hints                     initial solution and relaxation literals
 * </pre>
 * In EXPLAIN mode the soft constraints, the objectives, the relaxation costs and the hints are omitted, start domains are
 * widened to the horizon and every blameable fact (constraint, task window/pin/lock, resource calendar/max load, precedence
 * edge) becomes an assumption literal ({@code BuiltModel.assumptions()}).
 * <p>
 * Thread-safety: stateless apart from immutable configuration; one {@link #build} call per thread. Units: ticks.
 * OR-TOOLS-VERIFY: all OR-Tools calls were checked against the 9.15.6755 jar (javap); the model must be built AFTER the
 * native libraries are loaded ({@code Domain} is a JNI object).
 */
@Component
public final class CpModelBuilder {

    private static final long[] NO_RUNS = new long[0];
    private static final int GROUP_EDGES_ABOVE = 5000;

    private final CpSatContributorRegistry contributors;
    private final List<CpSatStructuralModule> modules;
    private final CpSatProperties properties;

    /**
     * @param contributors the CP-SAT contributor registry
     * @param modules      the structural modules (sorted here by {@code order()} then {@code name()})
     * @param properties   the CP-SAT properties
     */
    public CpModelBuilder(CpSatContributorRegistry contributors, Collection<CpSatStructuralModule> modules,
                          CpSatProperties properties) {
        this.contributors = contributors;
        List<CpSatStructuralModule> sorted = new ArrayList<>(modules == null ? List.of() : modules);
        sorted.sort(Comparator.comparingInt(CpSatStructuralModule::order).thenComparing(CpSatStructuralModule::name));
        this.modules = List.copyOf(sorted);
        this.properties = properties == null ? new CpSatProperties() : properties;
    }

    /** @return the CP-SAT contributor registry (source of the engine capabilities) */
    public CpSatContributorRegistry contributors() {
        return contributors;
    }

    /**
     * Builds the model.
     *
     * @param problem the compiled problem (index, constraints, objectives)
     * @param context the solve context (cancellation, time budget, options)
     * @param mode    SOLVE or EXPLAIN
     * @return the built model
     * @throws eu.isygoit.solver.SolveCancelledException when a stop was requested or the time budget ran out while building
     * @throws LimitExceededException                    when the model exceeds a size guard (design 6.13)
     */
    public BuiltModel build(CompiledProblem problem, SolveContext context, BuildMode mode) {
        long started = System.nanoTime();
        BuildMode effective = mode == null ? BuildMode.SOLVE : mode;
        CpModelContext ctx = new CpModelContext(problem, context, effective, properties);
        CandidateIndex index = ctx.index;
        ctx.checkpoint();
        ctx.guard("max-candidates", index.candidateCount(), index.limits().getMaxCandidates());
        ctx.guard("max-precedences", index.edgeCount, index.limits().getMaxPrecedences());

        createTimeVariables(ctx);                                  // 1
        createCandidates(ctx);                                     // 2
        ctx.checkpoint();
        applyCandidateStartDomains(ctx);                           // 4
        applyPrecedenceEdges(ctx);                                 // 5
        applyPinsLocksMaxLoad(ctx);                                // 6
        for (CpSatStructuralModule module : modules) {             // 7
            ctx.checkpoint();
            module.apply(ctx);
        }

        SolverOptions options = ctx.options();
        TierObjectiveBuilder tiers = new TierObjectiveBuilder(index.limits().getMaxTiers());
        CpSatEnforcerFactory factory = new CpSatEnforcerFactory(ctx.model, tiers, ctx.assumptions, options,
                newBools -> ctx.count(newBools, 0, 0, 0));
        List<String> explainSkipped = new ArrayList<>();
        applyConstraints(ctx, problem, factory, explainSkipped);   // 9
        if (effective == BuildMode.SOLVE) {
            applyObjectives(ctx, problem, tiers);                  // 10
        }
        ResourceModelBuilder.finalizeModels(ctx);                  // 11
        TierObjectiveBuilder.Plan plan = null;
        boolean hasObjective = false;
        if (effective == BuildMode.SOLVE && !tiers.isEmpty()) {    // 12
            plan = tiers.plan(options.lexicographicMode());
            if (plan.mode() == SolverOptions.LexicographicMode.WEIGHTED) {
                ctx.model.minimize(tiers.weightedObjective(plan));
                hasObjective = true;
            }
        }
        boolean hasHints = false;
        if (effective == BuildMode.SOLVE) {                        // 13
            hasHints = HintApplier.applyInitial(ctx, factory.relaxLiterals());
        } else {
            ctx.assumptions.install();
        }
        ctx.softLiterals = factory.softInstances().size();
        ctx.relaxLiterals = factory.relaxLiterals().size();
        return new BuiltModel(ctx, effective, tiers, plan, factory.softInstances(), factory.relaxLiterals(),
                explainSkipped, hasHints, hasObjective, System.nanoTime() - started);
    }

    // ============================================================================================ step 1

    private void createTimeVariables(CpModelContext ctx) {
        CandidateIndex index = ctx.index;
        CpModel model = ctx.model;
        int tasks = index.taskCount();
        ctx.trueLit = model.newBoolVar(ctx.named ? "true" : "");
        model.addEquality(ctx.trueLit, 1);
        ctx.count(1, 0, 1, 0);
        ctx.start = new IntVar[tasks];
        ctx.presence = new Literal[tasks];
        ctx.startDomain = new long[tasks][];
        ctx.slotValues = new long[tasks][];
        ctx.slotOfValue = new int[tasks][];
        ctx.startChoices = new Literal[tasks][];
        ctx.durVar = new IntVar[tasks];
        ctx.commonDuration = new int[tasks];
        ctx.endExpr = new LinearExpr[tasks];
        for (int t = 0; t < tasks; t++) {
            ctx.checkpoint(t);
            ctx.presence[t] = index.required[t] ? ctx.trueLit : model.newBoolVar(ctx.named ? "p_" + t : "");
            if (!index.required[t]) {
                ctx.count(1, 0, 0, 0);
            }
            long[] domain = baseDomain(ctx, t);
            readSlotValues(ctx, t, domain);
            if (ctx.slotValues[t] != null) {
                domain = runsOfValues(ctx.slotValues[t]);
            }
            ctx.startDomain[t] = domain;
            IntVar variable;
            String label = ctx.named ? "s_" + t : "";
            if (domain.length == 0) {
                variable = model.newIntVar(0, 0, label);
                forbidTask(ctx, t);
            } else if (domain.length == 2) {
                variable = model.newIntVar(domain[0], domain[1], label);
            } else {
                variable = model.newIntVarFromDomain(Domain.fromFlatIntervals(domain), label);
            }
            ctx.start[t] = variable;
            ctx.count(0, 1, 0, 0);
        }
    }

    /** The union start domain as inclusive runs: the index union (SOLVE) or {@code [0, H - minDuration]} (EXPLAIN). */
    private static long[] baseDomain(CpModelContext ctx, int t) {
        CandidateIndex index = ctx.index;
        if (ctx.explainMode()) {
            long hi = (long) index.horizonTicks() - minDuration(index, t);
            return hi < 0 ? NO_RUNS : new long[]{0, hi};
        }
        int from = index.taskStartRunStart[t];
        int to = index.taskStartRunStart[t + 1];
        if (to > from) {
            long[] runs = new long[to - from];
            for (int i = 0; i < runs.length; i++) {
                runs[i] = index.taskStartRuns[from + i];
            }
            return runs;
        }
        long lo = index.estStart(t);
        long hi = index.lstStart(t);
        return lo <= hi ? new long[]{lo, hi} : NO_RUNS;
    }

    /** Slot mode: sorted distinct allowed start values and their slots (only when the task lists slots). */
    private static void readSlotValues(CpModelContext ctx, int t, long[] domain) {
        CandidateIndex index = ctx.index;
        int from = index.taskSlotStart[t];
        int to = index.taskSlotStart[t + 1];
        if (to <= from) {
            return;
        }
        int minDuration = minDuration(index, t);
        TreeMap<Long, Integer> byValue = new TreeMap<>();
        for (int i = from; i < to; i++) {
            int slot = index.taskSlots[i];
            long value = index.slotStart[slot];
            if (index.slotEnd[slot] - value < minDuration || !contains(domain, value)) {
                continue;
            }
            byValue.putIfAbsent(value, slot);
        }
        long[] values = new long[byValue.size()];
        int[] slots = new int[byValue.size()];
        int k = 0;
        for (Map.Entry<Long, Integer> entry : byValue.entrySet()) {
            values[k] = entry.getKey();
            slots[k] = entry.getValue();
            k++;
        }
        ctx.slotValues[t] = values;
        ctx.slotOfValue[t] = slots;
    }

    private static int minDuration(CandidateIndex index, int t) {
        int begin = index.candidateBegin(t);
        int end = index.candidateEnd(t);
        if (begin == end) {
            return index.duration(t);
        }
        int min = Integer.MAX_VALUE;
        for (int c = begin; c < end; c++) {
            min = Math.min(min, index.candidateDuration(c));
        }
        return min;
    }

    /** Makes a task impossible: required tasks make the model infeasible, optional tasks are forced absent. */
    private static void forbidTask(CpModelContext ctx, int t) {
        Literal presence = ctx.presence[t];
        if (presence == ctx.trueLit) {
            ctx.model.addBoolOr(new Literal[]{ctx.trueLit.not()});
        } else {
            ctx.model.addEquality(presence, 0);
        }
        ctx.count(0, 0, 1, 0);
    }

    // ============================================================================================ step 2

    private void createCandidates(CpModelContext ctx) {
        CandidateIndex index = ctx.index;
        CpModel model = ctx.model;
        int tasks = index.taskCount();
        ctx.cand = new Literal[index.candidateCount()];
        for (int t = 0; t < tasks; t++) {
            ctx.checkpoint(t);
            int begin = index.candidateBegin(t);
            int end = index.candidateEnd(t);
            Literal presence = ctx.presence[t];
            boolean required = presence == ctx.trueLit;
            boolean forbidden = false;
            for (int q = index.taskReqStart[t]; q < index.taskReqStart[t + 1]; q++) {
                int[] members = candidatesOfRequirement(index, begin, end, q);
                int n = index.reqCount[q];
                int k = members.length;
                if (k < n) {
                    for (int c : members) {
                        ctx.cand[c] = presence;
                    }
                    if (!forbidden) {
                        forbidTask(ctx, t);
                        forbidden = true;
                    }
                    continue;
                }
                if ((k == 1 && n == 1) || k == n) {
                    for (int c : members) {
                        ctx.cand[c] = presence;
                    }
                    continue;
                }
                Literal[] literals = new Literal[k];
                for (int i = 0; i < k; i++) {
                    BoolVar variable = model.newBoolVar(ctx.named ? "x_" + members[i] : "");
                    ctx.cand[members[i]] = variable;
                    literals[i] = variable;
                }
                if (required) {
                    if (n == 1) {
                        model.addExactlyOne(literals);
                    } else {
                        model.addLinearConstraint(sum(literals), n, n);
                    }
                } else {
                    LinearExprBuilder balance = LinearExpr.newBuilder().addSum(literals);
                    balance.addTerm(presence, -(long) n);
                    model.addEquality(balance.build(), 0);
                }
                ctx.count(k, 0, 1, 0);
            }
            for (int c = begin; c < end; c++) {
                if (ctx.cand[c] == null) {
                    ctx.cand[c] = presence;
                }
            }
            if (index.taskReqStart[t + 1] - index.taskReqStart[t] >= 2) {
                atMostOnePerResource(ctx, t, begin, end);
            }
            createDuration(ctx, t, begin, end);
        }
    }

    private static int[] candidatesOfRequirement(CandidateIndex index, int begin, int end, int q) {
        int count = 0;
        for (int c = begin; c < end; c++) {
            if (index.candReq[c] == q) {
                count++;
            }
        }
        int[] result = new int[count];
        int i = 0;
        for (int c = begin; c < end; c++) {
            if (index.candReq[c] == q) {
                result[i++] = c;
            }
        }
        return result;
    }

    /** A resource fills at most one requirement of a task. */
    private static void atMostOnePerResource(CpModelContext ctx, int t, int begin, int end) {
        Map<Integer, List<Integer>> byResource = new LinkedHashMap<>();
        for (int c = begin; c < end; c++) {
            byResource.computeIfAbsent(ctx.index.candidateResource(c), key -> new ArrayList<>()).add(c);
        }
        for (List<Integer> candidates : byResource.values()) {
            if (candidates.size() < 2) {
                continue;
            }
            LinearExprBuilder total = LinearExpr.newBuilder();
            for (int c : candidates) {
                total.add(ctx.cand[c]);
            }
            ctx.model.addLinearConstraint(total.build(), 0, 1);
            ctx.count(0, 0, 1, 0);
        }
    }

    private static void createDuration(CpModelContext ctx, int t, int begin, int end) {
        CandidateIndex index = ctx.index;
        IntVar start = ctx.start[t];
        if (begin == end) {
            ctx.commonDuration[t] = index.duration(t);
            ctx.endExpr[t] = LinearExpr.affine(start, 1, index.duration(t));
            return;
        }
        int first = index.candidateDuration(begin);
        boolean same = true;
        for (int c = begin + 1; c < end; c++) {
            if (index.candidateDuration(c) != first) {
                same = false;
                break;
            }
        }
        if (same) {
            ctx.commonDuration[t] = first;
            ctx.endExpr[t] = LinearExpr.affine(start, 1, first);
            return;
        }
        long[] distinct = new long[end - begin];
        for (int c = begin; c < end; c++) {
            distinct[c - begin] = index.candidateDuration(c);
        }
        Arrays.sort(distinct);
        IntVar duration = ctx.model.newIntVarFromDomain(Domain.fromValues(distinct), ctx.named ? "dur_" + t : "");
        ctx.durVar[t] = duration;
        ctx.commonDuration[t] = -1;
        for (int c = begin; c < end; c++) {
            CpModelContext.enforce(ctx.model.addEquality(duration, index.candidateDuration(c)), ctx.cand[c]);
        }
        ctx.endExpr[t] = LinearExpr.newBuilder().add(start).add(duration).build();
        ctx.count(0, 1, end - begin, 0);
    }

    // ============================================================================================ step 4

    private void applyCandidateStartDomains(CpModelContext ctx) {
        CandidateIndex index = ctx.index;
        CpModel model = ctx.model;
        Map<Long, long[]> calendarStarts = new HashMap<>();
        for (int t = 0; t < index.taskCount(); t++) {
            ctx.checkpoint(t);
            if (ctx.slotValues[t] != null) {
                createStartChoice(ctx, t);
            }
            int begin = index.candidateBegin(t);
            int end = index.candidateEnd(t);
            for (int c = begin; c < end; c++) {
                Literal lit = ctx.cand[c];
                if (ctx.explainMode()) {
                    long[] runs = calendarStarts(index, index.candidateResource(c), index.candidateDuration(c),
                            calendarStarts);
                    if (runs == null) {
                        continue;
                    }
                    Literal assumption = ctx.assumptions.literal(AssumptionRegistry.KIND_RESOURCE,
                            index.resourceId(index.candidateResource(c)), "CALENDAR");
                    Literal[] enforcement = lit == ctx.trueLit
                            ? new Literal[]{assumption} : new Literal[]{lit, assumption};
                    if (runs.length == 0) {
                        model.addBoolOr(new Literal[]{lit.not(), assumption.not()});
                    } else {
                        CpModelContext.enforce(model.addLinearExpressionInDomain(ctx.start[t],
                                Domain.fromFlatIntervals(runs)), enforcement);
                    }
                    ctx.count(0, 0, 1, 0);
                    continue;
                }
                int from = index.candStartRunStart[c];
                int to = index.candStartRunStart[c + 1];
                if (to <= from) {
                    continue;
                }
                long[] runs = new long[to - from];
                for (int i = 0; i < runs.length; i++) {
                    runs[i] = index.candStartRuns[from + i];
                }
                CpModelContext.enforce(model.addLinearExpressionInDomain(ctx.start[t], Domain.fromFlatIntervals(runs)),
                        lit == ctx.trueLit ? new Literal[0] : new Literal[]{lit});
                ctx.count(0, 0, 1, 0);
            }
        }
    }

    /**
     * Slot mode: {@code y_k} literals, exactly one true when present ({@code sum y == presence}) and
     * {@code start == sum(value_k * y_k)} when present (definitional). Falls back to the plain domain when the number of
     * allowed starts exceeds {@code max-start-choices}.
     */
    private void createStartChoice(CpModelContext ctx, int t) {
        long[] values = ctx.slotValues[t];
        int k = values.length;
        Literal presence = ctx.presence[t];
        if (k == 0) {
            return;
        }
        Literal[] choice;
        if (k == 1) {
            choice = new Literal[]{presence};
        } else if (k > properties.getMaxStartChoices()) {
            return;
        } else {
            choice = new Literal[k];
            for (int i = 0; i < k; i++) {
                choice[i] = ctx.model.newBoolVar(ctx.named ? "y_" + t + "_" + i : "");
            }
            if (presence == ctx.trueLit) {
                ctx.model.addExactlyOne(choice);
            } else {
                LinearExprBuilder balance = LinearExpr.newBuilder().addSum(choice);
                balance.addTerm(presence, -1);
                ctx.model.addEquality(balance.build(), 0);
            }
            LinearExprBuilder channel = LinearExpr.newBuilder().add(ctx.start[t]);
            for (int i = 0; i < k; i++) {
                channel.addTerm(choice[i], -values[i]);
            }
            CpModelContext.enforce(ctx.model.addEquality(channel.build(), 0),
                    presence == ctx.trueLit ? new Literal[0] : new Literal[]{presence});
            ctx.count(k, 0, 2, 0);
        }
        ctx.startChoices[t] = choice;
        // resource specific durations longer than the slot: that (candidate, slot) pair is impossible
        CandidateIndex index = ctx.index;
        int minimum = minDuration(index, t);
        for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
            int duration = index.candidateDuration(c);
            if (duration <= minimum) {
                continue;
            }
            for (int i = 0; i < k; i++) {
                int slot = ctx.slotOfValue[t][i];
                if (index.slotEnd[slot] - index.slotStart[slot] < duration) {
                    ctx.model.addBoolOr(new Literal[]{ctx.cand[c].not(), choice[i].not()});
                    ctx.count(0, 0, 1, 0);
                }
            }
        }
    }

    /** EXPLAIN: available start runs of a resource for a duration, or null when the resource is always available. */
    private static long[] calendarStarts(CandidateIndex index, int r, int duration, Map<Long, long[]> cache) {
        int from = index.resRunStart[r];
        int to = index.resRunStart[r + 1];
        if (to <= from) {
            return null;
        }
        long key = ((long) r << 32) | (duration & 0xffffffffL);
        long[] cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        List<Long> runs = new ArrayList<>();
        for (int i = from; i + 1 < to; i += 2) {
            long lo = index.resRuns[i];
            long hi = index.resRuns[i + 1];
            if (hi - lo >= duration) {
                runs.add(lo);
                runs.add(hi - duration);
            }
        }
        long[] result = new long[runs.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = runs.get(i);
        }
        cache.put(key, result);
        return result;
    }

    // ============================================================================================ step 5

    private void applyPrecedenceEdges(CpModelContext ctx) {
        CandidateIndex index = ctx.index;
        int edges = index.edgeCount;
        boolean grouped = ctx.explainMode() && edges > GROUP_EDGES_ABOVE;
        PrecedenceRef.Type[] types = PrecedenceRef.Type.values();
        for (int e = 0; e < edges; e++) {
            ctx.checkpoint(e);
            int predecessor = index.edgePred[e];
            int successor = index.edgeSucc[e];
            Literal[] condition = new Literal[0];
            if (ctx.explainMode()) {
                String id = grouped
                        ? "*->" + index.taskId(successor)
                        : index.taskId(predecessor) + "->" + index.taskId(successor);
                condition = new Literal[]{
                        ctx.assumptions.literal(AssumptionRegistry.KIND_EDGE, id, "PRECEDENCE")};
            }
            int maxLag = index.edgeMaxLag[e];
            PrecedenceEmitter.emit(ctx, predecessor, successor, types[index.edgeType[e]], index.edgeMinLag[e],
                    maxLag == Integer.MAX_VALUE ? null : Integer.valueOf(maxLag),
                    index.edgeImpliesPresence[e] != 0 ? PrecedenceRef.Presence.IMPLIES : PrecedenceRef.Presence.WHEN_BOTH,
                    condition);
        }
    }

    // ============================================================================================ step 6

    private void applyPinsLocksMaxLoad(CpModelContext ctx) {
        CandidateIndex index = ctx.index;
        CpModel model = ctx.model;
        if (ctx.explainMode()) {
            explainTaskFacts(ctx);
        }
        // partial pin: the named resource MUST be used (candForced)
        for (int t = 0; t < index.taskCount(); t++) {
            ctx.checkpoint(t);
            for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                if (!index.candForced[c] || ctx.cand[c] == ctx.presence[t]) {
                    continue; // not forced, or aliased with the presence literal (then already used whenever present)
                }
                Literal[] enforcement = ctx.explainMode()
                        ? new Literal[]{ctx.assumptions.literal(AssumptionRegistry.KIND_TASK, index.taskId(t),
                        "PIN_CONFLICT")}
                        : new Literal[0];
                if (ctx.presence[t] == ctx.trueLit) {
                    CpModelContext.enforce(model.addBoolOr(new Literal[]{ctx.cand[c]}), enforcement);
                } else {
                    CpModelContext.enforce(model.addImplication(ctx.presence[t], ctx.cand[c]), enforcement);
                }
                ctx.count(0, 0, 1, 0);
            }
        }
        applyLocks(ctx);
        ResourceModelBuilder.applyMaxLoad(ctx);
    }

    /** EXPLAIN: window (release / deadline) and pin facts of each task as assumption-guarded constraints. */
    private static void explainTaskFacts(CpModelContext ctx) {
        CandidateIndex index = ctx.index;
        CpModel model = ctx.model;
        for (int t = 0; t < index.taskCount(); t++) {
            ctx.checkpoint(t);
            Literal presence = ctx.presence[t];
            String id = index.taskId(t);
            int minDuration = minDuration(index, t);
            long latestStart = (long) index.latestEnd(t) - minDuration;
            boolean windowBinds = index.release(t) > 0 || latestStart < (long) index.horizonTicks() - minDuration;
            if (windowBinds) {
                Literal assumption = ctx.assumptions.literal(AssumptionRegistry.KIND_TASK, id, "WINDOW_EMPTY");
                Literal[] enforcement = presence == ctx.trueLit
                        ? new Literal[]{assumption} : new Literal[]{assumption, presence};
                CpModelContext.enforce(model.addGreaterOrEqual(ctx.start[t], index.release(t)), enforcement);
                CpModelContext.enforce(model.addLessOrEqual(ctx.start[t], latestStart), enforcement);
                ctx.count(0, 0, 2, 0);
            }
            if (index.pinStart(t) >= 0) {
                Literal assumption = ctx.assumptions.literal(AssumptionRegistry.KIND_TASK, id, "PIN_CONFLICT");
                Literal[] enforcement = presence == ctx.trueLit
                        ? new Literal[]{assumption} : new Literal[]{assumption, presence};
                CpModelContext.enforce(model.addEquality(ctx.start[t], index.pinStart(t)), enforcement);
                ctx.count(0, 0, 1, 0);
            }
        }
    }

    /** Locked initial assignments: presence, start and candidate literals are fixed (guarded by LOCKED in EXPLAIN). */
    private static void applyLocks(CpModelContext ctx) {
        InitialSolution initial = ctx.problem().initialSolution();
        if (initial == null) {
            return;
        }
        CandidateIndex index = ctx.index;
        CpModel model = ctx.model;
        for (InitialAssignment assignment : initial.assignments()) {
            if (!assignment.locked()) {
                continue;
            }
            int t = index.taskIdx(assignment.taskId());
            if (t < 0 || t >= index.taskCount()) {
                continue;
            }
            Literal[] enforcement = ctx.explainMode()
                    ? new Literal[]{ctx.assumptions.literal(AssumptionRegistry.KIND_TASK, index.taskId(t),
                    "LOCKED_CONFLICT")}
                    : new Literal[0];
            if (ctx.presence[t] != ctx.trueLit) {
                CpModelContext.enforce(model.addEquality(ctx.presence[t], 1), enforcement);
            }
            if (assignment.start() != null) {
                int tick;
                try {
                    tick = index.horizon().toTick(assignment.start());
                } catch (RuntimeException unaligned) {
                    tick = -1;
                }
                if (tick < 0) {
                    CpModelContext.enforce(model.addBoolOr(new Literal[]{ctx.trueLit.not()}), enforcement);
                } else {
                    CpModelContext.enforce(model.addEquality(ctx.start[t], tick), enforcement);
                }
            }
            if (!assignment.resourceIds().isEmpty()) {
                Set<Integer> chosen = new HashSet<>();
                for (String resourceId : assignment.resourceIds()) {
                    int r = index.resourceIdx(resourceId);
                    if (r >= 0) {
                        chosen.add(r);
                    }
                }
                int begin = index.candidateBegin(t);
                int end = index.candidateEnd(t);
                for (int q = index.taskReqStart[t]; q < index.taskReqStart[t + 1]; q++) {
                    int[] members = candidatesOfRequirement(index, begin, end, q);
                    boolean anyLocked = false;
                    for (int c : members) {
                        anyLocked |= chosen.contains(index.candidateResource(c));
                    }
                    if (!anyLocked) {
                        continue;
                    }
                    for (int c : members) {
                        long value = chosen.contains(index.candidateResource(c)) ? 1L : 0L;
                        CpModelContext.enforce(model.addEquality(ctx.cand[c], value), enforcement);
                    }
                }
            }
            ctx.count(0, 0, 3, 0);
        }
    }

    // ================================================================================================ 9, 10

    private void applyConstraints(CpModelContext ctx, CompiledProblem problem, CpSatEnforcerFactory factory,
                                  List<String> explainSkipped) {
        SolverOptions options = ctx.options();
        int counter = 0;
        for (CompiledConstraint compiled : problem.constraints()) {
            ctx.checkpoint(counter++);
            if (!compiled.active()) {
                continue;
            }
            eu.isygoit.dto.request.Constraint constraint = compiled.constraint();
            boolean soft = constraint.enforcement() == IEnumEnforcement.Types.SOFT;
            if (soft && ctx.explainMode()) {
                continue; // soft constraints cannot make the problem infeasible
            }
            CpSatConstraintContributor contributor = contributors.constraint(constraint.type())
                    .orElseThrow(() -> new IllegalStateException(
                            "no CP-SAT contributor for constraint type '" + constraint.type() + "' (constraint '"
                                    + constraint.id() + "'); the problem verifier must reject it"));
            boolean relaxable = CpSatEnforcerFactory.isRelaxable(constraint, options);
            EnforcementSupport support = contributor.support();
            boolean supported = soft ? support.soft() : relaxable ? support.relaxable() : support.hard();
            if (!supported) {
                throw new IllegalStateException("the CP-SAT contributor of '" + constraint.type()
                        + "' does not support the enforcement of constraint '" + constraint.id() + "'");
            }
            Enforcer enforcer = factory.create(compiled);
            if (ctx.explainMode()) {
                try {
                    contributor.apply(ctx, compiled, enforcer);
                } catch (LimitExceededException tooLarge) {
                    explainSkipped.add(constraint.id());
                }
            } else {
                contributor.apply(ctx, compiled, enforcer);
            }
        }
    }

    private void applyObjectives(CpModelContext ctx, CompiledProblem problem, TierObjectiveBuilder tiers) {
        int counter = 0;
        for (CompiledObjective compiled : problem.objectives()) {
            ctx.checkpoint(counter++);
            if (compiled.objective().weight() == 0) {
                continue; // disabled objective (the validator warns)
            }
            CpSatObjectiveContributor contributor = contributors.objective(compiled.objective().type())
                    .orElseThrow(() -> new IllegalStateException(
                            "no CP-SAT contributor for objective type '" + compiled.objective().type()
                                    + "' (objective '" + compiled.objective().id() + "')"));
            contributor.contribute(ctx, compiled, tiers);
        }
    }

    // ================================================================================================ helpers

    private static LinearExpr sum(Literal[] literals) {
        return LinearExpr.newBuilder().addSum(literals).build();
    }

    /** Membership of a value in flat inclusive runs. */
    private static boolean contains(long[] runs, long value) {
        for (int i = 0; i + 1 < runs.length; i += 2) {
            if (value < runs[i]) {
                return false;
            }
            if (value <= runs[i + 1]) {
                return true;
            }
        }
        return false;
    }

    /** Flat inclusive runs of sorted distinct values (consecutive values merged). */
    private static long[] runsOfValues(long[] values) {
        if (values.length == 0) {
            return NO_RUNS;
        }
        long[] out = new long[values.length * 2];
        int n = 0;
        for (long value : values) {
            if (n > 0 && out[n - 1] + 1 == value) {
                out[n - 1] = value;
            } else {
                out[n++] = value;
                out[n++] = value;
            }
        }
        return Arrays.copyOf(out, n);
    }
}
