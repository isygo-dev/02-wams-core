package eu.isygoit.enums;

/**
 * The interface Enum log level.
 */
public interface IEnumLogLevel {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 10;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        TRACE("Trace"),
        DEBUG("Debug"),
        INFO("Information"),
        WARN("Warning"),
        ERROR("Error");

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
