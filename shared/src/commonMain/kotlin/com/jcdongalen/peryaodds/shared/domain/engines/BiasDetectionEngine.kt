package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.BiasClassification
import com.jcdongalen.peryaodds.shared.domain.models.BiasResult
import com.jcdongalen.peryaodds.shared.domain.models.OutcomeBias
import com.jcdongalen.peryaodds.shared.domain.models.ProbabilityResult

/**
 * Engine responsible for detecting statistical bias in game outcomes.
 * 
 * Classifies each outcome as Hot, Cold, or Neutral based on deviation
 * from expected probability.
 */
object BiasDetectionEngine {
    
    /**
     * Detects bias for each outcome in the probability result.
     * 
     * For each outcome:
     * - Computes deviation = (observed - expected) / expected
     * - Classifies as:
     *   - Hot if deviation > 0
     *   - Cold if deviation < 0
     *   - Neutral if deviation == 0
     * 
     * @param probResult The probability result containing observed and expected probabilities
     * @return BiasResult containing deviation and classification for each outcome
     */
    fun detectBias(probResult: ProbabilityResult): BiasResult {
        val perOutcome = probResult.perOutcome.mapValues { (_, outcomeProbability) ->
            val observed = outcomeProbability.observed
            val expected = outcomeProbability.expected
            
            // Compute deviation: (observed - expected) / expected
            val deviation = if (expected != 0.0) {
                (observed - expected) / expected
            } else {
                0.0
            }
            
            // Classify based on deviation
            val classification = when {
                deviation > 0.0 -> BiasClassification.Hot
                deviation < 0.0 -> BiasClassification.Cold
                else -> BiasClassification.Neutral
            }
            
            OutcomeBias(
                deviation = deviation,
                classification = classification
            )
        }
        
        return BiasResult(perOutcome = perOutcome)
    }
}
