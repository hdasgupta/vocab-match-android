# VocabMatch - Android 4-Letter Vocabulary Game Suite

**VocabMatch** is a native Android deduction vocabulary game built using modern Android architecture, Material 3 design, Kotlin, and Android's **built-in SQLite database engine**.

---

## 🎮 The 4 Game Modes & History Logs

The application features four distinct 4-letter vocabulary game modes, each with persistent SQLite database game logs and detailed historical inspect dialogs:

### 1. Guess Word (Deduction Mode)
- **Gameplay**: Player deduces a secret 4-letter word. Each guess reports matching letter counts and exact in-place matches.
- **Scoring**: $\text{Points Earned} = \frac{100}{\text{Number of Guesses}}$ (e.g. 1 guess: 100 pts, 2: 50 pts, 4: 25 pts, 5: 20 pts). Clue hints halve the points earned.
- **History Tracking**:
  - Persisted in SQLite `game_records` table (`secret_word`, `guess_count`, `points`, `timestamp`).
  - Dedicated **"History Log"** button in performance card (`btnViewGuessHistory`).
  - Tap any past game to open the **Guess Word Record Dialog** displaying secret word, dictionary definition, guesses taken, and points earned.

### 2. Change a Letter (Word Chain Mode)
- **Gameplay**: Turn-based word chain alternating between player and computer. Each move changes exactly 1 letter to form a new valid dictionary word.
- **Scoring**: +2 points per valid word played. Surrendering finalizes the chain score.
- **History Tracking**:
  - Persisted in SQLite `chain_game_records` table (`chain_length`, `user_words`, `points`, `winner`, `timestamp`).
  - Dedicated **"History Log"** button in stats card (`btnViewChainHistory`).
  - Tap any past game to inspect total chain length, user words submitted, winner outcome (Player Victory 🏆 vs Computer Surrender), and points earned.

### 3. Rearrange (Anagram Discovery Mode)
- **Gameplay**: Unscramble a 4-letter base word into all possible valid anagrams. Progressive points: 1st word = 25 pts, 2nd = 50 pts, 3rd = 100 pts.
- **History Tracking**:
  - Persisted in SQLite `rearrange_game_records` table (`base_word`, `words_found_count`, `words_found_csv`, `words_missed_csv`, `possible_words_count`, `points`, `timestamp`).
  - Dedicated **"History Log"** button in stats card (`btnViewRearrangeHistory`).
  - Tap any past record to inspect found vs missed words with full definitions.

### 4. Crossword (Interconnected 4x4 Puzzle)
- **Gameplay**: Solve 4 interconnected 4-letter words (2 Across, 2 Down) with minimal clues and letter reveals.
- **History Tracking**:
  - Persisted in SQLite `crossword_game_records` table (`across1`, `across2`, `down1`, `down2`, `correct_guesses`, `hints_taken`, `points`, `is_completed`, `timestamp`).
  - Dedicated **"Stats / History"** button (`btnCrosswordHistory`).
  - Tap any past record to view all 4 solution words, definitions, and hint penalties.

### 🌐 All Games History (Unified History Hub)
- Accessible from the Navigation Drawer under **"All Games History"** (`nav_all_history`).
- Aggregates and chronologically sorts game records across all four game modes.
- Filter chips allow filtering between: `All Games`, `Guess Word`, `Change a Letter`, `Rearrange`, and `Crossword`.
- Color-coded badges instantly identify the game type (Blue for Guess Word, Purple for Word Chain, Green for Rearrange, Amber for Crossword).
- Tapping any record opens its detailed breakdown dialog.

---

## 🚀 GitHub Actions CI/CD (Automatic APK Generation)

The repository includes a ready-to-use GitHub Actions workflow located at `.github/workflows/build-apk.yml`.

### Workflow Capabilities:
- **Trigger**: Automatically executes on every `push` or `pull_request` to `main` and `master` branches, as well as manual triggers (`workflow_dispatch`).
- **Build Environment**: Ubuntu latest with Eclipse Temurin JDK 17.
- **Gradle Caching**: Uses Gradle build caching to speed up subsequent builds.
- **APK Generation**: Executes `gradle assembleDebug --stacktrace`.
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
│       │   │   │   ├── DatabaseHelper.kt  <- SQLite helper with history queries for all 4 games
│       │   │   │   ├── WordDictionary.kt  <- 4-letter dictionary loader
│       │   │   │   └── WordMeanings.kt    <- Word definitions and clues
│       │   │   ├── logic/
│       │   │   │   ├── GameEngine.kt          <- Guess Word matching & 100/N scoring
│       │   │   │   ├── ChangeLetterEngine.kt  <- Word chain rules & AI opponent
│       │   │   │   ├── RearrangeEngine.kt     <- Anagram generation & validation
│       │   │   │   └── CrosswordEngine.kt     <- Interlocking crossword generator
│       │   │   ├── model/
│       │   │   │   ├── GameStats.kt
│       │   │   │   ├── GuessWordRecord.kt         <- Guess Word history record
│       │   │   │   ├── ChainGameRecord.kt         <- Word chain history record
│       │   │   │   ├── RearrangeRecord.kt         <- Rearrange history record
│       │   │   │   ├── CrosswordModels.kt         <- Crossword models & history record
│       │   │   │   └── UnifiedGameHistoryRecord.kt<- Unified history model across all modes
│       │   │   └── ui/
│       │   │       ├── MainActivity.kt            <- Main screen, navigation & history dialogs
│       │   │       ├── GuessWordHistoryAdapter.kt <- Guess Word history RecyclerView adapter
│       │   │       ├── ChainGameHistoryAdapter.kt <- Word Chain history RecyclerView adapter
│       │   │       ├── RearrangeHistoryAdapter.kt <- Rearrange history RecyclerView adapter
│       │   │       ├── CrosswordHistoryAdapter.kt <- Crossword history RecyclerView adapter
│       │   │       └── UnifiedGameHistoryAdapter.kt<- Unified All-Games history adapter
│       │   └── res/
│       │       ├── layout/
│       │       │   ├── activity_main.xml          <- Material 3 main dashboard with history buttons
│       │       │   ├── item_guess_word_history.xml<- Guess Word history card layout
│       │       │   ├── item_chain_game_history.xml<- Word Chain history card layout
│       │       │   ├── item_rearrange_history.xml <- Rearrange history card layout
│       │       │   ├── item_crossword_history.xml <- Crossword history card layout
│       │       │   └── item_unified_game_history.xml<- Unified history card layout
│       │       ├── menu/
│       │       │   └── drawer_menu.xml            <- Navigation drawer with All Games History
│       │       └── values/
│       │           ├── colors.xml
│       │           ├── strings.xml
│       │           └── themes.xml
│       └── test/java/com/example/vocabgame/
│           ├── GameEngineTest.kt
│           ├── ChangeLetterEngineTest.kt
│           ├── RearrangeEngineTest.kt
│           └── CrosswordEngineTest.kt
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```
