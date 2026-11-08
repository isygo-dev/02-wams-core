# isygoPlan

isygoPlan accepts a generic planning problem, validates and normalizes it, then creates a tracked solve job. The
runnable API host is `10-starter-test/80-plan-management/04-plan-service`; persistence adapters are in `01-plan-jpa`;
shared request, domain, and port contracts are in `02-plan-shared`; application services, solver adapters, Kafka, and
Camel integrations are in `03-plan-starter-parent`.

## Flow

1. `POST /api/v1/private/plans/jobs` validates the request and persists an idempotent `CREATED` job row. With
   `app.plan.execution.kafka-enabled=true`, it sends the job through Camel's `direct:plan-job-queue` route and the Kafka
   consumer processes it sequentially. With the property set to `false`, a bounded in-process executor runs the same job
   processor without using Kafka for job dispatch.
2. Both modes return the created job status immediately. Clients poll `GET /jobs/{jobId}` until a terminal status, then
   fetch the persisted result with `GET /jobs/{jobId}/result`. Kafka mode commits the offset only after the terminal
   state/result has been persisted.
3. Job logs are sent asynchronously through Camel to the plan-log Kafka topic. A separate sequential Camel route writes
   logs to Cassandra and commits only after the write succeeds.
4. Status/result reads use Redis when `app.plan.cache.redis-enabled=true`, otherwise bounded Caffeine; PostgreSQL
   remains the source of truth.

The job row insert uses PostgreSQL `ON CONFLICT DO NOTHING`, so repeated submissions with the same `meta.jobId` do not
create duplicate jobs or enqueue duplicates. The Cassandra log primary key is `(job_id, bucket_start)` with descending
event-time clustering; six-hour buckets bound partition growth. The table uses 30-day TTL and
`TimeWindowCompactionStrategy`, and log-range reads clamp to the retained 30-day window. Production should provision the
Cassandra keyspace with an appropriate replication strategy before applying `01-plan-jpa/src/main/resources/schema.cql`.

## Supported planning semantics

- Tasks are required by default; required tasks receive exactly one resource/slot assignment. Set `required: false` for
  an optional task, which may be listed in `solution.unassignedTasks`. `maximize_task_priority` rewards assigning
  higher-priority optional tasks when capacity is limited. A resource is available in a slot up to its configured
  capacity; the default is one. Resource `attributes.capacity` and `resource_capacity` constraints may set a higher or
  lower limit. `task.candidates.capabilityTags` must all exist in `resource.attributes.capabilities`.
- `task_eligibility` constraints require a `scope` of `{ "kind": "task", "value": "<taskId>" }` and accept optional
  `params.resourceIds` and `params.slotIds` lists.
- `minimize_resource_cost` reads numeric `resource.attributes.cost`; `minimize_slot_cost` reads numeric
  `slot.attributes.cost`; `maximize_task_priority` reads numeric `task.attributes.priority`. Objective weights scale the
  contributions.
- `SOFT` is supported for `task_eligibility`; `resource_capacity` must use hard enforcement. Authorized relaxable
  constraints are considered in ascending `relaxation.priority`, and `solverOptions.relaxationLevels`, when supplied, is
  an allow-list of constraint IDs. `maxRelaxation` is binary: `0` keeps a constraint active and `1` permits dropping the
  whole constraint; fractional values are rejected. `STRICTLY_REQUIRED` constraints are never relaxed.
- Available engines are `cp_sat` and `timefold`. Solver time is bounded by `solverOptions.maxTimeSeconds`.
- Timefold uses 32-bit hard/soft scores; requests whose worst-case combined soft score exceeds `Integer.MAX_VALUE` are
  rejected instead of failing during solving.

The request schema and a runnable example are in `04-plan-starter-ui/src/main/resources/schemas` and `.../examples`. The
smoke script submits a new job ID and polls for completion.

## Local configuration

Set `PLAN_JDBC_PASSWORD`, then start the local dependencies and application with Docker Compose. The compose setup
initializes the PostgreSQL job table and Cassandra log table; Kafka uses a single-node KRaft broker.

```powershell
docker compose up --build
```

For running the jar outside Compose, the default `local` profile expects PostgreSQL at `localhost:5432`, Kafka at
`localhost:9094`, and Cassandra at `localhost:9042`. `PLAN_KAFKA_ENABLED` defaults to `true` (Kafka job dispatch); set
it to `false` to dispatch jobs directly to the in-process executor. The plan-log routes continue to use Kafka in either
mode. The Compose Kafka broker advertises `kafka:9092` to containers and `localhost:9094` to host clients. Set
`PLAN_JDBC_URL`, `PLAN_JDBC_USERNAME`, `PLAN_JDBC_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `PLAN_CASSANDRA_CONTACT_POINTS`,
`PLAN_CASSANDRA_LOCAL_DATACENTER`, and `PLAN_CASSANDRA_KEYSPACE` as needed.

Set `SPRING_PROFILES_ACTIVE=prod` in production and provide the required database, Kafka, Cassandra, and Redis
environment variables. Do not rely on the local Cassandra `SimpleStrategy` keyspace initialization in production.

## API

- `POST /api/v1/private/plans/jobs` — submit a `PlanningRequest`; returns `202 Accepted` with `CREATED` status in either
  execution mode.
- `GET /api/v1/private/plans/jobs/{jobId}` — fetch the persisted job status.
- `GET /api/v1/private/plans/jobs/summary?limit=50` — fetch total/status counts and recent jobs for the management
  dashboard.
- `GET /api/v1/private/plans/jobs/{jobId}/result` — fetch the result after completion.
- `GET /api/v1/private/plans/jobs/{jobId}/logs` — fetch recent logs or logs within an ISO-8601 `from`/`to` range;
  optional `minLevel` and `limit` parameters.
- `DELETE /api/v1/private/plans/jobs/{jobId}` — cancel a queued job; returns `409` if it is already processing or
  terminal.

## Management UI

The Vaadin UI is implemented in `04-plan-starter-ui`, following the MMS starter UI layout and shared dashboard
components. The runnable host `10-starter-test/80-plan-management/04-plan-service` includes this UI starter and its
views call the plan API through a Feign client rather than using persistence services directly. Open `/plan` for the
dashboard, status summary, recent jobs, and job-ID lookup. Open a job from the grid to follow status updates and inspect
its result; the job view links to the job's recent logs.

- `GET /actuator/health` — health/liveness endpoint.

## Build and smoke check

From `08-plan-core`, build the runnable module and its reactor dependencies with
`mvn -pl 04-plan-starter-ui -am package`. After starting the application, run
`04-plan-starter-ui/src/main/resources/examples/smoke-test.ps1`. This task intentionally does not run compilation or
tests.
