package eu.isygoit.solver;

import eu.isygoit.exception.UnknownSolverException;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registry engine id to engine (design 5.7 / D-10). Canonical ids are lower-case ({@code cp_sat}, {@code timefold});
 * the aliases {@code cp-sat} and {@code cpsat} resolve to {@code cp_sat}; lookup is case-insensitive and trims blanks.
 * <p>
 * Created in {@code PlanningEngineConfiguration}; immutable after construction, thread-safe; iteration order is ascending
 * canonical id. Two engines with the same canonical id fail construction with an {@link IllegalStateException} naming
 * both classes.
 */
public final class SolverRegistry {

    private static final Map<String, String> ALIASES = Map.of("cp-sat", "cp_sat", "cpsat", "cp_sat");

    private final Map<String, SolverEngine> engines;

    /**
     * @param engines all engine beans
     * @throws IllegalStateException on duplicate (or blank) canonical id
     */
    public SolverRegistry(Collection<SolverEngine> engines) {
        Map<String, SolverEngine> byId = new TreeMap<>();
        if (engines != null) {
            for (SolverEngine engine : engines) {
                String id = canonicalId(engine.name());
                if (id == null || id.isEmpty()) {
                    throw new IllegalStateException("The engine " + engine.getClass().getName() + " declares a blank name()");
                }
                SolverEngine previous = byId.putIfAbsent(id, engine);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate solver engine id '" + id + "': registered by "
                            + previous.getClass().getName() + " and " + engine.getClass().getName());
                }
            }
        }
        this.engines = Collections.unmodifiableMap(byId);
    }

    /**
     * Normalises an engine id: trim, lower-case ({@link Locale#ROOT}), then alias resolution
     * ({@code cp-sat}, {@code cpsat} to {@code cp_sat}). Other ids are returned lower-cased.
     *
     * @param name an engine id or alias, may be {@code null}
     * @return the canonical id, or {@code null} for {@code null}
     */
    public static String canonicalId(String name) {
        if (name == null) {
            return null;
        }
        String id = name.trim().toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(id, id);
    }

    /**
     * @param name an engine id or alias
     * @return the engine
     * @throws UnknownSolverException when the name is {@code null} or unknown
     */
    public SolverEngine get(String name) {
        SolverEngine engine = engines.get(canonicalId(name));
        if (engine == null) {
            throw new UnknownSolverException(name, engines.keySet());
        }
        return engine;
    }

    /** @return the engine, or empty when the name is {@code null} or unknown */
    public Optional<SolverEngine> find(String name) {
        return Optional.ofNullable(engines.get(canonicalId(name)));
    }

    /** @return the canonical ids of the registered engines, ascending, unmodifiable */
    public Collection<String> supportedEngines() {
        return engines.keySet();
    }
}
