# Command reference / runner for common Gradle & repo maintenance tasks.
# Run with no arguments to see the command list.
param(
    [Parameter(Position = 0)]
    [ValidateSet('build', 'release', 'install', 'test', 'uitest', 'lint', 'check', 'clean', 'help')]
    [string]$Command = 'help',

    [switch]$Deep
)

$RepoRoot = Split-Path -Parent $PSScriptRoot
$Gradlew = Join-Path $RepoRoot 'gradlew.bat'

function Invoke-Gradle {
    param([string[]]$Tasks)
    & $Gradlew @Tasks
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

function Show-Help {
    Write-Host @'
Usage:
  .\scripts\dev.ps1                  Show this list
  .\scripts\dev.ps1 <command>        Run a command
  .\scripts\dev.ps1 clean -Deep      Also wipe .gradle/.kotlin/.idea caches

Commands:
  build     Assemble debug APK                          (gradlew assembleDebug)
  release   Build release AAB, moved to app\release\app-release.aab (gradlew bundleRelease)
  install   Build & install debug APK on a connected device/emulator (gradlew installDebug)
  test      Run JVM unit tests                           (gradlew testDebugUnitTest)
  uitest    Run Compose UI tests on a connected device/emulator
                                              (gradlew connectedDebugAndroidTest)
  lint      Run Android Lint                              (gradlew lintDebug)
  check     test + lint together (JVM only; uitest needs a device)
  clean     Remove build/ and app/build/                  (gradlew clean)
            -Deep also removes .gradle/, .kotlin/, .idea/caches/
            (safe: all regenerate on next build/sync, just slower once)
'@
}

switch ($Command) {
    'build'   { Invoke-Gradle @('assembleDebug') }
    'release' {
        Invoke-Gradle @('bundleRelease')
        $builtAab = Join-Path $RepoRoot 'app\build\outputs\bundle\release\app-release.aab'
        $destDir = Join-Path $RepoRoot 'app\release'
        $dest = Join-Path $destDir 'app-release.aab'
        if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir | Out-Null }
        Move-Item -Force $builtAab $dest
        Write-Host "AAB moved to $dest"
    }
    'install' { Invoke-Gradle @('installDebug') }
    'test'    { Invoke-Gradle @('testDebugUnitTest') }
    'uitest'  { Invoke-Gradle @('connectedDebugAndroidTest') }
    'lint'    { Invoke-Gradle @('lintDebug') }
    'check'   { Invoke-Gradle @('testDebugUnitTest', 'lintDebug') }
    'clean' {
        Invoke-Gradle @('clean')
        if ($Deep) {
            Write-Host "Deep clean: removing .gradle/, .kotlin/, .idea/caches/ ..."
            foreach ($dir in @('.gradle', '.kotlin', '.idea\caches')) {
                $path = Join-Path $RepoRoot $dir
                if (Test-Path $path) {
                    Remove-Item -Recurse -Force $path
                    Write-Host "  removed $dir"
                }
            }
        }
    }
    default { Show-Help }
}
