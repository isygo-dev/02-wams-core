package eu.isygoit.enums;

/**
 * The interface Enum key origin.
 */
public interface IEnumKeyOrigin {

    int STR_ENUM_SIZE = 3;

    enum Types implements IEnum {

        /**
         * Key generated internally by the system (default KMS behavior).
         */
        CUSTOM_GENERATED("CUSTOM_GENERATED"),

        /**
         * Key imported from external system (BYOK - Bring Your Own Key).
         */
        EXTERNAL("EXTERNAL"),

        /**
         * Key generated inside hardware security module (HSM).
         */
        HSM("HSM"), WAMS_KMS("WAMS_KMS");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}