# Analysis run: prices -> grade earlier runs -> news -> analysis input -> claude -p -> predictions.
# Run it any time, as often as needed: every run is stored as its own run (runId) and graded later,
# so runs can be compared to see whether a prompt/input change improved the analysis.
# register-tasks.ps1 also schedules it (weekdays 08:00, runs late if the PC was asleep or off).
#
# Rule (user decision 2026-11-19): when the KOSPI200 night futures moved 0.5% or more, the rule targets
# (api-stock StockAnalysisService.RULE_SYMBOLS) follow the night futures direction without claude -
# in the replay v9 made the same call on all such days. Only the other targets / days go to claude.
# Catch-up (2026-11-19): reference dates of the last 5 trading days with no prediction (usage limit,
# api-stock down, a run during market hours) are analyzed as of 08:00 the next morning and stored as
# replay runs, so missed days fill themselves on the next run.
$JobName = 'morning'
. "$PSScriptRoot\common.ps1"

# replay label of catch-up runs (Korean, built from code points: this file stays ASCII-only)
$CatchUpLabel = -join [char[]](0xB204, 0xB77D, 0xCC44, 0xC6C0)

function Submit-Run([string]$ModelId, [string]$Version, $Predictions, [string]$AsOf) {
    $path = '/api/stock/analysis/predictions'
    if ($AsOf) { $path += "?asOf=$AsOf&morning=true&replay=" + [uri]::EscapeDataString($CatchUpLabel) }
    $submit = Invoke-Api POST $path @{ modelId = $ModelId; promptVersion = $Version; predictions = @($Predictions) }
    Write-Log ("predictions{0}: run {1} saved {2}, rejected {3} [{4}] model {5} {6}" -f `
            $(if ($AsOf) { " $AsOf" } else { '' }), $submit.runId, $submit.saved, @($submit.rejected).Count,
            ($submit.rejected -join ' / '), $ModelId, $Version)
}

# One analysis: now (AsOf empty) or as of 08:00 the morning after reference date AsOf.
function Invoke-Analysis([string]$AsOf) {
    $query = "userId=$UserId"
    if ($AsOf) { $query += "&asOf=$AsOf&morning=true" }
    $analysisInput = Invoke-ApiRaw GET "/api/stock/analysis/input?$query" | ConvertFrom-Json
    $items = @($analysisInput.items)
    if ($symbols -ne 'all') { $items = @($items | Where-Object { $wanted -contains $_.symbol }) }
    Write-Log ("targets{0}: {1} [{2}]" -f $(if ($AsOf) { " $AsOf" } else { '' }), $items.Count, $symbols)
    if ($items.Count -eq 0) { return }

    $night = @($analysisInput.market.lines | Where-Object { $_.key -eq 'K200NF' }) | Select-Object -First 1
    if ($night -and $null -ne $night.change1dPct) {
        $nightPct = ([decimal]$night.change1dPct).ToString([Globalization.CultureInfo]::InvariantCulture)
        # Non-fatal: an api-stock deployed before 2026-11-19 has no /rule-predictions - claude does all.
        $rule = @()
        try {
            # ForEach-Object unrolls: PowerShell 5.1 ConvertFrom-Json returns a JSON array as one object
            $rule = @(Invoke-Api GET ("/api/stock/analysis/rule-predictions?night={0}&symbols={1}" -f `
                        $nightPct, (($items | ForEach-Object { $_.symbol }) -join ',')) | ForEach-Object { $_ })
        } catch { Write-Log "rule skipped: $_" }
        if ($rule.Count -gt 0) {
            Submit-Run 'rule' 'rule' $rule $AsOf
            $ruled = @($rule | ForEach-Object { $_.symbol })
            $items = @($items | Where-Object { $ruled -notcontains $_.symbol })
        }
    }

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
    if ($predictions.Count -eq 0) { return }
    $modelId = @($result.modelUsage.PSObject.Properties.Name) | Select-Object -First 1
    # Invoke-Claude always runs --model opus, so a missing modelUsage still means opus.
    if (-not $modelId) { $modelId = 'opus' }
    Submit-Run $modelId $promptVersion $predictions $AsOf
}

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
    $wanted = @($symbols.Split(',') | ForEach-Object { $_.Trim() })

    # During market hours a run would see part of the day and be graded as late - leave that reference
    # date to the catch-up below, which analyzes it as of 08:00.
    $now = Get-Date
    $marketHours = $now.DayOfWeek -ne 'Saturday' -and $now.DayOfWeek -ne 'Sunday' -and $now.Hour -ge 9 -and $now.Hour -lt 16
    $failed = $null
    if ($marketHours) {
        Write-Log 'market hours - skip the run for now, catch-up only'
    } else {
        try { Invoke-Analysis '' } catch { $failed = $_; Write-Log "analysis failed: $_" }
    }

    if ($symbols -ne 'all') {
        $missing = @()
        try {
            $missing = @(Invoke-Api GET ("/api/stock/analysis/missing-dates?symbols={0}" -f ($wanted -join ',')) | ForEach-Object { $_ })
        } catch { Write-Log "catch-up skipped: $_" }
        foreach ($day in $missing) {
            try { Invoke-Analysis ([string]$day) } catch { $failed = $_; Write-Log "catch-up $day failed: $_" }
        }
    }
    if ($failed) { throw $failed }
    exit 0
} catch {
    Write-Log "FAILED: $_"
    exit 1
}
