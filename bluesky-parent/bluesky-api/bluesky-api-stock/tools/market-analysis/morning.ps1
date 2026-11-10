# Analysis run: prices -> grade earlier runs -> news -> analysis input -> claude -p -> predictions.
# Run it any time, as often as needed: every run is stored as its own run (runId) and graded later,
# so runs can be compared to see whether a prompt/input change improved the analysis.
# register-tasks.ps1 also schedules it (weekdays 08:00, runs late if the PC was asleep or off).
$JobName = 'morning'
. "$PSScriptRoot\common.ps1"

try {
    Write-Log "start ($Api)"
    Wait-Api
    Update-PriceHistory
    $grade = Invoke-Api POST '/api/stock/analysis/grades'
    Write-Log ("grades: graded {0}, pending {1}" -f $grade.graded, $grade.pending)
    Update-News

    $inputJson = Invoke-ApiRaw GET "/api/stock/analysis/input?userId=$UserId"
    $itemCount = @(($inputJson | ConvertFrom-Json).items).Count
    if ($itemCount -eq 0) {
        Write-Log 'no analysis targets - done'
        exit 0
    }

    $promptTemplate = Get-Content "$PSScriptRoot\prompt.md" -Raw -Encoding UTF8
    if ($promptTemplate -notmatch 'promptVersion:\s*(\S+)') { throw 'prompt.md has no promptVersion line' }
    $promptVersion = $Matches[1]
    $schema = Get-Content "$PSScriptRoot\schema.json" -Raw -Encoding UTF8
    $prompt = $promptTemplate.Replace('{{INPUT_JSON}}', $inputJson)

    $result = $null
    foreach ($attempt in 1, 2) {
        try {
            $result = Invoke-Claude $prompt $schema
            break
        } catch {
            Write-Log "claude attempt $attempt failed: $_"
            if ($attempt -eq 2) { throw }
        }
    }

    $modelId = @($result.modelUsage.PSObject.Properties.Name) | Select-Object -First 1
    if (-not $modelId) { $modelId = 'sonnet' }
    $submit = Invoke-Api POST '/api/stock/analysis/predictions' @{
        modelId       = $modelId
        promptVersion = $promptVersion
        predictions   = @($result.structured_output.predictions)
    }
    Write-Log ("predictions: run {0} saved {1}, rejected {2} [{3}] model {4} {5}" -f `
            $submit.runId, $submit.saved, @($submit.rejected).Count, ($submit.rejected -join ' / '), $modelId, $promptVersion)
    exit 0
} catch {
    Write-Log "FAILED: $_"
    exit 1
}
