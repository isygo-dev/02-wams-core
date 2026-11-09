package eu.isygoit.solver.index;

import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceSelector;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates {@link ResourceSelector}s with set algebra on bitsets over the resources (design 5.4 step 3).
 * <p>
 * Inverted indexes {@code skill -> bitset}, {@code type -> bitset}, {@code tag -> bitset} and {@code id -> idx} are built
 * once in O(R + total skills/tags). A selector is evaluated in O(R / 64) per distinct selector; results are cached by
 * the selector record itself ({@code equals/hashCode}), so a thousand tasks sharing one selector evaluate it once.
 * <p>
 * Semantics: {@code resourceIds} (non-empty, not containing the literal {@code ALL}) restricts to those ids; the other
 * lists restrict as documented on {@link ResourceSelector} ({@code skills}/{@code tags}: ALL of them, {@code anySkills}:
 * at least one, {@code resourceTypeIds}: any of the types); {@code excludeResourceIds} is removed last. Unknown ids,
 * skills or types simply match nothing.
 * <p>
 * Not thread-safe (the caches are plain maps); one instance per index build. Returned bitsets and arrays are shared:
 * callers must not modify them.
 */
public final class SelectorEvaluator {

    private static final String ALL = "ALL";

    private final int resourceCount;
    private final Map<String, Integer> idIndex = new HashMap<>();
    private final Map<String, BitSet> skills = new HashMap<>();
    private final Map<String, BitSet> types = new HashMap<>();
    private final Map<String, BitSet> tags = new HashMap<>();
    private final Map<ResourceSelector, BitSet> bitCache = new HashMap<>();
    private final Map<ResourceSelector, int[]> arrayCache = new HashMap<>();

    /**
     * Creates the evaluator.
     *
     * @param resources the resources in request order (index = position)
     */
    public SelectorEvaluator(List<Resource> resources) {
        this.resourceCount = resources.size();
        for (int r = 0; r < resources.size(); r++) {
            Resource resource = resources.get(r);
            if (resource.id() != null) {
                idIndex.putIfAbsent(resource.id(), r);
            }
            for (String skill : resource.skills()) {
                skills.computeIfAbsent(skill, k -> new BitSet(resourceCount)).set(r);
            }
            for (String tag : resource.tags()) {
                tags.computeIfAbsent(tag, k -> new BitSet(resourceCount)).set(r);
            }
            if (resource.typeId() != null) {
                types.computeIfAbsent(resource.typeId(), k -> new BitSet(resourceCount)).set(r);
            }
        }
    }

    /**
     * Evaluates a selector.
     *
     * @param selector the selector ({@code null} selects every resource)
     * @return the selected resources as a (shared, read-only) bitset
     */
    public BitSet evaluate(ResourceSelector selector) {
        ResourceSelector key = selector == null ? ResourceSelector.all() : selector;
        BitSet cached = bitCache.get(key);
        if (cached != null) {
            return cached;
        }
        BitSet result = new BitSet(resourceCount);
        result.set(0, resourceCount);
        if (!key.resourceIds().isEmpty() && !containsAll(key.resourceIds())) {
            BitSet ids = new BitSet(resourceCount);
            for (String id : key.resourceIds()) {
                Integer idx = idIndex.get(id);
                if (idx != null) {
                    ids.set(idx);
                }
            }
            result.and(ids);
        }
        if (!key.resourceTypeIds().isEmpty()) {
            result.and(anyOf(types, key.resourceTypeIds()));
        }
        for (String skill : key.skills()) {
            BitSet bits = skills.get(skill);
            if (bits == null) {
                result.clear();
                break;
            }
            result.and(bits);
        }
        if (!key.anySkills().isEmpty()) {
            result.and(anyOf(skills, key.anySkills()));
        }
        for (String tag : key.tags()) {
            BitSet bits = tags.get(tag);
            if (bits == null) {
                result.clear();
                break;
            }
            result.and(bits);
        }
        for (String id : key.excludeResourceIds()) {
            Integer idx = idIndex.get(id);
            if (idx != null) {
                result.clear(idx);
            }
        }
        bitCache.put(key, result);
        return result;
    }

    /**
     * Evaluates a selector to an ascending index array (cached, shared, read-only).
     *
     * @param selector the selector ({@code null} selects every resource)
     * @return the selected resource indices, ascending
     */
    public int[] resources(ResourceSelector selector) {
        ResourceSelector key = selector == null ? ResourceSelector.all() : selector;
        int[] cached = arrayCache.get(key);
        if (cached != null) {
            return cached;
        }
        BitSet bits = evaluate(key);
        int[] out = new int[bits.cardinality()];
        int n = 0;
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
            out[n++] = i;
        }
        arrayCache.put(key, out);
        return out;
    }

    /** @return the number of resources selected by the selector */
    public int cardinality(ResourceSelector selector) {
        return evaluate(selector).cardinality();
    }

    /** @return the resource index of an id, or {@code -1} */
    public int resourceIdx(String id) {
        Integer idx = id == null ? null : idIndex.get(id);
        return idx == null ? -1 : idx;
    }

    /** @return the number of distinct selectors evaluated so far (cache size, for tests) */
    public int distinctSelectors() {
        return bitCache.size();
    }

    private BitSet anyOf(Map<String, BitSet> index, List<String> keys) {
        BitSet union = new BitSet(resourceCount);
        for (String key : keys) {
            BitSet bits = index.get(key);
            if (bits != null) {
                union.or(bits);
            }
        }
        return union;
    }

    private static boolean containsAll(List<String> ids) {
        for (String id : ids) {
            if (ALL.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }
}
