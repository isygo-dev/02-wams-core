package eu.isygoit.model.schema;

import eu.isygoit.enums.IEnumJobStatusCode;

/**
 * The interface Schema constant size.
 */
public interface SchemaConstantSize extends ComSchemaConstantSize {

    /**
     * The constant CHAT_MESSAGE.
     */
    int CHAT_MESSAGE = 512;
    /**
     * The constant FROM_FULL_NAME.
     */
    int FROM_FULL_NAME = 128;

    int PLAN_JOB_STATUS = IEnumJobStatusCode.STR_ENUM_SIZE;
    int PLAN_ERROR_CODE = 128;
}
