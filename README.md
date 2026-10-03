# Tracker

An offline-first Android app for tracking your **workouts**, **nutrition** and **body weight** in one place. Built with Kotlin, Jetpack Compose and Material Design 3.

All data stays on your device. You don't need an account or an internet connection.

## ✨ Features

### 🏋️ Workouts
- Create workout plans and add exercises to them
- The Home screen suggests which plan to train next
- Log weight-and-reps sets during an active workout
- Built-in hold timer for timed sets (planks, holds, etc.)
- Workout history grouped by month, with a filter by plan

### 📈 Progress
- A details screen for each exercise, with a progress chart and full history
- Switch between progress metrics and time ranges

### 🥗 Nutrition
- Log daily nutrition and edit any past day
- Set your own daily targets and see today's progress at a glance
- Nutrition chart with a period selector
- Optional daily reminder at 9 PM if you haven't logged anything that day

### ⚖️ Body Weight
- See your current weight, a weight chart and your full history

### ⚙️ Settings
- Light, dark or system theme
- Turn the nutrition reminder on or off
- Delete all data from the device

### 🎨 Design
- Material Design 3 with a blue brand theme, plus light and dark modes
- Animated splash screen, screen transitions and list animations
- Layouts adapt to larger screens

## 📦 Download

Get the latest APK from the [Releases](https://github.com/Aboud-KHalaf/Android-Tracker/releases) page and open it on your phone. Requires **Android 7.0 (API 24)** or newer.

## 🛠️ Tech Stack

| Area | Library |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, `StateFlow`, repositories |
| Navigation | Navigation Compose with type-safe routes (kotlinx.serialization) |
| Storage | Room (with migrations and exported schemas) |
| Background | AlarmManager for the daily nutrition reminder |
| Dependency injection | Manual (`AppContainer`) |
| Testing | JUnit, Robolectric, Compose UI tests |

## 🏗️ Architecture

The app follows **MVVM** with unidirectional data flow:

```
UI (Compose screens)  →  ViewModels (StateFlow UI state)  →  Repositories  →  Room database
```

```
app/src/main/java/com/example/tracker/
├── data/        # Room entities, DAOs, database and repository implementations
├── domain/      # Domain models, repository interfaces and business logic
│                #   (progress, nutrition, weight, plans, reminders)
├── di/          # AppContainer: manual dependency wiring
└── ui/          # Compose screens and ViewModels, one package per feature
    ├── home/ plan/ workout/ history/ exercise/ exercises/
    ├── nutrition/ weight/ settings/ splash/
    ├── navigation/ common/
    └── theme/   # Colors, typography, shapes and spacing tokens
```

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

Unit tests and Robolectric-based UI flow tests (workout, nutrition, weight, settings):

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

## 👤 Author

Built by [Aboud](https://aboud-khalaf.github.io/My-Portfolio/).
