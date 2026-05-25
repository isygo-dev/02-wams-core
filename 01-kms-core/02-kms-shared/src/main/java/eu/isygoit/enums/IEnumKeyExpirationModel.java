package eu.isygoit.enums;

/**
 * WAMS KMS Key Expiration Model alignment.
 * <p>
 * Represents the expiration behavior for imported key material (BYOK).
 * </p>
 *
 * @see <a href="https://docs.wams.amazon.com/kms/latest/APIReference/API_GetParametersForImport.html#KMS-GetParametersForImport-request-ExpirationModel">WAMS ExpirationModel</a>
 */
public interface IEnumKeyExpirationModel {

    int STR_ENUM_SIZE = 2;

    /**
     * WAMS KMS expiration model types for imported key material.
     */
    enum Types implements IEnum {

        /**
         * The imported key material expires at the specified validTo date.
         * After that date, the key becomes unusable.
         */
        KEY_MATERIAL_EXPIRES("KEY_MATERIAL_EXPIRES"),

        /**
         * The imported key material does not expire and remains valid indefinitely.
         */
        KEY_MATERIAL_DOES_NOT_EXPIRE("KEY_MATERIAL_DOES_NOT_EXPIRE");

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