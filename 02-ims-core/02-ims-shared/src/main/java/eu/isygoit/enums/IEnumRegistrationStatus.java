package eu.isygoit.enums;


public interface IEnumRegistrationStatus {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 10;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {

        NEW("New"),
        CONFIRMED("Confirmed"),
        REJECTED("Rejected"),
        PROCESSED("Processed");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
