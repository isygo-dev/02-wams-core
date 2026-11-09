#!/usr/bin/env bash
# Smoke test for the plan-service REST API (bash equivalent of smoke-test.ps1), Engine v2 golden scenarios.
# Usage: BASE_URL=http://localhost:40417 TOKEN=<jwt> [ENGINE=cp_sat|timefold] ./smoke-test.sh [g1|g2|g3|g8|g9|g10|g11|all]
#   ENGINE=timefold runs only the Timefold-compatible subset (G1, G2, G3: solver status FEASIBLE, G2 INFEASIBLE) and
#   asserts via POST /validate?engine=timefold that G8, G9, G10, G11 are rejected (engineCompatible.timefold=false).
# Requires: curl, jq. Exit code 0 = all steps passed.
set -u

BASE_URL="${BASE_URL:-http://localhost:40417}"
BASE_URL="${BASE_URL%/}"
TOKEN="${TOKEN:-}"
ENGINE="${ENGINE:-cp_sat}"
SCENARIO="${1:-all}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-180}"
JOB_TIMEOUT="${JOB_TIMEOUT:-120}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API="$BASE_URL/api/v1/private/plan/job"
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

AUTH=()
[ -n "$TOKEN" ] && AUTH=(-H "Authorization: Bearer $TOKEN")

command -v jq >/dev/null || { echo "jq is required"; exit 2; }

# Scenario table (design 9.5): id, file, expected job status, expected solver status, jq check on the /result body.
# Sets: FILE EXP_JOB EXP_SOLVER CHECK_DESC CHECK_JQ
spec() {
  local id="$1"
  case "$id:$ENGINE" in
    g1:cp_sat)
      FILE="g1-capacity-feasible.json"; EXP_JOB="SUCCEEDED"; EXP_SOLVER="OPTIMAL"
      CHECK_DESC="3 assignments, 2 in slot-early, 1 in slot-late, tier-0 value 100"
      CHECK_JQ='(.solution.assignments|length)==3 and ([.solution.assignments[]|select(.slotId=="slot-early")]|length)==2 and ([.solution.assignments[]|select(.slotId=="slot-late")]|length)==1 and any(.solution.qualityDetails[]; .tier==0 and .rawValue==100)' ;;
    g1:timefold)
      FILE="g1-capacity-feasible.json"; EXP_JOB="SUCCEEDED"; EXP_SOLVER="FEASIBLE"
      CHECK_DESC="3 assignments, tier-0 value 100"
      CHECK_JQ='(.solution.assignments|length)==3 and any(.solution.qualityDetails[]; .tier==0 and .rawValue==100)' ;;
    g2:*)
      FILE="g2-infeasible-capability.json"; EXP_JOB="INFEASIBLE"; EXP_SOLVER="INFEASIBLE"
      CHECK_DESC="no solution, core PRESOLVE_ANALYSIS with TASK weld-job NO_ELIGIBLE_RESOURCE"
      CHECK_JQ='.solution==null and (.diagnostics.core.method // "")=="PRESOLVE_ANALYSIS" and any((.diagnostics.core.items // [])[]; .kind=="TASK" and .id=="weld-job" and .reasonCode=="NO_ELIGIBLE_RESOURCE")' ;;
    g3:cp_sat)
      FILE="g3-optional-priorities.json"; EXP_JOB="SUCCEEDED"; EXP_SOLVER="OPTIMAL"
      CHECK_DESC="optional-medium and optional-low NOT_SELECTED, tier-0 score 9.0"
      CHECK_JQ='([.solution.unassignedTasks[]|select(.reasonCode=="NOT_SELECTED")|.taskId]|sort)==["optional-low","optional-medium"] and any(.solution.qualityDetails[]; .tier==0 and .score==9)' ;;
    g3:timefold)
      FILE="g3-optional-priorities.json"; EXP_JOB="SUCCEEDED"; EXP_SOLVER="FEASIBLE"
      CHECK_DESC="optional-medium and optional-low unassigned, tier-0 score 9.0"
      CHECK_JQ='([.solution.unassignedTasks[]|.taskId]|sort)==["optional-low","optional-medium"] and any(.solution.qualityDetails[]; .tier==0 and .score==9)' ;;
    g8:cp_sat)
      FILE="g8-jobshop.json"; EXP_JOB="SUCCEEDED"; EXP_SOLVER="OPTIMAL"
      CHECK_DESC="6 assignments, makespan 75"
      CHECK_JQ='(.solution.assignments|length)==6 and ([.solution.assignments[]|.endTick]|max)==75' ;;
    g9:cp_sat)
      FILE="g9-roster.json"; EXP_JOB="SUCCEEDED"; EXP_SOLVER="OPTIMAL"
      CHECK_DESC="fairness tier-0 value 16, d1-night on ana, no soft violation"
      CHECK_JQ='any(.solution.qualityDetails[]; .tier==0 and .rawValue==16) and any(.solution.assignments[]; .taskId=="d1-night" and any(.resources[]; .resourceId=="ana")) and (.solution.softViolations|length)==0' ;;
    g10:cp_sat)
      FILE="g10-project.json"; EXP_JOB="SUCCEEDED"; EXP_SOLVER="OPTIMAL"
      CHECK_DESC="makespan 11, E ends at 11 (deadline 12)"
      CHECK_JQ='([.solution.assignments[]|.endTick]|max)==11 and any(.solution.assignments[]; .taskId=="E" and .endTick==11)' ;;
    g11:cp_sat)
      FILE="g11-prize-collecting.json"; EXP_JOB="SUCCEEDED"; EXP_SOLVER="OPTIMAL"
      CHECK_DESC="P1,P2,P5 selected, P3/P4 unassigned (forgone 90/30), tier -1 score 190"
      CHECK_JQ='([.solution.assignments[]|.taskId]|sort)==["P1","P2","P5"] and ([.solution.unassignedTasks[]|[.taskId,.forgonePrize]]|sort)==[["P3",90],["P4",30]] and any(.solution.qualityDetails[]; .tier==-1 and .score==190)' ;;
    *) return 1 ;;
  esac
  return 0
}

if [ "$ENGINE" = "timefold" ]; then
  IDS=(g1 g2 g3)
  REJECTED=(g8-jobshop.json g9-roster.json g10-project.json g11-prize-collecting.json)
else
  IDS=(g1 g2 g3 g8 g9 g10 g11)
  REJECTED=()
fi

echo "Waiting for $BASE_URL/actuator/health ..."
end=$((SECONDS + HEALTH_TIMEOUT))
until [ "$(curl -fsS -m 5 "$BASE_URL/actuator/health" 2>/dev/null | jq -r '.status' 2>/dev/null)" = "UP" ]; do
  [ $SECONDS -gt $end ] && { echo "Service not healthy after ${HEALTH_TIMEOUT}s"; exit 2; }
  sleep 3
done
echo "Service is UP (engine under test: $ENGINE)"

failed=0

# Extra smoke steps: capabilities and validate (no job is created).
code="$(curl -sS -o "$TMP" -w '%{http_code}' "${AUTH[@]}" -H 'Accept: application/json' "$API/capabilities")"
if [ "$code" = "200" ]; then echo "PASS capabilities"; else echo "FAIL capabilities: HTTP $code: $(cat "$TMP")"; failed=$((failed + 1)); fi

spec "${IDS[0]}"
code="$(curl -sS -o "$TMP" -w '%{http_code}' -X POST "$API/validate" \
  "${AUTH[@]}" -H 'Content-Type: application/json' -H 'Accept: application/json' -d @"$DIR/$FILE")"
if [ "$code" = "200" ]; then echo "PASS validate"; else echo "FAIL validate: HTTP $code: $(cat "$TMP")"; failed=$((failed + 1)); fi

# Timefold rejection checks (validation level only, no job).
for file in "${REJECTED[@]}"; do
  [ "$SCENARIO" != "all" ] && continue
  code="$(curl -sS -o "$TMP" -w '%{http_code}' -X POST "$API/validate?engine=timefold" \
    "${AUTH[@]}" -H 'Content-Type: application/json' -H 'Accept: application/json' -d @"$DIR/$file")"
  if [ "$code" = "200" ] && [ "$(jq -r '.engineCompatible.timefold' "$TMP")" = "false" ] \
     && [ "$(jq -r '.engineCompatible.cp_sat' "$TMP")" = "true" ]; then
    echo "PASS reject-timefold $file"
  else
    echo "FAIL reject-timefold $file: HTTP $code: $(head -c 400 "$TMP")"; failed=$((failed + 1))
  fi
done

for id in "${IDS[@]}"; do
  [ "$SCENARIO" != "all" ] && [ "$SCENARIO" != "$id" ] && continue
  spec "$id" || { echo "SKIP $id: not part of engine $ENGINE"; continue; }

  ref="smoke-$id-$(date +%s)-$RANDOM"
  body="$(jq --arg ref "$ref" --arg engine "$ENGINE" '.meta.clientRef = $ref | .solverOptions.engine = $engine' "$DIR/$FILE")"
  echo
  echo "== $id ($FILE) engine=$ENGINE clientRef=$ref"

  code="$(curl -sS -o "$TMP" -w '%{http_code}' -X POST "$API/submit" \
    "${AUTH[@]}" -H 'Content-Type: application/json' -H 'Accept: application/json' -d "$body")"
  if [ "$code" != "202" ]; then
    echo "FAIL $id: submit returned HTTP $code: $(cat "$TMP")"
    failed=$((failed + 1)); continue
  fi
  job_id="$(jq -r '.id' "$TMP")"
  echo "submitted: id=$job_id code=$(jq -r '.code' "$TMP") status=$(jq -r '.status' "$TMP")"

  end=$((SECONDS + JOB_TIMEOUT)); status=""; status_json="{}"
  while :; do
    sleep 1
    status_json="$(curl -fsS "${AUTH[@]}" -H 'Accept: application/json' "$API/$job_id")"
    status="$(echo "$status_json" | jq -r '.status')"
    case "$status" in SUCCEEDED|INFEASIBLE|FAILED|CANCELLED) break ;; esac
    if [ $SECONDS -gt $end ]; then echo "job did not finish within ${JOB_TIMEOUT}s (last status $status)"; break; fi
  done
  echo "job status: $status $(echo "$status_json" | jq -r '[.errorCode, .message] | map(select(. != null)) | join(" ")')"

  solver=""; check_ok="false"
  if result="$(curl -fsS "${AUTH[@]}" -H 'Accept: application/json' "$API/$job_id/result")"; then
    solver="$(echo "$result" | jq -r '.status')"
    echo "result: solver status=$solver assignments=$(echo "$result" | jq '(.solution.assignments // []) | length') unassigned=$(echo "$result" | jq '(.solution.unassignedTasks // []) | length')"
    check_ok="$(echo "$result" | jq "$CHECK_JQ" 2>/dev/null || echo false)"
  else
    echo "result: not available"
  fi

  if [ "$status" = "$EXP_JOB" ] && [ "$solver" = "$EXP_SOLVER" ] && [ "$check_ok" = "true" ]; then
    echo "PASS $id ($CHECK_DESC)"
  else
    echo "FAIL $id: expected job=$EXP_JOB solver=$EXP_SOLVER and [$CHECK_DESC] (values check: $check_ok)"
    failed=$((failed + 1))
  fi
done

echo
if [ "$failed" -gt 0 ]; then echo "$failed step(s) failed"; exit 1; fi
echo "All steps passed"
