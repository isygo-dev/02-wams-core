package eu.isygoit.dto.request.params;

import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PrecedenceRef;

/**
 * Params of {@code precedence}: bulk/soft/relaxable precedence between two task sets (hard edges of the
 * definition belong in {@code Task.predecessors}). Defaults: {@code type} FINISH_START, {@code minLag} 0,
 * {@code pairing} ALL_PAIRS. {@code from}/{@code to} are mandatory (checked by the family validator).
 * <pre>{@code
 * { "from": { "kind": "tag", "value": "cut" }, "to": { "kind": "tag", "value": "weld" },
 *   "type": "FINISH_START", "minLag": 2, "pairing": "BY_ORDER" }
 * }</pre>
 *
 * @param from    scope of the predecessor tasks
 * @param to      scope of the successor tasks
 * @param type    edge type
 * @param minLag  minimum lag in ticks
 * @param maxLag  maximum lag in ticks, null = none
 * @param pairing how the two sets are paired
 */
public record PrecedenceParams(
        ConstraintScope from,
        ConstraintScope to,
        PrecedenceRef.Type type,
        Integer minLag,
        Integer maxLag,
        PairingMode pairing) {

    /**
     * Pairing of the {@code from} and {@code to} sets. ALL_PAIRS: every from x every to (guarded by maxPairwise);
     * BY_ORDER: i-th from -> i-th to.
     */
    public enum PairingMode { ALL_PAIRS, BY_ORDER }

    public PrecedenceParams {
        type = type == null ? PrecedenceRef.Type.FINISH_START : type;
        minLag = minLag == null ? Integer.valueOf(0) : minLag;
        pairing = pairing == null ? PairingMode.ALL_PAIRS : pairing;
    }
}
