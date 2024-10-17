package eu.isygoit.enums;

/**
 * The interface Enum algo peb config.
 */
public interface IEnumAlgoPEBConfig {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 22;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {

        /**
         * Pbewithmd 5 anddes types.
         */
        PBEWITHMD5ANDDES("PBEWITHMD5ANDDES"),
        /**
         * Pbewithmd 5 andtripledes types.
         */
        PBEWITHMD5ANDTRIPLEDES("PBEWITHMD5ANDTRIPLEDES"),
        /**
         * Pbewithsha 1 anddesede types.
         */
        PBEWITHSHA1ANDDESEDE("PBEWITHSHA1ANDDESEDE"),

        /**
         * Pbewithsha 1 andrc 2 40 types.
         */
        PBEWITHSHA1ANDRC2_40("PBEWITHSHA1ANDRC2_40");


        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }


    }
}

