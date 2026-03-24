# InstantMomir

An Android application for printing to Bluetooth thermal printers. Connect to any compatible Bluetooth thermal printer and send custom text directly from your Android device.

## Features

- Scan for and connect to paired Bluetooth thermal printers
- Enter custom text via an edit field and print on demand
- Lightweight — no cloud dependency, fully local

## Screenshots

<img height=550 width=275 src="https://user-images.githubusercontent.com/15203737/74158109-c61e7680-4c19-11ea-9e46-022e00cd96fe.jpg" hspace=24><img height=550 width=275 src="https://user-images.githubusercontent.com/15203737/74158619-af2c5400-4c1a-11ea-9f7c-3be923dc194e.jpg"/>

---

## Development Setup

### Requirements

| Tool | Minimum Version |
|---|---|
| JDK | 17 (bundled with Android Studio Meerkat+) |
| Android Studio | Meerkat (2024.3.2) or newer |
| Android SDK | API 35 (Android 15) |
| Android Build Tools | 35.x |
| Gradle wrapper | 8.11.1 (managed automatically) |
| Android Gradle Plugin | 8.9.0 (managed automatically) |

### Cloning the Repository

```bash
git clone https://github.com/ROMzombie/InstantMomir.git
cd InstantMomir
```

### Opening in Android Studio

1. Launch Android Studio → **File → Open** → select the `InstantMomir` folder.
2. Android Studio will detect the Gradle project and prompt you to sync — click **Sync Now**.
3. Install any missing SDK components listed in the SDK Manager if prompted.

### Building from the Command Line

```powershell
# Windows — debug build
.\gradlew assembleDebug

# Run unit tests
.\gradlew test

# Run lint
.\gradlew lint
```

---

## Developing with Antigravity on Windows

[Antigravity](https://antigravity.dev) is an AI coding assistant that runs inside VS Code and pairs with Android Studio for Android development. The recommended workflow on Windows is:

### Prerequisites

1. **VS Code** with the **Antigravity** extension installed.
2. **Android Studio** installed and the Android SDK configured at a path with no spaces (e.g. `C:\Android\Sdk`).
3. Ensure `ANDROID_HOME` is set in your system environment variables:
   ```powershell
   [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Android\Sdk", "User")
   ```
4. Add the platform-tools directory to your `PATH` so `adb` is available everywhere:
   ```powershell
   # Add to your PowerShell profile or system PATH
   $env:PATH += ";C:\Android\Sdk\platform-tools"
   ```

### Opening the Project

1. Open the `InstantMomir` folder in VS Code.
2. Antigravity will detect the Gradle project structure and provide code navigation, refactoring, and build assistance.
3. For UI editing and emulator management, keep Android Studio open alongside VS Code.

### Recommended Workflow

- Use **Antigravity / VS Code** for: code edits, refactoring, README changes, Gradle config, git operations.
- Use **Android Studio** for: Layout Editor, emulator, `Run` / `Debug`, Logcat, Profiler.
- Build and install via the command line (see ADB section below) to iterate quickly without leaving VS Code.

---

## Deploying to a Local Device via ADB

### Enable Developer Mode on Your Device

1. Go to **Settings → About Phone**.
2. Tap **Build Number** seven times until you see *"You are now a developer"*.
3. Go to **Settings → Developer Options** and enable:
   - **USB Debugging**
   - (Optional) **Wireless Debugging** for cable-free deployment

### Wired Deployment

1. Connect your device via USB.
2. Accept the *"Allow USB Debugging"* prompt on the device.
3. Verify the device is detected:
   ```powershell
   adb devices
   # Expected output: <serial>   device
   ```
4. Build and install:
   ```powershell
   .\gradlew installDebug
   ```
   The app will be installed and available in your app drawer.

### Wireless Deployment (Android 11+)

1. On the device: **Settings → Developer Options → Wireless Debugging → Enable**.
2. Tap **Pair device with pairing code** and note the IP, port, and code shown.
3. Pair from PowerShell:
   ```powershell
   adb pair <ip>:<pairing-port>
   # Enter the pairing code when prompted
   ```
4. Connect to the device:
   ```powershell
   adb connect <ip>:<port>
   adb devices   # confirm it shows "device"
   ```
5. Install:
   ```powershell
   .\gradlew installDebug
   ```

### Useful ADB Commands

```powershell
# View live logcat output (filter to your app)
adb logcat --pid=$(adb shell pidof -s com.sanxynet.bluetoothprinter)

# Uninstall the app
adb uninstall com.sanxynet.bluetoothprinter

# Reboot the device
adb reboot
```

---

## Related Articles

- [How to Print from Android Devices using Thermal Bluetooth Printer – Part 1](https://medium.com/@folahan/how-to-print-from-android-devices-using-thermal-bluetooth-printer-8dbc4750699d)
- [How to Print from Android Devices using Thermal Bluetooth Printer – Part 2](https://medium.com/@folahan/how-to-print-from-android-devices-using-thermal-bluetooth-printer-part-two-6418ba9469ac)

---

## License

InstantMomir is made available under the [MIT License](https://opensource.org/licenses/MIT).

```
MIT License

Copyright (c) 2018-2022 Yusuf Adefolahan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
