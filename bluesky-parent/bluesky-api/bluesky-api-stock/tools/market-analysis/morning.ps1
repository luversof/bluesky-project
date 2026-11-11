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

    $promptTemplate = Get-Content "$PSScriptRoot\prompt.md" -Raw -Encoding UTF8
    if ($promptTemplate -notmatch 'promptVersion:\s*(\S+)') { throw 'prompt.md has no promptVersion line' }
    $promptVersion = $Matches[1]
    $schema = Get-Content "$PSScriptRoot\schema.json" -Raw -Encoding UTF8

    # Targets are analyzed 10 at a time: with ~40 targets one prompt would carry 100k+ characters.
    $predictions = @()
    $result = $null
    for ($chunk = 0; ; $chunk++) {
        $inputJson = Invoke-ApiRaw GET "/api/stock/analysis/input?userId=$UserId&chunk=$chunk&chunkSize=10"
        $itemCount = @(($inputJson | ConvertFrom-Json).items).Count
        if ($itemCount -eq 0) { break }
        $prompt = $promptTemplate.Replace('{{INPUT_JSON}}', $inputJson)
        foreach ($attempt in 1, 2) {
            try {
                $result = Invoke-Claude $prompt $schema
                break
            } catch {
                Write-Log "claude chunk $chunk attempt $attempt failed: $_"
                if ($attempt -eq 2) { throw }
            }
        }
        $predictions += @($result.structured_output.predictions)
        # A short chunk is the last one. This also stops an api-stock deployed before chunking, which
        # ignores chunk= and returns every target each time, after one call instead of looping.
        if ($itemCount -ne 10) { break }
        if ($chunk -ge 20) { throw 'more than 20 chunks - stopping (chunk= ignored?)' }
    }
    if ($predictions.Count -eq 0) {
        Write-Log 'no analysis targets - done'
        exit 0
    }

    $modelId = @($result.modelUsage.PSObject.Properties.Name) | Select-Object -First 1
    if (-not $modelId) { $modelId = 'sonnet' }
    $submit = Invoke-Api POST '/api/stock/analysis/predictions' @{
        modelId       = $modelId
        promptVersion = $promptVersion
        predictions   = $predictions
    }
    Write-Log ("predictions: run {0} saved {1}, rejected {2} [{3}] model {4} {5}" -f `
            $submit.runId, $submit.saved, @($submit.rejected).Count, ($submit.rejected -join ' / '), $modelId, $promptVersion)
    exit 0
} catch {
    Write-Log "FAILED: $_"
    exit 1
}
