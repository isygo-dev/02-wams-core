package eu.isygoit.plan.engine;

import eu.isygoit.plan.exception.UnknownSolverException;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SolverRegistry {
    private final Map<String, SolverEngine> engines;

    public SolverRegistry(Collection<SolverEngine> engines) {
        this.engines = engines.stream().collect(Collectors.toUnmodifiableMap(
                engine -> engine.name().toLowerCase(), Function.identity()));
    }

    public SolverEngine get(String name) {
        SolverEngine engine = name == null ? null : engines.get(name.toLowerCase());
        if (engine == null) {
            throw new UnknownSolverException(name, engines.keySet());
        }
        return engine;
    }

    public Collection<String> supportedEngines() {
        return engines.keySet();
    }
}
