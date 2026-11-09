package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of the assumption literals of an EXPLAIN-mode model (design 6.10, phase B).
 * <p>
 * Every user visible fact that may be blamed for an infeasibility (a catalogue constraint, the window or pin of a task, the
 * calendar of a resource, a precedence edge) gets exactly one literal, created on demand and cached by
 * {@code (kind, id, reasonCode)}. The registry keeps the literals in creation order (deterministic) and translates the
 * indices returned by {@code CpSolver.sufficientAssumptionsForInfeasibility()} back into {@link AssumptionRef}s.
 * <p>
 * Thread-safety: not thread-safe; used by the single building thread, then read by the explainer. Units: none.
 * OR-TOOLS-VERIFY: {@code CpModel.addAssumptions(Literal[])} / {@code clearAssumptions()} (OR-Tools 9.15 Java).
 */
public final class AssumptionRegistry {

    /** Assumption kind of a catalogue constraint. */
    public static final String KIND_CONSTRAINT = "CONSTRAINT";
    /** Assumption kind of a task fact (window, pin, lock). */
    public static final String KIND_TASK = "TASK";
    /** Assumption kind of a resource fact (calendar, max load). */
    public static final String KIND_RESOURCE = "RESOURCE";
    /** Assumption kind of a precedence edge (or of the edges of one successor). */
    public static final String KIND_EDGE = "EDGE";

    private final CpModel model;
    private final Map<String, AssumptionRef> byKey = new LinkedHashMap<>();
    private final Map<Integer, AssumptionRef> byIndex = new HashMap<>();
    private final List<AssumptionRef> refs = new ArrayList<>();

    /**
     * @param model the EXPLAIN model that owns the created literals
     */
    public AssumptionRegistry(CpModel model) {
        this.model = model;
    }

    /**
     * Returns the assumption literal of an item, creating it on first use.
     *
     * @param kind       {@link #KIND_CONSTRAINT} | {@link #KIND_TASK} | {@link #KIND_RESOURCE} | {@link #KIND_EDGE}
     * @param id         constraint / task / resource id or edge description
     * @param reasonCode core reason code (CONSTRAINT, WINDOW_EMPTY, PIN_CONFLICT, LOCKED_CONFLICT, CALENDAR, PRECEDENCE, ...)
     * @return the (shared) literal; never {@code null}
     */
    public Literal literal(String kind, String id, String reasonCode) {
        String key = kind + '|' + id + '|' + reasonCode;
        AssumptionRef existing = byKey.get(key);
        if (existing != null) {
            return existing.literal();
        }
        BoolVar literal = model.newBoolVar("assume:" + key);
        AssumptionRef ref = new AssumptionRef(literal, kind, id, reasonCode);
        byKey.put(key, ref);
        byIndex.put(literal.getIndex(), ref);
        refs.add(ref);
        return literal;
    }

    /** @return the assumption descriptors in creation order (unmodifiable) */
    public List<AssumptionRef> refs() {
        return Collections.unmodifiableList(refs);
    }

    /** @return the number of registered assumptions */
    public int size() {
        return refs.size();
    }

    /** @return the literals in creation order (a fresh array) */
    public Literal[] literals() {
        Literal[] result = new Literal[refs.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = refs.get(i).literal();
        }
        return result;
    }

    /**
     * @param literalIndex the proto index of a literal ({@code Literal.getIndex()} or an element of the solver core)
     * @return the descriptor, or {@code null} when the index is not an assumption of this registry
     */
    public AssumptionRef byLiteralIndex(int literalIndex) {
        return byIndex.get(literalIndex);
    }

    /**
     * Translates a sufficient-assumptions core into descriptors. Unknown indices (for example negated literals) are skipped;
     * the result keeps the order of the core and contains no duplicates.
     *
     * @param coreIndices the solver core ({@code CpSolver.sufficientAssumptionsForInfeasibility()})
     * @return the blamed items (never {@code null})
     */
    public List<AssumptionRef> resolve(Collection<Integer> coreIndices) {
        List<AssumptionRef> result = new ArrayList<>();
        if (coreIndices == null) {
            return result;
        }
        for (Integer index : coreIndices) {
            if (index == null) {
                continue;
            }
            AssumptionRef ref = byIndex.get(index);
            if (ref != null && !result.contains(ref)) {
                result.add(ref);
            }
        }
        return result;
    }

    /**
     * (Re)installs the given assumptions on the model: clears the current ones and adds the subset. Used by the explainer for
     * the deletion filter. The default assumption set is installed by the builder with {@link #install()}.
     *
     * @param subset the assumptions to keep, non-null
     */
    public void install(Collection<AssumptionRef> subset) {
        model.clearAssumptions();
        if (subset.isEmpty()) {
            return;
        }
        Literal[] literals = new Literal[subset.size()];
        int i = 0;
        for (AssumptionRef ref : subset) {
            literals[i++] = ref.literal();
        }
        model.addAssumptions(literals);
    }

    /** Installs every registered assumption on the model. */
    public void install() {
        install(refs);
    }
}
