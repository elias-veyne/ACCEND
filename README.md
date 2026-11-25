<p align="center">
  <img src="logo.png" alt="ACCEND logo" width="180" />
</p>

<h1 align="center">ACCEND</h1>

<p align="center">
  <strong>A tactical daily discipline system for physical, mental, and financial growth — now cloud-synchronized.</strong>
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

Every completed protocol awards EXP. Your level, total EXP and global rank are synchronized
through Firebase, so a reinstall never costs you your history.

## Features

- **Google Sign-In** via Android Credential Manager + Firebase Auth. On re-login the app restores
  your custom name and profile picture from `users/{uid}` in Firestore and skips Onboarding —
  Auth profile details never overwrite your in-app identity.
- **Cloud sync** — every Exercise / Nutrition / Mind / Money log is written locally with Room
  and pushed to `users/{uid}/logs/{yyyy-MM-dd}` in Firestore. On login the last 30 days are
  pulled back down and merged with local data. Empty local snapshots never overwrite remote logs.
- **Profile photos** are uploaded to Firebase Storage so the leaderboard shows your in-app avatar
  on every device.
- **Delete Account** (Settings → Danger Zone) removes the Firestore user document and all log
  subcollections, the stored avatar, the Firebase Auth account and all local Room/DataStore data
  after a re-authentication confirmation.
- **EXP & levels** — completing protocols grants EXP (500 EXP per level). The Home screen shows
  your level, total EXP and progress to the next level.
- **Global leaderboard** — live Firestore query, top 50 operators by EXP, with profile pictures
  and podium treatment matching the tactical UI.
- **Push notifications** — Firebase Cloud Messaging device tokens are saved to the user document;
  `AccendMessagingService` displays incoming alerts and refreshes tokens.
- **Adaptive launcher icon** generated from the ACCEND emblem.
- All of the original local-first features: Daily Work checklists, Analysis charts, Money ledger,
  Settings, timers, reminders and telemetry export/import.

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Local storage | Room |
| Backend | Firebase Auth, Cloud Firestore, Cloud Messaging (Firebase BoM) |
| Auth UI | Android Credential Manager + Google Identity |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle (Kotlin DSL), Android Gradle Plugin |
| Distribution | GitHub Actions → GitHub Releases |

## Firebase configuration

`app/google-services.json` is committed so builds are reproducible. It belongs to the
`accend-4ec40` Firebase project and contains the `com.accend.app` Android client plus the web
OAuth client used by Google Sign-In (`default_web_client_id`).

When adding a new signing certificate (for example a new upload keystore), register its SHA-1
fingerprint in Firebase Console → Project settings → Your apps → **com.accend.app**, otherwise
Google Sign-In will be rejected by Google. Firestore security rules must allow authenticated
users to read/write their own `users/{uid}` document and `users/{uid}/logs/**` subcollection.

## Download the APK

The latest Android APK is published on the **[Releases](../../releases)** tab:

1. Open **[Releases](../../releases)** and select the newest version (for example `v1.1.4`).
2. Under **Assets**, download `ACCEND-v<version>.apk`.
3. Transfer it to your Android device and open the file.
4. When prompted, allow installation from this source (Android blocks unknown sources by
   default), then install and launch ACCEND.

> Every push to `main` triggers the [build workflow](../../actions/workflows/build.yml). When it
> completes successfully, the APK is attached to the release for that version.

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
app/src/main/java/com/accend/app/
  data/cloud/      Firebase Auth, Firestore sync, leaderboard, FCM token, account deletion
  data/db/         Room database, entities and DAOs
  data/repo/       Local repositories (push to Firestore after every write)
  domain/          Scores, streaks, progression calculations
  ui/              Compose screens (Home, Daily Work, Analysis, Money,
                   Leaderboard, Settings, Sign-In)
  work/            Local reminders + Firebase Messaging service
app/google-services.json                Firebase configuration
.github/workflows/build.yml             Build + release automation
gradle/libs.versions.toml               Centralised dependency versions
```

---

Built with intent. StaySharp. Keep ACCENDing.
