$ErrorActionPreference = "Stop"

$resolutions = @(
    @{ name = "phone"; size = "1080x1920"; density = "420" },
    @{ name = "tablet7"; size = "1200x1920"; density = "320" },
    @{ name = "tablet10"; size = "1600x2560"; density = "320" }
)

$outDir = "screenshots"
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
}

Write-Host "Checking for connected device..."
$devices = (adb devices | Select-String -Pattern "\bdevice$")
if (-not $devices) {
    Write-Warning "No device connected or device is offline. Make sure the emulator is running."
    exit 1
}

Write-Host "Getting current display settings..."
$origSizeOutput = (adb shell wm size | Out-String)
$origDensityOutput = (adb shell wm density | Out-String)

Write-Host "Original Size Output: $origSizeOutput.Trim()"
Write-Host "Original Density Output: $origDensityOutput.Trim()"

if ($origSizeOutput -match "Physical size:\s*(\d+x\d+)") { $origSize = $matches[1] } else { $origSize = "reset" }
if ($origDensityOutput -match "Physical density:\s*(\d+)") { $origDensity = $matches[1] } else { $origDensity = "reset" }

Write-Host "Installing the debug APK..."
adb install -r -t app\build\outputs\apk\debug\app-debug.apk

Write-Host "Waking up emulator and dismissing lock screen..."
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
Start-Sleep -Seconds 1

foreach ($res in $resolutions) {
    Write-Host "Setting resolution for $($res.name) to $($res.size) at $($res.density) dpi..."
    adb shell wm size $($res.size)
    adb shell wm density $($res.density)
    
    # Wait for UI to rescale and settle
    Start-Sleep -Seconds 2
    
    Write-Host "Launching Instant Momir app and waiting for draw..."
    adb shell am start -W -n net.romzombie.momir/.MainActivity
    Start-Sleep -Seconds 2
    
    $outName = "$($res.name)_main.png"
    Write-Host "Capturing screenshot to $outName..."
    adb shell screencap -p /sdcard/$outName
    adb pull /sdcard/$outName "$outDir\$outName"
    
    # Clean up from device
    adb shell rm /sdcard/$outName
}

Write-Host "Restoring original display settings..."
if ($origSize -eq "reset") { adb shell wm size reset } else { adb shell wm size $origSize }
if ($origDensity -eq "reset") { adb shell wm density reset } else { adb shell wm density $origDensity }

Write-Host "Screenshots have been saved to the '$outDir' folder."
