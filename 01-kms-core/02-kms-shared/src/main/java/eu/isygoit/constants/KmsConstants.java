package eu.isygoit.constants;

/**
 * The type Kms constants.
 */
public class KmsConstants {

    /**
     * The constant API_BASE_PATH.
     */
    public static final String API_BASE_PATH = "/api/v1/private/key";

    // API Paths
    /**
     * The constant API_KEYS_PATH.
     */
    public static final String API_KEYS_PATH = "/keys";
    /**
     * The constant API_ENCRYPT_PATH.
     */
    public static final String API_ENCRYPT_PATH = "/encrypt";
    /**
     * The constant API_DECRYPT_PATH.
     */
    public static final String API_DECRYPT_PATH = "/decrypt";
    /**
     * The constant API_REENCRYPT_PATH.
     */
    public static final String API_REENCRYPT_PATH = "/reencrypt";
    /**
     * The constant API_SIGN_PATH.
     */
    public static final String API_SIGN_PATH = "/sign";
    /**
     * The constant API_VERIFY_PATH.
     */
    public static final String API_VERIFY_PATH = "/verify";
    /**
     * The constant API_AUDIT_PATH.
     */
    public static final String API_AUDIT_PATH = "/audit/logs";
    /**
     * The constant API_DATAKEY_PATH.
     */
    public static final String API_DATAKEY_PATH = "/datakey/generate";
    /**
     * The constant AUDIT_ACTION_CREATE_KEY.
     */
    public static final String AUDIT_ACTION_CREATE_KEY = "CREATE_KEY";

    // Audit Actions
    /**
     * The constant AUDIT_ACTION_GET_KEY_METADATA.
     */
    public static final String AUDIT_ACTION_GET_KEY_METADATA = "GET_KEY_METADATA";
    /**
     * The constant AUDIT_ACTION_LIST_KEYS.
     */
    public static final String AUDIT_ACTION_LIST_KEYS = "LIST_KEYS";
    /**
     * The constant AUDIT_ACTION_ENABLE_KEY.
     */
    public static final String AUDIT_ACTION_ENABLE_KEY = "ENABLE_KEY";
    /**
     * The constant AUDIT_ACTION_DISABLE_KEY.
     */
    public static final String AUDIT_ACTION_DISABLE_KEY = "DISABLE_KEY";
    /**
     * The constant AUDIT_ACTION_ROTATE_KEY.
     */
    public static final String AUDIT_ACTION_ROTATE_KEY = "ROTATE_KEY";
    /**
     * The constant AUDIT_ACTION_ENCRYPT.
     */
    public static final String AUDIT_ACTION_ENCRYPT = "ENCRYPT";
    /**
     * The constant AUDIT_ACTION_DECRYPT.
     */
    public static final String AUDIT_ACTION_DECRYPT = "DECRYPT";
    /**
     * The constant AUDIT_ACTION_REENCRYPT.
     */
    public static final String AUDIT_ACTION_REENCRYPT = "REENCRYPT";
    /**
     * The constant AUDIT_ACTION_SIGN.
     */
    public static final String AUDIT_ACTION_SIGN = "SIGN";
    /**
     * The constant AUDIT_ACTION_VERIFY.
     */
    public static final String AUDIT_ACTION_VERIFY = "VERIFY";
    /**
     * The constant AUDIT_ACTION_GENERATE_DATA_KEY.
     */
    public static final String AUDIT_ACTION_GENERATE_DATA_KEY = "GENERATE_DATA_KEY";
    /**
     * The constant KEY_NOT_FOUND_MESSAGE.
     */
    public static final String KEY_NOT_FOUND_MESSAGE = "Key not found";

    // Error Messages
    /**
     * The constant KEY_ALREADY_EXISTS_MESSAGE.
     */
    public static final String KEY_ALREADY_EXISTS_MESSAGE = "Key already exists";
    /**
     * The constant INVALID_KEY_STATE_MESSAGE.
     */
    public static final String INVALID_KEY_STATE_MESSAGE = "Invalid key state";
    /**
     * The constant ENCRYPTION_FAILED_MESSAGE.
     */
    public static final String ENCRYPTION_FAILED_MESSAGE = "Encryption operation failed";
    /**
     * The constant DECRYPTION_FAILED_MESSAGE.
     */
    public static final String DECRYPTION_FAILED_MESSAGE = "Decryption operation failed";
    /**
     * The constant SIGNING_FAILED_MESSAGE.
     */
    public static final String SIGNING_FAILED_MESSAGE = "Signing operation failed";
    /**
     * The constant VERIFICATION_FAILED_MESSAGE.
     */
    public static final String VERIFICATION_FAILED_MESSAGE = "Signature verification failed";
    /**
     * The constant DEFAULT_PENDING_WINDOW_DAYS.
     */
    public static final Integer DEFAULT_PENDING_WINDOW_DAYS = 7;

    // Default Values
    /**
     * The constant MAX_PENDING_WINDOW_DAYS.
     */
    public static final Integer MAX_PENDING_WINDOW_DAYS = 30;
    /**
     * The constant MIN_PENDING_WINDOW_DAYS.
     */
    public static final Integer MIN_PENDING_WINDOW_DAYS = 7;
    /**
     * The constant DEFAULT_LIST_LIMIT.
     */
    public static final Integer DEFAULT_LIST_LIMIT = 100;
    /**
     * The constant MAX_LIST_LIMIT.
     */
    public static final Integer MAX_LIST_LIMIT = 1000;
    /**
     * The constant DEFAULT_AUDIT_LIMIT.
     */
    public static final Integer DEFAULT_AUDIT_LIMIT = 100;
    /**
     * The constant ARN_PREFIX.
     */
    public static final String ARN_PREFIX = "arn:kms";

    private KmsConstants() {
        // Utility class
    }
}

