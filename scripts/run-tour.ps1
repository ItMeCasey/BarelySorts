# Dev only: runs the client tour for one layer, e.g.  scripts\run-tour.ps1 neoforge-26.2
# The tour creates a fresh survival superflat world itself. This script clears the old one, writes options.txt with
# the toasts off, starts the client with the tour enabled, moves its window to DISPLAY6, waits for the tour to finish and
# closes the client. Frames land in <layer>\run\screenshots\barelysorts_<layer>_*.png. Never touches the cursor or keyboard.
param([Parameter(Mandatory = $true)][string]$Layer)

$root = Split-Path $PSScriptRoot -Parent
$dir = Join-Path $root $Layer
$mc = ($Layer -split '-', 2)[1]
$loader = ($Layer -split '-', 2)[0]
$run = Join-Path $dir 'run'
New-Item -ItemType Directory -Force (Join-Path $run 'saves') | Out-Null

$world = Join-Path $run 'saves/barelysorts_tour'
if (Test-Path $world) { Remove-Item -Recurse -Force $world }

# Keep any existing options, add the quiet ones.
$opts = Join-Path $run 'options.txt'
$lines = if (Test-Path $opts) { Get-Content $opts } else { @() }
foreach ($kv in 'tutorialStep:none', 'onboardAccessibility:false', 'skipMultiplayerWarning:true', 'joinedFirstServer:true', 'pauseOnLostFocus:false') {
    $key = $kv.Split(':')[0]
    $lines = @($lines | Where-Object { $_ -notlike "$key`:*" }) + $kv
}
Set-Content -Path $opts -Value $lines
Remove-Item -Force (Join-Path $run 'screenshots\barelysorts_*') -ErrorAction SilentlyContinue
Remove-Item -Force (Join-Path $run 'eula.txt') -ErrorAction SilentlyContinue
# The mod keeps its settings and locks in the config folder; start every tour from none.
Remove-Item -Force (Join-Path $run 'config/barelysorts*') -ErrorAction SilentlyContinue

# Loom's run tasks start the game with the JVM that runs Gradle, so give Gradle the JDK this Minecraft version needs.
$jdk = switch -Wildcard ($mc) {
    '1.20.*' { 'C:/Program Files/Eclipse Adoptium/jdk-17.0.5.8-hotspot' }
    '1.21.*' { 'C:/Users/casey/tools/jdk-21' }
    default { 'C:/Program Files/Microsoft/jdk-25.0.4.7-hotspot' }
}
$env:JAVA_HOME = $jdk
# Optional extra JVM flags for a flaky machine, e.g. $env:BS_TOUR_JVM = '-XX:TieredStopAtLevel=1' (C1 only, avoids JIT crashes).
if ($env:BS_TOUR_JVM) { $env:JAVA_TOOL_OPTIONS = $env:BS_TOUR_JVM }

$log = Join-Path $root "scratch-tour-$Layer.log"
$env:BARELYSORTS_TOUR = $Layer
$task = Start-Process -FilePath (Join-Path $dir 'gradlew.bat') -WorkingDirectory $dir -PassThru -NoNewWindow `
    -RedirectStandardOutput $log -RedirectStandardError "$log.err" `
    -ArgumentList 'runClient -Ptour --no-daemon'
Remove-Item Env:\BARELYSORTS_TOUR

& powershell -NoProfile -File C:\Users\casey\.claude\tools\place-window.ps1 -Match 'Minecraft' -Wait 300

$deadline = (Get-Date).AddMinutes(8)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    $text = Get-Content $log -Raw -ErrorAction SilentlyContinue
    if ($text -match 'BS-TOUR (DONE|FAILED)') { break }
    if ($task.HasExited) { break }
}
Get-Content $log -ErrorAction SilentlyContinue | Select-String 'BS-TOUR|Exception|ERROR' | Select-Object -Last 60
if (-not $task.HasExited) { Start-Sleep -Seconds 8 }
if (-not $task.HasExited) { Stop-Process -Id $task.Id -Force }
Get-ChildItem (Join-Path $run 'screenshots\barelysorts_*') -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
