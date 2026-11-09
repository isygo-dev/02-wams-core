package eu.isygoit.solver.index;

import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Task;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves {@link ConstraintScope}s and tag groups to ascending index arrays, once per distinct scope (cached).
 * <p>
 * Scope kinds: {@code all}, {@code task}, {@code taskType}, {@code resource}/{@code resourceId}, {@code resourceType}
 * and {@code tag}. For task resolution {@code tag} selects TASK tags, for resource resolution it selects RESOURCE tags
 * (design 2.3). A kind that does not apply to the requested entity type resolves to an empty array; the verifier
 * reports such scopes as {@code INVALID_SCOPE}. Built in O(T + R) from the request lists; immutable afterwards (the
 * caches are concurrent maps), so it is thread-safe. Returned arrays are shared: callers must not modify them.
 */
public final class ScopeResolver {

    private static final int[] EMPTY = new int[0];
    private static final String KEY_SEPARATOR = "|";

    private final int taskCount;
    private final int resourceCount;
    private final Map<String, Integer> taskIndex = new HashMap<>();
    private final Map<String, Integer> resourceIndex = new HashMap<>();
    private final TreeMap<String, int[]> taskTags = new TreeMap<>();
    private final TreeMap<String, int[]> resourceTags = new TreeMap<>();
    private final Map<String, int[]> taskTypes = new HashMap<>();
    private final Map<String, int[]> resourceTypes = new HashMap<>();
    private final int[] allTasks;
    private final int[] allResources;
    private final Map<String, int[]> taskCache = new ConcurrentHashMap<>();
    private final Map<String, int[]> resourceCache = new ConcurrentHashMap<>();
    private final Map<String, int[][]> groupCache = new ConcurrentHashMap<>();

    /**
     * Creates the resolver.
     *
     * @param tasks     tasks in request order
     * @param resources resources in request order
     */
    public ScopeResolver(List<Task> tasks, List<Resource> resources) {
        this.taskCount = tasks.size();
        this.resourceCount = resources.size();
        allTasks = range(tasks.size());
        allResources = range(resources.size());
        Map<String, IntList> tagBuckets = new TreeMap<>();
        Map<String, IntList> typeBuckets = new HashMap<>();
        for (int t = 0; t < tasks.size(); t++) {
            Task task = tasks.get(t);
            if (task.id() != null) {
                taskIndex.putIfAbsent(task.id(), t);
            }
            for (String tag : task.tags()) {
                tagBuckets.computeIfAbsent(tag, k -> new IntList()).add(t);
            }
            if (task.typeId() != null) {
                typeBuckets.computeIfAbsent(task.typeId(), k -> new IntList()).add(t);
            }
        }
        flush(tagBuckets, taskTags);
        flush(typeBuckets, taskTypes);
        tagBuckets = new TreeMap<>();
        typeBuckets = new HashMap<>();
        for (int r = 0; r < resources.size(); r++) {
            Resource resource = resources.get(r);
            if (resource.id() != null) {
                resourceIndex.putIfAbsent(resource.id(), r);
            }
            for (String tag : resource.tags()) {
                tagBuckets.computeIfAbsent(tag, k -> new IntList()).add(r);
            }
            if (resource.typeId() != null) {
                typeBuckets.computeIfAbsent(resource.typeId(), k -> new IntList()).add(r);
            }
        }
        flush(tagBuckets, resourceTags);
        flush(typeBuckets, resourceTypes);
    }

    private static void flush(Map<String, IntList> buckets, Map<String, int[]> target) {
        for (Map.Entry<String, IntList> e : buckets.entrySet()) {
            target.put(e.getKey(), e.getValue().toArray());
        }
    }

    private static int[] range(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        return a;
    }

    /** @return the task index of an id, or {@code -1} */
    public int taskIdx(String id) {
        Integer i = id == null ? null : taskIndex.get(id);
        return i == null ? -1 : i;
    }

    /** @return the resource index of an id, or {@code -1} */
    public int resourceIdx(String id) {
        Integer i = id == null ? null : resourceIndex.get(id);
        return i == null ? -1 : i;
    }

    /** @return number of tasks */
    public int taskCount() {
        return taskCount;
    }

    /** @return number of resources */
    public int resourceCount() {
        return resourceCount;
    }

    /** @return task indices selected by the scope, ascending (cached, shared) */
    public int[] resolveTasks(ConstraintScope scope) {
        ConstraintScope s = scope == null ? ConstraintScope.all() : scope;
        String key = s.kind() + KEY_SEPARATOR + s.value();
        return taskCache.computeIfAbsent(key, k -> computeTasks(s));
    }

    /** @return resource indices selected by the scope, ascending (cached, shared) */
    public int[] resolveResources(ConstraintScope scope) {
        ConstraintScope s = scope == null ? ConstraintScope.all() : scope;
        String key = s.kind() + KEY_SEPARATOR + s.value();
        return resourceCache.computeIfAbsent(key, k -> computeResources(s));
    }

    private int[] computeTasks(ConstraintScope s) {
        String v = s.value();
        switch (s.kind()) {
            case ConstraintScope.ALL:
                return allTasks;
            case "task": {
                int i = taskIdx(v);
                return i < 0 ? EMPTY : new int[]{i};
            }
            case "taskType":
                return v == null ? EMPTY : taskTypes.getOrDefault(v, EMPTY);
            case "tag":
                return v == null ? EMPTY : taskTags.getOrDefault(v, EMPTY);
            default:
                return EMPTY;
        }
    }

    private int[] computeResources(ConstraintScope s) {
        String v = s.value();
        switch (s.kind()) {
            case ConstraintScope.ALL:
                return allResources;
            case "resource":
            case "resourceId": {
                int i = resourceIdx(v);
                return i < 0 ? EMPTY : new int[]{i};
            }
            case "resourceType":
                return v == null ? EMPTY : resourceTypes.getOrDefault(v, EMPTY);
            case "tag":
                return v == null ? EMPTY : resourceTags.getOrDefault(v, EMPTY);
            default:
                return EMPTY;
        }
    }

    /**
     * @param prefix a task tag prefix ({@code null} or empty matches every tag)
     * @return one ascending group per matching task tag, ordered by tag
     */
    public int[][] groupTasksByTagPrefix(String prefix) {
        String p = prefix == null ? "" : prefix;
        return groupCache.computeIfAbsent(p, k -> {
            List<int[]> groups = new ArrayList<>(prefixView(taskTags, p).values());
            return groups.toArray(new int[0][]);
        });
    }

    /** @return ascending distinct task indices that carry a tag starting with {@code prefix} */
    public int[] tasksWithTagPrefix(String prefix) {
        return union(prefixView(taskTags, prefix));
    }

    /** @return ascending distinct resource indices that carry a tag starting with {@code prefix} */
    public int[] resourcesWithTagPrefix(String prefix) {
        return union(prefixView(resourceTags, prefix));
    }

    /** @return task indices of a task type (shared array) */
    public int[] tasksOfType(String typeId) {
        return taskTypes.getOrDefault(typeId, EMPTY);
    }

    /** @return resource indices of a resource type (shared array) */
    public int[] resourcesOfType(String typeId) {
        return resourceTypes.getOrDefault(typeId, EMPTY);
    }

    private static Map<String, int[]> prefixView(TreeMap<String, int[]> tags, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return tags;
        }
        return tags.subMap(prefix, true, prefix + Character.MAX_VALUE, true);
    }

    private static int[] union(Map<String, int[]> groups) {
        if (groups.isEmpty()) {
            return EMPTY;
        }
        BitSet bits = new BitSet();
        for (int[] group : groups.values()) {
            for (int i : group) {
                bits.set(i);
            }
        }
        int[] out = new int[bits.cardinality()];
        int n = 0;
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
            out[n++] = i;
        }
        return out;
    }
}
