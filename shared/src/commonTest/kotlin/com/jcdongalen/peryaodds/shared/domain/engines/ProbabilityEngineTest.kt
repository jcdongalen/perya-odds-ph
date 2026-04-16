package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*
import kotlin.math.abs
import kotlin.test.*

/**
 * Unit tests for ProbabilityEngine.
 * Tests specific scenarios and edge cases for probability computation and confidence level assignment.
 * 
 * Requirements: 3.4, 5.1, 5.2, 5.3, 5.4
 */
class ProbabilityEngineTest {
    
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
    
    @Test
    fun `zero-round edge case returns expected defaults`() {
        // Requirement 3.4: If the Dataset contains 0 observations, return default expected probability
        val emptySession = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 0,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.computeProbabilities(emptySession, testConfig)
        
        // Verify totalRounds is 0
        assertEquals(0, result.totalRounds, "Total rounds should be 0 for empty session")
        
        // Verify confidence level is Low for 0 rounds
        assertEquals(ConfidenceLevel.Low, result.confidenceLevel, "Confidence level should be Low for 0 rounds")
        
        // Verify all outcomes have default expected probability
        for (outcome in testConfig.outcomes) {
            val outcomeProbability = result.perOutcome[outcome]
            assertNotNull(outcomeProbability, "Outcome '$outcome' should have probability data")
            
            // Both observed and expected should equal the default expected probability (1/6)
            assertTrue(
                abs(outcomeProbability.observed - testConfig.expectedProbability) < EPSILON,
                "Observed probability for '$outcome' should be ${testConfig.expectedProbability} but was ${outcomeProbability.observed}"
            )
            
            assertTrue(
                abs(outcomeProbability.expected - testConfig.expectedProbability) < EPSILON,
                "Expected probability for '$outcome' should be ${testConfig.expectedProbability} but was ${outcomeProbability.expected}"
            )
        }
    }
    
    @Test
    fun `confidence level boundary at 99 rounds is Low`() {
        // Requirement 5.1: < 100 observations → Low
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 99,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.computeProbabilities(session, testConfig)
        
        assertEquals(ConfidenceLevel.Low, result.confidenceLevel, "99 rounds should have Low confidence level")
    }
    
    @Test
    fun `confidence level boundary at 100 rounds is Medium`() {
        // Requirement 5.2: 100–199 observations → Medium
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 100,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.computeProbabilities(session, testConfig)
        
        assertEquals(ConfidenceLevel.Medium, result.confidenceLevel, "100 rounds should have Medium confidence level")
    }
    
    @Test
    fun `confidence level boundary at 199 rounds is Medium`() {
        // Requirement 5.2: 100–199 observations → Medium
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 199,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.computeProbabilities(session, testConfig)
        
        assertEquals(ConfidenceLevel.Medium, result.confidenceLevel, "199 rounds should have Medium confidence level")
    }
    
    @Test
    fun `confidence level boundary at 200 rounds is High`() {
        // Requirement 5.3: 200–499 observations → High
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 200,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.computeProbabilities(session, testConfig)
        
        assertEquals(ConfidenceLevel.High, result.confidenceLevel, "200 rounds should have High confidence level")
    }
    
    @Test
    fun `confidence level boundary at 499 rounds is High`() {
        // Requirement 5.3: 200–499 observations → High
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 499,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.computeProbabilities(session, testConfig)
        
        assertEquals(ConfidenceLevel.High, result.confidenceLevel, "499 rounds should have High confidence level")
    }
    
    @Test
    fun `confidence level boundary at 500 rounds is VeryHigh`() {
        // Requirement 5.4: ≥ 500 observations → Very High
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 500,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.computeProbabilities(session, testConfig)
        
        assertEquals(ConfidenceLevel.VeryHigh, result.confidenceLevel, "500 rounds should have VeryHigh confidence level")
    }
}
