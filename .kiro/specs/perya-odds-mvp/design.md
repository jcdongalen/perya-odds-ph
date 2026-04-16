# Design Document: Perya Odds MVP

## Overview

Perya Odds PH is a cross-platform mobile application (Android/iOS) built with Kotlin Multiplatform Mobile (KMM). It enables Filipino perya players to manually record game outcomes, compute observed probabilities, detect statistical bias, and receive strategy recommendations — all derived from user-collected data.

**Platform Requirements:**
- **Android**: Minimum API 23 (Android 6.0) to latest stable versions
- **iOS**: Latest stable versions

The MVP ships with a single supported game: the **3-Ball Drop Card Game** (6-card variant). The architecture is designed as a multi-game plugin/registry system so additional perya game types can be added without restructuring the core platform.

Key design principles:
- **Shared business logic**: All engines, models, and repositories are written once in Kotlin and shared between Android and iOS.
- **Offline-first**: All data is stored locally on-device using platform-specific storage (DataStore on Android, UserDefaults on iOS) via expect/actual.
- **Game-agnostic core**: Engines (Observation, Probability, Bias, Strategy) operate on a `GameConfig` abstraction, not hardcoded game rules.
- **No prediction**: The app computes statistics from observed data only. No ML, no outcome prediction.
- **Fast input**: The Observation Screen is optimized for rapid round entry during live play.
- **Native UI**: Android uses Jetpack Compose, iOS uses SwiftUI for optimal platform experience.

---

## Architecture

The application follows a layered KMM architecture with shared business logic and platform-specific UI:

```
┌─────────────────────────────────────────────────────┐
│                 Platform UI Layer                   │
│  Android (Jetpack Compose)  │  iOS (SwiftUI)        │
│  - GameSelectionScreen      │  - GameSelectionView  │
│  - ObservationScreen        │  - ObservationView    │
│  - AnalyticsDashboard       │  - AnalyticsDashboard │
│  - StrategyScreen           │  - StrategyView       │
└──────────────────────┬──────────────────────────────┘
                       │ Platform ViewModels/ObservableObjects
┌──────────────────────▼──────────────────────────────┐
│            Shared Kotlin Module (commonMain)        │
│                                                     │
│  ┌─────────────────────────────────────────────┐  │
│  │         Presentation Layer                  │  │
│  │  GameSessionViewModel (shared state logic)  │  │
│  └─────────────────────┬───────────────────────┘  │
│                        │                           │
│  ┌─────────────────────▼───────────────────────┐  │
│  │            Engine Layer                     │  │
│  │  ObservationEngine │ ProbabilityEngine      │  │
│  │  BiasDetectionEngine │ StrategyEngine       │  │
│  └─────────────────────┬───────────────────────┘  │
│                        │                           │
│  ┌─────────────────────▼───────────────────────┐  │
│  │         Game Registry Layer                 │  │
│  │  GameRegistry  │  GameConfig (per game)     │  │
│  └─────────────────────┬───────────────────────┘  │
│                        │                           │
│  ┌─────────────────────▼───────────────────────┐  │
│  │         Persistence Layer                   │  │
│  │  SessionRepository (expect/actual)          │  │
│  └─────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
┌───────▼────────┐           ┌────────▼────────┐
│  androidMain   │           │    iosMain      │
│  DataStore     │           │  UserDefaults   │
└────────────────┘           └─────────────────┘
```

### State Management

State is managed in the shared `GameSessionViewModel` using Kotlin StateFlow:
- `activeGameType`: the currently selected `Game_Type`
- `sessions`: a map of `Game_Type → GameSession`
- `strategyMode`: `1 | 2 | 3` (number of cards in strategy)

All engine functions are pure — they take data in, return results out, with no side effects. Side effects (persistence) are handled in the ViewModel.

Platform-specific wrappers:
- **Android**: ViewModel wrapper using Jetpack ViewModel + Compose State
- **iOS**: ObservableObject wrapper for SwiftUI

### Navigation

**Android**: Jetpack Compose Navigation
```
NavHost
  └── GameSelectionScreen (start destination if no active game)
  └── MainNavigation (Bottom Navigation)
        ├── ObservationScreen
        ├── AnalyticsDashboard
        └── StrategyScreen
```

**iOS**: SwiftUI NavigationStack + TabView
```
NavigationStack
  └── GameSelectionView (shown if no active game)
  └── TabView
        ├── ObservationView
        ├── AnalyticsDashboard
        └── StrategyView
```

---

## Components and Interfaces

### GameRegistry

Central registry of all supported game types. Each game registers a `GameConfig` object.

```kotlin
interface GameRegistry {
  fun getAll(): List<GameConfig>
  fun getById(gameType: String): GameConfig?
  fun register(config: GameConfig)
}
```

The registry is populated at app startup. For the MVP, only `three_ball_drop` is registered as active; future games are registered with `comingSoon = true`.

---

### ObservationEngine

Responsible for validating and recording round observations.

```kotlin
interface ObservationEngine {
  fun recordObservation(session: GameSession, hits: List<String>): Result<GameSession, ValidationError>
  fun resetSession(gameType: String): GameSession
}
```

- Validates that `hits.size == gameConfig.hitsPerRound` (3 for the MVP game).
- Allows duplicate card selections within a round.
- Returns an updated `GameSession` (immutable update pattern).

---

### ProbabilityEngine

Computes observed and expected probabilities, and assigns a confidence level.

```kotlin
interface ProbabilityEngine {
  fun computeProbabilities(session: GameSession, config: GameConfig): ProbabilityResult
}

data class ProbabilityResult(
  val perOutcome: Map<String, OutcomeProbability>,
  val confidenceLevel: ConfidenceLevel,
  val totalRounds: Int
)
```

- `observed(card) = hits(card) / (totalRounds × hitsPerRound)`
- `expected(card) = 1 / outcomeCount` (1/6 for the MVP game)
- Returns default expected probabilities when `totalRounds == 0`.

---

### BiasDetectionEngine

Classifies each outcome as Hot, Cold, or Neutral.

```kotlin
interface BiasDetectionEngine {
  fun detectBias(probResult: ProbabilityResult): BiasResult
}

data class BiasResult(
  val perOutcome: Map<String, OutcomeBias>
)
```

- `deviation(card) = (observed - expected) / expected`
- `deviation > 0` → Hot; `deviation < 0` → Cold; `deviation == 0` → Neutral.

---

### StrategyEngine

Selects top-N cards and computes win probability.

```kotlin
interface StrategyEngine {
  fun recommend(session: GameSession, config: GameConfig, mode: Int): StrategyResult
}

data class StrategyResult(
  val selectedCards: List<String>,
  val individualProbabilities: Map<String, Double>,
  val winProbability: Double,
  val confidenceLevel: ConfidenceLevel,
  val mode: Int
)
```

- Selects top-N cards by observed probability; ties broken by canonical order.
- `winProbability = 1 − (1 − sumOfSelectedProbabilities)³`
- For empty datasets, falls back to expected probability (1/6 per card).

---

### SessionRepository

Handles persistence using expect/actual pattern for platform-specific storage.

```kotlin
interface SessionRepository {
  suspend fun loadAll(): Map<String, GameSession>
  suspend fun save(gameType: String, session: GameSession)
  suspend fun delete(gameType: String)
}
```

**Android implementation**: Uses DataStore Preferences
**iOS implementation**: Uses UserDefaults

Storage key format: `perya_odds_session_{gameType}`

---

### UI Components

Components are implemented separately for each platform but follow the same design:

**Android (Jetpack Compose Composables)**
| Component | Responsibility |
|---|---|
| `CardSelector` | Renders 6 card buttons; enforces max-3 selection with multi-select support |
| `ProbabilityBar` | Horizontal bar showing observed vs expected frequency |
| `BiasIndicator` | Color-coded badge: red (Hot), blue (Cold), grey (Neutral) |
| `ConfidenceBadge` | Displays Low / Medium / High / Very High |
| `StrategyModeToggle` | 3-way toggle for 1 / 2 / 3 card modes |
| `DisclaimerBanner` | Persistent disclaimer text on Strategy screen |
| `WinProbabilityDisplay` | Shows computed win probability as percentage |

**iOS (SwiftUI Views)**
| Component | Responsibility |
|---|---|
| `CardSelectorView` | Renders 6 card buttons; enforces max-3 selection with multi-select support |
| `ProbabilityBarView` | Horizontal bar showing observed vs expected frequency |
| `BiasIndicatorView` | Color-coded badge: red (Hot), blue (Cold), grey (Neutral) |
| `ConfidenceBadgeView` | Displays Low / Medium / High / Very High |
| `StrategyModeToggleView` | 3-way toggle for 1 / 2 / 3 card modes |
| `DisclaimerBannerView` | Persistent disclaimer text on Strategy screen |
| `WinProbabilityDisplayView` | Shows computed win probability as percentage |

---

## Data Models

```kotlin
// Identifies a game type
typealias GameType = String // e.g. "three_ball_drop"

// Configuration for a specific game type — registered in GameRegistry
data class GameConfig(
  val gameType: GameType,
  val displayName: String,           // e.g. "3-Ball Drop Card Game"
  val outcomes: List<String>,        // e.g. ['Ace', 'King', 'Queen', 'Jack', '10', '9']
  val hitsPerRound: Int,             // e.g. 3
  val expectedProbability: Double,   // e.g. 1/6 ≈ 0.1667
  val comingSoon: Boolean
)

// A single recorded round
@Serializable
data class Observation(
  val id: String,                    // UUID
  val timestamp: Long,               // Unix ms
  val hits: List<String>             // e.g. ['Ace', 'Ace', 'King'] — size == hitsPerRound
)

// Accumulated data for one game type on this device
@Serializable
data class GameSession(
  val gameType: GameType,
  val totalRounds: Int,
  val hitCounts: Map<String, Int>,   // outcome → total hits across all rounds
  val observations: List<Observation>, // full history (for future replay/undo)
  val lastUpdated: Long              // Unix ms
)

// Output of ProbabilityEngine
data class ProbabilityResult(
  val perOutcome: Map<String, OutcomeProbability>,
  val confidenceLevel: ConfidenceLevel,
  val totalRounds: Int
)

data class OutcomeProbability(
  val observed: Double,   // 0.0 – 1.0
  val expected: Double    // 0.0 – 1.0 (constant per game config)
)

// Output of BiasDetectionEngine
data class BiasResult(
  val perOutcome: Map<String, OutcomeBias>
)

data class OutcomeBias(
  val deviation: Double,                      // (observed - expected) / expected
  val classification: BiasClassification
)

enum class BiasClassification {
  Hot, Cold, Neutral
}

// Output of StrategyEngine
data class StrategyResult(
  val selectedCards: List<String>,
  val individualProbabilities: Map<String, Double>,
  val winProbability: Double,
  val confidenceLevel: ConfidenceLevel,
  val mode: Int
)

// Confidence classification
enum class ConfidenceLevel {
  Low, Medium, High, VeryHigh
}

// Validation error from ObservationEngine
data class ValidationError(
  val code: ValidationErrorCode,
  val message: String
)

enum class ValidationErrorCode {
  TOO_FEW_HITS, TOO_MANY_HITS, INVALID_OUTCOME
}

// Generic result type
sealed class Result<out T, out E> {
  data class Success<T>(val value: T) : Result<T, Nothing>()
  data class Error<E>(val error: E) : Result<Nothing, E>()
}
```

### Confidence Level Thresholds

| Rounds | Confidence Level |
|--------|-----------------|
| 0 – 99 | Low |
| 100 – 199 | Medium |
| 200 – 499 | High |
| 500+ | Very High |

### Win Probability Formula

For N selected cards with observed probabilities p₁, p₂, ..., pN:

```
winProbability = 1 − (1 − (p₁ + p₂ + ... + pN))³
```

This models the probability of at least one of the selected cards being hit in a round of 3 balls.

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Property-based testing is appropriate here because the core engines (ProbabilityEngine, BiasDetectionEngine, StrategyEngine, ObservationEngine) are pure functions whose behavior must hold across all valid inputs — varying round counts, hit distributions, and card selections. We use **fast-check** (TypeScript PBT library) with a minimum of 100 iterations per property.

---

### Property 1: Observation recording updates hit counts and round count correctly

*For any* `GameSession` and any valid hits array of length `hitsPerRound`, after calling `ObservationEngine.recordObservation`, the resulting session's `totalRounds` SHALL equal the previous `totalRounds + 1`, and each card's `hitCount` SHALL increase by exactly the number of times that card appears in the hits array, with all other cards' hit counts unchanged.

**Validates: Requirements 2.2, 2.6**

---

### Property 2: Invalid observations are rejected without mutating session

*For any* hits array whose length is not equal to `hitsPerRound`, `ObservationEngine.recordObservation` SHALL return an error result and the session SHALL be identical to the input session (totalRounds, hitCounts, and observations all unchanged).

**Validates: Requirements 2.3, 2.4**

---

### Property 3: Observed probability formula holds for any session

*For any* `GameSession` with `totalRounds > 0` and any outcome card, the observed probability computed by `ProbabilityEngine` SHALL equal `hitCounts[card] / (totalRounds × hitsPerRound)`, and the expected probability SHALL equal `1 / outcomes.length` — and the sum of all observed probabilities across all outcomes SHALL equal 1.0 within floating-point tolerance.

**Validates: Requirements 3.1, 3.2**

---

### Property 4: Deviation formula and bias classification are consistent

*For any* `ProbabilityResult`, the `BiasDetectionEngine` SHALL compute `deviation(card) = (observed − expected) / expected` for each outcome, and classify it as Hot when deviation > 0, Cold when deviation < 0, and Neutral when deviation = 0.

**Validates: Requirements 4.1, 4.2, 4.3, 4.4**

---

### Property 5: Confidence level assignment is exhaustive and monotone

*For any* non-negative integer `totalRounds`, `ProbabilityEngine` SHALL assign exactly one `ConfidenceLevel` (Low for < 100, Medium for 100–199, High for 200–499, Very High for ≥ 500), and the assigned level SHALL be monotonically non-decreasing as `totalRounds` increases.

**Validates: Requirements 5.1, 5.2, 5.3, 5.4**

---

### Property 6: Strategy top-N selection is correct for any session and mode

*For any* `GameSession` and strategy mode N ∈ {1, 2, 3}, the N cards returned by `StrategyEngine.recommend` SHALL be the N cards with the highest observed probability in the dataset, with ties broken by canonical order (Ace, King, Queen, Jack, 10, 9).

**Validates: Requirements 6.2, 6.5, 7.2, 8.2**

---

### Property 7: Win probability formula is correctly applied for any mode

*For any* strategy result with N selected cards and their observed probabilities p₁…pN, the `winProbability` SHALL equal `1 − (1 − (p₁ + p₂ + ... + pN))³` within floating-point tolerance.

**Validates: Requirements 6.3, 7.3, 8.3**

---

### Property 8: Dataset isolation — observations for one game type do not affect another

*For any* two distinct game types A and B with separate `GameSession` objects, recording any valid observation for game type A SHALL leave game type B's session (totalRounds, hitCounts, observations) completely unchanged.

**Validates: Requirements 1.7**

---

### Property 9: Session reset always produces a clean empty dataset

*For any* `GameSession` regardless of accumulated data, after `ObservationEngine.resetSession`, the resulting session SHALL have `totalRounds === 0`, all `hitCounts` equal to 0, and an empty `observations` array.

**Validates: Requirements 11.4, 11.5**

---

### Property 10: Persistence round-trip is lossless for any session

*For any* `GameSession`, serializing it to storage via `SessionRepository.save` and then loading it back via `SessionRepository.loadAll` SHALL produce a session that is deeply equal to the original (same totalRounds, hitCounts, observations, gameType, and lastUpdated).

**Validates: Requirements 11.1, 11.2**

---

## Error Handling

| Scenario | Handling |
|---|---|
| User submits < 3 hits | `ObservationEngine` returns `TOO_FEW_HITS` error; UI shows inline validation message |
| User selects > 3 cards | `CardSelector` UI prevents 4th selection; no engine call made |
| AsyncStorage read failure on launch | App initializes empty sessions; shows a non-blocking toast warning |
| AsyncStorage write failure | Error is logged; user is notified via toast; data remains in memory for the session |
| Unknown game type requested | `GameRegistry.getById` returns `undefined`; navigation falls back to `GameSelectionScreen` |
| Empty dataset on strategy/analytics screens | Engines return default expected values; UI shows "No data recorded yet" message |

---

## Testing Strategy

### Unit Tests (Kotlin Test + JUnit)

All shared business logic is tested in `commonTest`:
- `ObservationEngine`: valid/invalid hit arrays, duplicate cards, hit count accumulation
- `ProbabilityEngine`: probability computation, confidence level thresholds, zero-round edge case
- `BiasDetectionEngine`: deviation formula, all three classification branches
- `StrategyEngine`: top-N selection, tie-breaking by canonical order, win probability formula
- `SessionRepository`: serialization/deserialization, missing key handling

### Property-Based Tests (Kotest Property Testing, minimum 100 iterations each)

Each property test is tagged with:
`// Feature: perya-odds-mvp, Property {N}: {property_text}`

| Property | Test Description |
|---|---|
| P1 | Recording any valid observation updates round count and hit counts correctly |
| P2 | Any invalid hits array (wrong length) is rejected without mutating session |
| P3 | Observed probability formula and sum-to-1 hold for any valid session |
| P4 | Deviation formula and bias classification are consistent for any probability result |
| P5 | Confidence level is monotone and exhaustive for any round count |
| P6 | Top-N card selection is correct for any session and mode (1, 2, or 3) |
| P7 | Win probability formula holds for any selected card probabilities and mode |
| P8 | Dataset isolation: observations for one game type never affect another |
| P9 | Reset always produces a clean empty session for any prior state |
| P10 | Persistence round-trip is lossless for any session |

### Integration Tests

- App launch → first-time `GameSelectionScreen` display
- Full observation flow: select game → record 3 rounds → verify analytics values
- Strategy mode toggle: 1-card → 2-card → 3-card → verify win probability updates
- Persistence: record observations → simulate app restart → verify data reloaded

### Manual / Exploratory Tests

- Disclaimer visibility on Strategy screen in all modes
- "Coming Soon" label on non-MVP game types in `GameSelectionScreen`
- Dataset reset confirmation flow
- Performance: rapid observation entry (10+ rounds per minute)
