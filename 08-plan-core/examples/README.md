# Plan examples and smoke test (Engine v2, request schema 2.0)

Golden scenario requests (`PlanningRequest` JSON, `meta.version` = `"2.0"`) and a smoke test for the running plan-service
(default `http://localhost:40417`, REST prefix `/api/v1/private/plan/job`). The scenario table below is design section 9.5
(`docs/ENGINE_V2_DESIGN.md`); the expected outcomes are asserted at service level by `EngineV2GoldenTest` and at job level by
the smoke test.

`value` = `ObjectiveScore.rawValue` of the stated tier (exact scaled integer), `score` = the human value (`rawValue / scale` for
decimal objectives such as costs, `rawValue` for tick/count objectives). Default `objectiveScale` is 100.

| Id  | File                                    | Scenario                                                                                   | Engine(s)            | Expected status                | Expected key facts |
|-----|-----------------------------------------|--------------------------------------------------------------------------------------------|----------------------|--------------------------------|--------------------|
| G1  | `g1-capacity-feasible.json`             | 3 one-hour sessions, lab with capacity 2, slots `slot-early` (cost 0) / `slot-late` (cost 1.0), `minimize_slot_cost` | cp_sat, timefold | `OPTIMAL` (timefold `FEASIBLE`) | 2 sessions in early, 1 in late; tier-0 value 100 (score 1.0); job `SUCCEEDED` |
| G2  | `g2-infeasible-capability.json`         | required task needs skill `welding`, no resource has it                                    | cp_sat, timefold     | `INFEASIBLE`                   | no solution; `core.method=PRESOLVE_ANALYSIS`, items `[TASK weld-job NO_ELIGIBLE_RESOURCE]`; job `INFEASIBLE` |
| G2b | `g2b-infeasible-core.json`              | A, B pinned at tick 0 (`Task.pin`) on two machines + hard `no_overlap` `no-overlap-ab` over tag `ab` | cp_sat      | `INFEASIBLE`                   | `core.method` in {`ASSUMPTIONS`, `ASSUMPTIONS_SHRUNK`}; items contain `CONSTRAINT no-overlap-ab`, `TASK A`, `TASK B` (fallback `NONE`/`minimal=false` only under the `T-EXPLAIN-FALLBACK` flag, risk R-5) |
| G3  | `g3-optional-priorities.json`           | 1 required + 3 optional tasks (priority 9/5/1), lab capacity 1, 2 one-hour slots, `maximize_task_priority` | cp_sat, timefold | `OPTIMAL` (timefold `FEASIBLE`) | `mandatory-session` + `optional-high` assigned; `optional-medium`/`optional-low` unassigned (`NOT_SELECTED`); tier-0 score 9.0 |
| G4  | `g4-precedence-chain.json`              | t1(3) -> t2(4) -> t3(5) on one machine, FS lag 0 then `minLag` 2, `minimize_makespan`      | cp_sat (timefold rejects `PRECEDENCES` + `FREE_START`) | `OPTIMAL` | starts 0, 3, 9; ends 3, 7, 14; makespan 14 |
| G5  | `g5-relaxation.json`                    | A, B (4 ticks) on one machine; `time_window` `latestEnd` tick 6 for both, RELAXABLE: `deadline-A` priority 5, `deadline-B` priority 0; `minimize_makespan` | cp_sat | `OPTIMAL` | exactly ONE relaxation `deadline-B` (`violationCount=1`, `measure=2`); makespan 8; `Metrics.tiers[-2].value = 1000`; `deadline-A` kept; single model build |
| G6  | `g6-cost-minimisation.json`             | 3 tasks of 60 ticks; `cheap` (costPerTick 1.0, unavailable from tick 120) and `expensive` (3.0); `minimize_resource_cost`; horizon 240 | cp_sat | `OPTIMAL` | 2 tasks on cheap, 1 on expensive; tier-0 value 30000 (score 300.0); with `objectiveScale=1000` value 300000 |
| G7  | generated in code (`ScenarioFixtures.bigJobShop`) | seeded 30 jobs x 15 machines job-shop, `maxTimeSeconds=60`, `minimize_makespan`, token cancelled after 2 s | cp_sat | `STOPPED_FEASIBLE` (`UNKNOWN` only without incumbent) | returns <= 5 s after the cancel; `search.solutionsFound >= 1`; job level: STOPPING -> CANCELLED with the result persisted |
| G8  | `g8-jobshop.json`                       | 3 jobs x 2 operations on 2 machines                                                        | cp_sat               | `OPTIMAL`                      | makespan 75; precedence respected; machines never overlap |
| G9  | `g9-roster.json`                        | 5 pinned shifts, 3 nurses, min rest 11, max 8 ticks per day, skills, fairness              | cp_sat               | `OPTIMAL`                      | `minimize_max_load` tier-0 value 16; `d1-night` on `ana`; no soft violation |
| G10 | `g10-project.json`                      | crew pool of capacity 2, 5 activities, precedence, deadline                                | cp_sat               | `OPTIMAL`                      | makespan 11; E ends at 11 <= deadline 12; pool load never above 2 |
| G11 | `g11-prize-collecting.json`             | 5 optional tasks, one technician, 480 ticks, automatic objective                           | cp_sat               | `OPTIMAL`                      | P1, P2, P5 selected; tier -1 score 190; P3, P4 unassigned with `forgonePrize` 90 / 30 |
| G12 | `03-plan-starter-parent/src/test/resources/requests/school-timetable.json` | 4 lessons, 2 teachers, class no-overlap | cp_sat, timefold (see note) | `OPTIMAL` (timefold `FEASIBLE`) | 2 lessons Monday, 2 Tuesday; tier-0 value 200 (score 2.0) |
| G13 | G8 with `deterministic=true`, run twice (also `numWorkers=4`) | determinism                                                                | cp_sat               | `OPTIMAL`                      | identical assignments (JSON equal ignoring `Metrics`) |
| G14 | `g14-setup-times.json`                  | 3 tasks (classes A, A, B) on one unary machine, setup 5 between different classes, `minimize_makespan` (tier 0) + `minimize_setup` (tier 1) | cp_sat | `OPTIMAL` | makespan 35; setup tier value 5; the two A tasks adjacent |

Notes:

* G12 on Timefold: the matrix of design 4.3 does not give `no_overlap` a Timefold contributor, so the file as written (with the
  `class-no-overlap` constraint) is rejected by Timefold. The 4 lessons of the file never collide per class (math lessons use the
  `p1` slots, science lessons the `p2` slots), so the Timefold run uses the same request without the constraint
  (`ScenarioFixtures.schoolTimetableWithoutNoOverlap()`).
* Rejection tests (`engine=timefold`, validation level): G4, G8, G9, G10, G11 must report the exact `ENGINE_UNSUPPORTED_*` code and
  `engineCompatible = {cp_sat:true, timefold:false}`.
* Every file carries a placeholder `meta.clientRef`. `clientRef` is an optional client-chosen idempotency key (per tenant, at most
  128 characters): resubmitting the same value returns the existing job instead of creating a second one. The smoke test replaces
  it with a fresh value on every run. The job itself is identified by the server-generated `Long` id and the business code
  (`PLN-000001`).
* Times are ISO instants and must sit on the tick grid of `horizon.tickSeconds` (`TIME_NOT_ALIGNED` otherwise). Durations, lags and
  `minRestTicks` are in ticks.

## Run

```powershell
./smoke-test.ps1                                  # G1, G2, G3, G8, G9, G10, G11 on cp_sat
./smoke-test.ps1 -Scenario g8 -Token $env:TOKEN   # one scenario, with bearer token
./smoke-test.ps1 -Engine timefold                 # G1, G2, G3 on Timefold + rejection checks for G8-G11
```

```bash
BASE_URL=http://localhost:40417 TOKEN=<jwt> ./smoke-test.sh [g1|g2|g3|g8|g9|g10|g11|all]   # needs curl and jq
ENGINE=timefold ./smoke-test.sh                                                           # Timefold subset
```

The script waits for `/actuator/health` (`UP`), calls `GET /capabilities` and `POST /validate` once, then for each scenario
submits the job (`POST /submit`, expects `202` and a `PlanJobDto` with `id`, `code`, `status`), polls `GET /{id}` until
`SUCCEEDED`, `INFEASIBLE`, `FAILED` or `CANCELLED` (timeout 120 s), reads `GET /{id}/result`, and exits `0` only when job status,
solver status and the key values of the table (tier values, makespan, unassigned tasks, core method...) match. With
`-Engine timefold` / `ENGINE=timefold` the solver status of G1 and G3 is `FEASIBLE` (Timefold never proves optimality) and
`POST /validate?engine=timefold` must flag G8, G9, G10, G11 as `engineCompatible.timefold=false`. `-Token` / `TOKEN` is an
optional bearer token: the plan API sits behind the stack JWT filter, so a token may be required depending on the profile.
Manual submission example:

```bash
curl -X POST http://localhost:40417/api/v1/private/plan/job/submit \
  -H 'Content-Type: application/json' -d @g1-capacity-feasible.json
```
