<#
.SYNOPSIS
  Smoke test for the plan-service REST API (Engine v2 golden scenarios G1, G2, G3, G8, G9, G10, G11).
.DESCRIPTION
  Waits for /actuator/health, calls GET /capabilities and POST /validate once, then submits the golden scenarios
  (POST /submit, fresh meta.clientRef per run), polls the job by its Long id until it reaches a terminal status,
  fetches the result and compares job status, solver status and the key values of design 9.5 with the expected ones.
  With -Engine timefold only the Timefold-compatible subset (G1, G2, G3; solver status FEASIBLE except G2 INFEASIBLE)
  is submitted and POST /validate?engine=timefold must reject G8, G9, G10, G11 (engineCompatible.timefold = false).
  Exit code 0 = all passed.
.EXAMPLE
  ./smoke-test.ps1
  ./smoke-test.ps1 -BaseUrl http://localhost:40417 -Token $env:PLAN_TOKEN -Scenario g8
  ./smoke-test.ps1 -Engine timefold
#>
param(
    [string]$BaseUrl = $(if ($env:BASE_URL) { $env:BASE_URL } else { 'http://localhost:40417' }),
    [string]$Token = $env:TOKEN,                       # optional bearer token (JWT) for /api/v1/private/**
    [ValidateSet('all', 'g1', 'g2', 'g3', 'g8', 'g9', 'g10', 'g11')][string]$Scenario = 'all',
    [ValidateSet('cp_sat', 'timefold')][string]$Engine = 'cp_sat',
    [int]$HealthTimeoutSeconds = 180,
    [int]$JobTimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$api = "$BaseUrl/api/v1/private/plan/job"
$terminal = @('SUCCEEDED', 'INFEASIBLE', 'FAILED', 'CANCELLED')
$headers = @{ 'Accept' = 'application/json' }
if ($Token) { $headers['Authorization'] = "Bearer $Token" }

function Has-Score($result, [int]$tier, [string]$field, [double]$value) {
    foreach ($q in @($result.solution.qualityDetails)) {
        if ($q.tier -eq $tier -and [double]$q.$field -eq $value) { return $true }
    }
    return $false
}
function Max-End($result) { (@($result.solution.assignments) | Measure-Object -Property endTick -Maximum).Maximum }
function Sorted-Ids($items) { (@($items) | ForEach-Object { $_.taskId } | Sort-Object) -join ',' }

# Scenario table (design 9.5): Check = scriptblock receiving the /result body, returns $true when the key values match.
$cpSat = @(
    @{ Id = 'g1'; File = 'g1-capacity-feasible.json'; ExpectedJob = 'SUCCEEDED'; ExpectedSolver = 'OPTIMAL'
       Desc = '3 assignments, 2 in slot-early, 1 in slot-late, tier-0 value 100'
       Check = { param($r)
           $a = @($r.solution.assignments)
           $a.Count -eq 3 -and @($a | Where-Object { $_.slotId -eq 'slot-early' }).Count -eq 2 -and
               @($a | Where-Object { $_.slotId -eq 'slot-late' }).Count -eq 1 -and (Has-Score $r 0 'rawValue' 100) } },
    @{ Id = 'g2'; File = 'g2-infeasible-capability.json'; ExpectedJob = 'INFEASIBLE'; ExpectedSolver = 'INFEASIBLE'
       Desc = 'no solution, core PRESOLVE_ANALYSIS with TASK weld-job NO_ELIGIBLE_RESOURCE'
       Check = { param($r)
           $null -eq $r.solution -and $r.diagnostics.core.method -eq 'PRESOLVE_ANALYSIS' -and
               @($r.diagnostics.core.items | Where-Object { $_.kind -eq 'TASK' -and $_.id -eq 'weld-job' -and $_.reasonCode -eq 'NO_ELIGIBLE_RESOURCE' }).Count -ge 1 } },
    @{ Id = 'g3'; File = 'g3-optional-priorities.json'; ExpectedJob = 'SUCCEEDED'; ExpectedSolver = 'OPTIMAL'
       Desc = 'optional-medium and optional-low NOT_SELECTED, tier-0 score 9.0'
       Check = { param($r)
           (Sorted-Ids ($r.solution.unassignedTasks | Where-Object { $_.reasonCode -eq 'NOT_SELECTED' })) -eq 'optional-low,optional-medium' -and
               (Has-Score $r 0 'score' 9) } },
    @{ Id = 'g8'; File = 'g8-jobshop.json'; ExpectedJob = 'SUCCEEDED'; ExpectedSolver = 'OPTIMAL'
       Desc = '6 assignments, makespan 75'
       Check = { param($r) @($r.solution.assignments).Count -eq 6 -and (Max-End $r) -eq 75 } },
    @{ Id = 'g9'; File = 'g9-roster.json'; ExpectedJob = 'SUCCEEDED'; ExpectedSolver = 'OPTIMAL'
       Desc = 'fairness tier-0 value 16, d1-night on ana, no soft violation'
       Check = { param($r)
           $night = @($r.solution.assignments | Where-Object { $_.taskId -eq 'd1-night' })
           (Has-Score $r 0 'rawValue' 16) -and $night.Count -eq 1 -and
               @($night[0].resources | Where-Object { $_.resourceId -eq 'ana' }).Count -eq 1 -and
               @($r.solution.softViolations).Count -eq 0 } },
    @{ Id = 'g10'; File = 'g10-project.json'; ExpectedJob = 'SUCCEEDED'; ExpectedSolver = 'OPTIMAL'
       Desc = 'makespan 11, E ends at 11 (deadline 12)'
       Check = { param($r)
           (Max-End $r) -eq 11 -and @($r.solution.assignments | Where-Object { $_.taskId -eq 'E' -and $_.endTick -eq 11 }).Count -eq 1 } },
    @{ Id = 'g11'; File = 'g11-prize-collecting.json'; ExpectedJob = 'SUCCEEDED'; ExpectedSolver = 'OPTIMAL'
       Desc = 'P1,P2,P5 selected, P3/P4 unassigned (forgone 90/30), tier -1 score 190'
       Check = { param($r)
           (Sorted-Ids $r.solution.assignments) -eq 'P1,P2,P5' -and (Sorted-Ids $r.solution.unassignedTasks) -eq 'P3,P4' -and
               (@($r.solution.unassignedTasks | Where-Object { ($_.taskId -eq 'P3' -and $_.forgonePrize -eq 90) -or ($_.taskId -eq 'P4' -and $_.forgonePrize -eq 30) }).Count -eq 2) -and
               (Has-Score $r -1 'score' 190) } }
)
if ($Engine -eq 'timefold') {
    # Timefold never reports OPTIMAL (FEASIBLE at best); G2 is INFEASIBLE through the pre-solve analyzer.
    $scenarios = @($cpSat | Where-Object { @('g1', 'g2', 'g3') -contains $_.Id })
    foreach ($s in $scenarios) { if ($s.ExpectedSolver -eq 'OPTIMAL') { $s.ExpectedSolver = 'FEASIBLE' } }
    $rejected = @('g8-jobshop.json', 'g9-roster.json', 'g10-project.json', 'g11-prize-collecting.json')
} else {
    $scenarios = $cpSat
    $rejected = @()
}
$allScenarios = $scenarios
if ($Scenario -ne 'all') { $scenarios = @($scenarios | Where-Object { $_.Id -eq $Scenario }) }

Write-Host "Waiting for $BaseUrl/actuator/health ..."
$deadline = (Get-Date).AddSeconds($HealthTimeoutSeconds)
while ($true) {
    try {
        $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
        if ($health.status -eq 'UP') { Write-Host "Service is UP (engine under test: $Engine)"; break }
    } catch { }
    if ((Get-Date) -gt $deadline) { Write-Host "Service not healthy after $HealthTimeoutSeconds s"; exit 2 }
    Start-Sleep -Seconds 3
}

$failed = 0

# Extra smoke steps: capabilities and validate (no job is created).
try {
    $null = Invoke-RestMethod -Uri "$api/capabilities" -Headers $headers
    Write-Host 'PASS capabilities'
} catch {
    Write-Host "FAIL capabilities: $($_.Exception.Message)"; $failed++
}
try {
    $validateBody = Get-Content -Raw -Path (Join-Path $PSScriptRoot $allScenarios[0].File)
    $null = Invoke-RestMethod -Method Post -Uri "$api/validate" -Headers $headers -ContentType 'application/json' -Body $validateBody
    Write-Host 'PASS validate'
} catch {
    Write-Host "FAIL validate: $($_.Exception.Message)"; $failed++
}

# Timefold rejection checks (validation level only, no job).
if ($Scenario -eq 'all') {
    foreach ($file in $rejected) {
        try {
            $body = Get-Content -Raw -Path (Join-Path $PSScriptRoot $file)
            $report = Invoke-RestMethod -Method Post -Uri "$api/validate?engine=timefold" -Headers $headers -ContentType 'application/json' -Body $body
            if ($report.engineCompatible.timefold -eq $false -and $report.engineCompatible.cp_sat -eq $true) {
                Write-Host "PASS reject-timefold $file"
            } else {
                Write-Host "FAIL reject-timefold ${file}: engineCompatible=$($report.engineCompatible | ConvertTo-Json -Compress)"; $failed++
            }
        } catch {
            Write-Host "FAIL reject-timefold ${file}: $($_.Exception.Message)"; $failed++
        }
    }
}

foreach ($s in $scenarios) {
    $clientRef = "smoke-$($s.Id)-$([guid]::NewGuid().ToString('N'))"
    $body = Get-Content -Raw -Path (Join-Path $PSScriptRoot $s.File)
    $body = [regex]::Replace($body, '("clientRef"\s*:\s*")[^"]+(")', ('${1}' + $clientRef + '${2}'), 1)
    $body = [regex]::Replace($body, '("engine"\s*:\s*")[^"]+(")', ('${1}' + $Engine + '${2}'), 1)

    Write-Host ""
    Write-Host "== $($s.Id) ($($s.File)) engine=$Engine clientRef=$clientRef"
    try {
        $created = Invoke-RestMethod -Method Post -Uri "$api/submit" -Headers $headers `
            -ContentType 'application/json' -Body $body
        $jobId = $created.id
        Write-Host "submitted: id=$jobId code=$($created.code) status=$($created.status)"

        $jobDeadline = (Get-Date).AddSeconds($JobTimeoutSeconds)
        do {
            Start-Sleep -Seconds 1
            $status = Invoke-RestMethod -Uri "$api/$jobId" -Headers $headers
            if ((Get-Date) -gt $jobDeadline) {
                throw "job did not finish within $JobTimeoutSeconds s (last status $($status.status))"
            }
        } while ($terminal -notcontains $status.status)
        Write-Host "job status: $($status.status) $($status.errorCode) $($status.message)"

        $solverStatus = $null
        $valuesOk = $false
        try {
            $result = Invoke-RestMethod -Uri "$api/$jobId/result" -Headers $headers
            $solverStatus = $result.status
            $assigned = @($result.solution.assignments).Count
            $unassigned = @($result.solution.unassignedTasks).Count
            Write-Host "result: solver status=$solverStatus assignments=$assigned unassigned=$unassigned"
            $valuesOk = [bool](& $s.Check $result)
        } catch {
            Write-Host "result: not available or check failed ($($_.Exception.Message))"
        }

        if ($status.status -eq $s.ExpectedJob -and $solverStatus -eq $s.ExpectedSolver -and $valuesOk) {
            Write-Host "PASS $($s.Id) ($($s.Desc))"
        } else {
            Write-Host "FAIL $($s.Id): expected job=$($s.ExpectedJob) solver=$($s.ExpectedSolver) and [$($s.Desc)] (values ok: $valuesOk)"
            $failed++
        }
    } catch {
        Write-Host "FAIL $($s.Id): $($_.Exception.Message)"
        $failed++
    }
}

Write-Host ""
if ($failed -gt 0) { Write-Host "$failed step(s) failed"; exit 1 }
Write-Host 'All steps passed'
exit 0
