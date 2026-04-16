package com.jcdongalen.peryaodds.shared.domain.models

data class StrategyResult(
    val selectedCards: List<String>,
    val individualProbabilities: Map<String, Double>,
    val winProbability: Double,
    val confidenceLevel: ConfidenceLevel,
    val mode: Int
)
