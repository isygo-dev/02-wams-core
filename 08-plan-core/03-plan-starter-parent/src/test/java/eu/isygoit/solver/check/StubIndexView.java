package eu.isygoit.solver.check;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.solver.diag.PreSolveAnalyzer;
import eu.isygoit.solver.spi.CandidateIndexView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only index derived directly from a {@link PlanningProblem}: a miniature, readable stand-in for WP2's
 * {@code CandidateIndex} so the WP6 classes can be tested without it. Candidates are the (task, requirement, resource)
 * triples whose resource matches the selector and has {@code capacity >= amount}; start domain is
 * {@code [release, latestEnd - minDuration]}. {@link #prune} and {@link #window} emulate what compile-time pruning does.
 */
public final class StubIndexView implements CandidateIndexView, PreSolveAnalyzer.Structure {

    private final PlanningProblem problem;
    private final Horizon horizon;
    private final int horizonTicks;
    private final Map<String, Integer> taskIx = new HashMap<>();
    private final Map<String, Integer> resourceIx = new HashMap<>();
    private final Map<String, Integer> slotIx = new HashMap<>();
    /** per task: candidates as {requirement, resource, duration, amount} */
    private final List<List<int[]>> cands = new ArrayList<>();
    private final int[] est;
    private final int[] lst;
    private int[] candBegin;
    private int[] flatTask;
    private int[] flatResource;
    private int[] flatDuration;
    private int[] flatAmount;
    private int[] flatReq;

    private StubIndexView(PlanningProblem problem) {
        this.problem = problem;
        this.horizon = problem.horizon();
        this.horizonTicks = (int) horizon.horizonTicks();
        for (int t = 0; t < problem.tasks().size(); t++) {
            taskIx.put(problem.tasks().get(t).id(), t);
        }
        for (int r = 0; r < problem.resources().size(); r++) {
            resourceIx.put(problem.resources().get(r).id(), r);
        }
        for (int s = 0; s < problem.slots().size(); s++) {
            slotIx.put(problem.slots().get(s).id(), s);
        }
        est = new int[problem.tasks().size()];
        lst = new int[problem.tasks().size()];
        for (int t = 0; t < problem.tasks().size(); t++) {
            Task task = problem.tasks().get(t);
            List<int[]> list = new ArrayList<>();
            int q = 0;
            for (ResourceRequirement req : task.effectiveRequirements()) {
                int amount = req.amount() != null ? req.amount() : task.demand();
                for (int r = 0; r < problem.resources().size(); r++) {
                    Resource resource = problem.resources().get(r);
                    if (SelectorMatcher.matches(req.selector(), resource) && resource.capacity() >= amount) {
                        list.add(new int[]{q, r, duration(task), amount});
                    }
                }
                q++;
            }
            cands.add(list);
            est[t] = release(task);
            lst[t] = latestEnd(task) - duration(task);
        }
        reindex();
    }

    /** @return a stub index of the problem */
    public static StubIndexView of(PlanningProblem problem) {
        return new StubIndexView(problem);
    }

    /** Removes a candidate, as pruning by an eligibility constraint would. */
    public StubIndexView prune(String taskId, String resourceId) {
        int r = resourceIx.get(resourceId);
        cands.get(taskIx.get(taskId)).removeIf(c -> c[1] == r);
        reindex();
        return this;
    }

    /** Overrides the start domain of a task. */
    public StubIndexView window(String taskId, int estStart, int lstStart) {
        est[taskIx.get(taskId)] = estStart;
        lst[taskIx.get(taskId)] = lstStart;
        return this;
    }

    private void reindex() {
        int n = problem.tasks().size();
        candBegin = new int[n + 1];
        int total = 0;
        for (int t = 0; t < n; t++) {
            candBegin[t] = total;
            total += cands.get(t).size();
        }
        candBegin[n] = total;
        flatTask = new int[total];
        flatResource = new int[total];
        flatDuration = new int[total];
        flatAmount = new int[total];
        flatReq = new int[total];
        int c = 0;
        for (int t = 0; t < n; t++) {
            for (int[] cand : cands.get(t)) {
                flatTask[c] = t;
                flatReq[c] = cand[0];
                flatResource[c] = cand[1];
                flatDuration[c] = cand[2];
                flatAmount[c] = cand[3];
                c++;
            }
        }
    }

    private int duration(Task task) {
        return task.duration() == null ? 0 : task.duration();
    }

    private int release(Task task) {
        return task.release() == null ? 0 : horizon.ceilTick(task.release());
    }

    private int latestEnd(Task task) {
        return task.deadline() == null ? horizonTicks : Math.min(horizonTicks, horizon.floorTick(task.deadline()));
    }

    // ------------------------------------------------------------------------------------------ CandidateIndexView

    @Override
    public int taskCount() {
        return problem.tasks().size();
    }

    @Override
    public int resourceCount() {
        return problem.resources().size();
    }

    @Override
    public int slotCount() {
        return problem.slots().size();
    }

    @Override
    public Horizon horizon() {
        return horizon;
    }

    @Override
    public int horizonTicks() {
        return horizonTicks;
    }

    @Override
    public PlanLimitsProperties limits() {
        return new PlanLimitsProperties();
    }

    @Override
    public String taskId(int t) {
        return problem.tasks().get(t).id();
    }

    @Override
    public String resourceId(int r) {
        return problem.resources().get(r).id();
    }

    @Override
    public String slotId(int s) {
        return problem.slots().get(s).id();
    }

    @Override
    public int taskIdx(String id) {
        return taskIx.getOrDefault(id, -1);
    }

    @Override
    public int resourceIdx(String id) {
        return resourceIx.getOrDefault(id, -1);
    }

    @Override
    public int slotIdx(String id) {
        return slotIx.getOrDefault(id, -1);
    }

    @Override
    public int[] resolveTasks(ConstraintScope scope) {
        List<Integer> out = new ArrayList<>();
        for (int t = 0; t < taskCount(); t++) {
            Task task = problem.tasks().get(t);
            boolean in = switch (scope.kind()) {
                case "all" -> true;
                case "task" -> task.id().equals(scope.value());
                case "tag" -> task.tags().contains(scope.value());
                default -> false;
            };
            if (in) {
                out.add(t);
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public int[] resolveResources(ConstraintScope scope) {
        List<Integer> out = new ArrayList<>();
        for (int r = 0; r < resourceCount(); r++) {
            if ("all".equals(scope.kind()) || problem.resources().get(r).id().equals(scope.value())) {
                out.add(r);
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public int[][] groupTasksByTagPrefix(String prefix) {
        return new int[0][];
    }

    @Override
    public int candidateCount() {
        return flatTask.length;
    }

    @Override
    public int duration(int task) {
        return duration(problem.tasks().get(task));
    }

    @Override
    public int demand(int task) {
        return problem.tasks().get(task).demand();
    }

    @Override
    public int priority(int task) {
        return problem.tasks().get(task).priority();
    }

    @Override
    public boolean required(int task) {
        return problem.tasks().get(task).requiredTask();
    }

    @Override
    public long prize(int task) {
        return problem.tasks().get(task).effectivePrize();
    }

    @Override
    public int release(int task) {
        return release(problem.tasks().get(task));
    }

    @Override
    public int latestEnd(int task) {
        return latestEnd(problem.tasks().get(task));
    }

    @Override
    public int estStart(int task) {
        return est[task];
    }

    @Override
    public int lstStart(int task) {
        return lst[task];
    }

    @Override
    public int capacity(int resource) {
        return problem.resources().get(resource).capacity();
    }

    @Override
    public int maxLoadTicks(int resource) {
        Integer max = problem.resources().get(resource).maxLoadTicks();
        return max == null ? Integer.MAX_VALUE : max;
    }

    @Override
    public int candidateBegin(int task) {
        return candBegin[task];
    }

    @Override
    public int candidateEnd(int task) {
        return candBegin[task + 1];
    }

    @Override
    public int candidateTask(int candidate) {
        return flatTask[candidate];
    }

    @Override
    public int candidateResource(int candidate) {
        return flatResource[candidate];
    }

    @Override
    public int candidateDuration(int candidate) {
        return flatDuration[candidate];
    }

    @Override
    public int candidateAmount(int candidate) {
        return flatAmount[candidate];
    }

    // ------------------------------------------------------------------------------------------ Structure

    @Override
    public int[] requirementNeeded(int task) {
        List<ResourceRequirement> reqs = problem.tasks().get(task).effectiveRequirements();
        int[] out = new int[reqs.size()];
        for (int q = 0; q < out.length; q++) {
            out[q] = reqs.get(q).count();
        }
        return out;
    }

    @Override
    public int[] requirementCandidates(int task) {
        int[] out = new int[problem.tasks().get(task).effectiveRequirements().size()];
        for (int[] cand : cands.get(task)) {
            out[cand[0]]++;
        }
        return out;
    }

    @Override
    public int[] forcedCandidates(int task) {
        int[] need = requirementNeeded(task);
        int[] have = requirementCandidates(task);
        int[] tmp = new int[cands.get(task).size()];
        int n = 0;
        for (int c = candBegin[task]; c < candBegin[task + 1]; c++) {
            int q = flatReq[c];
            if (have[q] == need[q] && have[q] > 0) {
                tmp[n++] = c;
            }
        }
        return Arrays.copyOf(tmp, n);
    }

    @Override
    public long availableTicks(int resource, int lo, int hi) {
        long total = Math.max(0, hi - lo);
        for (TimeWindow window : problem.resources().get(resource).unavailable()) {
            long a = Math.max(lo, horizon.floorTick(window.start()));
            long b = Math.min(hi, horizon.ceilTick(window.end()));
            if (b > a) {
                total -= b - a;
            }
        }
        return total;
    }
}
