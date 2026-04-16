package com.jcdongalen.peryaodds.shared.domain.registry.games

import com.jcdongalen.peryaodds.shared.domain.models.GameConfig

/**
 * Game configuration for the 3-Ball Drop Card Game (6-card variant).
 * 
 * This is the MVP game for Perya Odds PH.
 * - 6 possible outcomes: Ace, King, Queen, Jack, 10, 9
 * - Each round produces exactly 3 hits (3 balls dropped)
 * - Expected probability per card: 1/6 ≈ 16.67%
 */
val ThreeBallDropConfig = GameConfig(
    gameType = "three_ball_drop",
    displayName = "3-Ball Drop Card Game",
    outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
    hitsPerRound = 3,
    expectedProbability = 1.0 / 6.0,
    comingSoon = false
)
