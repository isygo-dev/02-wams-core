package eu.isygoit.model.schema;

/**
 * The interface Schema column constant name.
 */
public interface SchemaColumnConstantName extends ComSchemaColumnConstantName {

    /**
     * Cassandra only (plan_log_entries partition key, bigint = plan_job.id). The relational plan_job table has no
     * job_id column: its primary key is the stack C_ID.
     */
    String C_JOB_ID = "job_id";
    /**
     * Optional client-chosen idempotency reference of a job (unique per tenant when present).
     */
    String C_CLIENT_REF = "client_ref";
    String C_SUBMITTED_AT = "submitted_at";
    String C_STARTED_AT = "started_at";
    String C_DISPATCHED_AT = "dispatched_at";
    String C_DISPATCH_ATTEMPTS = "dispatch_attempts";
    String C_COMPLETED_AT = "completed_at";
    String C_ERROR_CODE = "error_code";
    String C_MESSAGE = "message";
    String C_RESULT_JSON = "result_json";
    String C_REQUEST_JSON = "request_json";
    String C_DIAGNOSTICS_JSON = "diagnostics_json";
    String C_BUCKET_START = "bucket_start";
    String C_EVENT_TIME = "event_time";
    String C_EVENT_ID = "event_id";
    String C_LEVEL = "level";
    String C_STEP = "step";
    String C_ENGINE = "engine";
    String C_DETAILS = "details";
    String C_DURATION_MILLIS = "duration_millis";
    String C_TRACE_ID = "trace_id";
    String C_SPAN_ID = "span_id";
}
