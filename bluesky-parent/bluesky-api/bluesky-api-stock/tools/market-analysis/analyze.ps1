# Analysis part of morning.ps1 / evening.ps1 (dot-sourced after common.ps1, prices / news / market-daily collected).
#
# User decision 2026-11-27: a prediction runs at any time and judges the next close (D+1) and the 5th trading day
# (D+5) with the data that existed at that moment. Every run is stored as its own run and graded.
# - Live: GET /analysis/as-of-now gives the reference date (last closed trading day) and the slot of now
#   (CLOSE 16:00 / EVENING 20:00 / NIGHT 06:00 / PREMARKET 08:30 / INTRADAY after the open). The input holds only
#   what that slot can see: night futures from NIGHT on, the NXT premarket price from PREMARKET on.
# - Backfill: (reference date, slot) pairs of the last 60 trading days with no prediction of the current prompt
#   version are analyzed as of that moment (replay "re-analysis <version>"), newest date first, four slots a day,
#   at most 10 per run to spread the claude usage (BLUESKY_REANALYSIS_ONLY=<n>: backfill only, up to n).
#   Versions are compared on the same (reference date, slot) pairs only.
# Rule v10 per slot: premarket price -> D+1 rule; night futures >= 0.5% -> D+5 (and D+1 without premarket) rule;
# everything else goes to claude.

# "re-analysis " prefix, same as api-stock StockAnalysisService.REANALYSIS_PREFIX (code points: this file stays ASCII)
$ReanalysisPrefix = (-join [char[]](0xC7AC, 0xBD84, 0xC11D)) + ' '

function Submit-Run([string]$ModelId, [string]$Version, $Predictions, [string]$AsOf, [string]$Slot, [string]$Label) {
    $path = "/api/stock/analysis/predictions?asOf=$AsOf&slot=$Slot"
    if ($Label) { $path += '&replay=' + [uri]::EscapeDataString($Label) }
    $submit = Invoke-Api POST $path @{ modelId = $ModelId; promptVersion = $Version; predictions = @($Predictions) }
    Write-Log ("predictions {0} {1}{2}: run {3} saved {4}, rejected {5} [{6}] model {7} {8}" -f `
            $AsOf, $Slot, $(if ($Label) { ' (backfill)' } else { ' (live)' }), $submit.runId, $submit.saved,
            @($submit.rejected).Count, ($submit.rejected -join ' / '), $ModelId, $Version)
}

# One analysis of reference date AsOf as of slot Slot. Label empty = live run (stored with the time it ran).
function Invoke-Analysis([string]$AsOf, [string]$Slot, [string]$Label) {
    $analysisInput = Invoke-ApiRaw GET "/api/stock/analysis/input?userId=$UserId&asOf=$AsOf&slot=$Slot" | ConvertFrom-Json
    $items = @($analysisInput.items)
    if ($symbols -ne 'all') { $items = @($items | Where-Object { $wanted -contains $_.symbol }) }
    Write-Log ("targets {0} {1}: {2} [{3}]" -f $AsOf, $Slot, $items.Count, $symbols)
    if ($items.Count -eq 0) { return $false }

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
        # ForEach-Object unrolls: PowerShell 5.1 ConvertFrom-Json returns a JSON array as one object
        $rule = @(Invoke-Api GET "/api/stock/analysis/rule-predictions?$query" | ForEach-Object { $_ })
        # Targets the rule covers for both horizons skip claude; D+1-only targets still go to claude for D+5.
        $full = @($rule | Where-Object { $_.horizonDays -eq 5 } | ForEach-Object { $_.symbol })
        $fullRule = @($rule | Where-Object { $full -contains $_.symbol })
        $d1Only = @($rule | Where-Object { $full -notcontains $_.symbol })
        if ($fullRule.Count -gt 0) {
            Submit-Run 'rule' $promptVersion $fullRule $AsOf $Slot $Label
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
    if ($predictions.Count -eq 0) { return $true }
    if ($d1Only.Count -gt 0) {
        $ruledD1 = @($d1Only | ForEach-Object { $_.symbol })
        $predictions = @($predictions | Where-Object { -not ($_.horizonDays -eq 1 -and $ruledD1 -contains $_.symbol) }) + $d1Only
    }
    $modelId = @($result.modelUsage.PSObject.Properties.Name) | Select-Object -First 1
    # Invoke-Claude always runs --model opus, so a missing modelUsage still means opus.
    if (-not $modelId) { $modelId = 'opus' }
    Submit-Run $modelId $promptVersion $predictions $AsOf $Slot $Label
    return $true
}

# Live run + backfill. Throws the first live failure after the backfill ran.
function Invoke-AnalysisRun {
    # BLUESKY_PROMPT_FILE: a candidate prompt (candidates/prompt-vN.md) to compare with the adopted one on the same pairs
    $promptFile = if ($env:BLUESKY_PROMPT_FILE) { $env:BLUESKY_PROMPT_FILE } else { "$PSScriptRoot\prompt.md" }
    $script:promptTemplate = Get-Content $promptFile -Raw -Encoding UTF8
    if ($promptTemplate -notmatch 'promptVersion:\s*(\S+)') { throw 'prompt.md has no promptVersion line' }
    $script:promptVersion = $Matches[1]
    $script:schema = Get-Content "$PSScriptRoot\schema.json" -Raw -Encoding UTF8

    # Only BLUESKY_ANALYSIS_SYMBOLS are analyzed (comma list, 'all' = every target). Default Samsung
    # Electronics only: analyzing every target used up the Claude usage limit (user decision 2026-11-12).
    $script:symbols = if ($env:BLUESKY_ANALYSIS_SYMBOLS) { $env:BLUESKY_ANALYSIS_SYMBOLS } else { '005930' }
    $script:wanted = @($symbols.Split(',') | ForEach-Object { $_.Trim() })

    $failed = $null
    $backfillOnly = [int]$env:BLUESKY_REANALYSIS_ONLY
    if ($backfillOnly -gt 0) {
        Write-Log "backfill only, up to $backfillOnly"
    } else {
        try {
            $now = Invoke-Api GET "/api/stock/analysis/as-of-now?symbol=$($wanted[0])"
            [void](Invoke-Analysis ([string]$now.baseDate) ([string]$now.slot) '')
        } catch { $failed = $_; Write-Log "live analysis failed: $_" }
    }

    if ($symbols -ne 'all') {
        $todo = @()
        try {
            $todo = @(Invoke-Api GET ("/api/stock/analysis/missing-slots?symbols={0}&lookback=60&version={1}" -f `
                        ($wanted -join ','), [uri]::EscapeDataString($promptVersion)) | ForEach-Object { $_ })
            # BLUESKY_BACKFILL_SLOTS: comma list (e.g. EVENING) - a candidate that changes one slot's input only backfills that slot
            if ($env:BLUESKY_BACKFILL_SLOTS) {
                $onlySlots = @($env:BLUESKY_BACKFILL_SLOTS.Split(',') | ForEach-Object { $_.Trim() })
                $todo = @($todo | Where-Object { $onlySlots -contains $_.slot })
            }
            $todo = @($todo | Select-Object -First $(if ($backfillOnly -gt 0) { $backfillOnly } else { 10 }))
        } catch { Write-Log "backfill skipped: $_" }
        foreach ($t in $todo) {
            # Not fatal and stop at the first failure: it is usually the usage limit, the next run continues.
            try { [void](Invoke-Analysis ([string]$t.baseDate) ([string]$t.slot) ($ReanalysisPrefix + $promptVersion)) }
            catch { Write-Log "backfill $($t.baseDate) $($t.slot) failed, stop: $_"; break }
        }
    }
    if ($failed) { throw $failed }
}
