package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.*

/**
 * Unit tests for StrategyEngine.
 * Tests specific scenarios for strategy recommendation logic.
 * 
 * Requirements: 6.2, 6.3, 6.5, 7.2, 7.3, 8.2, 8.3
 */
class StrategyEngineTest {
    
    private val probabilityEngine = DefaultProbabilityEngine()
    private val engine = DefaultStrategyEngine(probabilityEngine)
    
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
    fun `tie-breaking by canonical order when probabilities are equal`() {
        // Requirement 6.5: Ties broken by canonical order (Ace, King, Queen, Jack, 10, 9)
        // Create a session where all cards have equal observed probability
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 6,
            hitCounts = mapOf(
                "Ace" to 3,
                "King" to 3,
                "Queen" to 3,
                "Jack" to 3,
                "10" to 3,
                "9" to 3
            ),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        // Test mode 1: should select Ace (first in canonical order)
        val result1 = engine.recommend(session, testConfig, 1)
        assertEquals(1, result1.selectedCards.size, "Mode 1 should select 1 card")
        assertEquals("Ace", result1.selectedCards[0], "Should select Ace when all probabilities are equal")
        
        // Test mode 2: should select Ace and King (first two in canonical order)
        val result2 = engine.recommend(session, testConfig, 2)
        assertEquals(2, result2.selectedCards.size, "Mode 2 should select 2 cards")
        assertEquals(listOf("Ace", "King"), result2.selectedCards, "Should select Ace and King in canonical order")
        
        // Test mode 3: should select Ace, King, and Queen (first three in canonical order)
        val result3 = engine.recommend(session, testConfig, 3)
        assertEquals(3, result3.selectedCards.size, "Mode 3 should select 3 cards")
        assertEquals(listOf("Ace", "King", "Queen"), result3.selectedCards, "Should select Ace, King, Queen in canonical order")
    }
    
    @Test
    fun `tie-breaking with partial ties`() {
        // Requirement 6.5: Ties broken by canonical order
        // Create a session where some cards have equal probability
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 10,
            hitCounts = mapOf(
                "Ace" to 6,    // 0.20 probability (highest)
                "King" to 4,   // 0.133 probability (tied)
                "Queen" to 4,  // 0.133 probability (tied)
                "Jack" to 4,   // 0.133 probability (tied)
                "10" to 4,     // 0.133 probability (tied)
                "9" to 8       // 0.267 probability (second highest)
            ),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        // Mode 2: should select 9 (highest) and Ace (second highest)
        val result2 = engine.recommend(session, testConfig, 2)
        assertEquals(2, result2.selectedCards.size)
        assertEquals(listOf("9", "Ace"), result2.selectedCards, "Should select highest probabilities")
        
        // Mode 3: should select 9, Ace, and King (first of the tied cards in canonical order)
        val result3 = engine.recommend(session, testConfig, 3)
        assertEquals(3, result3.selectedCards.size)
        assertEquals(listOf("9", "Ace", "King"), result3.selectedCards, "Should break tie by canonical order")
    }
    
    @Test
    fun `empty dataset fallback to expected probability`() {
        // Requirement 6.2, 6.3: Fall back to expected probability (1/6) when dataset is empty
        val emptySession = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 0,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        // Test mode 1
        val result1 = engine.recommend(emptySession, testConfig, 1)
        assertEquals(1, result1.selectedCards.size)
        assertEquals("Ace", result1.selectedCards[0], "Should select first card in canonical order for empty dataset")
        
        // Verify individual probability is expected probability (1/6)
        val aceProb1 = result1.individualProbabilities["Ace"]
        assertNotNull(aceProb1)
        assertTrue(
            abs(aceProb1 - testConfig.expectedProbability) < EPSILON,
            "Individual probability should be expected probability (1/6) for empty dataset"
        )
        
        // Test mode 2
        val result2 = engine.recommend(emptySession, testConfig, 2)
        assertEquals(2, result2.selectedCards.size)
        assertEquals(listOf("Ace", "King"), result2.selectedCards, "Should select first two cards in canonical order")
        
        // Verify all individual probabilities are expected probability
        result2.individualProbabilities.values.forEach { prob ->
            assertTrue(
                abs(prob - testConfig.expectedProbability) < EPSILON,
                "All individual probabilities should be expected probability (1/6)"
            )
        }
        
        // Test mode 3
        val result3 = engine.recommend(emptySession, testConfig, 3)
        assertEquals(3, result3.selectedCards.size)
        assertEquals(listOf("Ace", "King", "Queen"), result3.selectedCards, "Should select first three cards in canonical order")
    }
    
    @Test
    fun `win probability formula for mode 1`() {
        // Requirement 6.3, 7.3: winProbability = 1 − (1 − p)³
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 10,
            hitCounts = mapOf(
                "Ace" to 6  // 6 / (10 * 3) = 0.20 probability
            ),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.recommend(session, testConfig, 1)
        
        assertEquals("Ace", result.selectedCards[0])
        
        val aceProb = result.individualProbabilities["Ace"]
        assertNotNull(aceProb)
        assertTrue(abs(aceProb - 0.20) < EPSILON, "Ace probability should be 0.20")
        
        // Compute expected win probability: 1 - (1 - 0.20)³ = 1 - 0.80³ = 1 - 0.512 = 0.488
        val expectedWinProb = 1.0 - (1.0 - 0.20).pow(3)
        assertTrue(
            abs(result.winProbability - expectedWinProb) < EPSILON,
            "Win probability should be 1 - (1 - 0.20)³ = ${expectedWinProb}, but was ${result.winProbability}"
        )
    }
    
    @Test
    fun `win probability formula for mode 2`() {
        // Requirement 7.3: winProbability = 1 − (1 − (p1 + p2))³
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 10,
            hitCounts = mapOf(
                "Ace" to 6,   // 6 / 30 = 0.20
                "King" to 9   // 9 / 30 = 0.30
            ),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.recommend(session, testConfig, 2)
        
        assertEquals(2, result.selectedCards.size)
        assertTrue(result.selectedCards.contains("Ace"))
        assertTrue(result.selectedCards.contains("King"))
        
        val aceProb = result.individualProbabilities["Ace"]
        val kingProb = result.individualProbabilities["King"]
        assertNotNull(aceProb)
        assertNotNull(kingProb)
        
        // Compute expected win probability: 1 - (1 - (0.20 + 0.30))³ = 1 - 0.50³ = 1 - 0.125 = 0.875
        val sumProb = aceProb + kingProb
        val expectedWinProb = 1.0 - (1.0 - sumProb).pow(3)
        assertTrue(
            abs(result.winProbability - expectedWinProb) < EPSILON,
            "Win probability should be 1 - (1 - ${sumProb})³ = ${expectedWinProb}, but was ${result.winProbability}"
        )
    }
    
    @Test
    fun `win probability formula for mode 3`() {
        // Requirement 8.3: winProbability = 1 − (1 − (p1 + p2 + p3))³
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 10,
            hitCounts = mapOf(
                "Ace" to 6,    // 6 / 30 = 0.20
                "King" to 9,   // 9 / 30 = 0.30
                "Queen" to 3   // 3 / 30 = 0.10
            ),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result = engine.recommend(session, testConfig, 3)
        
        assertEquals(3, result.selectedCards.size)
        assertTrue(result.selectedCards.contains("Ace"))
        assertTrue(result.selectedCards.contains("King"))
        assertTrue(result.selectedCards.contains("Queen"))
        
        val aceProb = result.individualProbabilities["Ace"]
        val kingProb = result.individualProbabilities["King"]
        val queenProb = result.individualProbabilities["Queen"]
        assertNotNull(aceProb)
        assertNotNull(kingProb)
        assertNotNull(queenProb)
        
        // Compute expected win probability: 1 - (1 - (0.20 + 0.30 + 0.10))³ = 1 - 0.40³ = 1 - 0.064 = 0.936
        val sumProb = aceProb + kingProb + queenProb
        val expectedWinProb = 1.0 - (1.0 - sumProb).pow(3)
        assertTrue(
            abs(result.winProbability - expectedWinProb) < EPSILON,
            "Win probability should be 1 - (1 - ${sumProb})³ = ${expectedWinProb}, but was ${result.winProbability}"
        )
    }
    
    @Test
    fun `win probability with empty dataset uses expected probability`() {
        // Verify win probability calculation works correctly with fallback expected probabilities
        val emptySession = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 0,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        // Mode 1: 1 card with expected probability 1/6
        val result1 = engine.recommend(emptySession, testConfig, 1)
        val expectedWinProb1 = 1.0 - (1.0 - (1.0/6.0)).pow(3)
        assertTrue(
            abs(result1.winProbability - expectedWinProb1) < EPSILON,
            "Mode 1 win probability with empty dataset should be ${expectedWinProb1}"
        )
        
        // Mode 2: 2 cards with expected probability 1/6 each
        val result2 = engine.recommend(emptySession, testConfig, 2)
        val expectedWinProb2 = 1.0 - (1.0 - (2.0/6.0)).pow(3)
        assertTrue(
            abs(result2.winProbability - expectedWinProb2) < EPSILON,
            "Mode 2 win probability with empty dataset should be ${expectedWinProb2}"
        )
        
        // Mode 3: 3 cards with expected probability 1/6 each
        val result3 = engine.recommend(emptySession, testConfig, 3)
        val expectedWinProb3 = 1.0 - (1.0 - (3.0/6.0)).pow(3)
        assertTrue(
            abs(result3.winProbability - expectedWinProb3) < EPSILON,
            "Mode 3 win probability with empty dataset should be ${expectedWinProb3}"
        )
    }
    
    @Test
    fun `strategy selects top N cards by observed probability`() {
        // Requirement 6.2, 7.2, 8.2: Select top-N cards by observed probability
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 20,
            hitCounts = mapOf(
                "Ace" to 15,   // 15 / 60 = 0.25 (highest)
                "King" to 12,  // 12 / 60 = 0.20 (second)
                "Queen" to 9,  // 9 / 60 = 0.15 (third)
                "Jack" to 6,   // 6 / 60 = 0.10 (fourth)
                "10" to 3,     // 3 / 60 = 0.05 (fifth)
                "9" to 15      // 15 / 60 = 0.25 (tied for highest, but comes after Ace in canonical order)
            ),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        // Mode 1: should select Ace (tied highest, but first in canonical order)
        val result1 = engine.recommend(session, testConfig, 1)
        assertEquals(listOf("Ace"), result1.selectedCards)
        
        // Mode 2: should select Ace and 9 (both have 0.25 probability)
        val result2 = engine.recommend(session, testConfig, 2)
        assertEquals(2, result2.selectedCards.size)
        assertTrue(result2.selectedCards.contains("Ace"))
        assertTrue(result2.selectedCards.contains("9"))
        
        // Mode 3: should select Ace, 9, and King (top 3 by probability)
        val result3 = engine.recommend(session, testConfig, 3)
        assertEquals(3, result3.selectedCards.size)
        assertTrue(result3.selectedCards.contains("Ace"))
        assertTrue(result3.selectedCards.contains("9"))
        assertTrue(result3.selectedCards.contains("King"))
    }
    
    @Test
    fun `confidence level is passed through from probability engine`() {
        // Verify that confidence level from ProbabilityEngine is correctly included in StrategyResult
        val sessionLow = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 50,
            hitCounts = mapOf("Ace" to 10),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val resultLow = engine.recommend(sessionLow, testConfig, 1)
        assertEquals(ConfidenceLevel.Low, resultLow.confidenceLevel, "Should have Low confidence for 50 rounds")
        
        val sessionMedium = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 150,
            hitCounts = mapOf("Ace" to 30),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val resultMedium = engine.recommend(sessionMedium, testConfig, 1)
        assertEquals(ConfidenceLevel.Medium, resultMedium.confidenceLevel, "Should have Medium confidence for 150 rounds")
        
        val sessionHigh = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 300,
            hitCounts = mapOf("Ace" to 60),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val resultHigh = engine.recommend(sessionHigh, testConfig, 1)
        assertEquals(ConfidenceLevel.High, resultHigh.confidenceLevel, "Should have High confidence for 300 rounds")
        
        val sessionVeryHigh = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 600,
            hitCounts = mapOf("Ace" to 120),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val resultVeryHigh = engine.recommend(sessionVeryHigh, testConfig, 1)
        assertEquals(ConfidenceLevel.VeryHigh, resultVeryHigh.confidenceLevel, "Should have VeryHigh confidence for 600 rounds")
    }
    
    @Test
    fun `mode is correctly set in result`() {
        val session = GameSession(
            gameType = testConfig.gameType,
            totalRounds = 10,
            hitCounts = mapOf("Ace" to 6),
            observations = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        
        val result1 = engine.recommend(session, testConfig, 1)
        assertEquals(1, result1.mode, "Mode should be 1")
        
        val result2 = engine.recommend(session, testConfig, 2)
        assertEquals(2, result2.mode, "Mode should be 2")
        
        val result3 = engine.recommend(session, testConfig, 3)
        assertEquals(3, result3.mode, "Mode should be 3")
    }
}
