# Tracker

An offline-first Android app for tracking your **workouts**, any **stats** you measure (weight, calories, body fat, waist, sleep...) and **progress photos** in one place. Built with Kotlin, Jetpack Compose and Material Design 3.

All data stays on your device, and you don't need an account. When you want your numbers elsewhere, export any screen's analytics, or everything at once, as a PDF or Excel file. To move to a new phone, back up everything to one file and restore it there. Health Connect sync is optional and reads data that is already on the phone. The only feature that uses the internet is the optional exercise catalog, which sends just your search to [wger.de](https://wger.de).

🌐 **Website:** [aboud-khalaf.github.io/Android-Tracker](https://aboud-khalaf.github.io/Android-Tracker/)

## ✨ Features

### 🗓️ Programs and weekly schedule
- Create workout programs from a template — **Full Body**, **Upper/Lower**, **Push/Pull/Legs** — or start from scratch
- Pick your training days; each day gets its own exercise list, named after the split (e.g. Push, Pull, Legs, Push…)
- Add or remove training days, move a day to another weekday, or add days without a fixed weekday
- Keep several programs and choose which one is active; Home shows today's workout from it, or the next one on a rest day
- Templates are plain data, so new splits need no code

### 🏋️ Workouts
- Add exercises to each training day, or browse a free online exercise catalog ([wger](https://wger.de)): search by name, see each exercise's picture, muscles, equipment and instructions, and add it in one tap
- **Replace** an exercise (e.g. Bench Press → Dumbbell Press) without rebuilding the day: it keeps its place and targets
- **Drag and drop** exercises into a new order with the handle or a long press; the order is saved and restored (Move up/down stay available in the menu and to TalkBack)
- **Targets per exercise**: sets, reps and weight, or hold time, edited on their own without starting a workout. They prefill each new workout; what you actually lift is recorded separately and shown next to them as "Recorded last workout"
- Log weight-and-reps sets during an active workout
- Mark a set as a **warm-up**, **drop set** or **to failure**; add warm-ups before the working sets with one tap. Warm-ups are numbered apart and left out of progress, records, volume and "last time" comparisons
- Optionally rate each weighted set's effort on the **RPE** scale (6–10), shown next to the set once it's done
- **Notes**: a note on the whole workout (shown in history) and on each exercise (shown in its progress history and, next time, under the exercise as "Last time")
- **Supersets**: pair exercises on a training day ("Superset with next"); during the workout, completing a set goes straight to the next exercise of the superset, and the rest comes after each round
- Built-in hold timer for timed sets (planks, holds, etc.)
- Workout history grouped by month, with a filter by training day

### ⏱️ Workouts that survive leaving the app
- While a workout is in progress, an ongoing notification shows it with a running clock; tap it to jump back in
- The clock counts from the stored start time, so it stays right with the screen locked, in another app, or after Android closes the app
- Every set is saved the moment you log it. Reopen the app after it was closed (or the phone restarted) and the workout comes back on the exercise you were on, with your typed weight and reps and a running hold timer intact
- Only one workout can run at a time, so there are never duplicate timers

### 📈 Progress
- **Sets this week by muscle**: working sets per muscle group (chest, back, shoulders, arms, legs, core) this week next to last week. Exercises added from the catalog get their group automatically; set or change it on any exercise
- A details screen for each exercise, with a progress chart and full history
- Switch between progress metrics (top set weight, **estimated one-rep max**, reps, volume) and time ranges
- Export an exercise's progress for the selected metric and range as a PDF or Excel file

### 📊 Stats
- Track any number once a day: create a stat with a name, unit, decimal places, an optional target, an icon and a colour
- Start from suggestions such as **Weight**, **Body fat**, **Waist**, **Calories**, **Protein**, **Steps**, **Water**, **Sleep** and **Resting heart rate**, or make your own
- Each stat shows its latest value, progress toward its target (going up or down follows from your first entry), a chart with a period selector and the full history by month; edit or delete any day
- Export a stat's selected period as a PDF or Excel file
- Optional daily reminder (9 PM by default, at a time you choose) when a stat isn't logged that day
- Updating from an older version turns the old weight and daily log (calories, protein, steps) into stats, targets included

### 📸 Progress photos
- Take a front, side and back photo from the Stats tab, one pose after another; every session is kept, even several on the same day
- A human outline with head, feet, shoulder, waist, hip and knee marks shows where and how to stand, so every photo is framed the same way
- Optionally note measurements with each session (weight, body fat, waist, chest, arms, thighs and more, or your own); the ones you measured last time are ready to fill in
- Your last photo of each pose shows faintly over the camera, so you can line up in the same spot
- A 3 or 10 second self-timer gives you time to step back from a propped-up phone; switch between the front and back camera
- See every session's photos and measurements with their change, and compare your first and latest photo of a pose side by side
- Photos stay inside the app: they aren't added to your gallery or included in cloud backups

### ⚙️ Settings
- **Health Connect** (Android 9+, optional): read the last 30 days of steps, weight and sleep into your Steps, Weight and Sleep stats when the app opens or on "Sync now". Nothing is written to Health Connect or sent anywhere
- Light, dark or system theme
- **Wallpaper colors** (Android 12+): color the app from your wallpaper instead of the blue brand theme
- Turn the daily stats reminder on or off and choose when it arrives
- Export all data (exercises, every workout set and every stat) as a PDF or Excel file
- **Back up** everything, progress photos included, to one `.zip` file, and **restore** it on this or another phone (it replaces the data there; backups from older versions are migrated)
- Delete all data, progress photos included, from the device

### 📲 On your home screen
- **App shortcuts**: long-press the app icon to start today's workout (or jump back into the one in progress), log a stat, or take progress photos
- **Up next widget**: your next training day (or the workout in progress) with a Start/Resume button, plus this week's workout count, in the app's colours and light/dark theme

### 🎨 Design
- **Onboarding** on first launch: a short welcome, an optional first program from a split template, and the stats you want to track (every step can be skipped)
- Material Design 3 with a blue brand theme, plus light and dark modes
- Animated splash screen, screen transitions and list animations
- Layouts adapt to larger screens
- **Arabic** translation with a right-to-left layout; numbers typed with Arabic digits (٠–٩) are understood everywhere

## 📦 Download

Get the latest APK from the [website](https://aboud-khalaf.github.io/Android-Tracker/) or the [Releases](https://github.com/Aboud-KHalaf/Android-Tracker/releases) page and open it on your phone. Requires **Android 7.0 (API 24)** or newer.

## 🛠️ Tech Stack

| Area | Library |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, `StateFlow`, repositories |
| Navigation | Navigation Compose with type-safe routes (kotlinx.serialization) |
| Networking | `HttpURLConnection` + kotlinx.serialization for the [wger REST API](https://wger.de/api/v2/) (no API key) |
| Camera | CameraX (`camera-view` with `LifecycleCameraController`) |
| Images | Coil |
| Storage | Room (with migrations and exported schemas) |
| Widget | Jetpack Glance |
| Background | AlarmManager for the daily stats reminder; an ongoing notification for the workout in progress |
| Dependency injection | Manual (`AppContainer`) |
| Testing | JUnit, Robolectric, Compose UI tests |
| Code checks | Android lint and detekt, run in CI (`./gradlew lintDebug detekt`) |

## 🏗️ Architecture

The app follows **MVVM** with unidirectional data flow:

```
UI (Compose screens)  →  ViewModels (StateFlow UI state)  →  Repositories  →  Room database
                                                                            ↘  wger API (exercise catalog)
                                                                            ↘  app files (progress photos)
```

```
app/src/main/java/com/example/tracker/
├── data/        # Room entities, DAOs, database and repository implementations
│   ├── export/  # PDF and Excel (.xlsx) writers for exported reports
│   └── remote/  # HTTP client and the wger exercise catalog API
├── domain/      # Domain models, repository interfaces and business logic
│                #   (progress, stats, photos, programs, plans, workout sessions,
│                #    catalog, reminders, export)
├── di/          # AppContainer: manual dependency wiring
├── reminder/    # Daily stats reminder: alarms, notification, reboot receiver
├── workout/     # Workout-in-progress notification and its reboot/update receiver
└── ui/          # Compose screens and ViewModels, one package per feature
    ├── home/ program/ plan/ catalog/ workout/ history/ exercise/ exercises/
    ├── stats/ photos/ settings/ splash/
    ├── navigation/ common/
    └── theme/   # Colors, typography, shapes and spacing tokens
```

### Programs and training days

A **program** has a weekly schedule of **training days**. Each training day is stored as a plan (`workout_plans`) with its own ordered exercise list and an optional weekday, so workouts, history and progress keep working exactly as before. Updating from an older version moves existing plans into one active program called "My program", unscheduled until you pick weekdays.

Each plan exercise has a **default configuration** (target sets, reps, weight or hold time). Starting a workout copies it into the workout's sets; anything left blank repeats last time. Logged sets belong to the workout, so editing targets never changes history.

### Workout sessions in the background

A workout in progress needs no code running in the background: every set is written to Room when it is logged, and the clock is worked out from the stored `started_at` instead of a running timer. So the app uses an **ongoing notification with a system chronometer** rather than a foreground service, which would only keep the process alive for no benefit and need a special foreground service type and permission.

- `ActiveWorkoutSession` mirrors the stored workout into the notification while the app runs, and once on every app start.
- `ActiveWorkoutRestoreReceiver` puts the notification back after a reboot or app update, since Android clears notifications then.
- On a fresh start, `TrackerAppViewModel` reopens the workout in progress. The exercise you had open is stored with the workout; typed input and a running hold timer live in `SavedStateHandle`, and typed values are also saved to the set when the app goes to the background.
- Notifications need the Android 13+ permission, which is asked for when you start a workout. Without it, workouts work the same, just without the notification.

## 🚀 Getting Started

**Requirements:** Android Studio (recent stable) and JDK 11+.

```bash
git clone https://github.com/Aboud-KHalaf/Android-Tracker.git
```

Open the project in Android Studio and run the `app` configuration, or build from the command line:

```bash
./gradlew :app:assembleDebug
```

### Running tests

Unit tests and Robolectric-based UI tests (workout, stats, settings, exercise catalog and drag-and-drop flows), plus tests for the database migrations, repositories, progress photo storage, workout notification and ViewModels:

```bash
./gradlew :app:testDebugUnitTest
```

### Building a release APK

Release builds are signed with a keystore that is **not** in the repository. To sign your own build, create a keystore and add a `keystore.properties` file to the project root:

```properties
storeFile=release.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

Then run:

```bash
./gradlew :app:assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`.

## 🌐 Website

The landing page lives in [`website/`](website/) as a single static HTML file that uses the app's Material 3 tokens. On every push to `main` that changes `website/`, the [Deploy website](.github/workflows/pages.yml) workflow publishes it to GitHub Pages. Its download buttons always point to the APK in the latest GitHub release.

To preview it locally:

```bash
python3 -m http.server 8765 --directory website
```

## 🙏 Credits

Exercise catalog data comes from the [wger](https://wger.de) project and its contributors, licensed [CC-BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).

## 👤 Author

Built by [Aboud](https://aboud-khalaf.github.io/My-Portfolio/).
