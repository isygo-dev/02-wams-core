# isygoPlan Implementation Plan

## Scope and constraints

- Replace the copied MMS service contents in `08-plan-core` with the generic planning service described in the request.
- Preserve the MMS-style four-module layout: `01-plan-jpa`, `02-plan-shared`, `03-plan-starter-parent`, and
  `04-plan-starter-ui`. Refactor package roots and class relationships to follow MMS: JPA models/repositories, shared
  DTOs/APIs, parent services/controllers/mappers/configuration, and a thin UI starter with resources. Do not retain the
  current `plan`-prefixed hexagonal package architecture as the final structure.
- Use MMS stack inheritance and base classes where they fit the planning service's semantics, including
  `AuditableEntity`/`AbstractEntity`, stack repository bases, shared service APIs, and mapped controller/service
  patterns. Keep solver and planning behavior generic; do not copy MMS-only mail, tenancy, or messaging features.
- Preserve the repository's existing Java and dependency-version management. Do not change the root `service-core` POM
  or existing dependency versions; add versions only for newly introduced libraries that have no existing managed
  version.
- Keep solver-specific classes out of domain and application modules, process each Kafka job sequentially, persist
  before acknowledging, and keep log persistence off the solve path.
- Remove MMS-only packages, resources, and generated build output from this copied module.
- Do not compile or run tests. Finish with source/configuration review only.

## Ordered implementation steps

1. **Inventory the template** — compare the copied module with MMS, inspect its POMs, package layout, application entry
   points, and inherited version management. Identify the minimum existing infrastructure that is genuinely reusable.
2. **Adapt the existing four Maven modules** — retain JPA/shared/starter-parent/starter-UI, replace their MMS artifact
   identities/dependencies, and keep the existing parent/version values.
3. **Implement the planning IR** — add immutable generic task/resource/slot/constraint/objective/request/result models,
   enforcement and relaxation policy types, options, validation errors, and a solver-independent solution model in the
   shared module.
4. **Implement shared APIs and services** — expose request/result DTOs and service APIs through MMS-style shared
   packages; place implementation services and solver contracts in the starter-parent module.
5. **Implement solver adapters** — add independent CP-SAT and Timefold packages in the starter-parent module, generic
   supported constraint/objective handlers, optional-task selection, model validation, solver limits, and sequential
   relaxation that never removes strictly-required constraints.
6. **Implement persistence and messaging** — place entities and repositories in MMS-style `eu.isygoit.model` and
   `eu.isygoit.repository` packages, then add Caffeine status cache, optional Redis behavior, and Camel-based Kafka
   producers/consumers. Concrete Kafka producers must extend the shared `eu.isygoit.com.event` producer types; Camel
   direct routes dispatch to typed processors that invoke those inherited producer methods, following the
   `KafkaEmailSenderConsumerProcessor` pattern. Keep job processing sequential with manual acknowledgement after
   persistence, make log publication best-effort, use a separate log consumer, and persist logs in Cassandra.
7. **Implement controllers and bootstrap** — place shared service APIs in `eu.isygoit.api`, mapped controllers in the
   starter-parent module, and a thin Spring Boot service runner/entry point in starter-ui, following MMS patterns where
   compatible. Add service-specific resources modeled on `10-starter-test/80-plan-management/04-plan-service` (filtered
   application metadata, optional Config Server import, banner, localization and logging resources), plus schemas,
   examples, Spring profiles, native OR-Tools loading, Docker/Compose artifacts, README, and smoke-test script. Define
   Camel routes in classpath XML and keep only route processors in Java; do not copy unrelated Hazelcast, mail,
   WebSocket, upload, or template resources.
8. **Remove copied MMS residue** — delete unrelated MMS UI, messaging, mail, WebSocket, model, resource, and
   generated-target content; verify no MMS package or artifact references remain under `08-plan-core`.
9. **Review against requirements** — inspect dependency directions, version changes, generic behavior, Camel-to-Kafka
   route wiring and commit ordering, service runner/resource wiring, cache loader setup, Cassandra key/TTL/compaction
   choices, production endpoint profile, and deliverable inventory. Do not compile or execute tests.
10. **Refactor to MMS architecture** — migrate the existing planning implementation into the package and inheritance
    conventions above without changing the four-module Maven hierarchy or losing planning behavior; remove obsolete
    hexagonal package roots after their responsibilities have been relocated.

## Known compatibility decisions

- The current repository parent sets Java 17 and Spring Boot `3.0.7`. Those existing values are preserved as requested
  rather than rewriting the parent to the separately listed future-version matrix.
- The starter-parent and starter-ui POMs follow the MMS hierarchy: they inherit `service-stack-parent` and import
  `service-core` dependency management. The JPA/shared modules inherit `plan-core`; solver version properties therefore
  live in the starter-parent POM.
- `maxRelaxation` is a binary setting: only `0` and `1` are valid; `1` authorizes dropping the whole constraint during
  ordered relaxation, not partial violation. Keep request validation and the JSON schema aligned with this contract.
- The new solver libraries will use the requested Java-17-compatible versions unless an already managed repository
  version is found during inventory.
- Local/container services and cloud brokers are not assumed to be available during implementation.

## MMS architecture refactor progress

- JPA/Cassandra models and repository interfaces have started moving to `eu.isygoit.model` and `eu.isygoit.repository`;
  the PostgreSQL job entity now uses the stack `AuditableEntity` base, and the Cassandra log model uses
  `AbstractEntity`.
- Persistence adapters have moved to the starter-parent `eu.isygoit.service.impl` package. The REST contract is now in
  shared `eu.isygoit.api`, its controller and exception handler are in the starter-parent, and the UI entry point is now
  the root-package `PlanStarter`.
- The starter now has filtered application version metadata, optional Config Server import, a plan-specific banner,
  OpenAPI metadata, and console plus rolling-file Logback configuration. These are selectively adapted from
  `80-plan-management/04-plan-service`; no `ServiceStarter` API-extraction hook was added because this service has no
  API-permission model or extractor.
- `10-starter-test/80-plan-management/04-plan-service` is the runnable plan API and messaging host. It depends on
  `isygo-plan-starter-parent`, uses the root-package Spring Boot entry point without the unrelated API-extractor hook,
  and loads job/log publisher-consumer routes from `camel/plan-*-camel-routes.xml`. The duplicate route resources were
  removed from `04-plan-starter-ui` so the same Kafka consumers are not started by both runtimes.
- Java `RouteBuilder` classes were removed; typed producer/consumer processors retain serialization, persistence, and
  manual-ack behavior, while Camel route definitions remain XML-only.
- Remaining work: move the rest of the planning services, DTOs, enums and exceptions into the MMS shared/parent package
  conventions; align service/controller inheritance and mapper patterns; review entity schemas and bootstrapping
  configuration. The four-module Maven hierarchy and existing version management remain unchanged.
- `PlanningService.normalizeAndValidate` now resolves the configured solver before job submission can persist or dispatch
  the request. A new `PlanningServiceTest` covers request validation, unsupported solver/constraint rejection,
  required/optional result consistency, capacity and eligibility checks, infeasible/unknown results, and solver errors.
- Processing claims now stop duplicate workers from solving an already-claimed job, and unexpected solver runtime
  failures are persisted as terminal failures. Created jobs persist their request JSON and are periodically redispatched
  in both Kafka and local-executor modes, closing the crash window between creation and initial dispatch.
- Apply `01-plan-jpa/src/main/resources/db/upgrade/plan-job-request-recovery.sql` before deploying this change; the
  service uses `ddl-auto: validate` and does not auto-apply the new request-payload column.
- `SolverOptions` now applies the same documented defaults to omitted fields in a partially specified options object.
  The README no longer points to a JSON Schema path that is absent from the starter UI module.
- Infeasible result lookups now reconstruct a `SolveResult` from the persisted diagnostics when no result JSON exists.
  Generic input maps now retain JSON `null` values instead of failing during defensive copying.
- Final source review still needs to address other remaining requirements. These changes do not alter the four-module
  layout.
- As previously requested, this phase is source-only: do not compile or run tests.

## Completion record

| Step                                          | Status      |
|-----------------------------------------------|-------------|
| 1. Inventory the template                     | Done        |
| 2. Adapt the existing four Maven modules      | Done        |
| 3. Implement the planning IR                  | Done        |
| 4. Implement application and engine contracts | Done        |
| 5. Implement solver adapters                  | In progress |
| 6. Implement infrastructure                   | In progress |
| 7. Implement API and bootstrap                | In progress |
| 8. Remove copied MMS residue                  | Done        |
| 9. Review against requirements                | In progress |
| 10. Refactor to MMS architecture              | In progress |
