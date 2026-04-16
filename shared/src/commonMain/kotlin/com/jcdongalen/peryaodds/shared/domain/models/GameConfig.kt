package com.jcdongalen.peryaodds.shared.domain.models

data class GameConfig(
    val gameType: GameType,
    val displayName: String,
    val outcomes: List<String>,
    val hitsPerRound: Int,
    val expectedProbability: Double,
    val comingSoon: Boolean
)
