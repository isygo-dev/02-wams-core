package eu.isygoit.dto.request;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolverOptionsAliasTest {

    private static SolverOptions withEngine(String engine) {
        return new SolverOptions(engine, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static SolverOptions withMaxTime(Integer maxTime, Integer explainTime) {
        return new SolverOptions(null, maxTime, null, null, null, null, null, null, null, null, null, explainTime,
                null);
    }

    @Test
    void cpSatAliasesAreNormalised() {
        for (String alias : new String[]{"cp_sat", "cp-sat", "cpsat", "CP_SAT", "CP-SAT", "CpSat", " cp-sat ", "Cp_Sat"}) {
            assertEquals("cp_sat", withEngine(alias).engine(), alias);
            assertEquals("cp_sat", SolverOptions.normalizeEngine(alias), alias);
        }
    }

    @Test
    void timefoldIsCanonicalLowerCase() {
        assertEquals("timefold", withEngine("timefold").engine());
        assertEquals("timefold", withEngine("Timefold").engine());
        assertEquals("timefold", withEngine(" TIMEFOLD").engine());
    }

    @Test
    void missingEngineDefaultsToCpSat() {
        assertEquals("cp_sat", withEngine(null).engine());
        assertEquals("cp_sat", withEngine("").engine());
        assertEquals("cp_sat", withEngine("   ").engine());
        assertEquals("cp_sat", SolverOptions.DEFAULT_ENGINE);
    }

    @Test
    void unknownEngineIsKeptForTheRegistryToReject() {
        assertEquals("ortools", withEngine("ortools").engine());
        assertEquals("OrTools", withEngine(" OrTools ").engine());
    }

    @Test
    void defaults() {
        SolverOptions o = SolverOptions.defaults();
        assertEquals("cp_sat", o.engine());
        assertEquals(30, o.maxTimeSeconds());
        assertEquals(8, o.numWorkers());
        assertEquals(0L, o.seed());
        assertEquals(Boolean.FALSE, o.logSearch());
        assertTrue(o.relaxationLevels().isEmpty());
        assertEquals(Boolean.FALSE, o.deterministic());
        assertEquals(100, o.objectiveScale());
        assertEquals(0.0, o.gapTolerance());
        assertEquals(SolverOptions.LexicographicMode.AUTO, o.lexicographicMode());
        assertEquals(Boolean.TRUE, o.explainInfeasibility());
        assertEquals(6, o.explainTimeSeconds(), "min(10, 20% of 30)");
        assertEquals(Boolean.TRUE, o.keepIncumbentOnStop());
        assertEquals(o, withEngine(null));
    }

    @Test
    void explainTimeDefault() {
        assertEquals(1, withMaxTime(1, null).explainTimeSeconds());
        assertEquals(1, withMaxTime(4, null).explainTimeSeconds());
        assertEquals(2, withMaxTime(10, null).explainTimeSeconds());
        assertEquals(10, withMaxTime(50, null).explainTimeSeconds());
        assertEquals(10, withMaxTime(300, null).explainTimeSeconds());
        assertEquals(7, withMaxTime(10, 7).explainTimeSeconds(), "explicit value wins");
    }

    @Test
    void relaxationLevelsAreCopied() {
        List<String> levels = new ArrayList<>(List.of("c1"));
        SolverOptions o = new SolverOptions(null, null, null, null, null, levels, null, null, null, null, null,
                null, null);
        levels.add("c2");
        assertEquals(List.of("c1"), o.relaxationLevels());
        assertThrows(UnsupportedOperationException.class, () -> o.relaxationLevels().add("x"));
    }

    @Test
    void rangesAreEnforced() {
        assertThrows(IllegalArgumentException.class, () -> withMaxTime(0, null));
        assertThrows(IllegalArgumentException.class, () -> withMaxTime(10, 0));
        assertThrows(IllegalArgumentException.class, () -> new SolverOptions(null, null, 0, null, null, null, null,
                null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SolverOptions(null, null, 33, null, null, null, null,
                null, null, null, null, null, null));
        assertEquals(32, new SolverOptions(null, null, 32, null, null, null, null, null, null, null, null, null,
                null).numWorkers());
        assertThrows(IllegalArgumentException.class, () -> new SolverOptions(null, null, null,
                (long) Integer.MAX_VALUE + 1, null, null, null, null, null, null, null, null, null));
        assertEquals(Integer.MIN_VALUE, new SolverOptions(null, null, null, (long) Integer.MIN_VALUE, null, null,
                null, null, null, null, null, null, null).seed().intValue());
        assertThrows(IllegalArgumentException.class, () -> new SolverOptions(null, null, null, null, null, null,
                null, 0, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SolverOptions(null, null, null, null, null, null,
                null, 1_000_001, null, null, null, null, null));
        assertEquals(1_000_000, new SolverOptions(null, null, null, null, null, null, null, 1_000_000, null, null,
                null, null, null).objectiveScale());
        assertThrows(IllegalArgumentException.class, () -> new SolverOptions(null, null, null, null, null, null,
                null, null, 1.5, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SolverOptions(null, null, null, null, null, null,
                null, null, -0.1, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SolverOptions(null, null, null, null, null, null,
                null, null, Double.NaN, null, null, null, null));
        assertEquals(1.0, new SolverOptions(null, null, null, null, null, null, null, null, 1.0, null, null, null,
                null).gapTolerance());
    }
}
