package eu.isygoit.enums;

/**
 * The interface Enum job status code.
 */
public interface IEnumJobStatusCode {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 24;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        CREATED("Created"),
        ACCEPTED("Accepted"),
        VALIDATING("Validating"),
        NORMALIZING("Normalizing"),
        SOLVING("Solving"),
        STOPPING("Stopping"),
        SUCCEEDED("Succeeded"),
        INFEASIBLE("Infeasible"),
        FAILED("Failed"),
        CANCELLED("Cancelled");

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
