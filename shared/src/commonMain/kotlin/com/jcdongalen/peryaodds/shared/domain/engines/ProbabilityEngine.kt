package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*

interface ProbabilityEngine {
    fun computeProbabilities(session: GameSession, config: GameConfig): ProbabilityResult
}

class DefaultProbabilityEngine : ProbabilityEngine {
    override fun computeProbabilities(session: GameSession, config: GameConfig): ProbabilityResult {
        val totalRounds = session.totalRounds
        val confidenceLevel = assignConfidenceLevel(totalRounds)
        
        // Return default expected probabilities when totalRounds == 0
        if (totalRounds == 0) {
            val perOutcome = config.outcomes.associateWith { outcome ->
                OutcomeProbability(
                    observed = config.expectedProbability,
                    expected = config.expectedProbability
                )
            }
            return ProbabilityResult(
                perOutcome = perOutcome,
                confidenceLevel = confidenceLevel,
                totalRounds = totalRounds
            )
        }
        
        // Compute observed and expected probabilities for each outcome
        val totalHits = totalRounds * config.hitsPerRound
        val perOutcome = config.outcomes.associateWith { outcome ->
            val hitCount = session.hitCounts[outcome] ?: 0
            val observed = hitCount.toDouble() / totalHits
            OutcomeProbability(
                observed = observed,
                expected = config.expectedProbability
            )
        }
        
        return ProbabilityResult(
            perOutcome = perOutcome,
            confidenceLevel = confidenceLevel,
            totalRounds = totalRounds
        )
    }
    
    private fun assignConfidenceLevel(totalRounds: Int): ConfidenceLevel {
        return when {
            totalRounds < 100 -> ConfidenceLevel.Low
            totalRounds in 100..199 -> ConfidenceLevel.Medium
            totalRounds in 200..499 -> ConfidenceLevel.High
            else -> ConfidenceLevel.VeryHigh
        }
    }
}
