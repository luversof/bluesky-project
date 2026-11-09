# Evening job: prices (today's close) -> news -> grade matured predictions. No claude call.
# Registered by register-tasks.ps1 (weekdays 18:00, runs late if the PC was asleep or off).
$JobName = 'evening'
. "$PSScriptRoot\common.ps1"

try {
    Write-Log "start ($Api)"
    Wait-Api
    Update-PriceHistory
    Update-News
    $grade = Invoke-Api POST '/api/stock/analysis/grades'
    Write-Log ("grades: graded {0}, pending {1}" -f $grade.graded, $grade.pending)
    exit 0
} catch {
    Write-Log "FAILED: $_"
    exit 1
}
