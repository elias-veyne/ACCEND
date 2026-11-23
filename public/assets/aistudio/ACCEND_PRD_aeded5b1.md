# ACCEND — Product Requirements Document (v2 Rebuild)

**Audience:** OpenCode (AI coding agent). **Goal:** rebuild the app from scratch, wire CI, ship an APK to GitHub Releases with zero further clarification.
**Repo:** `github.com/elias-veyne/ACCEND-365` (public, `main`) · **Platform:** Android only · **Date:** 2026-11-21

---

## 1. Overview & Goals

### 1.1 Product
ACCEND is a single-user, local-first Android app: a daily discipline system for a 365-day cycle covering **physical strength**, **mental control** (meditation, deep work, reading, screen time) and **financial / skill runway**. No auth, no signup, no backend, no cloud sync, no analytics, **no INTERNET permission**.

### 1.2 Goals
1. Make today's checklist one scroll and a few taps.
2. Show honest progress: 30-day grid, Mind Score, radar of muscle volume, trend charts.
3. Hold the user accountable without clutter: exactly two notification types.
4. Premium dark red-purple UI, Compose animations only.
5. Fully reproducible CI: push to `main` → APK on GitHub Releases.

### 1.3 Non-goals (v2)
Investing/crypto/stocks, social features, accounts, cloud backup, widgets, Wear OS, tablets-specific layouts, localization (English only), Lottie/video.

### 1.4 Resolved ambiguities (OpenCode: implement exactly these decisions)

| # | Topic | Decision |
|---|-------|----------|
| A1 | Default "2-day miss" text | Harsh but free of slurs/profanity: **"Two days gone. Get back in the arena if you want to improve, or sit back and call it a dream."** The user can replace it with any text in Settings (max 160 chars; empty input restores the default). |
| A2 | "Day 1 (Sun) … Day 7 (Sat)" | These are **calendar weekdays** (1=Sunday … 7=Saturday), the weekly program. Independent of the journey day number `n` shown in the header. |
| A3 | Streaks "no-reset" | A streak counts completed days. A **single** missed day is bridged (does not reset, does not add). **Two consecutive** misses end the streak. Constant `STREAK_GRACE_DAYS = 1` in `AppConfig`; setting it to 0 gives strict behavior. No shame UI on break; "Best" streak is kept. |
| A4 | "Australian Pushups" | Keep the name exactly as given. Mapped to muscle group `BACK_PULL` (inverted-row style movement). |
| A5 | Saturday "Sleep 9h" duplicates Nutrition "Sleep 9h" | Saturday seeds 3 exercise rows (Walk, Stretch, Neck Mobility). The "Sleep 9h" row in the Recovery card is a **mirrored control** of `NutritionLog.sleep9h`, counted **once** in completion. |
| A6 | "Runway to 18 — June 2027" | `runwayTargetDate` preference, default **2027-06-01**, editable in Settings → Targets. |
| A7 | Money amounts | Whole NPR stored as `Long`. Display `NPR 1,250` (thousands separators, no decimals). |
| A8 | Screen-time toggle | Counts as done once a value (even 0h 0m) has been logged for the day. Over-target does not un-complete it. |
| A9 | Mind Score | Formula in §9.5. |
| A10 | Day boundary | Local midnight `00:00` (constant `DAY_ROLLOVER_HOUR = 0` in `AppConfig`). |
| A11 | Home 30-day grid | 6 columns × 5 rows, chronological, column-major. |
| A12 | `applicationId` | Read it from the existing repo and **keep it**. If none can be found use `com.accend.app`. Namespace for new code: `com.accend.app`. |
| A13 | Weight/height entry | Both in Daily Work (a collapsible "Body" card at the bottom) and in Settings → Body logs. |
| A14 | Progression for non-compound exercises | `VOLUME` ladder: "Level up" raises the target by a step (§8.3). |
| A15 | Daily timers in background | Timers use timestamps (not tick counts); no extra background notifications (keeps "no other notifications"). |

---

## 2. Tech Stack (exact)

| Area | Choice / version |
|------|------------------|
| Language | Kotlin **2.0.21** |
| Build | Gradle **8.11.1** (wrapper committed), Android Gradle Plugin **8.7.3**, Kotlin DSL, version catalog `gradle/libs.versions.toml` |
| JDK | 17 |
| UI | Jetpack Compose, BOM **2024.12.01**, Material 3 |
| Icons | `androidx.compose.material:material-icons-extended` (use `Icons.Outlined.*`) |
| Navigation | `androidx.navigation:navigation-compose:2.8.5` |
| DB | Room **2.6.1** (`room-runtime`, `room-ktx`, KSP compiler), KSP **2.0.21-1.0.28**, schema export on |
| Prefs | `androidx.datastore:datastore-preferences:1.1.1` |
| Background | `androidx.work:work-runtime-ktx:2.10.0` |
| Images | Coil **2.7.0** (`coil-compose`) |
| Async | kotlinx-coroutines **1.9.0**, Flow, `lifecycle-runtime-compose` + `lifecycle-viewmodel-compose` **2.8.7** |
| Serialization | `kotlinx-serialization-json:1.7.3` + plugin `org.jetbrains.kotlin.plugin.serialization` (JSON export/import and progression JSON) |
| Splash | `androidx.core:core-splashscreen:1.0.1` (system splash only paints the dark background; Compose splash is the real one) |
| DI | Manual (`AppContainer` in `Application`), **no Hilt/Dagger** |
| SDK | `minSdk 26`, `compileSdk 35`, `targetSdk 35` (latest stable validated against AGP 8.7.3; bump compile/target together with AGP) |
| Charts | **Custom Compose `Canvas`** (no chart library) |
| Font | Inter (OFL) bundled as TTF in `res/font` (§3.2) |

Forbidden: Firebase, any backend/auth SDK, analytics, Lottie, network libraries, `INTERNET` permission.

Release build: `isMinifyEnabled = true`, `isShrinkResources = true`. If R8 breaks the build or crashes, add the needed keep rule to `proguard-rules.pro`; disabling minify is a last resort and must be noted in the release notes.

---

## 3. Design System

### 3.1 Color tokens (dark only; no light theme)

| Token | Hex | Use |
|-------|-----|-----|
| `bgBase` | `#08060A` | App background, splash |
| `surfaceElevated` | `#16101C` | Bottom bar, sheets, dialogs |
| `surfaceCard` | `#1D1622` | Cards |
| `accentPrimary` | `#C2185B` | Active states, key numbers |
| `accentSecondary` | `#7B1FA2` | Secondary accent |
| `gradientAccent` | linear `#7B1FA2 → #C2185B` (left→right, or top→bottom for vertical bars) | Splash, buttons, ring, charts, active tab |
| `textPrimary` | `#F5F0F7` | Body/headers |
| `textMuted` | `#9A8FA3` | Secondary text, inactive icons |
| `success` | `#4ADE80` | Done/under target |
| `warning` | `#FBBF24` | Over target / caution |
| `danger` | `#EF4444` | Destructive actions |
| `border` | `#2A2030` | Dividers, card outlines (1dp) |

Map to a Material 3 `darkColorScheme` (`primary=accentPrimary`, `secondary=accentSecondary`, `background=bgBase`, `surface=surfaceCard`, `onSurface=textPrimary`, `outline=border`, `error=danger`). Provide an extra `AccendColors` object via `CompositionLocal` for tokens M3 lacks (`gradientAccent`, `success`, `warning`, `textMuted`).

### 3.2 Typography
Inter via bundled files `res/font/inter_regular|medium|semibold|bold|extrabold.ttf` (download once from the official Inter release, commit them with `OFL.txt` under `app/src/main/res/font/`; if download is impossible fall back to `FontFamily.SansSerif` and note it in the README).

| Style | Size / weight / tracking |
|-------|--------------------------|
| Wordmark ACCEND | 44sp ExtraBold, letter-spacing +6sp |
| Display (Mind Score number) | 56sp Bold |
| Title L | 28sp Bold |
| Title M | 20sp SemiBold |
| Body | 16sp Regular |
| Label | 13sp Medium, uppercase optional, +0.5sp |
| Caption | 12sp Regular, `textMuted` |

### 3.3 Spacing, shape, elevation
- Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 dp. Screen horizontal padding 20dp. Card inner padding 16dp. Section gap 24dp.
- Corner radius: cards **16dp**, modals/bottom sheets/dialogs **24dp**, chips fully rounded, grid cells **3dp**.
- Card: `surfaceCard` fill + 1dp `border`, no default Material elevation.
- **Primary CTA glow:** gradient fill, height 52dp, radius 16dp, plus a purple glow shadow (`#7B1FA2` at ~40% alpha, blur 24dp, y-offset 6dp; implement via `Modifier.drawBehind` + `Paint.setShadowLayer`/`BlurMaskFilter`, or `Modifier.shadow(ambientColor/spotColor = accentSecondary)`).

### 3.4 Motion & feedback
- Compose animations only (`animate*AsState`, `AnimatedVisibility`, `Crossfade`, `AnimatedContent`).
- Tab switch: crossfade 200ms. Sub-screens: shared slide-in-from-right 280ms + fade, `FastOutSlowInEasing`.
- Checkbox/toggle tick: scale 1→1.15→1 over 160ms and color fill; **haptic** via helper `Haptics.tick(view)` (`HapticFeedbackConstants.CONFIRM` on API 30+, `VIRTUAL_KEY` below; `Haptics.heavy(view)` = `LONG_PRESS` for day-complete and timer-complete).
- Progress rings and chart lines animate in on first composition (600ms).
- Respect the system "remove animations" setting (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` → skip animations).

### 3.5 Iconography
Material Symbols **outlined** only (`Icons.Outlined.*`). **No emoji anywhere** in UI strings (use ✓/✗ drawn as icons, not glyphs). Tab icons: Home=`Home`, Daily Work=`Checklist` (`TaskAlt`), Analysis=`Insights`, Money=`AccountBalanceWallet`, Settings=`Settings`.

### 3.6 Reusable components (put in `ui/components`)
`AccendTheme`, `PrimaryButton` (glow), `SecondaryButton` (outlined), `AccendCard`, `SectionHeader`, `ToggleRow` (label + custom checkbox), `ExerciseRow`, `ProgressRing`, `ContributionGrid`, `StatChip`, `ChartCard`, `LineChart`, `BarChart`, `RadarChart`, `EmptyState` (icon + one line + optional action), `AccendBottomSheet`, `ConfirmDialog`, `NumberStepper`, `PfpAvatar` (image via Coil, fallback = initial letter on gradient circle), `AccendSnackbar`.

---

## 4. App Flow

### 4.1 Launch decision
`MainActivity` installs the system splash (background `#08060A`, no icon, `keepOnScreenCondition` until DataStore `onboarded` is read, max 300ms) and then shows the **Compose splash** route.

### 4.2 Splash (preserve the existing implementation's pattern)
1. **Before wiping the repo**, copy the existing splash source (composable/activity, animation code, theme/styles) to `docs/legacy-splash-reference/` using the `.kt.txt` extension (not compiled). Keep it as the reference.
2. Port the existing splash's animation structure into `ui/splash/SplashScreen.kt`, rebranded: gold → red-purple. If the old implementation is not Compose, reproduce its timing and motion in Compose.
3. Visual: `#08060A` background, ACCEND wordmark centered (Title style above, `gradientAccent` text brush), a soft **purple radial glow** behind it (radial gradient `#7B1FA2` alpha 0.45 → transparent, radius ≈ 220dp, slow breathing scale 1.0↔1.08).
4. Timeline: wordmark fade+scale-in 400ms → **hold 1.2s** → fade out 300ms → navigate.
5. Destination: `onboarded == false` → Onboarding; else Home. Splash route is popped off the back stack.

### 4.3 First launch → Onboarding (single screen)
- Title "Welcome to ACCEND", subtitle "Build the person you'll be at 18."
- 96dp circular avatar (tap to pick; picker = `ActivityResultContracts.PickVisualMedia` image-only, no storage permission). Fallback: initial letter on gradient circle, updates live as the name is typed.
- Name field: required, trimmed, 1–24 chars. Inline error under 1 char.
- `Begin` (PrimaryButton), disabled until name is valid.
- On `Begin` (single DB transaction): copy picked image into `filesDir/pfp.jpg` (center-crop square, 512px, JPEG q=90; store `file://` URI) → insert `User(id=1, name, pfpUri, joinedDate=today, onboarded=true)` → `SeedRepository.seedIfEmpty()` → write DataStore `onboarded=true` → schedule workers → on Android 13+ show the system `POST_NOTIFICATIONS` request (denial is fine; app continues) → navigate to Home (clear back stack).

### 4.4 Returning launch
Splash → Home. Run `DayEvaluator.catchUp()` (§9.1) before first Home emission.

### 4.5 Profile changes
Settings → Profile: edit name (same validation), change PFP (same picker + same crop/copy; delete previous file), "Remove photo" action.

---

## 5. Navigation & Tab Structure

Single `NavHost` (`MainNav`). Start destination `splash`.

| Route | Screen | Bottom bar |
|-------|--------|------------|
| `splash` | Splash | hidden |
| `onboarding` | Onboarding | hidden |
| `home` | Home | shown |
| `daily` | Daily Work | shown |
| `analysis` | Analysis | shown |
| `money` | Money | shown |
| `settings` | Settings | shown |
| `meditation` | Meditation timer (full screen) | hidden |
| `deepwork?minutes={m}` | Deep Work timer (full screen) | hidden |
| `progression` | Progression list | hidden |
| `settings/body` | Body logs (weight/height) | hidden |
| `settings/data` | Export / Import / Reset | hidden |

- Bottom bar: 5 items, `surfaceElevated` background, 1dp top `border`, icon + label. Active = `gradientAccent`-tinted icon (use `accentPrimary` tint + a 24×3dp gradient indicator under the icon), inactive = `textMuted`. Use `popUpTo(home){saveState=true}`, `launchSingleTop`, `restoreState=true`.
- A small `danger`-free dot badge on the Settings icon when any progression "level up" suggestion is pending.
- Back on `home` exits the app; back on other tabs returns to `home`.
- Deep link (used by notifications): `accend://daily` → `daily`.
- System bars: edge-to-edge, transparent status/navigation bars, light icons.

---

## 6. Screen-by-Screen Specification

Common: all screens scroll vertically with 20dp side padding, bottom padding = nav bar + 16dp. Data comes from ViewModels exposing `StateFlow<UiState>` collected with `collectAsStateWithLifecycle`. "Today" = `LocalDate.now(zone)` re-evaluated on `ON_RESUME` and at midnight (a ticker flow).

### 6.1 Home
**Layout (top → bottom):**
1. **Header row:** `PfpAvatar` 48dp left, text "Day {n} · {name}" (Title M), right side empty. Tapping avatar → `settings`.
2. **Mind Score ring card:** 200dp ring, 14dp stroke, sweep gradient `gradientAccent`, track `border`; center = score (Display) over caption "Mind Score". Animates from 0 on first show. Tap → `analysis`.
3. **Today's summary card** (tap → `daily`): four rows/chips: `Physical {done}/{total}` · `Meditation` · `Deep Work` · `Reading`, where the last three show a check icon (`success`) or a close icon (`textMuted`). Above the rows a thin completion bar (today's `completionPercent`).
4. **30-day completion grid card** (tap → `analysis`): grid on the left (6 cols × 5 rows, 12dp cells, 3dp radius, 4dp gaps, chronological column-major, last cell = today with a 1dp `accentPrimary` outline); on the right three stats: "Active days x/30", "Avg y%", "Perfect days z". Title "Last 30 days".
5. **Streak chips row** (horizontally scrollable if needed): `Physical {n}` · `Meditation {n}` · `Deep work {n}` (flame-free; use `LocalFireDepartment` icon outlined is acceptable). Each chip also shows "Best {m}" caption. Tap any → `daily`.

**Empty/new state:** Day 1 with no logs: ring shows 0, grid cells empty, streaks 0, summary shows 0/total. Show a one-line hint card "Start with today's checklist" with a button → `daily` while today's completion is 0.

### 6.2 Daily Work
Header: "Day {n}" + date, and a thin today-completion bar with percent. Sections as cards, each independently usable:

**A. Physical — {focus}** (focus = Upper / Core / Legs / "Rest / Active Recovery" by weekday)
- Rows from `Exercise` where `dayOfWeek == today.weekday`, ordered by `orderInDay`.
- Row: checkbox (left), exercise **current level name** (e.g. "Knee Pushups"), target text on the right ("20 reps", "3×20 s", "2×100 m", "20 min"). Tap row = toggle; tap the chevron/name area expands a one-line note showing "Level {i+1} of {N}" and the muscle group.
- Section header shows `{done}/{total}`.
- Saturday: card titled "Rest / Active Recovery" with rows Walk (20 min), Full-body Stretch (10 min), Neck Mobility, plus the mirrored "Sleep 9h" row (A5).

**B. Nutrition (5 toggles):** Ate protein · 2 heavy meals · Breakfast · Hydration 3L · Sleep 9h. (Hydration label uses fixed text "Hydration 3L".)

**C. Mind (4 rows):**
1. **Meditation:** row shows today's target minutes (ladder, §9.7) and a `Start` button → `meditation`. The tick is **set automatically** when a session completes (not manually tappable). Shows logged minutes after completion.
2. **Screen time:** inline input: hours (0–23) + minutes (0–59) steppers/fields and a `Save` text button. Shows "x h y m" and a status chip: under target `success`, over `warning`. Re-editable all day.
3. **Deep Work:** row with `Start` → `deepwork?minutes={default}`; a preset chip row (15 / 30 / 45 / 60) selects the minutes passed to the timer (default preset = `deepWorkDefault`). Tick set automatically when a full session completes. Shows today's total minutes.
4. **Reading:** if `readingTarget == 1` a plain checkbox; otherwise a stepper "{pages}/{target} pages". Done when `pages >= readingTarget`.

**D. Body (collapsible, collapsed by default):** Weight (kg, decimal, 1 place) and Height (cm, integer). A small caption "Weekly" / "Monthly". Auto-expands with a dot hint when the last weight entry is ≥ 7 days old or the last height entry is ≥ 30 days old (or none exists). Saving upserts that date's row.

**Behavior:**
- All sections independently tappable; every tick calls `Haptics.tick`.
- Day complete = all exercise rows + 5 nutrition + 4 mind items done (100%). On reaching 100%, play a one-time subtle glow pulse on the header bar and `Haptics.heavy`.
- Partial days count partially (§9.2).
- **Missed days:** there is no "missed" button. Missed days are auto-logged (§9.1, §9.6).
- Empty state: if (impossible but defensive) no exercises are seeded for the weekday, show `EmptyState("No exercises scheduled today")`.

### 6.3 Analysis
Vertical stack of `ChartCard`s; each card = title, optional caption (latest value), chart. All charts: dark background, purple gradient strokes/fills, **no gridlines**; only min/max y-labels and sparse x-labels (first/last date); touch-scrub shows a tooltip bubble. Target lines are dashed `textMuted`.

1. **30-day grid (large):** 6 × 5 cells sized to card width (cell ≈ (width − 5·6dp)/6, max 44dp), day-of-month number inside each cell (Caption). **Pager of 30-day blocks** anchored to journey days (block k = days 30k+1…30k+30, title "Days 1–30"); swipe or chevrons; block with future days shows them as outlined empty cells. Tap a cell → **Day detail sheet** (§6.10).
2. **Radar chart** — axes in order: Chest/Push, Back/Pull, Biceps, Triceps, Shoulders, Core, Legs, Neck, Forearms. Fill = cumulative completed volume for the last 30 days (§9.9). Gradient fill (alpha 0.35) with gradient outline; below it a compact legend list of raw volume numbers per axis.
3. **Mind Score trend** — line, last 30 days, y 0–100.
4. **Meditation minutes** — line, last 30 days.
5. **Screen time** — line (hours) with dashed target line at `screenTarget`.
6. **Deep work total** — bar chart, minutes per ISO week (Mon–Sun), last 12 weeks.
7. **Reading pages** — line, last 30 days.
8. **Weight** — weekly line (one point per week = latest entry that week), last 12 weeks, dashed goal line at `weightGoal`.
9. **Height** — monthly line, last 12 months, dashed goal line at `heightGoalCm` labelled "Growth window".

**Empty state per chart:** the card stays but the chart area shows muted text, e.g. "Log meditation to see this chart" (min 2 points required for a line chart; 1 point renders a single dot with its value).

### 6.4 Money
Currency NPR. Layout:
1. **Balance header:** "Balance" caption, big number `NPR {balance}` (Display), where `balance = Σ IN − Σ OUT − Σ jarDeposits + Σ jarWithdrawals` (spendable cash; the jar is separate).
2. **Save jar card:** "Saved" total = `Σ deposits − Σ withdrawals`. Buttons: `Add` / `Withdraw` (amount sheet). **Auto-suggest:** after saving any IN entry, show a bottom sheet "Move NPR {round(amount × savePercent/100)} to your jar?" with `Move` (creates a `SavingsMove` linked to the transaction) and `Skip`. Default `savePercent = 40`.
3. **Leak card:** "Fun spending this week" `NPR {sum}` (Mon–Sun, category = fun). No commentary, no lecture; show previous week's number as caption only.
4. **Skill Hours card:** total hours (1 decimal) + `Add hours` (sheet: hours with 0.25 step, tag text field with chips of recent tags). Tap card opens a simple list of recent skill logs (bottom sheet, swipe-delete).
5. **Runway to 18 card:** "{d} days to runway" (days until `runwayTargetDate`; if passed, "Runway reached") and "{h} skill hours banked".
6. **Ledger list:** grouped by date (header "Today", "Yesterday", else "12 Nov 2026"); each row: category icon, note/category, source or category caption, amount (IN `success` with `+`, OUT `textPrimary` with `−`). Swipe-to-delete with undo snackbar; tap to edit.
7. **FAB** (gradient, 56dp, `Add`) → add-transaction sheet.

**Add-transaction sheet:** segmented `In | Out`; amount (numeric keypad, whole numbers, required > 0); In → source chips: Pocket money · Gift · Freelance · Other; Out → category chips: Food · Transport · Data · Books · Fun · Other; date (default today, date picker, cannot be in the future); note (optional, max 80 chars); `Save`.
**Impulse check:** on saving an OUT with category Fun, show a modal "Need or want right now?" with `Need` / `Want` buttons (and a dismiss X). Answer stored in `impulseAnswer`; dismissing stores null; **never blocks** saving (the transaction is saved first).
**Empty states:** no transactions → `EmptyState("No entries yet. Tap + to log money in or out.")`; cards show zeros.
No investing screen, no crypto, no stocks.

### 6.5 Settings
Sectioned list:
- **Profile:** avatar + name row (edit, change/remove photo).
- **Notifications:** master toggle, "Daily reminder" toggle + time picker (default 20:00), "2-day miss alert" toggle, "2-day miss text" field (editable, default A1, "Reset to default" action). Under the toggles a Caption "Only two notification types exist."
- **Targets:** Screen time target (h, default 3), Reading pages target (default 1), Deep Work default duration (15/30/45/60, default 30), Save % (0–100, default 40), Weight goal (kg, default 75), Height goal (default 6'0" ≈ 183 cm; label "Growth window"; helper "A direction, not a deadline."), Runway date (default 2027-06-01).
- **Training:** `Progression` → `progression` (with badge if suggestions pending).
- **Body logs:** → `settings/body` (list + add/edit/delete for weight and height).
- **Data:** `Export data (JSON)`, `Import data (JSON)` → `settings/data`; **Danger zone:** `Reset journey`.
- **About:** version name (+ code), "Repository" row opening `https://github.com/elias-veyne/ACCEND-365` via `ACTION_VIEW`, licenses (Inter OFL).

### 6.6 Meditation timer (`meditation`)
Pure timer, nothing else. Full screen `#08060A`, keep screen on (`FLAG_KEEP_SCREEN_ON`), system bars hidden.
- Pre-start: large target minutes ("Week {w} · {m} min"), `Begin` button.
- Running: large circular countdown (sweep gradient, thin), remaining `mm:ss` in the center, no other controls except a subtle `End early` text button (bottom).
- Based on timestamps (`SystemClock.elapsedRealtime`): survives backgrounding. Leaving via Back while running → confirm dialog "End session? It won't be logged."
- Completion: haptic heavy pulse + gentle fade; then a **restlessness** step with three chips: "a lot" / "a little" / "still" (one required tap), then `Done`. Writes `MeditationLog(date, minutes=target, restlessness)` and returns to `daily`.
- Ending early or abandoning logs nothing. Re-entering the screen the same day starts a new session; extra completed sessions add minutes (the Mind tick stays set).

### 6.7 Deep Work timer (`deepwork`)
Same full-screen structure. Preset chips 15 / 30 / 45 / 60 (pre-selected from nav arg), `Start`. Running: countdown ring + `mm:ss`; buttons `Pause`/`Resume` and `Stop`.
- Completion → heavy haptic, `DeepWorkLog(minutes=full, completed=true)`, summary "Session complete · {m} min", button `Done`.
- `Stop` early: dialog "Log {elapsed} min as partial?" → `Log partial` (completed=false, minutes=elapsed rounded down, ≥ 1 min required) or `Discard`. Partial sessions add to total minutes (charts) but **do not** tick Deep Work.

### 6.8 Progression (`progression`)
- Exercises grouped by muscle group (headers: Chest/Push, Back/Pull, Biceps, Triceps, Shoulders, Core, Legs, Neck, Forearms, Recovery), each row: name (current level), target text, "Level i of N", scheduled weekday, and a `Level up` button (disabled at max level).
- `Level up` confirmation sheet shows "{old} → {new} · target {t}". Applies immediately: `currentLevelIndex++`, target/sets from the new ladder entry (or `+step` for VOLUME ladders), `levelSince = today`, clears snooze.
- **Suggestion:** when an exercise has ≥ 3 consecutive completed scheduled sessions at the current level (§9.9), the row shows a gradient pill "Ready to level up" and a `Not now` action (snooze until a newer completed session). Ignorable; no popups.
- Also a `Level down` overflow action per row (safety valve).

### 6.9 Export / Import / Reset (`settings/data`)
- **Export:** builds the JSON (§9.10) into `cacheDir/accend_export_{yyyyMMdd}.json`, exposes it via `FileProvider`, launches the system share sheet (`ACTION_SEND`, `application/json`). Also a secondary `Save to file` using `CreateDocument("application/json")`.
- **Import:** `OpenDocument("application/json")` → parse + validate (`app == "ACCEND"`, `schemaVersion <= current`) → confirmation dialog summarizing counts ("This replaces everything on this device") → single transaction wipe+insert → restart `DayEvaluator`, reschedule workers → success snackbar. Invalid/newer file → error dialog, nothing changes.
- **Reset journey (danger zone):** two confirmations: (1) dialog "Reset your journey?", (2) dialog requiring typing `RESET`. Wipes all log tables and money/skill/body data, re-seeds exercises at level 0, sets `joinedDate = today` (Day 1 again). **Keeps** user name/PFP and all preferences. Clears internal evaluator state.

### 6.10 Day detail sheet (from the Analysis grid)
Bottom sheet: date + "Day {n}", completion %, and rows: Physical x/y, Nutrition x/5, Meditation (min), Deep work (min), Reading (pages), Screen time (h m). Read-only.

---

## 7. Data Model (Room)

DB class `AccendDatabase`, name **`accend_v2.db`** (never reuse any old DB name), `version = 1`, `exportSchema = true`, no destructive migration. Dates are stored as ISO `yyyy-MM-dd` text via `LocalDate` TypeConverters; timestamps as epoch millis.

### 7.1 Entities

```kotlin
@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int = 1,            // singleton
    val name: String,
    val pfpUri: String?,                    // file:// in filesDir, or null
    val joinedDate: LocalDate,
    val onboarded: Boolean = true
) // dayNumber is COMPUTED: ChronoUnit.DAYS.between(joinedDate, today) + 1

enum class MuscleGroup { CHEST_PUSH, BACK_PULL, BICEPS, TRICEPS, SHOULDERS, CORE, LEGS, NECK, FOREARMS, RECOVERY }
enum class TargetUnit { REPS, SECONDS, METERS, MINUTES, NONE }

@Entity(tableName = "exercise", indices = [Index("dayOfWeek")])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                       // CURRENT level display name (denormalised from ladder)
    val muscleGroup: MuscleGroup,
    val currentLevelIndex: Int = 0,
    val sets: Int = 1,
    val targetReps: Int,                    // the numeric target (reps / seconds / meters / minutes per unit)
    val unit: TargetUnit,
    val orderInDay: Int,
    val dayOfWeek: Int,                     // 1=Sunday … 7=Saturday
    val levelSince: LocalDate? = null,
    val suggestionSnoozedOn: LocalDate? = null
)

@Entity(tableName = "exercise_progression")
data class ExerciseProgression(
    @PrimaryKey val exerciseId: Long,       // FK -> exercise.id (CASCADE)
    val levelsList: String                  // JSON: [{"name":"Knee Pushups","sets":1,"target":20}, ...]
)

@Entity(tableName = "exercise_log", indices = [Index(value = ["date","exerciseId"], unique = true)])
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val exerciseId: Long,                   // FK -> exercise.id (CASCADE)
    val done: Boolean,
    val levelNameSnapshot: String,
    val volumeSnapshot: Double              // computed at tick time (§9.9)
)

@Entity(tableName = "nutrition_log", indices = [Index(value = ["date"], unique = true)])
data class NutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val protein: Boolean = false, val heavyMeals: Boolean = false,
    val breakfast: Boolean = false, val hydration: Boolean = false, val sleep9h: Boolean = false
)

@Entity(tableName = "meditation_log", indices = [Index("date")])
data class MeditationLog(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate, val minutes: Int, val restlessness: String /* "a lot" | "a little" | "still" */)

@Entity(tableName = "screen_time_log", indices = [Index(value = ["date"], unique = true)])
data class ScreenTimeLog(@PrimaryKey(autoGenerate = true) val id: Long = 0, val date: LocalDate, val minutes: Int)

@Entity(tableName = "deep_work_log", indices = [Index("date")])
data class DeepWorkLog(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate, val minutes: Int, val completed: Boolean)

@Entity(tableName = "reading_log", indices = [Index(value = ["date"], unique = true)])
data class ReadingLog(@PrimaryKey(autoGenerate = true) val id: Long = 0, val date: LocalDate, val pages: Int)

@Entity(tableName = "day_log", indices = [Index(value = ["date"], unique = true)])
data class DayLog(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate, val completionPercent: Int /*0..100*/, val missedFlag: Boolean,
    val finalized: Boolean /* true once the date is in the past and evaluated */)

@Entity(tableName = "money_transaction", indices = [Index("date")])
data class MoneyTransaction(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate, val type: String /* IN | OUT */, val amount: Long,
    val category: String?,      // OUT: food|transport|data|books|fun|other
    val source: String?,        // IN: pocket_money|gift|freelance|other
    val note: String?, val impulseAnswer: String? /* NEED | WANT | null */)

@Entity(tableName = "savings_move")
data class SavingsMove(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate, val amount: Long, val direction: String /* DEPOSIT | WITHDRAW */,
    val linkedTransactionId: Long? = null)  // FK -> money_transaction.id (SET NULL)

@Entity(tableName = "skill_log", indices = [Index("date")])
data class SkillLog(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate, val hours: Double, val tag: String)

@Entity(tableName = "weight_log", indices = [Index(value = ["date"], unique = true)])
data class WeightLog(@PrimaryKey(autoGenerate = true) val id: Long = 0, val date: LocalDate, val kg: Double)

@Entity(tableName = "height_log", indices = [Index(value = ["date"], unique = true)])
data class HeightLog(@PrimaryKey(autoGenerate = true) val id: Long = 0, val date: LocalDate, val cm: Double)
```

### 7.2 Relationships
`exercise (1) — (1) exercise_progression`; `exercise (1) — (N) exercise_log`; `money_transaction (1) — (0..N) savings_move`. All other logs are keyed by `date` and joined in the repository; `day_log` is a **cache** derived from logs (never edited by UI).

### 7.3 DAO sketches

```kotlin
@Dao interface ExerciseDao {
  @Query("SELECT * FROM exercise WHERE dayOfWeek = :dow ORDER BY orderInDay") fun forDay(dow: Int): Flow<List<Exercise>>
  @Query("SELECT * FROM exercise ORDER BY muscleGroup, dayOfWeek, orderInDay") fun all(): Flow<List<Exercise>>
  @Query("SELECT COUNT(*) FROM exercise") suspend fun count(): Int
  @Upsert suspend fun upsert(e: Exercise): Long
  @Upsert suspend fun upsertProgression(p: ExerciseProgression)
  @Query("SELECT * FROM exercise_progression WHERE exerciseId = :id") suspend fun progression(id: Long): ExerciseProgression?
}
@Dao interface ExerciseLogDao {
  @Query("SELECT * FROM exercise_log WHERE date = :d") fun forDate(d: LocalDate): Flow<List<ExerciseLog>>
  @Query("SELECT * FROM exercise_log WHERE date BETWEEN :from AND :to") suspend fun range(from: LocalDate, to: LocalDate): List<ExerciseLog>
  @Query("SELECT * FROM exercise_log WHERE exerciseId = :id AND done = 1 ORDER BY date DESC") suspend fun doneFor(id: Long): List<ExerciseLog>
  @Upsert suspend fun upsert(l: ExerciseLog)
}
@Dao interface DayLogDao {
  @Query("SELECT * FROM day_log WHERE date BETWEEN :from AND :to ORDER BY date") fun range(from: LocalDate, to: LocalDate): Flow<List<DayLog>>
  @Query("SELECT * FROM day_log WHERE date = :d") suspend fun get(d: LocalDate): DayLog?
  @Query("SELECT MAX(date) FROM day_log WHERE finalized = 1") suspend fun lastFinalized(): LocalDate?
  @Upsert suspend fun upsert(d: DayLog)
}
@Dao interface NutritionDao { /* get(date): Flow<NutritionLog?>, upsert */ }
@Dao interface MindDao { /* meditation, deepWork, reading, screenTime: forDate Flow, range, insert/upsert, sumMinutesBetween */ }
@Dao interface MoneyDao {
  @Query("SELECT * FROM money_transaction ORDER BY date DESC, id DESC") fun ledger(): Flow<List<MoneyTransaction>>
  @Query("SELECT COALESCE(SUM(amount),0) FROM money_transaction WHERE type='IN'") fun totalIn(): Flow<Long>
  @Query("SELECT COALESCE(SUM(amount),0) FROM money_transaction WHERE type='OUT'") fun totalOut(): Flow<Long>
  @Query("SELECT COALESCE(SUM(amount),0) FROM money_transaction WHERE type='OUT' AND category='fun' AND date BETWEEN :from AND :to") fun funBetween(from: LocalDate, to: LocalDate): Flow<Long>
  @Insert suspend fun insert(t: MoneyTransaction): Long
  @Update suspend fun update(t: MoneyTransaction)
  @Delete suspend fun delete(t: MoneyTransaction)
}
@Dao interface SavingsDao { /* deposits/withdrawals sums Flow, insert, delete */ }
@Dao interface SkillDao { /* totalHours Flow, recent(limit) Flow, insert, delete, recentTags */ }
@Dao interface BodyDao { /* weight/height ranges, upsert, delete, latestWeightDate, latestHeightDate */ }
```

### 7.4 Preferences (DataStore `accend_prefs`)

| Key | Type | Default |
|-----|------|---------|
| `onboarded` | Boolean | false |
| `screenTarget` (minutes) | Int | 180 |
| `readingTarget` | Int | 1 |
| `deepWorkDefault` (minutes) | Int | 30 |
| `savePercent` | Int | 40 |
| `notifText2DayMiss` | String | default A1 text |
| `dailyReminderTime` | String `HH:mm` | `20:00` |
| `weightGoal` (kg) | Float | 75 |
| `heightGoalCm` | Int | 183 (shown as 6'0") |
| `runwayTargetDate` | String ISO | `2027-06-01` |
| `notificationsEnabled` (master) | Boolean | true |
| `dailyReminderEnabled` | Boolean | true |
| `missAlertEnabled` | Boolean | true |

Internal (non-exported) keys: `lastEvaluatedDate` (ISO), `miss2Armed` (Boolean, default true), `lastMissAlertDate` (ISO).

`AppConfig` constants: `DAY_ROLLOVER_HOUR=0`, `STREAK_GRACE_DAYS=1`, `MISS_ALERT_HOUR=7`, `MISS_ALERT_MINUTE=30`, `MIND_WINDOW_DAYS=14`.

---

## 8. Seed Data

`SeedRepository.seedIfEmpty()` (idempotent, runs in a transaction, called on onboarding and defensively at app start): if `exercise` is empty insert all rows below plus a `exercise_progression` row for each (ladder JSON; VOLUME ladders contain one entry = the starting level). `levelSince = joinedDate`.

### 8.1 Weekly program (weekday: 1=Sun … 7=Sat)
Notation: `sets×target unit`. "Group" = radar axis.

**Day 1 (Sun) — Upper**
| # | Name | Group | Target |
|---|------|-------|--------|
| 1 | Knee Pushups | CHEST_PUSH | 1×20 reps |
| 2 | Neck Tilts | NECK | 1×50 reps |
| 3 | Bicep Curls | BICEPS | 1×20 reps |
| 4 | Bench Dips | TRICEPS | 1×30 reps |
| 5 | Australian Pushups | BACK_PULL | 1×10 reps |
| 6 | Shoulder Taps | SHOULDERS | 1×10 reps |
| 7 | Superman Hold | BACK_PULL | 3×20 s |

**Day 2 (Mon) — Core**
| # | Name | Group | Target |
|---|------|-------|--------|
| 1 | Crunches | CORE | 1×30 reps |
| 2 | Leg Raises | CORE | 1×20 reps |
| 3 | Plank | CORE | 3×20 s |
| 4 | Russian Twists | CORE | 1×30 reps |
| 5 | Wrist Curls | FOREARMS | 1×20 reps |
| 6 | Forearm Plank | CORE | 1×20 s |

**Day 3 (Tue) — Legs**
| # | Name | Group | Target |
|---|------|-------|--------|
| 1 | Squats | LEGS | 1×30 reps |
| 2 | Lunges | LEGS | 1×20 reps |
| 3 | Calf Raises | LEGS | 1×40 reps |
| 4 | Glute Bridges | LEGS | 1×20 reps |
| 5 | Wall Sit | LEGS | 3×20 s |
| 6 | Broad Jumps | LEGS | 1×5 reps |

**Day 4 (Wed) — Upper**
| # | Name | Group | Target |
|---|------|-------|--------|
| 1 | Incline Pushups | CHEST_PUSH | 1×20 reps |
| 2 | Diamond Knee Pushups | TRICEPS | 1×10 reps |
| 3 | Towel Rows | BACK_PULL | 1×10 reps |
| 4 | Lateral Raises | SHOULDERS | 1×20 reps |
| 5 | Neck Extensions | NECK | 1×30 reps |
| 6 | Reverse Snow Angels | BACK_PULL | 1×20 reps |

**Day 5 (Thu) — Core**
| # | Name | Group | Target |
|---|------|-------|--------|
| 1 | Bicycle Crunches | CORE | 1×30 reps |
| 2 | Lying Knee Raises | CORE | 1×20 reps |
| 3 | Side Plank | CORE | 2×20 s (each side) |
| 4 | Flutter Kicks | CORE | 1×30 reps |
| 5 | Hollow Hold | CORE | 3×15 s |
| 6 | Forearm Curls | FOREARMS | 1×20 reps |

**Day 6 (Fri) — Legs**
| # | Name | Group | Target |
|---|------|-------|--------|
| 1 | Bulgarian Split Squats | LEGS | 1×20 reps |
| 2 | Jump Squats | LEGS | 1×15 reps |
| 3 | Single-Leg Glute Bridge | LEGS | 1×20 reps |
| 4 | Single-Leg Calf Raises | LEGS | 1×30 reps |
| 5 | Walking Lunges | LEGS | 1×20 reps |
| 6 | Sprint 100m | LEGS | 2×100 m |

**Day 7 (Sat) — Rest / Active Recovery** (group `RECOVERY`, excluded from the radar)
| # | Name | Target |
|---|------|--------|
| 1 | Walk | 1×20 min |
| 2 | Full-body Stretch | 1×10 min |
| 3 | Neck Mobility | no target (`NONE`) |
| — | Sleep 9h | not an Exercise row; mirrored from `NutritionLog.sleep9h` (A5) |

Display target text: reps → "20 reps"; sets>1 → "3×20 s"; meters → "2×100 m"; minutes → "20 min"; NONE → "".

### 8.2 Progression ladders (compound movements; format `Level name target`)
Level up moves to the next entry; `sets`/`target` come from the entry (sets inherited from the exercise).

- **Knee Pushups (Sun):** Knee Pushups 20 → Incline Pushups 15 → Full Pushups 10 → Decline Pushups 8 → Diamond Pushups 8 → Archer Pushups 6 → One-Arm Prep 5
- **Incline Pushups (Wed):** Incline Pushups 20 → Full Pushups 10 → Decline Pushups 8 → Archer Pushups 6 → One-Arm Prep 5
- **Diamond Knee Pushups:** Diamond Knee Pushups 10 → Diamond Incline Pushups 10 → Diamond Pushups 8 → Decline Diamond Pushups 6
- **Australian Pushups:** Australian (high bar) 10 → Australian (mid bar) 10 → Australian (low bar) 8 → Feet-Elevated Australian 8 → Archer Australian 6
- **Bench Dips:** Bench Dips (bent knees) 30 → Bench Dips (legs straight) 20 → Feet-Elevated Bench Dips 15 → Single-Leg Bench Dips 8
- **Towel Rows:** Towel Rows 10 → Towel Rows (feet forward) 10 → Towel Rows (slow 3 s) 8 → Single-Arm Towel Row 6
- **Squats:** Squats 30 → Tempo Squats 20 → Pause Squats 20 → Shrimp Squat Prep 8 → Pistol Prep 6
- **Lunges:** Lunges 20 → Reverse Lunges (pause) 15 → Deficit Lunges 15 → Jumping Lunges 10
- **Bulgarian Split Squats:** Bulgarian Split Squats 20 → Slow Bulgarian Split Squats 15 → Deep Bulgarian Split Squats 12 → Shrimp Squat 8
- **Glute Bridges:** Glute Bridges 20 → Single-Leg Glute Bridge 12 → Shoulder-Elevated Hip Thrust 12 → Single-Leg Hip Thrust 10
- **Single-Leg Glute Bridge:** Single-Leg Glute Bridge 20 → Shoulder-Elevated Single-Leg Hip Thrust 12
- **Calf Raises:** Calf Raises 40 → Single-Leg Calf Raises 20 → Deficit Single-Leg Calf Raises 15
- **Single-Leg Calf Raises:** Single-Leg Calf Raises 30 → Deficit Single-Leg Calf Raises 20

### 8.3 `VOLUME` ladders (every other exercise)
One ladder entry (the start). `Level up` increases `targetReps` by `step`, capped at `2 × starting target`; `step = 5` if start ≥ 20 (reps/seconds-per-set ≥ 20), `2` if start < 20 and unit is REPS, `5` for SECONDS, `1` for MINUTES/METERS-start (METERS step = 20). `Level down` mirrors. "Level i of N" for VOLUME exercises is displayed as "Target {t}" instead.

---

## 9. Shared Systems

### 9.1 Day math, evaluation and missed days
- `today = LocalDate.now(ZoneId.systemDefault())`. All "dates" are local dates at time of entry; they are **never re-interpreted** if the time zone later changes.
- `Day n = DAYS.between(joinedDate, today) + 1` (keeps counting past 365; at n > 365 Home shows a one-time "Cycle complete" card, app keeps working).
- Program day = `today.dayOfWeek` mapped to 1=Sun … 7=Sat.
- **`DayEvaluator.catchUp()`** (runs on app start/resume, after any import/reset, and inside `MissCheckWorker`):
  1. `start = (lastEvaluatedDate ?: joinedDate − 1) + 1`, `end = today − 1`.
  2. For each date in `[start, end]` (never beyond `today−1`; if `today < lastEvaluatedDate` do nothing): recompute completion, upsert `DayLog(finalized=true, missedFlag = completion == 0)`.
  3. Always recompute today's `DayLog` (finalized=false, missedFlag=false).
  4. If any evaluated day has completion > 0 → `miss2Armed = true`.
  5. Persist `lastEvaluatedDate = today − 1`.
- Any write to a log row triggers `recomputeDay(date)` (idempotent upsert).
- Days before `joinedDate` are ignored everywhere.

### 9.2 Completion %
`total = exerciseRowsForWeekday + 5 (nutrition) + 4 (mind)`; `done = ticked exercise rows + nutrition toggles true + [meditationDone, screenTimeLogged, deepWorkDone, readingDone]`; `completionPercent = round(100 × done / total)`. Saturday: exercise rows = 3 (Walk, Stretch, Neck Mobility); the mirrored Sleep 9h is the nutrition toggle (counted once). Total per weekday: Sun 7+9=16, Mon–Fri 6+9=15, Sat 3+9=12.

### 9.3 Streaks (Physical / Meditation / Deep-work)
Per-day booleans: `physicalDone` = all exercise rows ticked; `meditationDone` = ≥ 1 meditation log; `deepWorkDone` = ≥ 1 completed deep-work log.
```
streak(habit):
  count = if (done(today)) 1 else 0
  missesInRow = 0
  for d in (today-1 downTo joinedDate):
     if done(d): count++; missesInRow = 0
     else: missesInRow++; if (missesInRow > STREAK_GRACE_DAYS) break
  return count
```
Today not yet done is **not** a miss. `best` = same scan across the whole history, keep the max. No reset animation, no shaming copy.

### 9.4 30-day grid
- Cell color by `completionPercent` p for finalized/today dates: `p == 0` (past) → fill `surfaceCard` + 1dp `border`; `p in 1..100` → `lerp(#7B1FA2, #C2185B, p/100)` with alpha `0.35 + 0.65·p/100`; `p == 100` adds a faint glow (drawBehind). Future dates → border only. Today → extra 1dp `accentPrimary` outline.
- Home: last 30 days ending today (oldest first), 6 cols × 5 rows, column-major. Analysis: journey-anchored 30-day blocks (§6.3). Before `joinedDate` (when Home window starts earlier than day 1) render empty cells with no border.

### 9.5 Mind Score (0–100)
Per-day `mind(d) = 100 × (0.30·med + 0.30·deep + 0.20·read + 0.20·screen)`, each term in 0..1:
- `med = min(1, meditationMinutes(d) / ladderMinutes(week(d)))`
- `deep = min(1, completedDeepMinutes(d) / deepWorkDefault)`
- `read = min(1, pages(d) / readingTarget)`
- `screen = 0` if not logged; `1` if `minutes ≤ screenTarget`; else `max(0, 1 − (minutes − screenTarget)/screenTarget)`

`MindScore(day) = round(mean of mind(x) for x in [day−13 … day], x ≥ joinedDate)` (`MIND_WINDOW_DAYS = 14`). Today's value is included, so the ring rises live as habits are completed and moves smoothly across midnight. Trend chart = `MindScore` for each of the last 30 days.

### 9.6 Notifications (WorkManager; no other notifications exist)
Channels (created at app start): `daily_reminder` (IMPORTANCE_DEFAULT, name "Daily reminder"), `miss_alert` (IMPORTANCE_HIGH, name "Missed days"). Permission `POST_NOTIFICATIONS` (API 33+); if denied, workers still run but skip posting. Tap → `accend://daily`.

- **`DailyReminderWorker`** (`PeriodicWorkRequest` 24h, unique name `daily_reminder`, initial delay to next `dailyReminderTime`, `ExistingPeriodicWorkPolicy.UPDATE`): if master + daily reminder enabled and today's pending items > 0, post: **"Day {n} · {pending} left"** (`pending = total − done` for today). Skip when pending is 0.
- **`MissCheckWorker`** (`PeriodicWorkRequest` 24h, unique name `miss_check`, initial delay to next 07:30): runs `catchUp()`; if `missAlertEnabled` && master && `miss2Armed` && `DayLog(today−1).missed` && `DayLog(today−2).missed` (both ≥ joinedDate) → post high-priority notification with text = `notifText2DayMiss` (fallback default if blank), then `miss2Armed = false`, `lastMissAlertDate = today`. Fires **once per miss run**; a third consecutive miss does not re-fire. Completing any day (any evaluated day with completion > 0, including today with ≥ 1 item done) sets `miss2Armed = true` again. One missed day → nothing.
- **Scheduling:** `NotificationScheduler.sync()` is called on app start, after settings changes (time/toggles), after import/reset. When disabled, cancel the unique work. A `BOOT_COMPLETED` `BroadcastReceiver` calls `sync()` (WorkManager persists across reboot, this is a safety net). Delivery is approximate (WorkManager, ±15 min) — acceptable; do **not** use exact alarms.
- If a worker runs while its time has passed (device was off), it still evaluates; the miss alert may arrive late the same day.

### 9.7 Meditation ladder (journey week `w = (dayNumber−1)/7 + 1`)
| Week | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10+ |
|------|---|---|---|---|---|---|---|---|---|-----|
| Minutes | 1 | 2 | 3 | 5 | 7 | 10 | 12 | 15 | 17 | 20 |

### 9.8 Money math
`balance = ΣIN − ΣOUT − ΣdepositMoves + ΣwithdrawMoves`; `jar = ΣdepositMoves − ΣwithdrawMoves`. Jar deposit cannot exceed current balance; withdraw cannot exceed jar. Deleting an IN transaction also deletes its linked deposit moves only if confirmed in a dialog; otherwise block deletion with a message if it would make the balance negative. Weekly = Monday–Sunday of the current date. `runwayDays = max(0, DAYS.between(today, runwayTargetDate))`.

### 9.9 Radar volume & progression suggestions
- **Volume per completed row** (stored in `volumeSnapshot` at tick time): `sets × targetReps × unitWeight`, `unitWeight`: REPS 1.0, SECONDS 0.5, METERS 0.1, MINUTES 3.0, NONE 0 (excluded). Un-ticking sets `done=false` and `volumeSnapshot=0`.
- **Radar axis value** = Σ `volumeSnapshot` for `done` logs in the last 30 days whose exercise group matches. Plotted radius = `value / max(axisValues)` (shape shows balance); legend shows raw numbers. All zeros → empty state "Complete workouts to see your balance".
- **Level-up suggestion:** occurrences = dates ≥ `max(joinedDate, levelSince)` and ≤ today with `weekday == exercise.dayOfWeek` (today counts only if done). Walking occurrences newest→oldest, `consecutive` = number of `done` rows before the first not-done occurrence (today not done is skipped). Suggest when `consecutive ≥ 3` and (`suggestionSnoozedOn == null` or a done occurrence exists after it).

### 9.10 Export JSON format
```json
{ "app": "ACCEND", "schemaVersion": 1, "exportedAt": "2026-11-21T10:00:00Z",
  "user": {...}, "pfpThumbBase64": "optional 256px jpeg",
  "preferences": {...},
  "exercises": [...], "progressions": [...],
  "logs": { "exercise": [...], "nutrition": [...], "meditation": [...], "screenTime": [...],
            "deepWork": [...], "reading": [...], "day": [...], "weight": [...], "height": [...] },
  "money": { "transactions": [...], "savingsMoves": [...] }, "skills": [...] }
```
Uses `kotlinx.serialization`; dates as ISO strings; import re-creates ids preserving relations (map old→new ids).

---

## 10. GitHub Actions Workflow (`.github/workflows/build.yml`)

Behavior: triggers on push to `main` and manual dispatch; builds a **signed release APK** if all four secrets exist and publishes `ACCEND v{versionName}` (tag `v{versionName}`); otherwise builds a **debug APK** and uploads it only as an Actions artifact (no release). The same-version release is replaced on every push (tag moves to the newest commit); bump `APP_VERSION_NAME` in `gradle.properties` for a new version.

```yaml
name: Build ACCEND

on:
  push:
    branches: [main]
  workflow_dispatch:
    inputs:
      publish_release:
        description: "Publish a GitHub Release (only when signing secrets are set)"
        type: boolean
        default: true

permissions:
  contents: write

concurrency:
  group: build-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle (cache)
        uses: gradle/actions/setup-gradle@v4

      - name: Read version
        id: ver
        run: |
          NAME=$(grep -E '^APP_VERSION_NAME=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')
          if [ -z "$NAME" ]; then echo "APP_VERSION_NAME missing in gradle.properties" >&2; exit 1; fi
          echo "name=$NAME" >> "$GITHUB_OUTPUT"

      - name: Detect signing secrets
        id: sign
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          if [ -n "$KEYSTORE_BASE64" ] && [ -n "$KEYSTORE_PASSWORD" ] && [ -n "$KEY_ALIAS" ] && [ -n "$KEY_PASSWORD" ]; then
            echo "$KEYSTORE_BASE64" | base64 -d > "$RUNNER_TEMP/release.keystore"
            echo "signed=true" >> "$GITHUB_OUTPUT"
          else
            echo "signed=false" >> "$GITHUB_OUTPUT"
            echo "::warning::Keystore secrets missing - building an unsigned-for-release DEBUG APK and uploading it as an Actions artifact only."
          fi

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      - name: Build signed release APK
        if: steps.sign.outputs.signed == 'true'
        env:
          KEYSTORE_FILE: ${{ runner.temp }}/release.keystore
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          BUILD_NUMBER: ${{ github.run_number }}
        run: ./gradlew --no-daemon assembleRelease

      - name: Build debug APK (fallback)
        if: steps.sign.outputs.signed != 'true'
        env:
          BUILD_NUMBER: ${{ github.run_number }}
        run: ./gradlew --no-daemon assembleDebug

      - name: Collect APK
        id: apk
        run: |
          mkdir -p dist
          VER="${{ steps.ver.outputs.name }}"
          if [ "${{ steps.sign.outputs.signed }}" = "true" ]; then
            cp app/build/outputs/apk/release/app-release.apk "dist/ACCEND-v${VER}.apk"
            echo "file=dist/ACCEND-v${VER}.apk" >> "$GITHUB_OUTPUT"
          else
            cp app/build/outputs/apk/debug/app-debug.apk "dist/ACCEND-v${VER}-debug.apk"
            echo "file=dist/ACCEND-v${VER}-debug.apk" >> "$GITHUB_OUTPUT"
          fi

      - name: Upload APK as workflow artifact
        uses: actions/upload-artifact@v4
        with:
          name: ACCEND-apk
          path: dist/*.apk
          if-no-files-found: error
          retention-days: 30

      - name: Publish GitHub Release
        if: steps.sign.outputs.signed == 'true' && (github.event_name == 'push' || inputs.publish_release == true)
        env:
          GH_TOKEN: ${{ github.token }}
          TAG: v${{ steps.ver.outputs.name }}
          TITLE: ACCEND v${{ steps.ver.outputs.name }}
          APK: ${{ steps.apk.outputs.file }}
        run: |
          gh release delete "$TAG" --cleanup-tag --yes || true
          gh release create "$TAG" "$APK" \
            --target "$GITHUB_SHA" \
            --title "$TITLE" \
            --notes "Automated build from commit ${GITHUB_SHA::7}. Install the APK on Android (allow installs from this source)."
```

Gradle requirements the workflow relies on (`app/build.gradle.kts`):
```kotlin
val appVersionName = providers.gradleProperty("APP_VERSION_NAME").get()
android {
    namespace = "com.accend.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.accend.app"            // keep existing applicationId if the old repo had one (A12)
        minSdk = 26; targetSdk = 35
        versionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 1
        versionName = appVersionName
    }
    signingConfigs {
        val ks = System.getenv("KEYSTORE_FILE")
        if (ks != null && File(ks).exists()) {
            create("release") {
                storeFile = file(ks)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true; isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
    }
    buildFeatures { compose = true; buildConfig = true }
}
```
`gradle.properties` must contain `APP_VERSION_NAME=2.0.0`, `org.gradle.jvmargs=-Xmx3g`, `android.useAndroidX=true`, `kotlin.code.style=official`.

---

## 11. Execution Instructions & File Structure

### 11.1 Steps OpenCode must perform
1. Clone/open `github.com/elias-veyne/ACCEND-365`. Record the existing `applicationId`, version info and any signing/config notes.
2. **Read the existing splash screen implementation.** Copy its files to `docs/legacy-splash-reference/` (`*.kt.txt`) for reference. Note animation timings and structure.
3. Delete all other source files, resources, layouts, drawables and old Gradle config (keep `.git`, `LICENSE` if any, and `docs/legacy-splash-reference/`).
4. Create the new project per §2 (wrapper 8.11.1, version catalog, AGP/Kotlin/KSP versions) and rebuild per this PRD. Port the splash (§4.2) rebranded gold → red-purple.
5. Rename the app display name from "ACCEND-365" to **"ACCEND"** everywhere (`strings.xml` `app_name`, manifest label, splash, README, repo description text where in-repo).
6. Add `.github/workflows/build.yml` (§10), `.gitignore` (include `*.keystore`, `*.jks`, `local.properties`, `build/`, `.gradle/`), `README.md` (what/how/post-build steps from §14), `docs/PRD.md` (a copy of this file).
7. Build verification: if an Android SDK is available run `./gradlew assembleDebug` and fix errors; always commit Room schemas (`app/schemas/`).
8. Commit (no history rewrite, no force-push) with message `feat: rebuild ACCEND v2 (red-purple)`, push to `main`.
9. Watch the workflow (`gh run watch` / `gh run view --log-failed` if `gh` is authenticated). If it fails, fix and push again (max 5 iterations). The expected first run has **no secrets** → debug APK artifact; that is a pass.
10. Report: workflow run URL, artifact/release URL, and any deviations from this PRD.

### 11.2 Folder structure
```
ACCEND-365/
├─ .github/workflows/build.yml
├─ gradle/libs.versions.toml, gradle/wrapper/{gradle-wrapper.jar,gradle-wrapper.properties}
├─ gradlew, gradlew.bat, settings.gradle.kts, build.gradle.kts, gradle.properties
├─ docs/{PRD.md, legacy-splash-reference/*.kt.txt}
├─ README.md, .gitignore
└─ app/
   ├─ build.gradle.kts, proguard-rules.pro, schemas/
   └─ src/main/
      ├─ AndroidManifest.xml            (perms: POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, VIBRATE; NO INTERNET; allowBackup=false; FileProvider; boot receiver)
      ├─ res/{font/, values/{strings,colors,themes}.xml, xml/file_paths.xml, mipmap-*/ (adaptive icon: gradient "A" on #08060A)}
      └─ java/com/accend/app/
         ├─ AccendApp.kt, MainActivity.kt, AppContainer.kt, AppConfig.kt
         ├─ data/
         │  ├─ db/{AccendDatabase.kt, Converters.kt, entities/*.kt, dao/*.kt}
         │  ├─ prefs/{PrefsRepository.kt}
         │  ├─ repo/{ProgramRepository, LogRepository, MoneyRepository, BodyRepository, UserRepository, SeedRepository}.kt
         │  ├─ seed/SeedData.kt
         │  └─ export/{ExportModels.kt, ExportImportManager.kt}
         ├─ domain/{DayEvaluator.kt, CompletionCalculator.kt, StreakCalculator.kt, MindScoreCalculator.kt, VolumeCalculator.kt, ProgressionRules.kt, MeditationLadder.kt}
         ├─ work/{NotificationScheduler.kt, DailyReminderWorker.kt, MissCheckWorker.kt, BootReceiver.kt, Notifications.kt}
         ├─ ui/
         │  ├─ theme/{Color.kt, Type.kt, Theme.kt, Shapes.kt}
         │  ├─ components/*.kt  (§3.6)
         │  ├─ charts/{LineChart.kt, BarChart.kt, RadarChart.kt, ContributionGrid.kt, ProgressRing.kt}
         │  ├─ nav/{AccendNavHost.kt, Routes.kt, BottomBar.kt}
         │  ├─ splash/SplashScreen.kt
         │  ├─ onboarding/{OnboardingScreen.kt, OnboardingViewModel.kt}
         │  ├─ home/*  ├─ daily/*  ├─ analysis/*  ├─ money/*
         │  ├─ settings/{SettingsScreen, ProgressionScreen, BodyLogsScreen, DataScreen}.kt (+ViewModels)
         │  └─ timers/{MeditationScreen.kt, DeepWorkScreen.kt, TimerViewModel.kt}
         └─ util/{Haptics.kt, DateExt.kt, ImageUtils.kt, Format.kt}
```

---

## 12. Edge Cases

| Case | Required behavior |
|------|-------------------|
| First launch / no DB | `AppContainer` creates DB lazily; onboarding gates the rest; seed is idempotent. |
| Onboarding killed mid-way | `onboarded` is written last, in the same flow as the User insert; if absent, onboarding shows again. |
| Process death during a timer | Timer state (`startElapsed`, `targetMinutes`, mode) in `SavedStateHandle`; on restore compute remaining from timestamps. If the process was killed and nothing restorable → session lost, nothing logged. |
| Missed days | Per §9.1/§9.6: 1 missed → nothing; 2 consecutive → one alert; re-armed by any day with activity. |
| App not opened for many days | `catchUp()` backfills every date in order; DayLogs finalized as missed where 0; alert logic evaluated by worker/next open based on the last two days only. |
| Midnight rollover while app is open | Ticker flow detects date change → refresh "today", recompute, UI re-renders; open timers continue (their log date = date at completion). A session finishing after midnight logs to the new date. |
| Time zone change / DST | Dates are local-date strings; no conversion of stored rows. Forward jumps leave skipped dates to be evaluated normally; backward jumps (`today < lastEvaluatedDate`) → no evaluation and no changes; duplicate-date writes are upserts. |
| Clock manually changed | Same as above; never write DayLogs for dates > today. |
| Keystore secrets missing | Workflow builds debug APK, uploads artifact, warns, still succeeds (no release). |
| Wrong keystore password / alias | Release build fails with Gradle error; owner fixes secrets (documented in §14). |
| Empty states | Every list/chart has an `EmptyState` (§6); no screen shows a blank area or crashes with zero rows. |
| Notification permission denied | App works; Settings shows a muted banner "Notifications are blocked in system settings" with an `Open settings` button. |
| Battery optimization | Not requested. README notes reminders may be delayed on aggressive OEMs. |
| Large text / small screens (360dp) | Layouts use weights and wrap; no clipping at font scale 1.3. |
| Import malformed/newer | Rejected without changes (§6.9). |
| PFP file missing | Fall back to initial-letter avatar. |
| Delete exercise rows | Not user-facing; Reset re-seeds. |
| Reading target change mid-day | Done state recalculated live from pages vs target. |
| Decimal/overflow inputs | Money integers up to 99,999,999; hours 0–24 per skill entry; screen time ≤ 24h. |

---

## 13. Acceptance Criteria (OpenCode self-verifies before marking done)

**Build & repo**
- [ ] Old source removed; new project builds with `./gradlew assembleDebug` (or CI) with zero errors.
- [ ] `docs/legacy-splash-reference/` exists; splash ported and rebranded red-purple.
- [ ] App name is "ACCEND" in manifest/strings/splash/README; no "365" in the UI.
- [ ] Manifest has no `INTERNET` permission; `allowBackup=false`.
- [ ] `build.yml` present exactly per §10; first CI run is green; APK downloadable (artifact or Release).
- [ ] No secrets or keystores committed; `.gitignore` covers them.

**Flow & navigation**
- [ ] Splash holds 1.2s on `#08060A` with purple glow, then Onboarding (first launch) or Home (returning).
- [ ] Onboarding requires name, optional PFP, `Begin` writes User row, seeds data, lands on Home.
- [ ] 5 bottom tabs with correct icons/labels, active = purple accent; sub-screens hide the bar.

**Home / Daily / Analysis**
- [ ] Home shows "Day {n} · {name}", Mind Score ring, today's summary, 30-day grid (6×5), 3 streak chips; cards navigate correctly.
- [ ] Daily Work shows weekday-correct exercises; Saturday shows recovery list with mirrored Sleep 9h counted once.
- [ ] Nutrition (5), Mind (meditation, screen time, deep work, reading) behave per §6.2; haptic on every tick.
- [ ] Meditation timer follows the week ladder, logs restlessness, auto-ticks; Deep Work presets 15/30/45/60 log and auto-tick; partial sessions do not tick.
- [ ] Completion % matches §9.2 totals (Sun 16, Mon–Fri 15, Sat 12) and the grid updates live.
- [ ] Analysis has all 9 cards, pager over 30-day blocks, Day detail sheet, correct radar axes, target lines, empty states.

**Notifications & missed days**
- [ ] Daily reminder posts "Day {n} · {pending} left" at the chosen time and only when pending > 0.
- [ ] 2 consecutive zero-completion days → exactly one high-priority notification with the Settings text (default A1); one missed day → nothing; completing any day re-arms.
- [ ] Reschedules after reboot and after settings changes; only two notification types exist.

**Money**
- [ ] NPR balance, ledger grouped by date, add/edit/delete with undo, In sources / Out categories as specified.
- [ ] Save-jar suggestion = `savePercent`% after each IN; jar totals correct; Leak card = this week's fun spend; impulse modal on Fun, non-blocking.
- [ ] Skill hours add/total; Runway shows days to the configured date and hours banked. No investing/crypto/stocks UI.

**Settings & data**
- [ ] Profile edit (name, PFP, remove), all notification/target settings persist across restarts.
- [ ] Progression screen groups by muscle, manual Level up/down works, suggestion after 3 consecutive sessions, snooze works.
- [ ] Export JSON via share sheet; Import round-trips an export with identical data; invalid file rejected cleanly.
- [ ] Reset journey requires two confirmations (second = typing `RESET`), keeps settings/profile, re-seeds, Day 1 again.

**Quality**
- [ ] No emoji in UI strings; icons are Material Symbols outlined; Inter font (or documented fallback).
- [ ] Cards 16dp radius, modals 24dp, grid cells 12dp/3dp radius, CTA glow present.
- [ ] No crashes on first launch, rotation, process death mid-timer, empty DB, 360dp width, font scale 1.3.
- [ ] README documents §14.

---

## 14. Post-Build Setup (for the owner)

### 14.1 Create a signing keystore (once; back it up — losing it means you can never update the app in place)
```bash
keytool -genkeypair -v -keystore accend-release.keystore -alias accend \
  -keyalg RSA -keysize 2048 -validity 10000
```
Remember the store password, key alias (`accend`) and key password. **Never commit this file.**

### 14.2 (a) Add the keystore secrets to GitHub
1. Convert to base64 (single line):
   - Linux: `base64 -w0 accend-release.keystore`
   - macOS: `base64 -i accend-release.keystore | tr -d '\n'`
   - Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("accend-release.keystore"))`
2. On GitHub open the repo → **Settings → Secrets and variables → Actions → New repository secret**, and create four secrets:
   - `KEYSTORE_BASE64` = the base64 text
   - `KEYSTORE_PASSWORD` = store password
   - `KEY_ALIAS` = `accend` (or your alias)
   - `KEY_PASSWORD` = key password
3. **Settings → Actions → General → Workflow permissions** → select **Read and write permissions** → Save (needed to create releases).

### 14.3 (b) Trigger the build
- Automatic: any push to `main`.
- Manual: repo → **Actions → "Build ACCEND" → Run workflow** (branch `main`, leave "Publish a GitHub Release" ticked).
- Wait for the green check (≈ 5–10 min). Without secrets you will get a **debug** APK in the run's Artifacts only; with secrets, a signed release.

### 14.4 (c) Download and install the APK
1. Repo → **Releases** → open **ACCEND v{versionName}** → under *Assets* download `ACCEND-v{versionName}.apk`. (If no release exists yet: Actions → the latest run → *Artifacts* → `ACCEND-apk`, unzip.)
2. Transfer to the phone (or download on it directly), open the file, allow **Install unknown apps** for the app you used when prompted, then install.
3. First launch: allow notifications when asked. Done.

### 14.5 Releasing a new version
Edit `APP_VERSION_NAME` in `gradle.properties` (e.g. `2.0.1`), push to `main`; the workflow publishes a new `ACCEND v2.0.1` release. Re-pushing without changing the version replaces the existing release's APK.
