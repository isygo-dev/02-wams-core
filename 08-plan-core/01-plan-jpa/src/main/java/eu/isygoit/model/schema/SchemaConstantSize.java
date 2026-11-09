package eu.isygoit.model.schema;

import eu.isygoit.enums.IEnumJobStatusCode;

/**
 * The interface Schema constant size.
 */
public interface SchemaConstantSize extends ComSchemaConstantSize {

    int PLAN_JOB_STATUS = IEnumJobStatusCode.STR_ENUM_SIZE;
    int PLAN_ERROR_CODE = 128;
    int PLAN_MESSAGE = 1000;
    int PLAN_CLIENT_REF = 128;
    int PLAN_ENGINE = 64;
}
