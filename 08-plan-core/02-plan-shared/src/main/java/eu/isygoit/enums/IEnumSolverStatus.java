package eu.isygoit.enums;

/**
 * The interface Enum solver status.
 */
public interface IEnumSolverStatus {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 10;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        OPTIMAL("Optimal"),
        FEASIBLE("Feasible"),
        INFEASIBLE("Infeasible"),
        UNKNOWN("Unknown");

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
