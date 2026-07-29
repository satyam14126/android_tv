# 📺 BraveTV Shield Browser

**BraveTV Shield Browser** is a native Android TV web browser designed specifically for 10-foot television experiences. It features **Brave-like Ad & Tracker Blocking**, **TV Remote D-Pad Virtual Cursor Navigation**, and an **AES-256 Encrypted Password Vault**.

---

## 🚀 Key Features

* **🛡️ Brave-Style Shield & Ad Blocker**: Intercepts tracking domains (`easylist_hosts.txt`) and injects cosmetic CSS hiding scripts (`cosmetic_hide.js`) to block ads, popups, and redirects on sites like YouTube and FMovies.
* **🎮 Dual-Mode TV Remote Navigation**: Controlled entirely using standard TV remote D-pads (`UP`, `DOWN`, `LEFT`, `RIGHT`, `ENTER`). Features smooth virtual pointer physics and magnetic snapping to web elements.
* **🔒 Encrypted Credential Vault**: Saves logins securely using Android `MasterKey` and `EncryptedSharedPreferences` (AES-256 GCM) with automatic domain-isolated autofill.
* **🧹 Cache & Storage Controller**: View cache size and 1-click clear WebView cache, cookies, and IndexedDB storage.
* **🎥 4K / 1080p Video Hardware Acceleration**: Native HTML5 fullscreen video playback handling via custom `WebChromeClient`.

---

## 📁 Repository Structure

```
android_tv/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml             # Android TV Leanback configuration
│   │   ├── assets/
│   │   │   └── adblock/                    # Blocklists & cosmetic script
│   │   ├── java/com/antigravity/tvbrowser/
│   │   │   ├── MainActivity.kt             # Main TV activity
│   │   │   ├── adblock/AdBlockEngine.kt    # Request filter & ad shield
│   │   │   ├── navigation/                 # Virtual Cursor & D-pad controller
│   │   │   ├── security/                   # Encrypted KeyStore password vault
│   │   │   ├── cache/CacheManager.kt       # Web cache & cookie cleaner
│   │   │   └── ui/                         # WebChromeClient & URL parser
│   │   └── res/                            # TV layout & Leanback drawables
├── demo/index.html                         # Interactive Web Simulator Demo
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🛠️ Build & Installation

### Build APK via Gradle
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Try the Web Demo Simulator
Open `demo/index.html` in any web browser to simulate the TV remote control, virtual cursor, password vault, and ad shield in real time!