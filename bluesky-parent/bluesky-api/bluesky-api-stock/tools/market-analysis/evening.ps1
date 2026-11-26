# Evening job: prices (today's close) -> news -> market-daily -> grade matured predictions -> analysis
# (analyze.ps1): predicts the next trading day the evening before (user request 2026-11-27).
# Registered by register-tasks.ps1 (weekdays 18:00, runs late if the PC was asleep or off).
$JobName = 'evening'
. "$PSScriptRoot\common.ps1"
. "$PSScriptRoot\analyze.ps1"

try {
    Write-Log "start ($Api)"
    Wait-Api
    Update-PriceHistory
    Update-News
    Update-MarketDaily
    $grade = Invoke-Api POST '/api/stock/analysis/grades'
    Write-Log ("grades: graded {0}, pending {1}, regraded {2}" -f $grade.graded, $grade.pending, $grade.regraded)
    Invoke-AnalysisRun
    exit 0
} catch {
    Write-Log "FAILED: $_"
    exit 1
}
