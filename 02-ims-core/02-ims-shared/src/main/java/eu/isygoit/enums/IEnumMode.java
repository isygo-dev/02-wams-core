package eu.isygoit.enums;

/**
 * The interface Enum mode.
 */
public interface IEnumMode {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 10;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        /**
         * Light types.
         */
        LIGHT("light"),
        /**
         * Dark types.
         */
        DARK("dark"),
        /**
         * Semidark types.
         */
        SEMIDARK("semi-dark");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
