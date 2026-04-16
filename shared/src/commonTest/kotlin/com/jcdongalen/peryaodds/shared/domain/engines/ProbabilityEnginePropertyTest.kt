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
 * Property-based tests for ProbabilityEngine using Kotest Property Testing.
 * Each property test runs with a minimum of 100 iterations.
 */
class ProbabilityEnginePropertyTest {
    
    private val engine = DefaultProbabilityEngine()
    
    private val testConfig = GameConfig(
        gameType = "three_ball_drop",
        displayName = "3-Ball Drop Card Game",
        outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
        hitsPerRound = 3,
        expectedProbability = 1.0 / 6.0,
        comingSoon = false
    )
    
    // Floating-point tolerance for probability comparisons
    private val EPSILON = 1e-9
    
    // Arbitrary for generating GameSession with totalRounds > 0
    private fun arbNonEmptyGameSession(): Arb<GameSession> = arbitrary { rs ->
        val totalRounds = Arb.int(1..1000).bind()
        val totalHits = totalRounds * testConfig.hitsPerRound
        
        // Generate hit counts that sum to exactly totalHits
        val hitCounts = mutableMapOf<String, Int>()
        var remainingHits = totalHits
        
        // Distribute hits across outcomes
        for ((index, outcome) in testConfig.outcomes.withIndex()) {
            val maxHitsForThisOutcome = if (index == testConfig.outcomes.size - 1) {
                // Last outcome gets all remaining hits
                remainingHits
            } else {
                // Other outcomes get a random portion
                remainingHits
            }
            
            val hits = if (maxHitsForThisOutcome > 0) {
                Arb.int(0..maxHitsForThisOutcome).bind()
            } else {
                0
            }
            
            if (hits > 0) {
                hitCounts[outcome] = hits
            }
            remainingHits -= hits
        }
        
        // Ensure we've distributed exactly totalHits
        val actualTotal = hitCounts.values.sum()
        if (actualTotal != totalHits && testConfig.outcomes.isNotEmpty()) {
            // Adjust the first outcome to make the sum exact
            val firstOutcome = testConfig.outcomes.first()
            val adjustment = totalHits - actualTotal
            hitCounts[firstOutcome] = (hitCounts[firstOutcome] ?: 0) + adjustment
        }
        
        GameSession(
            gameType = testConfig.gameType,
            totalRounds = totalRounds,
            hitCounts = hitCounts,
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    // Feature: perya-odds-mvp, Property 3
    @Test
    fun `Property 3 - Observed probability formula holds for any session`() {
        /**
         * **Validates: Requirements 3.1, 3.2**
         * 
         * For any GameSession with totalRounds > 0 and any outcome card,
         * the observed probability computed by ProbabilityEngine SHALL equal
         * hitCounts[card] / (totalRounds × hitsPerRound), and the expected
         * probability SHALL equal 1 / outcomes.length — and the sum of all
         * observed probabilities across all outcomes SHALL equal 1.0 within
         * floating-point tolerance.
         */
        runBlocking {
            checkAll(100, arbNonEmptyGameSession()) { session ->
                // Compute probabilities
                val result = engine.computeProbabilities(session, testConfig)
                
                // Property 3a: Verify observed probability formula for each outcome
                val totalHits = session.totalRounds * testConfig.hitsPerRound
                
                for (outcome in testConfig.outcomes) {
                    val hitCount = session.hitCounts[outcome] ?: 0
                    val expectedObserved = hitCount.toDouble() / totalHits
                    val actualObserved = result.perOutcome[outcome]?.observed ?: 0.0
                    
                    assertTrue(
                        abs(actualObserved - expectedObserved) < EPSILON,
                        "Observed probability for '$outcome' should be $expectedObserved but was $actualObserved " +
                        "(hitCount=$hitCount, totalHits=$totalHits, totalRounds=${session.totalRounds})"
                    )
                }
                
                // Property 3b: Verify expected probability formula for each outcome
                val expectedProbability = 1.0 / testConfig.outcomes.size
                
                for (outcome in testConfig.outcomes) {
                    val actualExpected = result.perOutcome[outcome]?.expected ?: 0.0
                    
                    assertTrue(
                        abs(actualExpected - expectedProbability) < EPSILON,
                        "Expected probability for '$outcome' should be $expectedProbability but was $actualExpected"
                    )
                }
                
                // Property 3c: Verify sum of all observed probabilities equals 1.0
                val sumOfObserved = result.perOutcome.values.sumOf { it.observed }
                
                assertTrue(
                    abs(sumOfObserved - 1.0) < EPSILON,
                    "Sum of all observed probabilities should be 1.0 but was $sumOfObserved " +
                    "(difference: ${abs(sumOfObserved - 1.0)}, session: totalRounds=${session.totalRounds}, " +
                    "hitCounts=${session.hitCounts})"
                )
                
                // Property 3d: Verify totalRounds in result matches session
                assertEquals(
                    session.totalRounds,
                    result.totalRounds,
                    "Result totalRounds should match session totalRounds"
                )
            }
        }
    }
    
    // Feature: perya-odds-mvp, Property 5
    @Test
    fun `Property 5 - Confidence level assignment is exhaustive and monotone`() {
        /**
         * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
         * 
         * For any non-negative integer totalRounds, ProbabilityEngine SHALL assign
         * exactly one ConfidenceLevel (Low for < 100, Medium for 100–199, High for
         * 200–499, Very High for ≥ 500), and the assigned level SHALL be monotonically
         * non-decreasing as totalRounds increases.
         */
        runBlocking {
            // Arbitrary for generating non-negative totalRounds
            val arbTotalRounds = Arb.int(0..10000)
            
            checkAll(100, arbTotalRounds) { totalRounds ->
                // Create a minimal session with the given totalRounds
                val session = GameSession(
                    gameType = testConfig.gameType,
                    totalRounds = totalRounds,
                    hitCounts = emptyMap(),
                    observations = emptyList(),
                    lastUpdated = System.currentTimeMillis()
                )
                
                // Compute probabilities
                val result = engine.computeProbabilities(session, testConfig)
                
                // Property 5a: Verify exactly one confidence level is assigned (exhaustive)
                val assignedLevel = result.confidenceLevel
                
                // Property 5b: Verify the assigned level matches the expected threshold
                val expectedLevel = when {
                    totalRounds < 100 -> ConfidenceLevel.Low
                    totalRounds in 100..199 -> ConfidenceLevel.Medium
                    totalRounds in 200..499 -> ConfidenceLevel.High
                    else -> ConfidenceLevel.VeryHigh
                }
                
                assertEquals(
                    expectedLevel,
                    assignedLevel,
                    "For totalRounds=$totalRounds, expected confidence level $expectedLevel but got $assignedLevel"
                )
            }
            
            // Property 5c: Verify monotonicity - confidence level is non-decreasing
            // Test specific boundary transitions to ensure monotonicity
            val testCases = listOf(
                0 to ConfidenceLevel.Low,
                50 to ConfidenceLevel.Low,
                99 to ConfidenceLevel.Low,
                100 to ConfidenceLevel.Medium,
                150 to ConfidenceLevel.Medium,
                199 to ConfidenceLevel.Medium,
                200 to ConfidenceLevel.High,
                300 to ConfidenceLevel.High,
                499 to ConfidenceLevel.High,
                500 to ConfidenceLevel.VeryHigh,
                1000 to ConfidenceLevel.VeryHigh,
                10000 to ConfidenceLevel.VeryHigh
            )
            
            var previousLevel: ConfidenceLevel? = null
            val levelOrder = listOf(
                ConfidenceLevel.Low,
                ConfidenceLevel.Medium,
                ConfidenceLevel.High,
                ConfidenceLevel.VeryHigh
            )
            
            for ((rounds, expectedLevel) in testCases) {
                val session = GameSession(
                    gameType = testConfig.gameType,
                    totalRounds = rounds,
                    hitCounts = emptyMap(),
                    observations = emptyList(),
                    lastUpdated = System.currentTimeMillis()
                )
                
                val result = engine.computeProbabilities(session, testConfig)
                val currentLevel = result.confidenceLevel
                
                // Verify expected level
                assertEquals(
                    expectedLevel,
                    currentLevel,
                    "Boundary test: For totalRounds=$rounds, expected $expectedLevel but got $currentLevel"
                )
                
                // Verify monotonicity: current level should be >= previous level
                if (previousLevel != null) {
                    val previousIndex = levelOrder.indexOf(previousLevel)
                    val currentIndex = levelOrder.indexOf(currentLevel)
                    
                    assertTrue(
                        currentIndex >= previousIndex,
                        "Monotonicity violation: confidence level decreased from $previousLevel to $currentLevel " +
                        "as totalRounds increased"
                    )
                }
                
                previousLevel = currentLevel
            }
        }
    }
}
