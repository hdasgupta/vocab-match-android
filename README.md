# VocabMatch - Android 4-Letter Vocabulary Game

**VocabMatch** is a native Android deduction vocabulary game built using modern Android architecture, Material 3 design, Kotlin, and Android's **built-in SQLite database engine**.

---

## 🎮 How the Game Works

1. **Secret Word Selection**:
   - The computer randomly chooses a meaningful 4-letter English word from an internal curated vocabulary dictionary (`assets/words.txt`).
   - The secret word remains hidden from the player.

2. **Deduction Gameplay**:
   - The user enters a meaningful 4-letter English word.
   - The app verifies that the word is 4 letters and exists in the dictionary.
   - The computer reveals **how many letters match** with the hidden secret word (using proper multiset frequency matching to handle duplicate letters accurately), along with positional match indicators.
   - The user continues making guesses until the exact word is discovered.

3. **Scoring Rule**:
   - $\\text{Points Earned} = \\frac{100}{\\text{Number of Guesses}}$
   - Example:
     - 1 guess: **100.0 pts**
     - 2 guesses: **50.0 pts**
     - 4 guesses: **25.0 pts**
     - 5 guesses: **20.0 pts**

4. **Built-in Database & Statistics**:
   - Powered by Android's built-in `android.database.sqlite.SQLiteOpenHelper` (zero third-party ORM overhead).
   - Stores each game record and aggregates:
     - **Aggregated Points**: Total accumulated points across all games.
     - **Average Guesses**: Calculated as $\\frac{\\sum \\text{Total Guesses}}{\\text{Total Games Played}}$.
     - **Games Won**: Total completed games.
     - **Average Points per Game**.

---

## 🚀 GitHub Actions CI/CD (Automatic APK Generation)

The repository includes a ready-to-use GitHub Actions workflow located at `.github/workflows/build-apk.yml`.

### Workflow Capabilities:
- **Trigger**: Automatically executes on every `push` or `pull_request` to `main` and `master` branches, as well as manual triggers (`workflow_dispatch`).
- **Build Environment**: Ubuntu latest with Eclipse Temurin JDK 17.
- **Gradle Caching**: Uses Gradle build caching to speed up subsequent builds.
- **APK Generation**: Executes `./gradlew assembleDebug --stacktrace`.
- **Artifact Upload**: Automatically uploads the generated `app-debug.apk` as a downloadable GitHub Actions artifact retained for 14 days.

---

## 📁 Project Architecture & Structure

```
VocabMatchApp/
├── .github/
│   └── workflows/
│       └── build-apk.yml             <- GitHub Actions workflow for auto APK build
├── app/
│   ├── build.gradle                  <- Android application build config
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   └── words.txt          <- Curated 4-letter vocabulary dictionary
│       │   ├── java/com/example/vocabgame/
│       │   │   ├── data/
│       │   │   │   └── DatabaseHelper.kt  <- Built-in SQLiteOpenHelper implementation
│       │   │   ├── logic/
│       │   │   │   └── GameEngine.kt      <- Word matching & 100/N scoring logic
│       │   │   ├── model/
│       │   │   │   ├── GameStats.kt       <- Aggregated database statistics
│       │   │   │   └── GuessResult.kt     <- Individual guess model
│       │   │   └── ui/
│       │   │       ├── MainActivity.kt    <- Game screen controller & dialogs
│       │   │       └── GuessAdapter.kt    <- RecyclerView guess history adapter
│       │   └── res/
│       │       ├── layout/
│       │       │   ├── activity_main.xml  <- Material 3 main dashboard
│       │       │   ├── item_guess.xml     <- Guess card layout
│       │       │   └── dialog_victory.xml <- Victory stats dialog
│       │       └── values/
│       │           ├── colors.xml
│       │           ├── strings.xml
│       │           └── themes.xml
│       └── test/java/com/example/vocabgame/
│           └── GameEngineTest.kt          <- Unit tests for matching & scoring rules
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties  <- Gradle 8.5 wrapper configuration
├── build.gradle                       <- Top-level project build script
├── settings.gradle                    <- Project settings
├── gradle.properties
├── gradlew                            <- Gradle wrapper script (POSIX)
├── gradlew.bat                        <- Gradle wrapper script (Windows)
└── README.md
```

---

## 🛠️ Building & Running Locally

### Option A: Using Android Studio
1. Open **Android Studio** (Hedgehog 2023.1.1 or newer recommended).
2. Select **File > Open...** and browse to this extracted project folder.
3. Allow Gradle to sync dependencies.
4. Select an Android Emulator or connected physical device (Android 7.0 / API 24+).
5. Click **Run 'app'** (`Shift + F10`).

### Option B: Command Line (Gradle)
```bash
# Ensure execution permissions
chmod +x gradlew

# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```
