package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CumulativeConstraint;
import com.google.ortools.sat.IntervalVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.solver.index.CandidateIndex;

import java.util.ArrayList;
import java.util.List;

/**
 * Resource side of the structural model (design 6.4, "Resource models", build steps 3 and 11).
 * <ul>
 *   <li>{@link #finalizeModels}: for every resource creates {@code NoOverlap} (capacity 1, or every candidate amount equals
 *   the capacity and no block exists) or {@code Cumulative} over the candidate intervals and the blocks added by families
 *   ({@code ctx.blockResource}). Candidates with duration 0 occupy nothing and are skipped; a resource with fewer than two
 *   interval elements needs no constraint.</li>
 *   <li>{@link #applyMaxLoad}: the structural cap {@code load(r, TICKS) <= maxLoadTicks[r]}; in EXPLAIN mode it is guarded by
 *   a RESOURCE assumption.</li>
 * </ul>
 * Stateless utility; building thread only. Units: ticks and capacity units.
 */
public final class ResourceModelBuilder {

    private ResourceModelBuilder() {
    }

    /**
     * Creates the NoOverlap / Cumulative constraints of all resources (build step 11, after the families added their blocks).
     *
     * @param ctx the model context
     */
    public static void finalizeModels(CpModelContext ctx) {
        CandidateIndex index = ctx.index();
        CpModel model = ctx.model();
        int resources = index.resourceCount();
        for (int r = 0; r < resources; r++) {
            ctx.checkpoint(r);
            List<IntervalVar> intervals = new ArrayList<>();
            List<Integer> amounts = new ArrayList<>();
            boolean allAmountsFillCapacity = true;
            int capacity = index.capacity(r);
            for (int position = index.resCandStart[r]; position < index.resCandStart[r + 1]; position++) {
                int c = index.resCand[position];
                if (index.candidateDuration(c) <= 0) {
                    continue;
                }
                intervals.add(ctx.candInterval(c));
                int amount = Math.max(1, index.candidateAmount(c));
                amounts.add(amount);
                if (amount < capacity) {
                    allAmountsFillCapacity = false;
                }
            }
            List<CpModelContext.Block> blocks = ctx.blocksOf(r);
            if (intervals.isEmpty() || intervals.size() + blocks.size() < 2) {
                continue;
            }
            boolean unary = capacity <= 1 || (blocks.isEmpty() && allAmountsFillCapacity);
            if (unary) {
                List<IntervalVar> all = new ArrayList<>(intervals);
                for (CpModelContext.Block block : blocks) {
                    all.add(blockInterval(ctx, block));
                }
                model.addNoOverlap(all);
                ctx.count(0, 0, 1, 0);
            } else {
                CumulativeConstraint cumulative = model.addCumulative(capacity);
                for (int i = 0; i < intervals.size(); i++) {
                    cumulative.addDemand(intervals.get(i), Math.min(amounts.get(i), capacity));
                }
                for (CpModelContext.Block block : blocks) {
                    cumulative.addDemand(blockInterval(ctx, block), Math.min(block.demand(), capacity));
                }
                ctx.count(0, 0, 1, 0);
            }
        }
    }

    /**
     * Structural cap {@code load(r, TICKS) <= maxLoadTicks[r]} (build step 6). Skipped when the cap cannot bind. In EXPLAIN
     * mode the constraint is enforced by the RESOURCE assumption {@code (resourceId, CAPACITY_EXCEEDED)}.
     *
     * @param ctx the model context
     */
    public static void applyMaxLoad(CpModelContext ctx) {
        CandidateIndex index = ctx.index();
        CpModel model = ctx.model();
        for (int r = 0; r < index.resourceCount(); r++) {
            ctx.checkpoint(r);
            int cap = index.maxLoadTicks(r);
            if (cap == Integer.MAX_VALUE) {
                continue;
            }
            long total = 0L;
            for (int position = index.resCandStart[r]; position < index.resCandStart[r + 1]; position++) {
                total += index.candidateDuration(index.resCand[position]);
            }
            if (total <= cap) {
                continue;
            }
            LinearExpr load = ctx.load(r, eu.isygoit.dto.request.params.LoadMetric.TICKS);
            if (ctx.explainMode()) {
                Literal assumption = ctx.assumptions().literal(AssumptionRegistry.KIND_RESOURCE,
                        index.resourceId(r), "CAPACITY_EXCEEDED");
                CpModelContext.enforce(model.addLinearConstraint(load, 0, cap), assumption);
            } else {
                model.addLinearConstraint(load, 0, cap);
            }
            ctx.count(0, 0, 1, 0);
        }
    }

    private static IntervalVar blockInterval(CpModelContext ctx, CpModelContext.Block block) {
        CpModel model = ctx.model();
        int size = block.hi() - block.lo();
        LinearExpr start = LinearExpr.constant(block.lo());
        ctx.count(0, 0, 0, 1);
        if (block.presence() == null) {
            return model.newFixedSizeIntervalVar(start, size, "");
        }
        return model.newOptionalFixedSizeIntervalVar(start, size, block.presence(), "");
    }
}
