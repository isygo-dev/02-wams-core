package eu.isygoit.dto.request;

/**
 * A hard precedence edge "predecessor -> owner task" declared on the SUCCESSOR task ({@code Task.predecessors}).
 * <pre>{@code
 * { "taskId": "A1", "type": "FINISH_START", "minLag": 0, "maxLag": 30 }
 * }</pre>
 * Defaults applied here: {@code type} = FINISH_START, {@code minLag} = 0, {@code presence} = WHEN_BOTH.
 *
 * @param taskId   the PREDECESSOR task id (the owner of this reference is the successor)
 * @param type     edge type, default {@link Type#FINISH_START}
 * @param minLag   minimum lag in ticks, default 0, may be negative (lead)
 * @param maxLag   maximum lag in ticks, null = no upper lag
 * @param presence behaviour for optional tasks, default {@link Presence#WHEN_BOTH}
 */
public record PrecedenceRef(
        String taskId,
        Type type,
        Integer minLag,
        Integer maxLag,
        Presence presence) {

    /**
     * Edge types: which time point of the predecessor constrains which time point of the successor.
     */
    public enum Type { FINISH_START, START_START, FINISH_FINISH, START_FINISH }

    /**
     * Presence semantics. {@code IMPLIES}: successor present implies predecessor present.
     */
    public enum Presence { WHEN_BOTH, IMPLIES }

    public PrecedenceRef {
        type = type == null ? Type.FINISH_START : type;
        minLag = minLag == null ? Integer.valueOf(0) : minLag;
        presence = presence == null ? Presence.WHEN_BOTH : presence;
    }
}
