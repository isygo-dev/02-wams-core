package eu.isygoit.solver.cpsat;

import eu.isygoit.enums.IEnumEnforcement;

/**
 * Which enforcement modes one engine FORMULATION implements: hard, soft, relaxable (design 5.5). The capability matrix is
 * the intersection of the family's semantic support and the contributor's formulation support.
 * <p>
 * Immutable value record, thread-safe. Engine neutral (no OR-Tools types), therefore also used by the Timefold SPI and by
 * {@code EngineCapabilities}.
 *
 * @param hard      STRICTLY_REQUIRED / CONFIGURABLE_HARD are implemented
 * @param soft      SOFT is implemented
 * @param relaxable RELAXABLE_WITH_AUTHORIZATION is implemented
 */
public record EnforcementSupport(boolean hard, boolean soft, boolean relaxable) {

    /** Hard enforcement only. */
    public static final EnforcementSupport HARD_ONLY = new EnforcementSupport(true, false, false);

    /** Hard, soft and relaxable. */
    public static final EnforcementSupport ALL = new EnforcementSupport(true, true, true);

    /** Nothing supported. */
    public static final EnforcementSupport NONE = new EnforcementSupport(false, false, false);

    /** @return the component-wise AND of this and {@code o} */
    public EnforcementSupport and(EnforcementSupport o) {
        return new EnforcementSupport(hard && o.hard, soft && o.soft, relaxable && o.relaxable);
    }

    /**
     * @param enforcement a constraint enforcement value (may be {@code null}, treated as hard)
     * @return whether this support covers the enforcement: SOFT needs {@code soft}, RELAXABLE_WITH_AUTHORIZATION needs
     * {@code relaxable}, STRICTLY_REQUIRED / CONFIGURABLE_HARD (and null) need {@code hard}
     */
    public boolean supports(IEnumEnforcement.Types enforcement) {
        if (enforcement == IEnumEnforcement.Types.SOFT) {
            return soft;
        }
        if (enforcement == IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION) {
            return relaxable;
        }
        return hard;
    }
}
