package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*
import kotlin.test.*

class ObservationEngineTest {
    
    private val testConfig = GameConfig(
        gameType = "three_ball_drop",
        displayName = "3-Ball Drop Card Game",
        outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
        hitsPerRound = 3,
        expectedProbability = 1.0 / 6.0,
        comingSoon = false
    )
    
    private val emptySession = GameSession(
        gameType = "three_ball_drop",
        totalRounds = 0,
        hitCounts = emptyMap(),
        observations = emptyList(),
        lastUpdated = 0L
    )
    
    @Test
    fun `recordObservation with valid hits updates session correctly`() {
        val hits = listOf("Ace", "King", "Queen")
        
        val result = ObservationEngine.recordObservation(emptySession, testConfig, hits)
        
        assertTrue(result is Result.Success)
        val updatedSession = (result as Result.Success).value
        
        assertEquals(1, updatedSession.totalRounds)
        assertEquals(1, updatedSession.hitCounts["Ace"])
        assertEquals(1, updatedSession.hitCounts["King"])
        assertEquals(1, updatedSession.hitCounts["Queen"])
        assertEquals(1, updatedSession.observations.size)
        assertEquals(hits, updatedSession.observations[0].hits)
    }
    
    @Test
    fun `recordObservation with duplicate cards in same round updates counts correctly`() {
        val hits = listOf("Ace", "Ace", "Ace")
        
        val result = ObservationEngine.recordObservation(emptySession, testConfig, hits)
        
        assertTrue(result is Result.Success)
        val updatedSession = (result as Result.Success).value
        
        assertEquals(1, updatedSession.totalRounds)
        assertEquals(3, updatedSession.hitCounts["Ace"])
        assertNull(updatedSession.hitCounts["King"])
    }
    
    @Test
    fun `recordObservation accumulates hit counts across multiple rounds`() {
        val hits1 = listOf("Ace", "King", "Queen")
        val result1 = ObservationEngine.recordObservation(emptySession, testConfig, hits1)
        assertTrue(result1 is Result.Success)
        val session1 = (result1 as Result.Success).value
        
        val hits2 = listOf("Ace", "Ace", "Jack")
        val result2 = ObservationEngine.recordObservation(session1, testConfig, hits2)
        assertTrue(result2 is Result.Success)
        val session2 = (result2 as Result.Success).value
        
        assertEquals(2, session2.totalRounds)
        assertEquals(3, session2.hitCounts["Ace"]) // 1 from round 1, 2 from round 2
        assertEquals(1, session2.hitCounts["King"])
        assertEquals(1, session2.hitCounts["Queen"])
        assertEquals(1, session2.hitCounts["Jack"])
        assertEquals(2, session2.observations.size)
    }
    
    @Test
    fun `recordObservation with too few hits returns TOO_FEW_HITS error`() {
        val hits = listOf("Ace", "King") // Only 2 hits, need 3
        
        val result = ObservationEngine.recordObservation(emptySession, testConfig, hits)
        
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(ValidationErrorCode.TOO_FEW_HITS, error.code)
        assertTrue(error.message.contains("Expected 3 hits, but got 2"))
    }
    
    @Test
    fun `recordObservation with too many hits returns TOO_MANY_HITS error`() {
        val hits = listOf("Ace", "King", "Queen", "Jack") // 4 hits, need 3
        
        val result = ObservationEngine.recordObservation(emptySession, testConfig, hits)
        
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(ValidationErrorCode.TOO_MANY_HITS, error.code)
        assertTrue(error.message.contains("Expected 3 hits, but got 4"))
    }
    
    @Test
    fun `recordObservation with invalid outcome returns INVALID_OUTCOME error`() {
        val hits = listOf("Ace", "King", "InvalidCard")
        
        val result = ObservationEngine.recordObservation(emptySession, testConfig, hits)
        
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(ValidationErrorCode.INVALID_OUTCOME, error.code)
        assertTrue(error.message.contains("InvalidCard"))
    }
    
    @Test
    fun `recordObservation with error does not mutate session`() {
        val originalSession = emptySession.copy(totalRounds = 5, hitCounts = mapOf("Ace" to 10))
        val hits = listOf("Ace", "King") // Invalid: too few hits
        
        val result = ObservationEngine.recordObservation(originalSession, testConfig, hits)
        
        assertTrue(result is Result.Error)
        // Original session should be unchanged
        assertEquals(5, originalSession.totalRounds)
        assertEquals(10, originalSession.hitCounts["Ace"])
        assertEquals(0, originalSession.observations.size)
    }
    
    @Test
    fun `resetSession returns fresh session with zeroed data`() {
        val gameType = "three_ball_drop"
        
        val freshSession = ObservationEngine.resetSession(gameType)
        
        assertEquals(gameType, freshSession.gameType)
        assertEquals(0, freshSession.totalRounds)
        assertTrue(freshSession.hitCounts.isEmpty())
        assertTrue(freshSession.observations.isEmpty())
        assertTrue(freshSession.lastUpdated > 0) // Should have a timestamp
    }
    
    @Test
    fun `resetSession creates independent session regardless of previous state`() {
        // This test verifies that resetSession doesn't depend on any previous state
        val gameType = "three_ball_drop"
        
        val session1 = ObservationEngine.resetSession(gameType)
        val session2 = ObservationEngine.resetSession(gameType)
        
        // Both should be fresh sessions
        assertEquals(0, session1.totalRounds)
        assertEquals(0, session2.totalRounds)
        assertTrue(session1.hitCounts.isEmpty())
        assertTrue(session2.hitCounts.isEmpty())
    }
}
