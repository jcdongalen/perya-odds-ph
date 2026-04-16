package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*
import kotlin.math.pow

interface StrategyEngine {
    fun recommend(session: GameSession, config: GameConfig, mode: Int): StrategyResult
}

class DefaultStrategyEngine(
    private val probabilityEngine: ProbabilityEngine
) : StrategyEngine {
    override fun recommend(session: GameSession, config: GameConfig, mode: Int): StrategyResult {
        // Compute probabilities using ProbabilityEngine
        val probResult = probabilityEngine.computeProbabilities(session, config)
        
        // Sort outcomes by observed probability descending, break ties by canonical order index
        val sortedOutcomes = config.outcomes
            .mapIndexed { index, outcome ->
                Triple(outcome, probResult.perOutcome[outcome]?.observed ?: 0.0, index)
            }
            .sortedWith(compareByDescending<Triple<String, Double, Int>> { it.second }
                .thenBy { it.third })
        
        // Select top-N cards for mode N ∈ {1, 2, 3}
        val selectedCards = sortedOutcomes.take(mode).map { it.first }
        
        // Build individual probabilities map for selected cards
        val individualProbabilities = selectedCards.associateWith { card ->
            probResult.perOutcome[card]?.observed ?: config.expectedProbability
        }
        
        // Compute sum of selected probabilities
        val sumOfSelectedProbabilities = individualProbabilities.values.sum()
        
        // Compute winProbability = 1.0 − (1.0 − sumOfSelectedProbabilities).pow(3)
        val winProbability = 1.0 - (1.0 - sumOfSelectedProbabilities).pow(3)
        
        return StrategyResult(
            selectedCards = selectedCards,
            individualProbabilities = individualProbabilities,
            winProbability = winProbability,
            confidenceLevel = probResult.confidenceLevel,
            mode = mode
        )
    }
}
