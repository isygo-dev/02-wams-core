package eu.isygoit.enums;

/**
 * Custom Key Store Type Enumeration
 * <p>
 * Defines the types of custom key stores supported by the KMS
 *
 * @author Isygoit Team
 * @version 1.0
 */
public interface IEnumCustomKeyStoreType {

    enum Types implements IEnum {
        /**
         * CloudHSM-based custom key store using software-simulated HSM
         */
        CLOUDHSM("CLOUDHSM"),

        /**
         * External Key Store (XKS) for integration with external KMS
         */
        EXTERNAL_KEY_STORE("EXTERNAL_KEY_STORE");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public static Types fromValue(String value) {
            for (Types type : Types.values()) {
                if (type.meaning.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown custom key store type: " + value);
        }

        @Override
        public String meaning() {
            return meaning;
        }

        public boolean isCloudHsm() {
            return this == CLOUDHSM;
        }

        public boolean isExternalKeyStore() {
            return this == EXTERNAL_KEY_STORE;
        }
    }
}