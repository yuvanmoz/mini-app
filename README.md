# ⚡ Mini — Offline-First X/Twitter Knowledge Library

> A luxury, distraction-free Android companion for curating, organizing, and offline-reading your favorite posts and insights from X (formerly Twitter).

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%2B%29-green?logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple?logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Dagger Hilt](https://img.shields.io/badge/DI-Dagger%20Hilt-orange)](https://dagger.dev/hilt/)
[![Room DB](https://img.shields.io/badge/Storage-Room%20Database-blue)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## 💡 Overview

High-signal ideas and threads get buried on social timelines. **Mini** allows you to instantly save and archive posts from the X share sheet into a private, searchable offline library with zero algorithmic noise or tracking.

---

## ✨ Features

- **📥 Seamless 1-Tap Share Sheet Import:** Share any X/Twitter post directly into Mini; content, images, author profiles, and timestamps are extracted and saved locally.
- **📚 Collections & Custom Tags:** Group saved insights into contextual folders, tag key themes, and attach personal reflective notes.
- **🖼️ Full-Screen High-Res Image Viewer:** Clean zoomable lightbox for attached media and diagrams.
- **📱 Dynamic Home Screen Widgets:** Glanceable desktop widgets (`PostWidgetProvider`) that rotate your saved wisdom right on your home screen.
- **📌 Pinned Notification Reader:** Pin an important thread or note directly into your Android notification drawer for all-day focus.
- **🌊 Liquid Atmosphere Design:** High-end glassmorphism, floating pill navigation dock (`MiniFloatingPillDock`), dynamic theming, and SF Pro typography.
- **🔒 100% Offline & Local:** Stored in a local Room SQLite database with full data privacy.

---

## 🏗 Architecture & Tech Stack

Built following **Clean Architecture** and **Unidirectional Data Flow (UDF)**:

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3 with custom glass shaders)
- **Dependency Injection:** Dagger Hilt
- **Local Persistence:** Room Database (PostEntity, MediaItemEntity, TagEntity, CrossRefs)
- **Network & Parsing:** Retrofit 2 + FxTwitter / OEmbed content extraction
- **Preferences:** Android Jetpack DataStore
- **Widgets:** Android AppWidgetProvider with custom RemoteViews

---

## 📂 Project Structure

```text
Mini/
├── app/
│   ├── src/main/
│   │   ├── java/com/mini/app/
│   │   │   ├── MainActivity.kt           # Navigation host & deep-link share receiver
│   │   │   ├── ShareActivity.kt          # Instant background import share target
│   │   │   ├── data/
│   │   │   │   ├── local/                # Room DB, DAOs, and Entities
│   │   │   │   ├── remote/               # FxTwitter & OEmbed API services
│   │   │   │   └── repository/           # PostRepository implementation
│   │   │   ├── di/                       # Hilt dependency injection modules
│   │   │   ├── domain/model/             # Post, Tag, Author models
│   │   │   ├── ui/                       # Feed, Collections, Search, Settings, Widgets
│   │   │   ├── util/                     # XUrlParser, PinnedPostManager, WordExporter
│   │   │   └── widget/                   # Home screen desktop widget provider & service
│   │   └── res/                          # Vector drawables, layout XML, SF Pro fonts
│   └── build.gradle.kts
```

---

## 🚀 Building & Running

### Prerequisites

- Android Studio (2023.1+)
- JDK 17
- Android SDK 34+

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yuvanmoz/mini-app.git
   cd mini-app
   ```

2. **Open in Android Studio or build via CLI:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on device:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📄 License

Licensed under the Apache License, Version 2.0.
