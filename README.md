# 🛡️ PingShield
### The Ultimate PUBG Mobile Network Optimizer

![Build Status](https://github.com/YOUR_USERNAME/PingShield/actions/workflows/build-apk.yml/badge.svg)

## ⬇️ Download Latest APK
👉 Go to [Releases](https://github.com/YOUR_USERNAME/PingShield/releases/latest)
👉 Download PingShield.apk
👉 Install on your Android phone

## What it does
- Blocks all background apps during your PUBG match
- Monitors ping every 200ms live
- Auto-fixes spikes in under 200ms
- Zero ads, zero root, zero cost

## Requirements
- Android 11 or higher
- PUBG Mobile installed

## Tech Stack
- **Language:** Kotlin (100%, no Java)
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM with Hilt DI
- **Async:** Kotlin Coroutines + StateFlow
- **VPN:** Android VpnService (tun0 interface)
- **Build:** Gradle with GitHub Actions CI/CD

## Build from Source
```bash
git clone https://github.com/YOUR_USERNAME/PingShield.git
cd PingShield
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```
