package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for StrategyEngine using Kotest Property Testing.
 * Each property test runs with a minimum of 100 iterations.
 */
class StrategyEnginePropertyTest {
    
    private val testConfig = GameConfig(
        gameType = "three_ball_drop",
        displayName = "3-Ball Drop Card Game",
        outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
        hitsPerRound = 3,
        expectedProbability = 1.0 / 6.0,
        comingSoon = false
    )
    
    private val probabilityEngine = DefaultProbabilityEngine()
    private val strategyEngine = DefaultStrategyEngine(probabilityEngine)
    
    // Arbitrary for generating GameSession with varying states
    private fun arbGameSession(): Arb<GameSession> = arbitrary { rs ->
        val totalRounds = Arb.int(0..200).bind()
        val hitCounts = if (totalRounds == 0) {
            emptyMap()
        } else {
            // Generate realistic hit counts that sum to totalRounds * hitsPerRound
            val totalHits = totalRounds * testConfig.hitsPerRound
            val counts = mutableMapOf<String, Int>()
            var remaining = totalHits
            
            // Distribute hits across outcomes
            testConfig.outcomes.forEachIndexed { index, outcome ->
                if (index == testConfig.outcomes.size - 1) {
                    // Last outcome gets all remaining hits
                    if (remaining > 0) {
                        counts[outcome] = remaining
                    }
                } else {
                    // Random distribution for other outcomes
                    val maxForThisOutcome = remaining - (testConfig.outcomes.size - index - 1)
                    val hits = if (maxForThisOutcome > 0) {
                        Arb.int(0..maxForThisOutcome).bind()
                    } else {
                        0
                    }
                    if (hits > 0) {
                        counts[outcome] = hits
                        remaining -= hits
                    }
                }
            }
            
            counts
        }
        
        GameSession(
            gameType = testConfig.gameType,
            totalRounds = totalRounds,
            hitCounts = hitCounts,
            observations = emptyList(), // Simplified for property testing
            lastUpdated = Arb.long(0L..Long.MAX_VALUE).bind()
        )
    }
    
    // Arbitrary for strategy mode (1, 2, or 3)
    private fun arbMode(): Arb<Int> = Arb.of(1, 2, 3)
    
    // Feature: perya-odds-mvp, Property 6
    @Test
    fun `Property 6 - Strategy top-N selection is correct for any session and mode`() {
        /**
         * **Validates: Requirements 6.2, 6.5, 7.2, 8.2**
         * 
         * For any GameSession and strategy mode N ∈ {1, 2, 3}, the N cards returned
         * by StrategyEngine.recommend SHALL be the N cards with the highest observed
         * probability in the dataset, with ties broken by canonical order
         * (Ace, King, Queen, Jack, 10, 9).
         */
        runBlocking {
            checkAll(100, arbGameSession(), arbMode()) { session, mode ->
                // Get the strategy recommendation
                val result = strategyEngine.recommend(session, testConfig, mode)
                
                // Property 6a: The result should contain exactly N selected cards
                assertEquals(
                    mode,
                    result.selectedCards.size,
                    "Strategy should select exactly $mode cards for mode $mode"
                )
                
                // Property 6b: All selected cards should be valid outcomes
                assertTrue(
                    result.selectedCards.all { it in testConfig.outcomes },
                    "All selected cards should be valid outcomes from the game config"
                )
                
                // Property 6c: Selected cards should be the top-N by observed probability
                // Compute observed probabilities for all outcomes
                val probResult = probabilityEngine.computeProbabilities(session, testConfig)
                val observedProbabilities = testConfig.outcomes.associateWith { outcome ->
                    probResult.perOutcome[outcome]?.observed ?: testConfig.expectedProbability
                }
                
                // Sort outcomes by observed probability descending, then by canonical order
                val canonicalOrder = testConfig.outcomes.withIndex().associate { it.value to it.index }
                val expectedTopN = testConfig.outcomes
                    .sortedWith(
                        compareByDescending<String> { observedProbabilities[it] ?: 0.0 }
                            .thenBy { canonicalOrder[it] ?: Int.MAX_VALUE }
                    )
                    .take(mode)
                
                assertEquals(
                    expectedTopN,
                    result.selectedCards,
                    "Selected cards should be the top-$mode cards by observed probability, " +
                    "with ties broken by canonical order. " +
                    "Probabilities: ${observedProbabilities.entries.sortedByDescending { it.value }}"
                )
                
                // Property 6d: Selected cards should maintain canonical order when tied
                // Check that if any two consecutive selected cards have the same probability,
                // they appear in canonical order
                for (i in 0 until result.selectedCards.size - 1) {
                    val card1 = result.selectedCards[i]
                    val card2 = result.selectedCards[i + 1]
                    val prob1 = observedProbabilities[card1] ?: 0.0
                    val prob2 = observedProbabilities[card2] ?: 0.0
                    
                    if (prob1 == prob2) {
                        val index1 = canonicalOrder[card1] ?: Int.MAX_VALUE
                        val index2 = canonicalOrder[card2] ?: Int.MAX_VALUE
                        assertTrue(
                            index1 < index2,
                            "When cards have equal probability ($prob1), they should appear in canonical order. " +
                            "Found $card1 (index $index1) before $card2 (index $index2)"
                        )
                    }
                }
                
                // Property 6e: Individual probabilities map should match selected cards
                assertEquals(
                    result.selectedCards.toSet(),
                    result.individualProbabilities.keys,
                    "Individual probabilities map should contain exactly the selected cards"
                )
                
                // Property 6f: Individual probabilities should match computed observed probabilities
                result.selectedCards.forEach { card ->
                    val expectedProb = observedProbabilities[card] ?: testConfig.expectedProbability
                    val actualProb = result.individualProbabilities[card] ?: 0.0
                    assertEquals(
                        expectedProb,
                        actualProb,
                        0.0001,
                        "Individual probability for $card should match observed probability"
                    )
                }
                
                // Property 6g: Mode should be preserved in result
                assertEquals(
                    mode,
                    result.mode,
                    "Result should preserve the requested mode"
                )
            }
        }
    }
    
    // Feature: perya-odds-mvp, Property 7
    @Test
    fun `Property 7 - Win probability formula is correctly applied for any mode`() {
        /**
         * **Validates: Requirements 6.3, 7.3, 8.3**
         * 
         * For any strategy result with N selected cards and their observed probabilities
         * p₁…pN, the winProbability SHALL equal 1 − (1 − (p₁ + p₂ + ... + pN))³
         * within floating-point tolerance.
         */
        runBlocking {
            checkAll(100, arbGameSession(), arbMode()) { session, mode ->
                // Get the strategy recommendation
                val result = strategyEngine.recommend(session, testConfig, mode)
                
                // Property 7a: Win probability should be computed using the correct formula
                // Formula: winProbability = 1 − (1 − (p₁ + p₂ + ... + pN))³
                val sumOfSelectedProbabilities = result.individualProbabilities.values.sum()
                val expectedWinProbability = 1.0 - (1.0 - sumOfSelectedProbabilities).pow(3.0)
                
                assertEquals(
                    expectedWinProbability,
                    result.winProbability,
                    0.0001,
                    "Win probability should equal 1 − (1 − sum)³ where sum = ${sumOfSelectedProbabilities}. " +
                    "Expected: $expectedWinProbability, Actual: ${result.winProbability}"
                )
                
                // Property 7b: Win probability should be in valid range [0, 1]
                assertTrue(
                    result.winProbability >= 0.0 && result.winProbability <= 1.0,
                    "Win probability should be in range [0, 1], got ${result.winProbability}"
                )
                
                // Property 7c: Win probability should increase monotonically with sum of probabilities
                // (This is a mathematical property of the formula)
                // If sum > 0, then winProbability > 0
                if (sumOfSelectedProbabilities > 0.0) {
                    assertTrue(
                        result.winProbability > 0.0,
                        "Win probability should be positive when sum of probabilities is positive"
                    )
                }
                
                // Property 7d: Win probability should equal sum when sum is small
                // For small values, 1 - (1-x)³ ≈ 3x - 3x² + x³ ≈ 3x (first-order approximation)
                // But we verify the exact formula instead
                val manualCalculation = 1.0 - (1.0 - sumOfSelectedProbabilities).pow(3.0)
                assertEquals(
                    manualCalculation,
                    result.winProbability,
                    0.0001,
                    "Win probability should match manual calculation of the formula"
                )
                
                // Property 7e: Win probability should be consistent across all modes
                // The formula should work the same way regardless of mode (1, 2, or 3)
                // This is implicitly tested by running the property across all modes
                assertTrue(
                    result.mode in 1..3,
                    "Mode should be valid (1, 2, or 3)"
                )
            }
        }
    }
}
