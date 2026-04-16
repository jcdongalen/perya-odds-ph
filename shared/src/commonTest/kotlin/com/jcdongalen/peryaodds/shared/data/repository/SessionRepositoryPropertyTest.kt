package com.jcdongalen.peryaodds.shared.data.repository

import com.jcdongalen.peryaodds.shared.domain.models.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for SessionRepository using Kotest Property Testing.
 * Each property test runs with a minimum of 100 iterations.
 */
class SessionRepositoryPropertyTest {
    
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
    
    // Arbitrary for generating Observation
    private fun arbObservation(): Arb<Observation> = arbitrary { rs ->
        val hits = Arb.list(arbCard(), testConfig.hitsPerRound..testConfig.hitsPerRound).bind()
        Observation(
            id = Arb.uuid().bind().toString(),
            timestamp = Arb.long(0L..Long.MAX_VALUE).bind(),
            hits = hits
        )
    }
    
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
        
        val observations = if (totalRounds == 0) {
            emptyList()
        } else {
            Arb.list(arbObservation(), 0..totalRounds).bind()
        }
        
        GameSession(
            gameType = testConfig.gameType,
            totalRounds = totalRounds,
            hitCounts = hitCounts,
            observations = observations,
            lastUpdated = Arb.long(0L..Long.MAX_VALUE).bind()
        )
    }
    
    // Feature: perya-odds-mvp, Property 10
    @Test
    fun `Property 10 - Persistence round-trip is lossless for any session`() {
        /**
         * **Validates: Requirements 11.1, 11.2**
         * 
         * For any GameSession, serializing it to storage via SessionRepository.save
         * and then loading it back via SessionRepository.loadAll SHALL produce a
         * session that is deeply equal to the original (same totalRounds, hitCounts,
         * observations, gameType, and lastUpdated).
         */
        runBlocking {
            checkAll(100, arbGameSession()) { originalSession ->
                // Create a fresh in-memory repository for each test iteration
                val repository = InMemorySessionRepository()
                
                // Save the session
                val saveResult = repository.save(originalSession)
                
                // Property 10a: Save operation should succeed
                assertTrue(
                    saveResult is Result.Success,
                    "Saving a valid session should succeed"
                )
                
                // Load the session back
                val loadResult = repository.loadAll(listOf(originalSession.gameType))
                
                // Property 10b: Load operation should succeed
                assertTrue(
                    loadResult is Result.Success,
                    "Loading sessions should succeed after successful save"
                )
                
                val loadedSessions = (loadResult as Result.Success).value
                val loadedSession = loadedSessions[originalSession.gameType]
                
                // Property 10c: Loaded session should not be null
                assertTrue(
                    loadedSession != null,
                    "Loaded session should exist for the saved game type"
                )
                
                // Property 10d: gameType should be identical
                assertEquals(
                    originalSession.gameType,
                    loadedSession!!.gameType,
                    "gameType should be preserved in round-trip"
                )
                
                // Property 10e: totalRounds should be identical
                assertEquals(
                    originalSession.totalRounds,
                    loadedSession.totalRounds,
                    "totalRounds should be preserved in round-trip"
                )
                
                // Property 10f: hitCounts should be deeply equal
                assertEquals(
                    originalSession.hitCounts,
                    loadedSession.hitCounts,
                    "hitCounts should be preserved in round-trip"
                )
                
                // Property 10g: observations should be deeply equal
                assertEquals(
                    originalSession.observations.size,
                    loadedSession.observations.size,
                    "observations list size should be preserved in round-trip"
                )
                
                // Verify each observation is preserved
                originalSession.observations.forEachIndexed { index, originalObs ->
                    val loadedObs = loadedSession.observations[index]
                    assertEquals(
                        originalObs.id,
                        loadedObs.id,
                        "Observation $index: id should be preserved"
                    )
                    assertEquals(
                        originalObs.timestamp,
                        loadedObs.timestamp,
                        "Observation $index: timestamp should be preserved"
                    )
                    assertEquals(
                        originalObs.hits,
                        loadedObs.hits,
                        "Observation $index: hits should be preserved"
                    )
                }
                
                // Property 10h: lastUpdated should be identical
                assertEquals(
                    originalSession.lastUpdated,
                    loadedSession.lastUpdated,
                    "lastUpdated should be preserved in round-trip"
                )
                
                // Property 10i: The entire session should be deeply equal
                assertEquals(
                    originalSession,
                    loadedSession,
                    "The entire session should be deeply equal after round-trip"
                )
            }
        }
    }
}
