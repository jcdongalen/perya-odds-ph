package com.jcdongalen.peryaodds.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class GameSession(
    val gameType: GameType,
    val totalRounds: Int,
    val hitCounts: Map<String, Int>,
    val observations: List<Observation>,
    val lastUpdated: Long
)
