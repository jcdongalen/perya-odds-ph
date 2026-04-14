# 📄 Business Analysis Document (BA)
## Product: Perya Odds PH

---

# 1. 🧭 Product Overview

## 1.1 Name
Perya Odds PH

## 1.2 App ID
com.jcdongalen.peryaodds

## 1.3 Platforms
- Android (latest stable versions)
- iOS (latest stable versions)

## 1.4 Description
Perya Odds PH is a cross-platform probability analysis and observation tool for Philippine perya games. It helps users record real game outcomes and analyze statistical patterns to support decision-making.

The application does NOT guarantee winnings and does NOT predict outcomes. It provides statistical insights based on observed data.

---

# 2. 🎯 Product Vision

To provide a transparent, data-driven understanding of perya game outcomes using real-world observations and probability modeling.

---

# 3. 🎮 Supported Games

## 3.1 MVP (Phase 1)
### 3-Ball Drop Card Game (6 cards)

Cards:
- Ace
- King
- Queen
- Jack
- 10
- 9

Mechanics:
- 3 balls drop per round
- each ball independently lands on one of 6 cards
- user records results manually

---

## 3.2 Future Expansion
- Color Drop Games (3-ball system)
- 1-Ball 52-Card Deck Game
- Other Philippine perya variants

---

# 4. 🧠 Core System Principles

- All decisions are based on observed data
- No intuition-based recommendations
- No guaranteed outcomes
- Probability is derived from user-collected samples

---

# 5. 📊 Core Modules

---

## 5.1 Observation Engine

Users manually input results per round.

### Input:
- 3 card outcomes per round

### Output:
- frequency tracking
- dataset accumulation
- observed probability per card

---

## 5.2 Probability Engine

### Baseline assumption:
- Each card has equal probability:
  - 1/6 per ball

### Computes:
- expected vs observed probability
- deviation per card

---

## 5.3 Bias Detection Engine

### Purpose:
Identify statistical deviation from expected distribution.

### Formula:
deviation = (observed - expected) / expected

### Output:
- Hot cards (above expected frequency)
- Cold cards (below expected frequency)
- Neutral cards

---

## 5.4 Strategy Engine (Core Feature)

The Strategy Engine provides card selection recommendations using observed probability.

---

# 6. 🧠 Strategy System

---

## Core Rule

- Default mode = 1-card recommendation
- 2-card and 3-card modes require user activation
- ranking is ALWAYS based on observed probability

---

# 7. ⚙️ Strategy Modes

---

## 7.1 🟢 1-Card Mode (DEFAULT)

### Description
System recommends the single best card based on observed probability.

### Logic
p(card) = hits(card) / total_rounds

### Output
- 1 recommended card
- win probability
- confidence level

### Example
Recommended Card: King (18%)  
Win Probability: 42.1%  
Confidence: Medium  

---

## 7.2 🟡 2-Card Mode (OPTIONAL)

### Activation
User must explicitly enable this mode.

### Description
System selects top 2 cards with highest observed probability.

### Probability Model
p(S) = p1 + p2  
P(win) = 1 − (1 − p(S))³  

### Output
- selected cards
- win probability
- confidence level
- risk note

### Example
Strategy: 2-Card Mode  

Selected:
- King (18%)
- 10 (20%)

Win Probability: 76.2%  
Confidence: Medium  

---

## 7.3 🔵 3-Card Mode (ADVANCED)

### Activation
User must explicitly enable this mode.

### Description
System selects top 3 observed probability cards.

### Probability Model
p(S) = p1 + p2 + p3  
P(win) = 1 − (1 − p(S))³  

### Output
- selected cards
- win probability
- high exposure warning

### Example
Strategy: 3-Card Mode  

Selected:
- King (18%)
- 10 (20%)
- Ace (17%)

Win Probability: 89.5%  
Risk: High Exposure  

---

# 8. 📊 Strategy Comparison

| Mode | Default | Cards | Win Frequency | Risk |
|------|--------|------|--------------|------|
| 1-Card | Yes | 1 | Medium | Low |
| 2-Card | No | 2 | High | Medium |
| 3-Card | No | 3 | Very High | High |

---

# 9. 📈 Analytics System

## Card Metrics
- observed frequency
- expected frequency (16.67%)
- deviation score
- classification (hot / cold / neutral)

---

## Confidence Levels

| Samples | Confidence |
|--------|-----------|
| < 100 | Low |
| 100–200 | Medium |
| 200–500 | High |
| 500+ | Very High |

---

# 10. ⚠️ Risk & Disclaimer System

The app must always display:

- Based on observed data only
- No guarantee of future outcomes
- Higher card coverage increases cost and variance

---

# 11. 📱 UI Requirements

## Screens

### 1. Observation Screen
- fast input for 3-card results per round

### 2. Analytics Dashboard
- frequency distribution charts
- hot/cold indicators

### 3. Strategy Screen
- default 1-card recommendation
- toggle for 2-card / 3-card modes

---

# 12. ⚙️ System Architecture

## Shared Logic Layer
- probability engine
- observation analyzer
- strategy engine

## Platform Layer
- Android (Jetpack Compose)
- iOS (SwiftUI)

---

# 13. 🚀 Future Enhancements

- auto-switch strategy based on volatility
- EV-based optimization engine
- time-weighted probability (recent rounds matter more)
- expansion to all Philippine perya games

---

# 14. 📌 Product Summary

Perya Odds PH is a statistical analysis tool that:

- records real perya outcomes
- analyzes probability distributions
- detects deviations in observed data
- recommends 1/2/3-card strategies based on observed frequency

It is NOT:
- a prediction system
- a guaranteed winning tool
- an automated betting system
