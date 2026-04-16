package com.jcdongalen.peryaodds.shared.data.repository

import com.jcdongalen.peryaodds.shared.domain.models.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Unit tests for SessionRepository.
 * Tests specific scenarios using InMemorySessionRepository mock.
 * 
 * Requirements: 11.1, 11.2, 11.3
 */
class SessionRepositoryTest {
    
    private val testConfig = GameConfig(
        gameType = "three_ball_drop",
        displayName = "3-Ball Drop Card Game",
        outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
        hitsPerRound = 3,
        expectedProbability = 1.0 / 6.0,
        comingSoon = false
    )
    
    @Test
    fun `loadAll with missing key returns empty session`() = runBlocking {
        /**
         * Validates: Requirements 11.1, 11.3
         * 
         * When no previously persisted Dataset exists for a Game_Type on launch,
         * the repository SHALL initialize an empty Dataset for that Game_Type.
         */
        val repository = InMemorySessionRepository()
        
        // Load a game type that has never been saved
        val result = repository.loadAll(listOf("three_ball_drop"))
        
        // Should succeed
        assertTrue(result is Result.Success, "Loading missing key should succeed")
        
        val sessions = (result as Result.Success).value
        val session = sessions["three_ball_drop"]
        
        // Should return an empty session
        assertNotNull(session, "Missing key should return an empty session, not null")
        assertEquals("three_ball_drop", session.gameType)
        assertEquals(0, session.totalRounds)
        assertTrue(session.hitCounts.isEmpty(), "Empty session should have no hit counts")
        assertTrue(session.observations.isEmpty(), "Empty session should have no observations")
        assertTrue(session.lastUpdated > 0, "Empty session should have a valid timestamp")
    }
    
    @Test
    fun `loadAll with multiple missing keys returns empty sessions for all`() = runBlocking {
        /**
         * Validates: Requirements 11.1, 11.3
         * 
         * When loading multiple game types with no saved data,
         * all should return empty sessions.
         */
        val repository = InMemorySessionRepository()
        
        val gameTypes = listOf("three_ball_drop", "color_game", "wheel_of_fortune")
        val result = repository.loadAll(gameTypes)
        
        assertTrue(result is Result.Success)
        val sessions = (result as Result.Success).value
        
        // All game types should have empty sessions
        assertEquals(3, sessions.size)
        gameTypes.forEach { gameType ->
            val session = sessions[gameType]
            assertNotNull(session, "Game type $gameType should have a session")
            assertEquals(gameType, session.gameType)
            assertEquals(0, session.totalRounds)
            assertTrue(session.hitCounts.isEmpty())
            assertTrue(session.observations.isEmpty())
        }
    }
    
    @Test
    fun `save and load preserves session data correctly`() = runBlocking {
        /**
         * Validates: Requirements 11.1, 11.2
         * 
         * Serialization and deserialization should preserve all session data.
         */
        val repository = InMemorySessionRepository()
        
        val observation = Observation(
            id = "test-id-123",
            timestamp = 1234567890L,
            hits = listOf("Ace", "King", "Queen")
        )
        
        val originalSession = GameSession(
            gameType = "three_ball_drop",
            totalRounds = 1,
            hitCounts = mapOf("Ace" to 1, "King" to 1, "Queen" to 1),
            observations = listOf(observation),
            lastUpdated = 9876543210L
        )
        
        // Save the session
        val saveResult = repository.save(originalSession)
        assertTrue(saveResult is Result.Success, "Save should succeed")
        
        // Load it back
        val loadResult = repository.loadAll(listOf("three_ball_drop"))
        assertTrue(loadResult is Result.Success, "Load should succeed")
        
        val loadedSession = (loadResult as Result.Success).value["three_ball_drop"]
        assertNotNull(loadedSession, "Loaded session should not be null")
        
        // Verify all fields are preserved
        assertEquals(originalSession.gameType, loadedSession.gameType)
        assertEquals(originalSession.totalRounds, loadedSession.totalRounds)
        assertEquals(originalSession.hitCounts, loadedSession.hitCounts)
        assertEquals(originalSession.lastUpdated, loadedSession.lastUpdated)
        
        // Verify observations
        assertEquals(1, loadedSession.observations.size)
        val loadedObs = loadedSession.observations[0]
        assertEquals(observation.id, loadedObs.id)
        assertEquals(observation.timestamp, loadedObs.timestamp)
        assertEquals(observation.hits, loadedObs.hits)
    }
    
    @Test
    fun `save and load with complex session data preserves all details`() = runBlocking {
        /**
         * Validates: Requirements 11.1, 11.2
         * 
         * Test serialization with a more complex session containing multiple rounds
         * and various hit counts.
         */
        val repository = InMemorySessionRepository()
        
        val observations = listOf(
            Observation(
                id = "obs-1",
                timestamp = 1000L,
                hits = listOf("Ace", "Ace", "King")
            ),
            Observation(
                id = "obs-2",
                timestamp = 2000L,
                hits = listOf("Queen", "Jack", "10")
            ),
            Observation(
                id = "obs-3",
                timestamp = 3000L,
                hits = listOf("9", "9", "9")
            )
        )
        
        val originalSession = GameSession(
            gameType = "three_ball_drop",
            totalRounds = 3,
            hitCounts = mapOf(
                "Ace" to 2,
                "King" to 1,
                "Queen" to 1,
                "Jack" to 1,
                "10" to 1,
                "9" to 3
            ),
            observations = observations,
            lastUpdated = 3000L
        )
        
        // Save and load
        repository.save(originalSession)
        val loadResult = repository.loadAll(listOf("three_ball_drop"))
        
        assertTrue(loadResult is Result.Success)
        val loadedSession = (loadResult as Result.Success).value["three_ball_drop"]
        assertNotNull(loadedSession)
        
        // Verify complete equality
        assertEquals(originalSession, loadedSession)
    }
    
    @Test
    fun `save and load with empty hitCounts preserves empty map`() = runBlocking {
        /**
         * Validates: Requirements 11.1, 11.2
         * 
         * Edge case: empty session with no observations should serialize correctly.
         */
        val repository = InMemorySessionRepository()
        
        val emptySession = GameSession(
            gameType = "three_ball_drop",
            totalRounds = 0,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = 5000L
        )
        
        repository.save(emptySession)
        val loadResult = repository.loadAll(listOf("three_ball_drop"))
        
        assertTrue(loadResult is Result.Success)
        val loadedSession = (loadResult as Result.Success).value["three_ball_drop"]
        assertNotNull(loadedSession)
        
        assertEquals(0, loadedSession.totalRounds)
        assertTrue(loadedSession.hitCounts.isEmpty())
        assertTrue(loadedSession.observations.isEmpty())
        assertEquals(5000L, loadedSession.lastUpdated)
    }
    
    @Test
    fun `save overwrites existing session data`() = runBlocking {
        /**
         * Validates: Requirements 11.2
         * 
         * Saving a session with the same game type should overwrite previous data.
         */
        val repository = InMemorySessionRepository()
        
        val session1 = GameSession(
            gameType = "three_ball_drop",
            totalRounds = 1,
            hitCounts = mapOf("Ace" to 3),
            observations = listOf(
                Observation("id1", 1000L, listOf("Ace", "Ace", "Ace"))
            ),
            lastUpdated = 1000L
        )
        
        val session2 = GameSession(
            gameType = "three_ball_drop",
            totalRounds = 2,
            hitCounts = mapOf("King" to 3, "Queen" to 3),
            observations = listOf(
                Observation("id2", 2000L, listOf("King", "King", "King")),
                Observation("id3", 3000L, listOf("Queen", "Queen", "Queen"))
            ),
            lastUpdated = 3000L
        )
        
        // Save first session
        repository.save(session1)
        
        // Save second session (should overwrite)
        repository.save(session2)
        
        // Load and verify we get the second session
        val loadResult = repository.loadAll(listOf("three_ball_drop"))
        assertTrue(loadResult is Result.Success)
        
        val loadedSession = (loadResult as Result.Success).value["three_ball_drop"]
        assertNotNull(loadedSession)
        
        assertEquals(session2, loadedSession)
        assertEquals(2, loadedSession.totalRounds)
        assertEquals(3, loadedSession.hitCounts["King"])
        assertEquals(3, loadedSession.hitCounts["Queen"])
        assertNull(loadedSession.hitCounts["Ace"])
    }
    
    @Test
    fun `delete removes session data`() = runBlocking {
        /**
         * Validates: Requirements 11.3
         * 
         * Deleting a session should remove it from storage,
         * and subsequent loads should return an empty session.
         */
        val repository = InMemorySessionRepository()
        
        val session = GameSession(
            gameType = "three_ball_drop",
            totalRounds = 5,
            hitCounts = mapOf("Ace" to 15),
            observations = emptyList(),
            lastUpdated = 1000L
        )
        
        // Save the session
        repository.save(session)
        
        // Verify it exists
        val loadResult1 = repository.loadAll(listOf("three_ball_drop"))
        assertTrue(loadResult1 is Result.Success)
        val loadedSession1 = (loadResult1 as Result.Success).value["three_ball_drop"]
        assertEquals(5, loadedSession1?.totalRounds)
        
        // Delete the session
        val deleteResult = repository.delete("three_ball_drop")
        assertTrue(deleteResult is Result.Success, "Delete should succeed")
        
        // Load again - should return empty session
        val loadResult2 = repository.loadAll(listOf("three_ball_drop"))
        assertTrue(loadResult2 is Result.Success)
        val loadedSession2 = (loadResult2 as Result.Success).value["three_ball_drop"]
        assertNotNull(loadedSession2)
        assertEquals(0, loadedSession2.totalRounds)
        assertTrue(loadedSession2.hitCounts.isEmpty())
    }
    
    @Test
    fun `multiple game types are stored independently`() = runBlocking {
        /**
         * Validates: Requirements 11.1, 11.2
         * 
         * Different game types should have independent storage.
         */
        val repository = InMemorySessionRepository()
        
        val session1 = GameSession(
            gameType = "three_ball_drop",
            totalRounds = 1,
            hitCounts = mapOf("Ace" to 3),
            observations = emptyList(),
            lastUpdated = 1000L
        )
        
        val session2 = GameSession(
            gameType = "color_game",
            totalRounds = 2,
            hitCounts = mapOf("Red" to 6),
            observations = emptyList(),
            lastUpdated = 2000L
        )
        
        // Save both sessions
        repository.save(session1)
        repository.save(session2)
        
        // Load both
        val loadResult = repository.loadAll(listOf("three_ball_drop", "color_game"))
        assertTrue(loadResult is Result.Success)
        
        val sessions = (loadResult as Result.Success).value
        
        // Verify both are stored independently
        val loaded1 = sessions["three_ball_drop"]
        val loaded2 = sessions["color_game"]
        
        assertNotNull(loaded1)
        assertNotNull(loaded2)
        
        assertEquals(1, loaded1.totalRounds)
        assertEquals(2, loaded2.totalRounds)
        assertEquals(mapOf("Ace" to 3), loaded1.hitCounts)
        assertEquals(mapOf("Red" to 6), loaded2.hitCounts)
    }
    
    @Test
    fun `delete one game type does not affect others`() = runBlocking {
        /**
         * Validates: Requirements 11.3
         * 
         * Deleting one game type's session should not affect other game types.
         */
        val repository = InMemorySessionRepository()
        
        val session1 = GameSession(
            gameType = "three_ball_drop",
            totalRounds = 1,
            hitCounts = mapOf("Ace" to 3),
            observations = emptyList(),
            lastUpdated = 1000L
        )
        
        val session2 = GameSession(
            gameType = "color_game",
            totalRounds = 2,
            hitCounts = mapOf("Red" to 6),
            observations = emptyList(),
            lastUpdated = 2000L
        )
        
        // Save both
        repository.save(session1)
        repository.save(session2)
        
        // Delete only three_ball_drop
        repository.delete("three_ball_drop")
        
        // Load both
        val loadResult = repository.loadAll(listOf("three_ball_drop", "color_game"))
        assertTrue(loadResult is Result.Success)
        
        val sessions = (loadResult as Result.Success).value
        
        // three_ball_drop should be empty
        val loaded1 = sessions["three_ball_drop"]
        assertNotNull(loaded1)
        assertEquals(0, loaded1.totalRounds)
        
        // color_game should still have data
        val loaded2 = sessions["color_game"]
        assertNotNull(loaded2)
        assertEquals(2, loaded2.totalRounds)
        assertEquals(mapOf("Red" to 6), loaded2.hitCounts)
    }
}
