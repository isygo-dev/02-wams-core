package eu.isygoit.enums;

/**
 * WAMS KMS does not define explicit expiration models.
 * This enum represents a DOMAIN ABSTRACTION over WAMS lifecycle behaviors.
 */
public interface IEnumKeyExpirationModel {

    int STR_ENUM_SIZE = 3;

    /**
     * Domain-level key lifecycle model mapped to WAMS behaviors.
     */
    enum Types implements IEnum {

        /**
         * Key has no scheduled deletion and remains valid indefinitely
         * unless explicitly disabled or scheduled for deletion.
         */
        INDEFINITE_LIFECYCLE("INDEFINITE_LIFECYCLE"),

        /**
         * Key is governed by automatic rotation policy
         * (EnableKeyRotation in WAMS KMS).
         */
        ROTATION_GOVERNED("ROTATION_GOVERNED"),

        /**
         * Key is scheduled for deletion using ScheduleKeyDeletion API.
         */
        SCHEDULED_DELETION("SCHEDULED_DELETION");

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