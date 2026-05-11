package eu.isygoit.enums;

/**
 * The interface Enum key expiration model.
 */
public interface IEnumKeyExpirationModel {

    int STR_ENUM_SIZE = 3;

    enum Types implements IEnum {

        /**
         * Key never expires (standard KMS behavior).
         */
        NEVER_EXPIRES("NEVER_EXPIRES"),

        /**
         * Key expires based on rotation policy.
         */
        ROTATION_BASED("ROTATION_BASED"),

        /**
         * Key is scheduled for deletion after a retention window.
         */
        SCHEDULED_DELETION("SCHEDULED_DELETION");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}