# Requirements Document

## Introduction

Perya Odds PH is a cross-platform (Android/iOS) multi-game probability analysis and observation platform for Philippine perya games. The platform is designed to support multiple perya game types, allowing users to analyze observed outcomes and derive statistical insights for any supported game. The MVP focuses on the **3-Ball Drop Card Game** (6-card variant) as the first supported game, with the architecture designed to accommodate additional perya games in future releases.

For each supported game, users manually record outcomes, and the app computes observed probabilities, detects statistical bias, and recommends strategies. The app does NOT predict outcomes or guarantee winnings — all insights are derived solely from user-collected observed data.

## Glossary

- **App**: The Perya Odds PH mobile application running on Android or iOS.
- **Game**: A specific perya game type supported by the platform (e.g., 3-Ball Drop Card Game).
- **Game_Type**: An identifier representing a distinct perya game variant (e.g., `three_ball_drop`).
- **Active_Game**: The game currently selected by the user for observation and analysis.
- **Game_Session**: The accumulated dataset of observations for a specific Game_Type on a device.
- **Round**: A single game event within the Active_Game where outcomes are produced (e.g., 3 balls dropped).
- **Card**: One of the 6 possible outcomes in the 3-Ball Drop Card Game: Ace, King, Queen, Jack, 10, or 9.
- **Hit**: A single ball landing on a specific card in a round of the 3-Ball Drop Card Game (each round produces exactly 3 hits).
- **Observation**: A user-recorded set of outcomes for one round of the Active_Game.
- **Dataset**: The accumulated collection of all recorded observations for a Game_Session.
- **Observation_Engine**: The module responsible for accepting, storing, and accumulating round observations for the Active_Game.
- **Probability_Engine**: The module that computes expected and observed probabilities per outcome for the Active_Game.
- **Bias_Detection_Engine**: The module that classifies outcomes as Hot, Cold, or Neutral based on deviation from expected frequency.
- **Strategy_Engine**: The module that produces outcome selection recommendations based on observed probability for the Active_Game.
- **Analytics_Dashboard**: The UI screen displaying frequency distributions, deviation scores, and outcome classifications for the Active_Game.
- **Observation_Screen**: The UI screen for fast input of round results for the Active_Game.
- **Strategy_Screen**: The UI screen displaying strategy recommendations and mode toggles for the Active_Game.
- **Game_Selection_Screen**: The UI screen that allows users to select or switch the Active_Game.
- **Confidence_Level**: A classification (Low, Medium, High, Very High) based on the number of recorded samples in the current Dataset.
- **Hot_Card**: A card whose observed frequency exceeds its expected frequency by a positive deviation.
- **Cold_Card**: A card whose observed frequency is below its expected frequency by a negative deviation.
- **Neutral_Card**: A card whose observed frequency is within the neutral deviation range.
- **Win_Probability**: The computed probability of at least one selected card being hit in a round.
- **Deviation**: The normalized difference between observed and expected frequency: `(observed - expected) / expected`.

---

## Requirements

### Requirement 1: Game Selection and Switching

**User Story:** As a perya player, I want to select which perya game I am currently playing, so that the app tracks and analyzes data separately for each game type.

#### Acceptance Criteria

1. THE Game_Selection_Screen SHALL display all Game_Types currently supported by the App.
2. WHEN the App is launched for the first time, THE App SHALL present the Game_Selection_Screen to the user before any observation or analysis screens are shown.
3. WHEN a user selects a Game_Type, THE App SHALL set that Game_Type as the Active_Game and navigate to the Observation_Screen for that game.
4. THE App SHALL display the name of the Active_Game on the Observation_Screen, Analytics_Dashboard, and Strategy_Screen.
5. THE App SHALL provide a user-accessible option to switch the Active_Game from within the main navigation.
6. WHEN the user switches the Active_Game, THE App SHALL load the Game_Session associated with the newly selected Game_Type.
7. THE App SHALL maintain a separate Dataset for each Game_Type, ensuring that observations recorded for one Game_Type do not affect the Dataset of another Game_Type.
8. WHERE the MVP is active, THE Game_Selection_Screen SHALL list the 3-Ball Drop Card Game as the only available Game_Type, with additional games visually indicated as "Coming Soon".

---

### Requirement 2: Record Round Observations

**User Story:** As a perya player, I want to record the 3 card outcomes of each round, so that the app can accumulate real game data for analysis.

#### Acceptance Criteria

1. THE Observation_Screen SHALL display all 6 cards (Ace, King, Queen, Jack, 10, 9) as selectable inputs for each round of the Active_Game.
2. WHEN a user selects 3 cards for a round and confirms, THE Observation_Engine SHALL store the observation and append it to the Dataset for the Active_Game.
3. IF a user attempts to confirm a round with fewer than 3 card selections, THEN THE Observation_Screen SHALL display a validation error and prevent submission.
4. IF a user attempts to confirm a round with more than 3 card selections, THEN THE Observation_Screen SHALL prevent selection of additional cards beyond 3.
5. THE Observation_Engine SHALL allow the same card to be selected multiple times in a single round (e.g., 3 balls can land on the same card).
6. WHEN an observation is successfully recorded, THE Observation_Engine SHALL increment the total round count by 1 and update the hit count for each selected card in the Active_Game Dataset.

---

### Requirement 3: Compute Observed and Expected Probability

**User Story:** As a perya player, I want to see the observed probability of each card, so that I can understand how frequently each card has been hit based on real data.

#### Acceptance Criteria

1. THE Probability_Engine SHALL compute the observed probability for each card as: `hits(card) / (total_rounds × 3)` using the Dataset of the Active_Game.
2. THE Probability_Engine SHALL compute the expected probability for each card as `1/6` (approximately 16.67%) for the 3-Ball Drop Card Game.
3. WHEN the Dataset of the Active_Game contains at least 1 observation, THE Probability_Engine SHALL make computed probabilities available to the Analytics_Dashboard and Strategy_Engine.
4. IF the Dataset of the Active_Game contains 0 observations, THEN THE Probability_Engine SHALL return a default expected probability of 16.67% for all cards.

---

### Requirement 4: Detect Statistical Bias (Hot / Cold / Neutral)

**User Story:** As a perya player, I want to see which cards are statistically hot or cold, so that I can identify patterns in the observed data.

#### Acceptance Criteria

1. THE Bias_Detection_Engine SHALL compute the deviation for each card using the formula: `(observed_probability - expected_probability) / expected_probability` based on the Active_Game Dataset.
2. WHEN the deviation for a card is greater than 0, THE Bias_Detection_Engine SHALL classify that card as a Hot_Card.
3. WHEN the deviation for a card is less than 0, THE Bias_Detection_Engine SHALL classify that card as a Cold_Card.
4. WHEN the deviation for a card equals 0, THE Bias_Detection_Engine SHALL classify that card as a Neutral_Card.
5. THE Analytics_Dashboard SHALL display the deviation score and classification (Hot, Cold, or Neutral) for each of the 6 cards of the Active_Game.

---

### Requirement 5: Determine Confidence Level

**User Story:** As a perya player, I want to know how reliable the statistics are, so that I can judge whether I have enough data to trust the recommendations.

#### Acceptance Criteria

1. WHEN the Dataset of the Active_Game contains fewer than 100 observations, THE Probability_Engine SHALL assign a Confidence_Level of "Low".
2. WHEN the Dataset of the Active_Game contains between 100 and 199 observations (inclusive), THE Probability_Engine SHALL assign a Confidence_Level of "Medium".
3. WHEN the Dataset of the Active_Game contains between 200 and 499 observations (inclusive), THE Probability_Engine SHALL assign a Confidence_Level of "High".
4. WHEN the Dataset of the Active_Game contains 500 or more observations, THE Probability_Engine SHALL assign a Confidence_Level of "Very High".
5. THE Strategy_Screen SHALL display the current Confidence_Level alongside every strategy recommendation.

---

### Requirement 6: 1-Card Strategy Recommendation (Default Mode)

**User Story:** As a perya player, I want the app to recommend the single best card by default, so that I have a simple, low-risk starting point for my decisions.

#### Acceptance Criteria

1. THE Strategy_Engine SHALL operate in 1-Card Mode by default when the Active_Game is selected or a new Game_Session is started.
2. WHEN in 1-Card Mode, THE Strategy_Engine SHALL select the card with the highest observed probability from the Active_Game Dataset as the recommended card.
3. WHEN in 1-Card Mode, THE Strategy_Engine SHALL compute Win_Probability as: `1 − (1 − p(card))³` where `p(card)` is the observed probability of the recommended card.
4. THE Strategy_Screen SHALL display the recommended card, its observed probability, the computed Win_Probability, and the current Confidence_Level.
5. IF two or more cards share the highest observed probability, THEN THE Strategy_Engine SHALL select the card that appears first in the canonical order (Ace, King, Queen, Jack, 10, 9).

---

### Requirement 7: 2-Card Strategy Mode (Optional)

**User Story:** As a perya player, I want to optionally enable a 2-card strategy, so that I can increase my win frequency at the cost of higher exposure.

#### Acceptance Criteria

1. THE Strategy_Screen SHALL provide a toggle to activate 2-Card Mode.
2. WHEN 2-Card Mode is activated, THE Strategy_Engine SHALL select the top 2 cards with the highest observed probability from the Active_Game Dataset.
3. WHEN in 2-Card Mode, THE Strategy_Engine SHALL compute Win_Probability as: `1 − (1 − (p1 + p2))³` where `p1` and `p2` are the observed probabilities of the two selected cards.
4. THE Strategy_Screen SHALL display the 2 selected cards, their individual observed probabilities, the combined Win_Probability, the Confidence_Level, and a risk note indicating medium exposure.
5. WHEN 2-Card Mode is deactivated, THE Strategy_Engine SHALL revert to 1-Card Mode.

---

### Requirement 8: 3-Card Strategy Mode (Advanced, Optional)

**User Story:** As a perya player, I want to optionally enable a 3-card strategy, so that I can maximize win frequency when I accept higher risk.

#### Acceptance Criteria

1. THE Strategy_Screen SHALL provide a toggle to activate 3-Card Mode.
2. WHEN 3-Card Mode is activated, THE Strategy_Engine SHALL select the top 3 cards with the highest observed probability from the Active_Game Dataset.
3. WHEN in 3-Card Mode, THE Strategy_Engine SHALL compute Win_Probability as: `1 − (1 − (p1 + p2 + p3))³` where `p1`, `p2`, and `p3` are the observed probabilities of the three selected cards.
4. THE Strategy_Screen SHALL display the 3 selected cards, their individual observed probabilities, the combined Win_Probability, and a high exposure warning.
5. WHEN 3-Card Mode is activated, THE Strategy_Screen SHALL display a prominent warning stating that higher card coverage increases cost and variance.

---

### Requirement 9: Analytics Dashboard

**User Story:** As a perya player, I want to view a visual analytics dashboard, so that I can quickly understand the frequency distribution and bias of all 6 cards.

#### Acceptance Criteria

1. THE Analytics_Dashboard SHALL display the observed frequency, expected frequency (16.67%), and deviation score for each of the 6 cards of the Active_Game.
2. THE Analytics_Dashboard SHALL visually distinguish Hot_Cards, Cold_Cards, and Neutral_Cards using distinct indicators.
3. WHEN the Dataset of the Active_Game contains 0 observations, THE Analytics_Dashboard SHALL display a message indicating that no data has been recorded yet.
4. THE Analytics_Dashboard SHALL update its displayed values each time a new observation is recorded for the Active_Game.

---

### Requirement 10: Disclaimer and Risk Display

**User Story:** As a perya player, I want to always see a disclaimer about the app's limitations, so that I understand the app does not guarantee outcomes.

#### Acceptance Criteria

1. THE App SHALL display a disclaimer on the Strategy_Screen stating that recommendations are based on observed data only and do not guarantee future outcomes.
2. THE App SHALL display a disclaimer stating that higher card coverage increases cost and variance whenever 2-Card Mode or 3-Card Mode is active.
3. THE App SHALL NOT present any recommendation as a prediction of future game outcomes.

---

### Requirement 11: Session Data Persistence

**User Story:** As a perya player, I want my recorded observations to persist between app sessions, so that I do not lose accumulated data when I close the app.

#### Acceptance Criteria

1. WHEN the App is closed, THE Observation_Engine SHALL persist the Dataset for each Game_Type to local device storage.
2. WHEN the App is launched, THE Observation_Engine SHALL load the previously persisted Dataset for each Game_Type from local device storage.
3. IF no previously persisted Dataset exists for a Game_Type on launch, THEN THE Observation_Engine SHALL initialize an empty Dataset for that Game_Type.
4. THE App SHALL provide a user-accessible option to clear the Dataset for the Active_Game and reset all statistics for that Game_Type to their initial state.
5. WHEN the user confirms a Dataset reset for the Active_Game, THE Observation_Engine SHALL delete the persisted Dataset for that Game_Type and reinitialize an empty Dataset for it.
