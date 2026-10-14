# Tracker

An offline-first Android app for tracking your **workouts**, a **daily log** of calories, protein and steps, your **body weight** and **progress photos** in one place. Built with Kotlin, Jetpack Compose and Material Design 3.

All data stays on your device, and you don't need an account. When you want your numbers elsewhere, export any screen's analytics, or everything at once, as a PDF or Excel file. The only feature that uses the internet is the optional exercise catalog, which sends just your search to [wger.de](https://wger.de).

🌐 **Website:** [aboud-khalaf.github.io/Android-Tracker](https://aboud-khalaf.github.io/Android-Tracker/)

## ✨ Features

### 🏋️ Workouts
- Create workout plans and add exercises to them
- Browse a free online exercise catalog ([wger](https://wger.de)) while building a plan: search by name, see each exercise's picture, muscles, equipment and instructions, and add it in one tap
- The Home screen suggests which plan to train next
- Log weight-and-reps sets during an active workout
- Built-in hold timer for timed sets (planks, holds, etc.)
- Workout history grouped by month, with a filter by plan

### 📈 Progress
- A details screen for each exercise, with a progress chart and full history
- Switch between progress metrics and time ranges
- Export an exercise's progress for the selected metric and range as a PDF or Excel file

### 🥗 Daily log
- Log each day's calories, protein and steps, and edit any past day
- Set your own daily targets and see today's progress at a glance
- Charts and period averages with a period selector
- Export the selected period as a PDF or Excel file
- Optional daily reminder (9 PM by default, at a time you choose) if you haven't logged anything that day

### ⚖️ Body Weight
- See your current weight, a weight chart and your full history
- Export the selected period as a PDF or Excel file

### 📸 Progress photos
- Take a front, side and back photo from the Weight screen, one pose after another
- A body outline with head and feet lines shows where to stand, so every photo is framed the same way
- Your last photo of each pose shows faintly over the camera, so you can line up in the same spot
- A 3 or 10 second self-timer gives you time to step back from a propped-up phone; switch between the front and back camera
- See every day's photos, and compare your first and latest photo of a pose side by side
- Photos stay inside the app: they aren't added to your gallery or included in cloud backups

### ⚙️ Settings
- Light, dark or system theme
- Turn the daily log reminder on or off and choose when it arrives
- Export all data (exercises, every workout set, daily log and weight) as a PDF or Excel file
- Delete all data, progress photos included, from the device

### 🎨 Design
- Material Design 3 with a blue brand theme, plus light and dark modes
- Animated splash screen, screen transitions and list animations
- Layouts adapt to larger screens

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
| Background | AlarmManager for the daily nutrition reminder |
| Dependency injection | Manual (`AppContainer`) |
| Testing | JUnit, Robolectric, Compose UI tests |

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
│                #   (progress, nutrition, weight, photos, plans, catalog, reminders, export)
├── di/          # AppContainer: manual dependency wiring
└── ui/          # Compose screens and ViewModels, one package per feature
    ├── home/ plan/ catalog/ workout/ history/ exercise/ exercises/
    ├── nutrition/ weight/ photos/ settings/ splash/
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

Unit tests and Robolectric-based UI tests (workout, daily log, weight, settings and exercise catalog flows), plus tests for the progress photo storage and ViewModels:

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
