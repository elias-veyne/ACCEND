<p align="center">
  <img src="logo.png" alt="ACCEND logo" width="180" />
</p>

<h1 align="center">ACCEND</h1>

<p align="center">
  <strong>A tactical daily discipline system for physical, mental, and financial growth.</strong>
</p>

<p align="center">
  <a href="../../releases"><img src="https://img.shields.io/github/v/release/elias-veyne/ACCEND?style=flat-square&color=7C5CFC&label=release" alt="Latest release" /></a>
  <a href="../../actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/elias-veyne/ACCEND/build.yml?branch=main&style=flat-square&label=build" alt="Build status" /></a>
  <img src="https://img.shields.io/badge/platform-Android-green?style=flat-square" alt="Platform: Android" />
  <img src="https://img.shields.io/badge/license-Proprietary-lightgrey?style=flat-square" alt="License" />
</p>

---

## What is ACCEND?

ACCEND turns self-improvement into a daily system. Instead of vague goals, it gives you a
repeatable operating loop across three fronts:

- **Physical** — train, track, and progress without losing momentum.
- **Mental** — build focus and consistency through small, deliberate daily actions.
- **Financial** — grow your skill and runway with intention.

Everything is designed around one idea: show up today, and the numbers take care of tomorrow.

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | [Kotlin](https://kotlinlang.org/) |
| UI | [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3 |
| Local storage | [Room](https://developer.android.com/training/data-storage/room) |
| Build | Gradle (Kotlin DSL), Android Gradle Plugin |
| Distribution | GitHub Actions → GitHub Releases |

## Download the APK

The latest Android APK is published on the **[Releases](../../releases)** tab:

1. Open **[Releases](../../releases)** and select the newest version (for example `v1.0.0`).
2. Under **Assets**, download `ACCEND-v<version>.apk`.
3. Transfer it to your Android device and open the file.
4. When prompted, allow installation from this source (Android blocks unknown sources by
   default), then install and launch ACCEND.

> Every push to `main` triggers the [build workflow](../../actions/workflows/build.yml). When it
> completes successfully, the signed APK is attached to the release for that version.

## Build from source

Requirements: JDK 17 and Android SDK 35.

```bash
git clone https://github.com/elias-veyne/ACCEND.git
cd ACCEND
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`.

## Project structure

```
app/src/main/java/com/accend/app/   Kotlin application sources
app/src/main/res/                   Android resources
.github/workflows/build.yml         Build + release automation
gradle/libs.versions.toml           Centralised dependency versions
```

---

Built with intent. StaySharp. Keep ACCENDing.
