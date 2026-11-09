package eu.isygoit.solver.index;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Calendar;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.SolveCancelledException;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.index.IndexFixtures.ProblemB;
import eu.isygoit.solver.index.IndexFixtures.StubFamily;
import eu.isygoit.solver.spi.ModelCost;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.index.IndexFixtures.compiled;
import static eu.isygoit.solver.index.IndexFixtures.hard;
import static eu.isygoit.solver.index.IndexFixtures.ids;
import static eu.isygoit.solver.index.IndexFixtures.problem;
import static eu.isygoit.solver.index.IndexFixtures.resource;
import static eu.isygoit.solver.index.IndexFixtures.skills;
import static eu.isygoit.solver.index.IndexFixtures.task;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class CandidateIndexBuilderTest {

    private static CandidateIndex build(ProblemB b) {
        return CandidateIndexBuilder.build(b.problem(), List.of(), new PlanLimitsProperties(), null);
    }

    private static CandidateIndex build(ProblemB b, List<CompiledConstraint> constraints) {
        return CandidateIndexBuilder.build(b.problem(), constraints, new PlanLimitsProperties(), null);
    }

    // ------------------------------------------------------------------------------------------ basics

    @Test
    void buildsCandidatesDomainsAndCsrArraysFromSelectors() {
        CandidateIndex ix = build(problem(100)
                .resource(resource("r0").skills("math"))
                .resource(resource("r1").skills("science"))
                .task(task("t0").duration(10).requires(skills("math")))
                .task(task("t1").duration(20).requires(skills("science"))));

        assertThat(ix.taskCount).isEqualTo(2);
        assertThat(ix.resourceCount).isEqualTo(2);
        assertThat(ix.candidateCount).isEqualTo(2);
        assertThat(ix.taskCandStart).containsExactly(0, 1, 2);
        assertThat(ix.candResource).containsExactly(0, 1);
        assertThat(ix.candTask).containsExactly(0, 1);
        assertThat(ix.candDuration).containsExactly(10, 20);
        assertThat(ix.estStart).containsExactly(0, 0);
        assertThat(ix.lstStart).containsExactly(90, 80);
        assertThat(ix.taskReqStart).containsExactly(0, 1, 2);
        assertThat(ix.reqCount).containsExactly(1, 1);
        assertThat(ix.reqAmount).containsExactly(1, 1);
        assertThat(ix.reqCandStart).containsExactly(0, 1, 2);
        assertThat(ix.resCandStart).containsExactly(0, 1, 2);
        assertThat(ix.roles).containsExactly("default");
        assertThat(ix.required).containsExactly(true, true);
        assertThat(ix.taskStartRunStart).containsExactly(0, 0, 0);
        assertThat(ix.prunedTotal).isZero();
        assertThat(ix.taskIdx("t1")).isEqualTo(1);
        assertThat(ix.resourceIdx("r1")).isEqualTo(1);
        assertThat(ix.taskIdx("ghost")).isEqualTo(-1);
        assertThat(ix.taskId(0)).isEqualTo("t0");
        assertThat(ix.candidateBegin(1)).isEqualTo(1);
        assertThat(ix.candidateEnd(1)).isEqualTo(2);
        assertThat(ix.candidateDuration(1)).isEqualTo(20);
    }

    @Test
    void aTaskWithoutRequirementsUsesTheImplicitDefaultRequirementOnEveryResource() {
        CandidateIndex ix = build(problem(50)
                .resource(resource("r0")).resource(resource("r1"))
                .task(task("t0").duration(5)));
        assertThat(ix.candidateCount).isEqualTo(2);
        assertThat(ix.candResource).containsExactly(0, 1);
    }

    @Test
    void priorityPrizeDueAndOptionalDataAreConverted() {
        ProblemB b = problem(100).resource(resource("r0"));
        b.task(task("opt").duration(5).optional(50).priority(3).due(b.at(30)));
        b.task(task("req").duration(5));
        CandidateIndex ix = build(b);
        assertThat(ix.required).containsExactly(false, true);
        assertThat(ix.prize).containsExactly(50L, 0L);
        assertThat(ix.priority).containsExactly(3, 0);
        assertThat(ix.due).containsExactly(30, Integer.MIN_VALUE);
        assertThat(ix.demand).containsExactly(1, 1);
    }

    @Test
    void releaseAndDeadlineBoundTheStartWindow() {
        ProblemB b = problem(100).resource(resource("r0"));
        b.task(task("t0").duration(10).release(b.at(5)).deadline(b.at(50)));
        CandidateIndex ix = build(b);
        assertThat(ix.release[0]).isEqualTo(5);
        assertThat(ix.latestEnd[0]).isEqualTo(50);
        assertThat(ix.estStart[0]).isEqualTo(5);
        assertThat(ix.lstStart[0]).isEqualTo(40);
    }

    @Test
    void aPinFixesTheStartDomain() {
        ProblemB b = problem(100).resource(resource("r0"));
        b.task(task("t0").duration(10).pin(b.at(20)));
        CandidateIndex ix = build(b);
        assertThat(ix.pinStart[0]).isEqualTo(20);
        assertThat(ix.estStart[0]).isEqualTo(20);
        assertThat(ix.lstStart[0]).isEqualTo(20);
    }

    @Test
    void scopesAndTagGroupsAreResolvedOncePerScope() {
        CandidateIndex ix = build(problem(100).resource(resource("r0").tags("night")).resource(resource("r1"))
                .task(task("a").tags("class-7B", "g1"))
                .task(task("b").tags("class-7A", "g1"))
                .task(task("c").tags("class-7A")));
        assertThat(ix.resolveTasks(new ConstraintScope("tag", "g1"))).containsExactly(0, 1);
        assertThat(ix.resolveTasks(new ConstraintScope("task", "c"))).containsExactly(2);
        assertThat(ix.resolveTasks(ConstraintScope.all())).containsExactly(0, 1, 2);
        assertThat(ix.resolveResources(new ConstraintScope("tag", "night"))).containsExactly(0);
        assertThat(ix.resolveTasks(new ConstraintScope("tag", "g1"))).isSameAs(ix.resolveTasks(new ConstraintScope("tag", "g1")));
        int[][] groups = ix.groupTasksByTagPrefix("class-");
        assertThat(groups.length).isEqualTo(2);
        assertThat(groups[0]).containsExactly(1, 2); // class-7A
        assertThat(groups[1]).containsExactly(0);    // class-7B
    }

    // ------------------------------------------------------------------------------------------ calendars

    @Test
    void calendarRunsBecomeStartDomainsAndTasksThatFitNowhereAreRecorded() {
        ProblemB b = problem(100).resource(resource("r0").calendar("work"));
        b.calendar(new Calendar("work", null, null,
                List.of(new TimeWindow(b.at(10), b.at(20)), new TimeWindow(b.at(40), b.at(60))), null, null, null));
        b.task(task("a").duration(10)).task(task("b").duration(15)).task(task("c").duration(25));
        CandidateIndex ix = build(b);

        assertThat(ix.resRunStart).containsExactly(0, 4);
        assertThat(ix.resRuns).containsExactly(10, 20, 40, 60);
        assertThat(ix.candidateCount).isEqualTo(2);
        assertThat(ix.taskCandStart).containsExactly(0, 1, 2, 2);
        // a: [10,10] and [40,50]; b: [40,45]; c: nothing fits
        assertThat(ix.estStart[0]).isEqualTo(10);
        assertThat(ix.lstStart[0]).isEqualTo(50);
        assertThat(ix.taskStartRunStart).containsExactly(0, 4, 4, 4);
        assertThat(ix.taskStartRuns).containsExactly(10, 10, 40, 50);
        assertThat(ix.estStart[1]).isEqualTo(40);
        assertThat(ix.lstStart[1]).isEqualTo(45);
        assertThat(ix.prunedTotal).isEqualTo(1);
        assertThat(ix.prunedCandidates.get(0).reasonCode()).isEqualTo(CandidateIndexBuilder.REASON_CALENDAR);
        assertThat(ix.prunedCandidates.get(0).task()).isEqualTo(2);
        assertThat(ix.candStartRunStart).containsExactly(0, 0, 0);
    }

    @Test
    void unavailableWindowsSplitTheStartDomain() {
        ProblemB b = problem(100);
        b.resource(resource("r0").unavailable(b.at(30), b.at(40)));
        b.task(task("t0").duration(10));
        CandidateIndex ix = build(b);
        assertThat(ix.estStart[0]).isEqualTo(0);
        assertThat(ix.lstStart[0]).isEqualTo(90);
        assertThat(ix.taskStartRuns).containsExactly(0, 20, 40, 90);
        assertThat(ix.resRuns).containsExactly(0, 30, 40, 100);
    }

    @Test
    void aResourceWithoutAvailabilityPrunesAllItsCandidates() {
        ProblemB b = problem(100);
        b.resource(resource("r0").unavailable(b.at(0), b.at(100))).resource(resource("r1"));
        b.task(task("t0").duration(10));
        CandidateIndex ix = build(b);
        assertThat(ix.resNeverAvailable).containsExactly(true, false);
        assertThat(ix.candResource).containsExactly(1);
    }

    @Test
    void calendarRoundingWarningsAreCollected() {
        ProblemB b = problem(100).resource(resource("r0").calendar("work"));
        b.calendar(new Calendar("work", null, null,
                List.of(new TimeWindow(b.at(10).plusSeconds(30), b.at(20))), null, null, null));
        b.task(task("t0").duration(5));
        CandidateIndex ix = build(b);
        assertThat(ix.warnings).anyMatch(w -> w.code().equals("CALENDAR_ROUNDED")
                && w.path().equals("calendars[0].windows[0]"));
        assertThat(ix.estStart[0]).isEqualTo(11);
    }

    // ------------------------------------------------------------------------------------------ precedence

    @Test
    void precedenceEdgesTightenTheWindowsAndProduceATopologicalOrder() {
        CandidateIndex ix = build(problem(100).resource(resource("r0"))
                .task(task("A").duration(10))
                .task(task("B").duration(5).after("A")));
        assertThat(ix.edgeCount).isEqualTo(1);
        assertThat(ix.edgePred).containsExactly(0);
        assertThat(ix.edgeSucc).containsExactly(1);
        assertThat(ix.edgeType).containsExactly(PrecedenceGraph.FINISH_START);
        assertThat(ix.edgeMinLag).containsExactly(0);
        assertThat(ix.edgeMaxLag).containsExactly(Integer.MAX_VALUE);
        assertThat(ix.edgeImpliesPresence).containsExactly(0);
        assertThat(ix.succEdgeStart).containsExactly(0, 0, 1);
        assertThat(ix.topoOrder).containsExactly(0, 1);
        assertThat(ix.estStart).containsExactly(0, 10);
        assertThat(ix.lstStart).containsExactly(85, 95);
    }

    @Test
    void lagsAndTypesAreCarriedIntoTheEdgeArrays() {
        CandidateIndex ix = build(problem(100).resource(resource("r0"))
                .task(task("A").duration(10))
                .task(task("B").duration(5).after("A", PrecedenceRef.Type.START_START, 7)));
        assertThat(ix.edgeType).containsExactly(PrecedenceGraph.START_START);
        assertThat(ix.edgeMinLag).containsExactly(7);
        assertThat(ix.estStart[1]).isEqualTo(7);
    }

    @Test
    void aPrecedenceCycleIsRejectedWithItsPath() {
        ProblemB b = problem(100).resource(resource("r0"))
                .task(task("A").duration(5).after("B"))
                .task(task("B").duration(5).after("A"));
        assertThatThrownBy(() -> build(b))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Precedence cycle")
                .satisfies(e -> assertThat(((InvalidRequestException) e).code()).isEqualTo("PRECEDENCE_CYCLE"));
    }

    @Test
    void anImpossibleCriticalPathIsRecordedNotThrown() {
        CandidateIndex ix = build(problem(15).resource(resource("r0"))
                .task(task("A").duration(10))
                .task(task("B").duration(10).after("A")));
        assertThat(ix.candidateCount).isZero();
        assertThat(ix.prunedCandidates).extracting(p -> p.reasonCode())
                .contains(CandidateIndexBuilder.REASON_PRECEDENCE);
    }

    // ------------------------------------------------------------------------------------------ slots

    @Test
    void slotModeKeepsOnlySlotsLongEnoughAndStartsOnSlotStarts() {
        CandidateIndex ix = build(problem(100).resource(resource("r0"))
                .slot("s0", 0, 4, 0).slot("s1", 10, 14, 1).slot("s2", 20, 22, 2)
                .task(task("t0").duration(4).slots("ALL")));
        assertThat(ix.slotCount).isEqualTo(3);
        assertThat(ix.slotStart).containsExactly(0, 10, 20);
        assertThat(ix.slotEnd).containsExactly(4, 14, 22);
        assertThat(ix.slotCost).containsExactly(0.0, 1.0, 2.0);
        assertThat(ix.taskSlotStart).containsExactly(0, 2);
        assertThat(ix.taskSlots).containsExactly(0, 1);
        assertThat(ix.taskStartRuns).containsExactly(0, 0, 10, 10);
        assertThat(ix.estStart[0]).isEqualTo(0);
        assertThat(ix.lstStart[0]).isEqualTo(10);
        assertThat(ix.slotIdx("s1")).isEqualTo(1);
        assertThat(ix.slotId(2)).isEqualTo("s2");
    }

    @Test
    void listedSlotsAreOrderedByStartAndTheDurationDefaultsToTheFirstSlotLength() {
        CandidateIndex ix = build(problem(100).resource(resource("r0"))
                .slot("s0", 0, 4, 0).slot("s1", 10, 14, 1).slot("s2", 20, 22, 2)
                .task(task("t0").duration(2).slots("s2", "s1"))
                .task(task("t1").duration(null).slots("ALL")));
        assertThat(ix.taskSlotStart).containsExactly(0, 2, 4);
        assertThat(ix.taskSlots).containsExactly(1, 2, 0, 1);
        assertThat(ix.estStart[0]).isEqualTo(10);
        assertThat(ix.lstStart[0]).isEqualTo(20);
        assertThat(ix.duration[1]).isEqualTo(4);
    }

    // ------------------------------------------------------------------------------------------ requirements, pins

    @Test
    void amountAboveCapacityPrunesTheResource() {
        CandidateIndex ix = build(problem(100)
                .resource(resource("small").capacity(1)).resource(resource("big").capacity(2))
                .task(task("t0").duration(5).requires("default", skills(), 1, 2)));
        assertThat(ix.candResource).containsExactly(1);
        assertThat(ix.reqAmount).containsExactly(2);
        assertThat(ix.candAmount).containsExactly(2);
        assertThat(ix.capacity).containsExactly(1, 2);
        assertThat(ix.prunedCandidates.get(0).reasonCode()).isEqualTo(CandidateIndexBuilder.REASON_AMOUNT);
    }

    @Test
    void aFullPinRestrictsTheCandidatesAndAPartialPinForcesTheNamedResource() {
        CandidateIndex ix = build(problem(100)
                .resource(resource("r0")).resource(resource("r1")).resource(resource("r2"))
                .task(task("full").duration(5).pin(null, "r2"))
                .task(task("partial").duration(5).requires("default", skills(), 2, null).pin(null, "r1")));
        assertThat(ix.taskCandStart).containsExactly(0, 1, 4);
        assertThat(ix.candResource).containsExactly(2, 0, 1, 2);
        assertThat(ix.candForced).containsExactly(false, false, true, false);
        assertThat(ix.reqCount).containsExactly(1, 2);
    }

    @Test
    void severalRequirementsAreIndexedPerRoleAndResourceDurationsOverrideTheDuration() {
        ProblemB b = problem(100)
                .resource(resource("r0").skills("a")).resource(resource("r1").skills("a")).resource(resource("op"));
        b.task(task("t0").duration(10)
                .requires("machine", skills("a"), 1, null)
                .requires("operator", ids("op"), 1, 1));
        b.task(task("t1").duration(10).resourceDurations(Map.of("r0", 5, "r1", 20)).requires(skills("a")));
        CandidateIndex ix = build(b);
        assertThat(ix.roles).containsExactly("machine", "operator", "default");
        assertThat(ix.taskReqStart).containsExactly(0, 2, 3);
        assertThat(ix.reqRole).containsExactly(0, 1, 2);
        assertThat(ix.reqTask).containsExactly(0, 0, 1);
        assertThat(ix.reqCandStart).containsExactly(0, 2, 3, 5);
        assertThat(ix.candReq).containsExactly(0, 0, 1, 2, 2);
        assertThat(ix.candDuration).containsExactly(10, 10, 10, 5, 20);
        // t1: union [0,95]; the r1 candidate (duration 20) is narrower and becomes an override
        assertThat(ix.estStart[1]).isEqualTo(0);
        assertThat(ix.lstStart[1]).isEqualTo(95);
        assertThat(ix.candStartRunStart).containsExactly(0, 0, 0, 0, 0, 2);
        assertThat(ix.candStartRuns).containsExactly(0, 80);
    }

    @Test
    void resourceCsrListsTheCandidatesOfEachResourceAscending() {
        CandidateIndex ix = build(problem(100).resource(resource("r0")).resource(resource("r1"))
                .task(task("a").duration(5)).task(task("b").duration(5)).task(task("c").duration(5)));
        assertThat(ix.resCandStart).containsExactly(0, 3, 6);
        for (int r = 0; r < ix.resourceCount; r++) {
            int previous = -1;
            for (int k = ix.resCandStart[r]; k < ix.resCandStart[r + 1]; k++) {
                assertThat(ix.candResource[ix.resCand[k]]).isEqualTo(r);
                assertThat(ix.resCand[k]).isGreaterThan(previous);
                previous = ix.resCand[k];
            }
        }
    }

    @Test
    void resourceAttributesAndSetupMatricesAreIndexed() {
        ProblemB b = problem(100)
                .resource(resource("r0").maxLoad(30).setupMatrix("m1")).resource(resource("r1"))
                .matrix(new SetupMatrix("m1", 5, 1, List.of(new SetupEntry("a", "b", 9))));
        b.task(task("t0").duration(5).setupClass("a")).task(task("t1").duration(5).setupClass("b"))
                .task(task("t2").duration(5));
        CandidateIndex ix = build(b);
        assertThat(ix.maxLoadTicks).containsExactly(30, Integer.MAX_VALUE);
        assertThat(ix.resSetupMatrix).containsExactly(0, -1);
        assertThat(ix.setupClassNames).containsExactly("a", "b");
        assertThat(ix.setupClass).containsExactly(0, 1, -1);

        SetupTable table = SetupTable.build(ix, b.problem());
        assertThat(table.ticks(0, 0, 1)).isEqualTo(9);
        assertThat(table.ticks(0, 0, 0)).isEqualTo(1);
        assertThat(table.ticks(0, 1, 0)).isEqualTo(5);
        assertThat(table.ticks(0, -1, 0)).isZero();
        assertThat(table.ticks(1, 0, 1)).isZero();
        assertThat(table.hasSetup(0)).isTrue();
        assertThat(table.hasSetup(1)).isFalse();
        assertThat(table.maxTicks()).isEqualTo(9);
    }

    // ------------------------------------------------------------------------------------------ preprocess hooks

    @Test
    void hardFamiliesPruneCandidatesAndRestrictStartsThroughThePreprocessContext() {
        StubFamily family = new StubFamily("stub", (ctx, constraint) -> {
            for (int t : ctx.resolveTasks(constraint.scope())) {
                for (int c = ctx.candidates().candidateBegin(t); c < ctx.candidates().candidateEnd(t); c++) {
                    if (ctx.candidates().candidateResource(c) == 0) {
                        ctx.pruneCandidate(c, "ELIGIBILITY", constraint.id());
                    }
                }
                ctx.restrictStart(t, 10, 20);
            }
        }, ModelCost.ZERO);
        Constraint c1 = hard("c1", "stub");
        CandidateIndex ix = build(problem(100).resource(resource("r0")).resource(resource("r1"))
                .task(task("t0").duration(10)), List.of(compiled(0, c1, family)));
        assertThat(ix.candidateCount).isEqualTo(1);
        assertThat(ix.candResource).containsExactly(1);
        assertThat(ix.estStart[0]).isEqualTo(10);
        assertThat(ix.lstStart[0]).isEqualTo(20);
        assertThat(ix.prunedCandidates).hasSize(1);
        assertThat(ix.prunedCandidates.get(0).reasonCode()).isEqualTo("ELIGIBILITY");
        assertThat(ix.prunedCandidates.get(0).constraintId()).isEqualTo("c1");
    }

    @Test
    void perCandidateStartRestrictionsBecomeOverrides() {
        StubFamily family = new StubFamily("stub", (ctx, constraint) -> {
            for (int c = ctx.candidates().candidateBegin(0); c < ctx.candidates().candidateEnd(0); c++) {
                if (ctx.candidates().candidateResource(c) == 0) {
                    ctx.candidates().restrictCandidateStart(c, 50, 60);
                }
            }
        }, ModelCost.ZERO);
        CandidateIndex ix = build(problem(100).resource(resource("r0")).resource(resource("r1"))
                .task(task("t0").duration(10)), List.of(compiled(0, hard("c1", "stub"), family)));
        assertThat(ix.candidateCount).isEqualTo(2);
        assertThat(ix.estStart[0]).isEqualTo(0);
        assertThat(ix.lstStart[0]).isEqualTo(90);
        assertThat(ix.candStartRunStart).containsExactly(0, 2, 2);
        assertThat(ix.candStartRuns).containsExactly(50, 60);
    }

    @Test
    void softAndInactiveConstraintsDoNotRunPreprocess() {
        StubFamily family = new StubFamily("stub", (ctx, constraint) -> {
            throw new AssertionError("preprocess must not run");
        }, ModelCost.ZERO);
        Constraint soft = new Constraint("s", "stub", IEnumEnforcement.Types.SOFT, null, Map.of(), null, 1, null);
        CompiledConstraint inactive = new CompiledConstraint(1, hard("h", "stub"), family, null, false);
        CandidateIndex ix = build(problem(100).resource(resource("r0")).task(task("t0").duration(10)),
                List.of(compiled(0, soft, family), inactive));
        assertThat(ix.candidateCount).isEqualTo(1);
    }

    // ------------------------------------------------------------------------------------------ guards

    @Test
    void theCandidateLimitAbortsTheEnumeration() {
        PlanLimitsProperties limits = new PlanLimitsProperties();
        limits.setMaxCandidates(3);
        PlanningProblem p = problem(100).resource(resource("r0")).resource(resource("r1"))
                .task(task("a").duration(5)).task(task("b").duration(5)).problem();
        assertThatThrownBy(() -> CandidateIndexBuilder.build(p, List.of(), limits, null))
                .isInstanceOf(LimitExceededException.class)
                .satisfies(e -> assertThat(((LimitExceededException) e).limitName()).isEqualTo("max-candidates"));
    }

    @Test
    void thePrecedenceLimitAndTheHorizonLimitAreEnforced() {
        PlanLimitsProperties precedences = new PlanLimitsProperties();
        precedences.setMaxPrecedences(0);
        PlanningProblem p = problem(100).resource(resource("r0"))
                .task(task("A").duration(5)).task(task("B").duration(5).after("A")).problem();
        assertThatThrownBy(() -> CandidateIndexBuilder.build(p, List.of(), precedences, null))
                .isInstanceOf(LimitExceededException.class)
                .satisfies(e -> assertThat(((LimitExceededException) e).limitName()).isEqualTo("max-precedences"));
        PlanLimitsProperties horizon = new PlanLimitsProperties();
        horizon.setMaxHorizonTicks(50);
        assertThatThrownBy(() -> CandidateIndexBuilder.build(p, List.of(), horizon, null))
                .isInstanceOf(LimitExceededException.class)
                .satisfies(e -> assertThat(((LimitExceededException) e).limitName()).isEqualTo("max-horizon-ticks"));
    }

    @Test
    void aCancelledTokenStopsTheBuild() {
        SolverCancellationToken token = new SolverCancellationToken();
        token.cancel();
        PlanningProblem p = problem(100).resource(resource("r0")).task(task("a").duration(5)).problem();
        assertThatThrownBy(() -> CandidateIndexBuilder.build(p, List.of(), new PlanLimitsProperties(), token))
                .isInstanceOf(SolveCancelledException.class);
    }

    @Test
    void buildsTwoThousandTasksOnTwentyResourcesQuicklyAndSharesTheSelector() {
        ProblemB b = problem(1000);
        for (int r = 0; r < 20; r++) {
            b.resource(resource("r" + r).skills("s"));
        }
        for (int t = 0; t < 2000; t++) {
            b.task(task("t" + t).duration(5).requires(skills("s")));
        }
        CandidateIndex ix = assertTimeoutPreemptively(Duration.ofSeconds(20), () -> build(b));
        assertThat(ix.candidateCount).isEqualTo(40_000);
        assertThat(ix.taskCandStart[2000]).isEqualTo(40_000);
        assertThat(ix.resCandStart[20]).isEqualTo(40_000);
    }
}
