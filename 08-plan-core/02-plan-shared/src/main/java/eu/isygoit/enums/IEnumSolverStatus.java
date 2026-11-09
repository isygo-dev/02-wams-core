package eu.isygoit.enums;

/**
 * The interface Enum solver status.
 */
public interface IEnumSolverStatus {

    /**
     * The constant STR_ENUM_SIZE (20: room for STOPPED_FEASIBLE).
     */
    int STR_ENUM_SIZE = 20;

    /**
     * The enum Types.
     * <ul>
     *   <li>OPTIMAL: proven optimal for every tier.</li>
     *   <li>FEASIBLE: a solution exists, time/gap limit reached.</li>
     *   <li>INFEASIBLE: proven infeasible.</li>
     *   <li>UNKNOWN: nothing found, nothing proven.</li>
     *   <li>STOPPED_FEASIBLE: stop requested while an incumbent existed; the incumbent is returned.</li>
     * </ul>
     */
    enum Types implements IEnum {
        OPTIMAL("Optimal"),
        FEASIBLE("Feasible"),
        INFEASIBLE("Infeasible"),
        UNKNOWN("Unknown"),
        STOPPED_FEASIBLE("Stopped with a feasible solution");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        @Override
        public String meaning() {
            return meaning;
        }
    }
}
