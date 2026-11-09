package eu.isygoit.solver.cpsat.objective;

import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.ObjectiveScore;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.TierValue;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.testsupport.EngineTestFactory;
import eu.isygoit.testsupport.ScenarioFixtures;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Real-solver checks of the objective contributors against the expected values of design 9.5: G4 makespan 14, G6 value
 * 30000 (and 300000 at scale 1000), G11 value 190 at the auto tier -1, G14 setup 5 (+ makespan 35), total / maximum
 * tardiness 5 and 5, G9 fairness 16. Every value is the {@code rawValue} of the objective score computed INDEPENDENTLY by
 * the solution validator, so a pass also proves "model value == checker value" (the validator raises
 * {@code TIER_VALUE_MISMATCH} otherwise).
 * <p>
 * Skipped (assumption) when the OR-Tools native library cannot be loaded. The wiring comes from {@link EngineTestFactory}
 * (WP8) and the service contract of design 10.3.
 */
class ObjectivesCpSatTest {

    private static final String TARDINESS_JSON = """
            {
              "tenant": "example-tenant",
              "meta": { "clientRef": "objectives-tardiness", "version": "2.0", "domainName": "workshop", "timezone": "UTC" },
              "horizon": { "start": "2026-09-07T08:00:00Z", "end": "2026-09-07T09:00:00Z", "tickSeconds": 60 },
              "resources": [ { "id": "M1", "typeId": "machine", "capacity": 1 } ],
              "tasks": [
                { "id": "T1", "duration": 10, "dueDate": "2026-09-07T08:10:00Z" },
                { "id": "T2", "duration": 10, "dueDate": "2026-09-07T08:15:00Z" }
              ],
              "constraints": [],
              "objectives": [
                { "id": "total", "type": "minimize_total_tardiness", "weight": 1, "tier": 0 },
                { "id": "max", "type": "minimize_max_tardiness", "weight": 1, "tier": 1 }
              ],
              "solverOptions": { "engine": "cp_sat", "maxTimeSeconds": 10, "numWorkers": 1, "seed": 0, "deterministic": true }
            }
            """;

    @BeforeAll
    static void requireNatives() {
        Assumptions.assumeTrue(EngineTestFactory.cpSatEngine().isAvailable(), "OR-Tools natives not available");
    }

    private static SolveResult solve(PlanningRequest request) {
        SolveResult result = EngineTestFactory.planningService()
                .solve(1L, request, new SolverCancellationToken(), SolveListener.NOOP);
        assertNotNull(result.solution(), "a solution is expected");
        assertEquals(IEnumSolverStatus.Types.OPTIMAL, result.status());
        return result;
    }

    private static ObjectiveScore score(SolveResult result, String objectiveId) {
        return result.solution().qualityDetails().stream()
                .filter(s -> s.objectiveId().equals(objectiveId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no score for objective " + objectiveId));
    }

    private static TierValue tier(SolveResult result, int tier) {
        return result.metrics().tiers().stream()
                .filter(t -> t.tier() == tier)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no tier value for tier " + tier));
    }

    @Test
    void g4MakespanIsFourteenTicks() {
        SolveResult result = solve(ScenarioFixtures.loadExample("g4-precedence-chain.json"));
        assertEquals(14, score(result, "makespan").rawValue());
        assertEquals(14.0, score(result, "makespan").score());
        assertEquals(14, tier(result, 0).value());
    }

    @Test
    void g6ResourceCostIsThirtyThousandAndScalesWithTheObjectiveScale() {
        PlanningRequest g6 = ScenarioFixtures.loadExample("g6-cost-minimisation.json");
        SolveResult result = solve(g6);
        assertEquals(30000, score(result, "cost").rawValue());
        assertEquals(300.0, score(result, "cost").score(), 1e-9);

        SolveResult scaled = solve(ScenarioFixtures.withObjectiveScale(g6, 1000));
        assertEquals(300000, score(scaled, "cost").rawValue());
        assertEquals(300.0, score(scaled, "cost").score(), 1e-9);
    }

    @Test
    void g11AutoSelectedValueIsOneHundredNinetyAtTierMinusOne() {
        SolveResult result = solve(ScenarioFixtures.loadExample("g11-prize-collecting.json"));
        ObjectiveScore auto = score(result, "auto-selected-value");
        assertEquals(-1, auto.tier());
        assertEquals(190, auto.rawValue());
        assertEquals(190.0, auto.score());
        // maximisation tiers carry the NEGATED internal value (design 3)
        assertEquals(-190, tier(result, -1).value());
    }

    @Test
    void g14SetupCostsFiveTicksAfterTheMakespanTier() {
        SolveResult result = solve(ScenarioFixtures.loadExample("g14-setup-times.json"));
        assertEquals(35, score(result, "makespan").rawValue());
        assertEquals(5, score(result, "setup").rawValue());
        assertEquals(35, tier(result, 0).value());
        assertEquals(5, tier(result, 1).value());
    }

    @Test
    void tardinessTotalAndMaximumAreFiveTicks() throws IOException {
        PlanningRequest request = ScenarioFixtures.strictMapper().readValue(TARDINESS_JSON, PlanningRequest.class);
        SolveResult result = solve(request);
        assertEquals(5, score(result, "total").rawValue());
        assertEquals(5, score(result, "max").rawValue());
    }

    @Test
    void g9FairnessMinimisesTheMaximumLoadToSixteenTicks() {
        SolveResult result = solve(ScenarioFixtures.loadExample("g9-roster.json"));
        assertEquals(16, score(result, "fair").rawValue());
        assertEquals(16.0, score(result, "fair").score());
    }
}
