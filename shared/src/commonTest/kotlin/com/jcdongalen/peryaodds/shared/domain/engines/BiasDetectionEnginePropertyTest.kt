package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for BiasDetectionEngine using Kotest Property Testing.
 * Each property test runs with a minimum of 100 iterations.
 */
class BiasDetectionEnginePropertyTest {
    
    // Floating-point tolerance for deviation comparisons
    private val EPSILON = 1e-9
    
    // Arbitrary for generating valid ProbabilityResult
    private fun arbProbabilityResult(): Arb<ProbabilityResult> = arbitrary { rs ->
        val outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9")
        val totalRounds = Arb.int(0..1000).bind()
        
        // Generate observed probabilities that sum to 1.0
        val observedProbabilities = mutableMapOf<String, Double>()
        var remainingProbability = 1.0
        
        for ((index, outcome) in outcomes.withIndex()) {
            val prob = if (index == outcomes.size - 1) {
                // Last outcome gets remaining probability to ensure sum = 1.0
                remainingProbability.coerceIn(0.0, 1.0)
            } else {
                // Generate a random probability from remaining
                val maxProb = remainingProbability
                if (maxProb > 0.0) {
                    Arb.double(0.0..maxProb).bind()
                } else {
                    0.0
                }
            }
            observedProbabilities[outcome] = prob
            remainingProbability -= prob
        }
        
        // Expected probability is uniform for all outcomes
        val expectedProbability = 1.0 / outcomes.size
        
        // Build perOutcome map
        val perOutcome = outcomes.associateWith { outcome ->
            OutcomeProbability(
                observed = observedProbabilities[outcome] ?: 0.0,
                expected = expectedProbability
            )
        }
        
        // Generate confidence level
        val confidenceLevel = Arb.enum<ConfidenceLevel>().bind()
        
        ProbabilityResult(
            perOutcome = perOutcome,
            confidenceLevel = confidenceLevel,
            totalRounds = totalRounds
        )
    }
    
    // Feature: perya-odds-mvp, Property 4
    @Test
    fun `Property 4 - Deviation formula and bias classification are consistent`() {
        /**
         * **Validates: Requirements 4.1, 4.2, 4.3, 4.4**
         * 
         * For any ProbabilityResult, the BiasDetectionEngine SHALL compute
         * deviation(card) = (observed − expected) / expected for each outcome,
         * and classify it as Hot when deviation > 0, Cold when deviation < 0,
         * and Neutral when deviation = 0.
         */
        runBlocking {
            checkAll(100, arbProbabilityResult()) { probResult ->
                // Detect bias
                val biasResult = BiasDetectionEngine.detectBias(probResult)
                
                // Verify for each outcome
                for ((outcome, outcomeProbability) in probResult.perOutcome) {
                    val observed = outcomeProbability.observed
                    val expected = outcomeProbability.expected
                    
                    // Property 4a: Verify deviation formula
                    val expectedDeviation = if (expected != 0.0) {
                        (observed - expected) / expected
                    } else {
                        0.0
                    }
                    
                    val outcomeBias = biasResult.perOutcome[outcome]
                    assertTrue(
                        outcomeBias != null,
                        "BiasResult should contain outcome '$outcome'"
                    )
                    
                    assertTrue(
                        abs(outcomeBias.deviation - expectedDeviation) < EPSILON,
                        "Deviation for '$outcome' should be $expectedDeviation but was ${outcomeBias.deviation} " +
                        "(observed=$observed, expected=$expected)"
                    )
                    
                    // Property 4b, 4c, 4d: Verify classification consistency
                    val expectedClassification = when {
                        outcomeBias.deviation > 0.0 -> BiasClassification.Hot
                        outcomeBias.deviation < 0.0 -> BiasClassification.Cold
                        else -> BiasClassification.Neutral
                    }
                    
                    assertEquals(
                        expectedClassification,
                        outcomeBias.classification,
                        "Classification for '$outcome' with deviation ${outcomeBias.deviation} " +
                        "should be $expectedClassification but was ${outcomeBias.classification}"
                    )
                    
                    // Additional verification: ensure classification matches deviation sign
                    when (outcomeBias.classification) {
                        BiasClassification.Hot -> {
                            assertTrue(
                                outcomeBias.deviation > 0.0 || abs(outcomeBias.deviation) < EPSILON,
                                "Hot classification requires deviation > 0, but got ${outcomeBias.deviation}"
                            )
                        }
                        BiasClassification.Cold -> {
                            assertTrue(
                                outcomeBias.deviation < 0.0 || abs(outcomeBias.deviation) < EPSILON,
                                "Cold classification requires deviation < 0, but got ${outcomeBias.deviation}"
                            )
                        }
                        BiasClassification.Neutral -> {
                            assertTrue(
                                abs(outcomeBias.deviation) < EPSILON,
                                "Neutral classification requires deviation ≈ 0, but got ${outcomeBias.deviation}"
                            )
                        }
                    }
                }
                
                // Verify all outcomes from probResult are present in biasResult
                assertEquals(
                    probResult.perOutcome.keys,
                    biasResult.perOutcome.keys,
                    "BiasResult should contain exactly the same outcomes as ProbabilityResult"
                )
            }
        }
    }
}
