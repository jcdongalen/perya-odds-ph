package com.jcdongalen.peryaodds.shared.domain.models

data class ProbabilityResult(
    val perOutcome: Map<String, OutcomeProbability>,
    val confidenceLevel: ConfidenceLevel,
    val totalRounds: Int
)

data class OutcomeProbability(
    val observed: Double,
    val expected: Double
)
