package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for ObservationEngine using Kotest Property Testing.
 * Each property test runs with a minimum of 100 iterations.
 */
class ObservationEnginePropertyTest {
    
    private val testConfig = GameConfig(
        gameType = "three_ball_drop",
        displayName = "3-Ball Drop Card Game",
        outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
        hitsPerRound = 3,
        expectedProbability = 1.0 / 6.0,
        comingSoon = false
    )
    
    // Arbitrary for generating valid card names from the game config
    private fun arbCard(): Arb<String> = Arb.of(testConfig.outcomes)
    
    // Arbitrary for generating valid hits arrays (exactly hitsPerRound cards)
    private fun arbValidHits(): Arb<List<String>> = 
        Arb.list(arbCard(), testConfig.hitsPerRound..testConfig.hitsPerRound)
    
    // Arbitrary for generating GameSession with varying states
    private fun arbGameSession(): Arb<GameSession> = arbitrary { rs ->
        val totalRounds = Arb.int(0..100).bind()
        val hitCounts = if (totalRounds == 0) {
            emptyMap()
        } else {
            testConfig.outcomes.associateWith { 
                Arb.int(0..totalRounds * testConfig.hitsPerRound).bind()
            }.filterValues { it > 0 }
        }
        
        GameSession(
            gameType = testConfig.gameType,
            totalRounds = totalRounds,
            hitCounts = hitCounts,
            observations = emptyList(), // Simplified for property testing
            lastUpdated = Arb.long(0L..Long.MAX_VALUE).bind()
        )
    }
    
    // Feature: perya-odds-mvp, Property 1
    @Test
    fun `Property 1 - Observation recording updates hit counts and round count correctly`() {
        /**
         * **Validates: Requirements 2.2, 2.6**
         * 
         * For any GameSession and any valid hits array of length hitsPerRound,
         * after calling ObservationEngine.recordObservation, the resulting session's
         * totalRounds SHALL equal the previous totalRounds + 1, and each card's
         * hitCount SHALL increase by exactly the number of times that card appears
         * in the hits array, with all other cards' hit counts unchanged.
         */
        runBlocking {
            checkAll(100, arbGameSession(), arbValidHits()) { session, hits ->
            // Record the observation
            val result = ObservationEngine.recordObservation(session, testConfig, hits)
            
            // Verify result is successful
            assertTrue(result is Result.Success, "Recording valid observation should succeed")
            val updatedSession = (result as Result.Success).value
            
            // Property 1a: totalRounds increments by exactly 1
            assertEquals(
                session.totalRounds + 1,
                updatedSession.totalRounds,
                "totalRounds should increment by exactly 1"
            )
            
            // Property 1b: Each card's hitCount increases by the number of times it appears in hits
            val hitCountChanges = hits.groupingBy { it }.eachCount()
            
            for (card in testConfig.outcomes) {
                val expectedChange = hitCountChanges[card] ?: 0
                val previousCount = session.hitCounts[card] ?: 0
                val newCount = updatedSession.hitCounts[card] ?: 0
                
                assertEquals(
                    previousCount + expectedChange,
                    newCount,
                    "Card '$card' hitCount should increase by $expectedChange (appeared $expectedChange times in hits)"
                )
            }
            
            // Property 1c: Observations list grows by exactly 1
            assertEquals(
                session.observations.size + 1,
                updatedSession.observations.size,
                "observations list should grow by exactly 1"
            )
            
            // Property 1d: The new observation contains the correct hits
            val newObservation = updatedSession.observations.last()
            assertEquals(
                hits,
                newObservation.hits,
                "The new observation should contain the exact hits that were recorded"
            )
            }
        }
    }
    
    // Feature: perya-odds-mvp, Property 2
    @Test
    fun `Property 2 - Invalid observations rejected without mutation`() {
        /**
         * **Validates: Requirements 2.3, 2.4**
         * 
         * For any hits array whose length is not equal to hitsPerRound,
         * ObservationEngine.recordObservation SHALL return an error result and
         * the session SHALL be identical to the input session (totalRounds,
         * hitCounts, and observations all unchanged).
         */
        runBlocking {
            // Arbitrary for generating invalid hits arrays (wrong length)
            val arbInvalidHits = Arb.choice(
                // Too few hits: 0 to hitsPerRound-1
                Arb.list(arbCard(), 0 until testConfig.hitsPerRound),
                // Too many hits: hitsPerRound+1 to hitsPerRound+10
                Arb.list(arbCard(), (testConfig.hitsPerRound + 1)..(testConfig.hitsPerRound + 10))
            )
            
            checkAll(100, arbGameSession(), arbInvalidHits) { session, invalidHits ->
                // Create a deep copy of the session for comparison
                val originalSession = session.copy(
                    hitCounts = session.hitCounts.toMap(),
                    observations = session.observations.toList()
                )
                
                // Attempt to record the invalid observation
                val result = ObservationEngine.recordObservation(session, testConfig, invalidHits)
                
                // Property 2a: Result should be an error
                assertTrue(
                    result is Result.Error,
                    "Recording observation with ${invalidHits.size} hits (expected ${testConfig.hitsPerRound}) should return an error"
                )
                
                // Property 2b: Error should have the correct code
                val error = (result as Result.Error).error
                val expectedCode = when {
                    invalidHits.size < testConfig.hitsPerRound -> ValidationErrorCode.TOO_FEW_HITS
                    else -> ValidationErrorCode.TOO_MANY_HITS
                }
                assertEquals(
                    expectedCode,
                    error.code,
                    "Error code should be $expectedCode for ${invalidHits.size} hits"
                )
                
                // Property 2c: Session should remain completely unchanged
                assertEquals(
                    originalSession.totalRounds,
                    session.totalRounds,
                    "totalRounds should not change after invalid observation"
                )
                
                assertEquals(
                    originalSession.hitCounts,
                    session.hitCounts,
                    "hitCounts should not change after invalid observation"
                )
                
                assertEquals(
                    originalSession.observations.size,
                    session.observations.size,
                    "observations list size should not change after invalid observation"
                )
                
                assertEquals(
                    originalSession.gameType,
                    session.gameType,
                    "gameType should not change after invalid observation"
                )
            }
        }
    }
    
    // Feature: perya-odds-mvp, Property 9
    @Test
    fun `Property 9 - Session reset always produces clean session`() {
        /**
         * **Validates: Requirements 11.4, 11.5**
         * 
         * For any GameSession regardless of accumulated data, after
         * ObservationEngine.resetSession, the resulting session SHALL have
         * totalRounds === 0, all hitCounts equal to 0, and an empty observations array.
         */
        runBlocking {
            checkAll(100, arbGameSession()) { session ->
                // Reset the session
                val resetSession = ObservationEngine.resetSession(session.gameType)
                
                // Property 9a: totalRounds should be 0
                assertEquals(
                    0,
                    resetSession.totalRounds,
                    "Reset session should have totalRounds = 0"
                )
                
                // Property 9b: hitCounts should be empty (all counts are 0)
                assertTrue(
                    resetSession.hitCounts.isEmpty(),
                    "Reset session should have empty hitCounts map"
                )
                
                // Property 9c: observations should be empty
                assertTrue(
                    resetSession.observations.isEmpty(),
                    "Reset session should have empty observations list"
                )
                
                // Property 9d: gameType should be preserved
                assertEquals(
                    session.gameType,
                    resetSession.gameType,
                    "Reset session should preserve the gameType"
                )
                
                // Property 9e: lastUpdated should be set (non-zero)
                assertTrue(
                    resetSession.lastUpdated > 0,
                    "Reset session should have a valid lastUpdated timestamp"
                )
            }
        }
    }
}
