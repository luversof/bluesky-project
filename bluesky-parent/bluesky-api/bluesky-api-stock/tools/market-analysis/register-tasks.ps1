# Registers the morning / evening jobs in Windows Task Scheduler (folder \bluesky\).
# Run once in your own (non-admin) PowerShell:
#   powershell -ExecutionPolicy Bypass -File register-tasks.ps1 -UserId <your bluesky user id>
#
# - Weekdays only. Wake timers are NOT used: a sleeping PC is never woken.
# - StartWhenAvailable: a run missed while asleep or off starts as soon as the PC is back.
# - Runs only while you are logged on: claude keeps its login in your user profile.
# Remove: Unregister-ScheduledTask -TaskPath '\bluesky\' -TaskName 'market-analysis-*' -Confirm:$false
param(
    [Parameter(Mandatory = $true)][string]$UserId,
    [string]$Api = 'https://dev.bluesky.local:40134',
    [string]$MorningAt = '08:00',
    [string]$EveningAt = '18:00'
)
$ErrorActionPreference = 'Stop'

[Environment]::SetEnvironmentVariable('BLUESKY_USER_ID', $UserId, 'User')
[Environment]::SetEnvironmentVariable('BLUESKY_STOCK_API', $Api, 'User')

$weekdays = 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'
$principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Limited
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Hours 1)

foreach ($job in @(@{ Name = 'morning'; At = $MorningAt }, @{ Name = 'evening'; At = $EveningAt })) {
    $script = Join-Path $PSScriptRoot "$($job.Name).ps1"
    $action = New-ScheduledTaskAction -Execute 'powershell.exe' `
        -Argument "-NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$script`""
    $trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek $weekdays -At $job.At
    Register-ScheduledTask -TaskPath '\bluesky\' -TaskName "market-analysis-$($job.Name)" `
        -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Force | Out-Null
    Write-Host "registered \bluesky\market-analysis-$($job.Name) at $($job.At) (weekdays)"
}
Write-Host "logs: $env:LOCALAPPDATA\bluesky-market-analysis\logs"
