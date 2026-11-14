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
    Update-MarketDaily

    $promptTemplate = Get-Content "$PSScriptRoot\prompt.md" -Raw -Encoding UTF8
    if ($promptTemplate -notmatch 'promptVersion:\s*(\S+)') { throw 'prompt.md has no promptVersion line' }
    $promptVersion = $Matches[1]
    $schema = Get-Content "$PSScriptRoot\schema.json" -Raw -Encoding UTF8

    # Only BLUESKY_ANALYSIS_SYMBOLS are analyzed (comma list, 'all' = every target). Default Samsung
    # Electronics only: analyzing every target used up the Claude usage limit (user decision 2026-11-12).
    $symbols = if ($env:BLUESKY_ANALYSIS_SYMBOLS) { $env:BLUESKY_ANALYSIS_SYMBOLS } else { '005930' }
    $analysisInput = Invoke-ApiRaw GET "/api/stock/analysis/input?userId=$UserId" | ConvertFrom-Json
    $items = @($analysisInput.items)
    if ($symbols -ne 'all') {
        $wanted = $symbols.Split(',') | ForEach-Object { $_.Trim() }
        $items = @($items | Where-Object { $wanted -contains $_.symbol })
    }
    Write-Log ("targets: {0} [{1}]" -f $items.Count, $symbols)

    # Targets are analyzed 10 at a time: with ~40 targets one prompt would carry 100k+ characters.
    $predictions = @()
    $result = $null
    for ($i = 0; $i -lt $items.Count; $i += 10) {
        $analysisInput.items = @($items[$i..([Math]::Min($i + 10, $items.Count) - 1)])
        $prompt = $promptTemplate.Replace('{{INPUT_JSON}}', ($analysisInput | ConvertTo-Json -Depth 20 -Compress))
        foreach ($attempt in 1, 2) {
            try {
                $result = Invoke-Claude $prompt $schema
                break
            } catch {
                Write-Log "claude targets $i attempt $attempt failed: $_"
                if ($attempt -eq 2) { throw }
            }
        }
        $predictions += @($result.structured_output.predictions)
    }
    if ($predictions.Count -eq 0) {
        Write-Log 'no analysis targets - done'
        exit 0
    }

    $modelId = @($result.modelUsage.PSObject.Properties.Name) | Select-Object -First 1
    # Invoke-Claude always runs --model opus, so a missing modelUsage still means opus.
    if (-not $modelId) { $modelId = 'opus' }
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
