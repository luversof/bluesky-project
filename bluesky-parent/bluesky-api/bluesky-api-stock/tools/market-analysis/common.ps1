# Shared helpers for morning.ps1 / evening.ps1. Windows PowerShell 5.1 compatible.
# Keep this file ASCII-only: PowerShell 5.1 reads BOM-less files as ANSI.
#
# Settings (user environment variables, set by register-tasks.ps1):
#   BLUESKY_USER_ID     user id whose OpenApiConfig (KIS / NAVER / DART) is used. Required.
#   BLUESKY_STOCK_API   api-stock base URL. Default https://dev.bluesky.local:40134 (same as the gate's client-stock)
#   BLUESKY_CLAUDE_EXE  claude executable. Default: 'claude' on PATH, else the newest desktop-bundled one.

$ErrorActionPreference = 'Stop'

$Api = if ($env:BLUESKY_STOCK_API) { $env:BLUESKY_STOCK_API.TrimEnd('/') } else { 'https://dev.bluesky.local:40134' }
# .NET Framework may default to TLS 1.0/1.1
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
$UserId = $env:BLUESKY_USER_ID
if (-not $UserId) { throw 'BLUESKY_USER_ID is not set (run register-tasks.ps1)' }

$LogDir = Join-Path $env:LOCALAPPDATA 'bluesky-market-analysis\logs'
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$LogFile = Join-Path $LogDir ('{0:yyyyMMdd}-{1}.log' -f (Get-Date), $JobName)

function Write-Log([string]$Message) {
    $line = '{0:HH:mm:ss} {1}' -f (Get-Date), $Message
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
    Write-Host $line
}

# Returns the response body as text. Decodes UTF-8 ourselves: PowerShell 5.1 falls back to
# ISO-8859-1 when the response has no charset, which breaks Korean.
function Invoke-ApiRaw([string]$Method, [string]$Path, [string]$JsonBody) {
    $params = @{ Method = $Method; Uri = "$Api$Path"; TimeoutSec = 900; UseBasicParsing = $true }
    if ($JsonBody) {
        $params.ContentType = 'application/json; charset=utf-8'
        $params.Body = [Text.Encoding]::UTF8.GetBytes($JsonBody)
    }
    $response = Invoke-WebRequest @params
    return [Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray())
}

function Invoke-Api([string]$Method, [string]$Path, $Body) {
    $json = if ($null -ne $Body) { $Body | ConvertTo-Json -Depth 20 -Compress } else { $null }
    $text = Invoke-ApiRaw $Method $Path $json
    if ($text) { return $text | ConvertFrom-Json }
}

# After boot or resume the app (or its k8s pod) may still be starting. Wait up to 10 minutes.
function Wait-Api {
    $deadline = (Get-Date).AddMinutes(10)
    while ($true) {
        try {
            Invoke-Api GET '/api/stock/admin/price-histories/status' | Out-Null
            return
        } catch {
            if ((Get-Date) -gt $deadline) { throw "api-stock not reachable at $Api : $_" }
            Write-Log 'waiting for api-stock...'
            Start-Sleep -Seconds 15
        }
    }
}

# Same job as the admin screen's "price history update". If one is already running it is reused.
function Update-PriceHistory {
    $status = Invoke-Api POST "/api/stock/admin/price-histories?userId=$UserId"
    $deadline = (Get-Date).AddMinutes(20)
    while ($status.status -eq 'RUNNING') {
        if ((Get-Date) -gt $deadline) { throw 'price history update did not finish in 20 minutes' }
        Start-Sleep -Seconds 5
        $status = Invoke-Api GET '/api/stock/admin/price-histories/status'
    }
    if ($status.status -ne 'DONE') { throw "price history update $($status.status): $($status.failureReason)" }
    Write-Log ("prices: {0} symbols, failed [{1}]" -f $status.targetSymbolCount, ($status.failedSymbols -join ','))
}

function Update-News {
    $result = Invoke-Api POST "/api/stock/analysis/news?userId=$UserId"
    Write-Log ("news: naver {0}, dart {1}. {2}" -f $result.naverSaved, $result.dartSaved, ($result.messages -join ' / '))
}

function Resolve-Claude {
    if ($env:BLUESKY_CLAUDE_EXE) { return $env:BLUESKY_CLAUDE_EXE }
    # Only a real claude.exe: npm also puts claude.ps1 / claude.cmd shims on PATH, which
    # ProcessStartInfo cannot run with our quoting, and a failed npm postinstall leaves a
    # ~500 byte placeholder claude.exe.
    $candidates = @()
    $candidates += Get-Command claude.exe -CommandType Application -ErrorAction SilentlyContinue | ForEach-Object { $_.Source }
    $candidates += "$env:APPDATA\npm\node_modules\@anthropic-ai\claude-code\bin\claude.exe"
    $candidates += Get-ChildItem "$env:APPDATA\Claude\claude-code\*\*\claude.exe" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | ForEach-Object { $_.FullName }
    foreach ($path in $candidates) {
        if ($path -and (Test-Path $path) -and (Get-Item $path).Length -gt 1MB) { return $path }
    }
    throw 'claude executable not found (set BLUESKY_CLAUDE_EXE)'
}

# Windows argv quoting (CommandLineToArgvW rules). PowerShell 5.1 mangles quotes and drops empty
# strings when calling native programs, and the JSON schema argument has both.
function ConvertTo-NativeArg([string]$Arg) {
    '"' + (($Arg -replace '(\\*)"', '$1$1\"') -replace '(\\+)$', '$1$1') + '"'
}

# Runs claude -p with no tools (input only: news text may carry prompt injection), structured output.
# Returns the parsed claude result object.
function Invoke-Claude([string]$Prompt, [string]$Schema, [int]$TimeoutMinutes = 15) {
    $claudeArgs = @(
        '-p', '--model', 'sonnet',
        '--tools', '',
        '--output-format', 'json',
        '--json-schema', $Schema,
        '--no-session-persistence',
        '--strict-mcp-config',
        # project/user settings carry hooks and plugins meant for interactive sessions
        '--setting-sources', 'local'
    )
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = Resolve-Claude
    $psi.Arguments = ($claudeArgs | ForEach-Object { ConvertTo-NativeArg $_ }) -join ' '
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.StandardOutputEncoding = [Text.Encoding]::UTF8
    $psi.StandardErrorEncoding = [Text.Encoding]::UTF8
    # an empty folder: no CLAUDE.md or project settings get picked up
    $work = Join-Path $env:LOCALAPPDATA 'bluesky-market-analysis\work'
    New-Item -ItemType Directory -Force -Path $work | Out-Null
    $psi.WorkingDirectory = $work

    $process = [Diagnostics.Process]::Start($psi)
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $writer = New-Object IO.StreamWriter($process.StandardInput.BaseStream, (New-Object Text.UTF8Encoding($false)))
    $writer.Write($Prompt)
    $writer.Close()
    if (-not $process.WaitForExit($TimeoutMinutes * 60 * 1000)) {
        $process.Kill()
        throw "claude timed out after $TimeoutMinutes minutes"
    }
    $stdout = $stdoutTask.Result
    if (-not $stdout) { throw "claude exit $($process.ExitCode): $($stderrTask.Result)" }
    $result = $stdout | ConvertFrom-Json
    if ($result.is_error -or $null -eq $result.structured_output) {
        throw "claude failed (exit $($process.ExitCode)): $($result.result) $($stderrTask.Result)"
    }
    return $result
}
