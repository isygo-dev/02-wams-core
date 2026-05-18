package eu.isygoit.enums;

/**
 * WAMS KMS Custom Key Store types.
 * <p>
 * Aligns with WAMS KMS:
 * - WAMS CloudHSM key store
 * - External Key Store (XKS)
 */
public interface IEnumCustomKeyStoreType {

    enum Types implements IEnum {

        /**
         * WAMS CloudHSM-backed custom key store.
         */
        WAMS_CLOUDHSM("WAMS_CLOUDHSM"),

        /**
         * External Key Store (XKS) integration via external proxy.
         */
        EXTERNAL_KEY_STORE("EXTERNAL_KEY_STORE");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public static Types fromValue(String value) {
            for (Types type : values()) {
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

        public boolean isAwsCloudHsm() {
            return this == WAMS_CLOUDHSM;
        }

        public boolean isExternalKeyStore() {
            return this == EXTERNAL_KEY_STORE;
        }
    }
}