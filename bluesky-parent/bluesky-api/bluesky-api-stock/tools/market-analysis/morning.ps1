# Analysis run: prices -> grade earlier runs -> news -> analysis input -> claude -p -> predictions.
# Run it any time, as often as needed: every run is stored as its own run (runId) and graded later,
# so runs can be compared to see whether a prompt/input change improved the analysis.
# register-tasks.ps1 also schedules it (weekdays 08:30, runs late if the PC was asleep or off).
#
# Rule (user decision 2026-11-19): when the KOSPI200 night futures moved 0.5% or more, the rule targets
# (api-stock StockAnalysisService.RULE_SYMBOLS) follow the night futures direction without claude -
# in the replay v9 made the same call on all such days. Only the other targets / days go to claude.
# Rule v10 (2026-11-21): D+1 follows the NXT premarket price (input line NXT_<symbol>, before 08:30) on
# every day; D+5 stays the night futures rule on big days. On small night days the rule gives D+1 only:
# claude still runs for D+5 and its D+1 is replaced by the rule D+1. Run at 08:30 (register-tasks.ps1).
# Catch-up (2026-11-19): reference dates of the last 5 trading days with no prediction (usage limit,
# api-stock down, a run during market hours) are analyzed as of 08:30 the next morning and stored as
# replay runs, so missed days fill themselves on the next run.
# Re-analysis (user request 2026-11-21): reference dates of the last 60 trading days with no prediction
# from the current prompt version are analyzed again with it (replay "re-analysis <version>"), even when an
# older version already analyzed them, so a new version gathers graded results on past dates quickly.
# Only the current version runs (older versions are not re-run). At most 10 dates per run, newest first,
# to spread the claude usage: a new version fills its 60 dates over about 6 runs.
$JobName = 'morning'
. "$PSScriptRoot\common.ps1"

# replay label of catch-up runs (Korean, built from code points: this file stays ASCII-only)
$CatchUpLabel = -join [char[]](0xB204, 0xB77D, 0xCC44, 0xC6C0)
# "re-analysis " prefix, same as api-stock StockAnalysisService.REANALYSIS_PREFIX
$ReanalysisPrefix = (-join [char[]](0xC7AC, 0xBD84, 0xC11D)) + ' '

function Submit-Run([string]$ModelId, [string]$Version, $Predictions, [string]$AsOf, [string]$Label) {
    $path = '/api/stock/analysis/predictions'
    if ($AsOf) { $path += "?asOf=$AsOf&morning=true&replay=" + [uri]::EscapeDataString($Label) }
    $submit = Invoke-Api POST $path @{ modelId = $ModelId; promptVersion = $Version; predictions = @($Predictions) }
    Write-Log ("predictions{0}: run {1} saved {2}, rejected {3} [{4}] model {5} {6}" -f `
            $(if ($AsOf) { " $AsOf" } else { '' }), $submit.runId, $submit.saved, @($submit.rejected).Count,
            ($submit.rejected -join ' / '), $ModelId, $Version)
}

# One analysis: now (AsOf empty) or as of 08:30 the morning after reference date AsOf.
function Invoke-Analysis([string]$AsOf, [string]$Label = $CatchUpLabel) {
    $query = "userId=$UserId"
    if ($AsOf) { $query += "&asOf=$AsOf&morning=true" }
    $analysisInput = Invoke-ApiRaw GET "/api/stock/analysis/input?$query" | ConvertFrom-Json
    $items = @($analysisInput.items)
    if ($symbols -ne 'all') { $items = @($items | Where-Object { $wanted -contains $_.symbol }) }
    Write-Log ("targets{0}: {1} [{2}]" -f $(if ($AsOf) { " $AsOf" } else { '' }), $items.Count, $symbols)
    if ($items.Count -eq 0) { return }

    $inv = [Globalization.CultureInfo]::InvariantCulture
    $night = @($analysisInput.market.lines | Where-Object { $_.key -eq 'K200NF' }) | Select-Object -First 1
    # shortcut: one premarket value for all targets - only Samsung Electronics has it (and the rule), add
    # a per-symbol parameter when another rule target gets one.
    $pre = @($analysisInput.market.lines | Where-Object { $_.key -like 'NXT_*' }) | Select-Object -First 1
    $d1Only = @()
    if (($night -and $null -ne $night.change1dPct) -or ($pre -and $null -ne $pre.change1dPct)) {
        $query = "symbols=" + (($items | ForEach-Object { $_.symbol }) -join ',')
        if ($night -and $null -ne $night.change1dPct) { $query += "&night=" + ([decimal]$night.change1dPct).ToString($inv) }
        if ($pre -and $null -ne $pre.change1dPct) { $query += "&premarket=" + ([decimal]$pre.change1dPct).ToString($inv) }
        # Non-fatal: an api-stock deployed before 2026-11-19 has no /rule-predictions - claude does all.
        $rule = @()
        try {
            # ForEach-Object unrolls: PowerShell 5.1 ConvertFrom-Json returns a JSON array as one object
            $rule = @(Invoke-Api GET "/api/stock/analysis/rule-predictions?$query" | ForEach-Object { $_ })
        } catch { Write-Log "rule skipped: $_" }
        # Targets the rule covers for both horizons skip claude; D+1-only targets still go to claude for D+5.
        $full = @($rule | Where-Object { $_.horizonDays -eq 5 } | ForEach-Object { $_.symbol })
        $fullRule = @($rule | Where-Object { $full -contains $_.symbol })
        $d1Only = @($rule | Where-Object { $full -notcontains $_.symbol })
        if ($fullRule.Count -gt 0) {
            Submit-Run 'rule' $promptVersion $fullRule $AsOf $Label
            $items = @($items | Where-Object { $full -notcontains $_.symbol })
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
    if ($d1Only.Count -gt 0) {
        $ruledD1 = @($d1Only | ForEach-Object { $_.symbol })
        $predictions = @($predictions | Where-Object { -not ($_.horizonDays -eq 1 -and $ruledD1 -contains $_.symbol) }) + $d1Only
    }
    $modelId = @($result.modelUsage.PSObject.Properties.Name) | Select-Object -First 1
    # Invoke-Claude always runs --model opus, so a missing modelUsage still means opus.
    if (-not $modelId) { $modelId = 'opus' }
    Submit-Run $modelId $promptVersion $predictions $AsOf $Label
}

try {
    Write-Log "start ($Api)"
    Wait-Api
    Update-PriceHistory
    $grade = Invoke-Api POST '/api/stock/analysis/grades'
    Write-Log ("grades: graded {0}, pending {1}, regraded {2}" -f $grade.graded, $grade.pending, $grade.regraded)
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
    # date to the catch-up below, which analyzes it as of 08:30.
    $now = Get-Date
    $marketHours = $now.DayOfWeek -ne 'Saturday' -and $now.DayOfWeek -ne 'Sunday' -and $now.Hour -ge 9 -and $now.Hour -lt 16
    $failed = $null
    # BLUESKY_REANALYSIS_ONLY=<n>: run only the re-analysis, up to n dates - to fill a new version's 60
    # dates right after a deploy instead of 10 per morning (user request 2026-11-21).
    $reanalysisOnly = [int]$env:BLUESKY_REANALYSIS_ONLY
    if ($reanalysisOnly -gt 0) {
        Write-Log "re-analysis only, up to $reanalysisOnly dates"
    } elseif ($marketHours) {
        Write-Log 'market hours - skip the run for now, catch-up only'
    } else {
        try { Invoke-Analysis '' } catch { $failed = $_; Write-Log "analysis failed: $_" }
    }

    if ($symbols -ne 'all' -and $reanalysisOnly -eq 0) {
        $missing = @()
        try {
            $missing = @(Invoke-Api GET ("/api/stock/analysis/missing-dates?symbols={0}" -f ($wanted -join ',')) | ForEach-Object { $_ })
        } catch { Write-Log "catch-up skipped: $_" }
        foreach ($day in $missing) {
            try { Invoke-Analysis ([string]$day) } catch { $failed = $_; Write-Log "catch-up $day failed: $_" }
        }
    }

    if ($symbols -ne 'all') {
        $redo = @()
        try {
            $redo = @(Invoke-Api GET ("/api/stock/analysis/missing-dates?symbols={0}&lookback=60&version={1}" -f `
                        ($wanted -join ','), [uri]::EscapeDataString($promptVersion)) | ForEach-Object { $_ })
            [array]::Reverse($redo)
            $redo = @($redo | Select-Object -First $(if ($reanalysisOnly -gt 0) { $reanalysisOnly } else { 10 }))
        } catch { Write-Log "re-analysis skipped: $_" }
        foreach ($day in $redo) {
            # Not fatal and stop at the first failure: it is usually the usage limit, the next run continues.
            try { Invoke-Analysis ([string]$day) ($ReanalysisPrefix + $promptVersion) } catch { Write-Log "re-analysis $day failed, stop: $_"; break }
        }
    }
    if ($failed) { throw $failed }
    exit 0
} catch {
    Write-Log "FAILED: $_"
    exit 1
}
