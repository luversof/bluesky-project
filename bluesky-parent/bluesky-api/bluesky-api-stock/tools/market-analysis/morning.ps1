# Morning job: prices -> grade earlier runs -> news -> market-daily -> analysis (analyze.ps1).
# Run it any time: the analysis judges the next close with the data that exists now (live run), then backfills
# past (reference date, slot) pairs of the current prompt version (user decision 2026-11-27, see analyze.ps1).
# register-tasks.ps1 schedules it on weekdays 08:30 (runs late if the PC was asleep or off).
# Rule history: night futures rule 2026-11-19, rule v10 (NXT premarket for D+1) 2026-11-21 - EXPERIMENTS.md.
$JobName = 'morning'
. "$PSScriptRoot\common.ps1"
. "$PSScriptRoot\analyze.ps1"

try {
    Write-Log "start ($Api)"
    Wait-Api
    Update-PriceHistory
    $grade = Invoke-Api POST '/api/stock/analysis/grades'
    Write-Log ("grades: graded {0}, pending {1}, regraded {2}" -f $grade.graded, $grade.pending, $grade.regraded)
    Update-News
    Update-MarketDaily
    Invoke-AnalysisRun
    exit 0
} catch {
    Write-Log "FAILED: $_"
    exit 1
}
