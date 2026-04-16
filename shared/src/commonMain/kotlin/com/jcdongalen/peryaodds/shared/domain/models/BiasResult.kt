package com.jcdongalen.peryaodds.shared.domain.models

data class BiasResult(
    val perOutcome: Map<String, OutcomeBias>
)

data class OutcomeBias(
    val deviation: Double,
    val classification: BiasClassification
)

enum class BiasClassification {
    Hot, Cold, Neutral
}
