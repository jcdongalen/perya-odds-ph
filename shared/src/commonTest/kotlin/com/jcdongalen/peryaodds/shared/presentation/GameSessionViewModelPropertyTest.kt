package com.jcdongalen.peryaodds.shared.presentation

import com.jcdongalen.peryaodds.shared.data.repository.InMemorySessionRepository
import com.jcdongalen.peryaodds.shared.domain.models.*
import com.jcdongalen.peryaodds.shared.domain.registry.DefaultGameRegistry
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for GameSessionViewModel using Kotest Property Testing.
 * Each property test runs with a minimum of 100 iterations.
 */
class GameSessionViewModelPropertyTest {
    
    private val testConfigA = GameConfig(
        gameType = "game_type_a",
        displayName = "Game Type A",
        outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
        hitsPerRound = 3,
        expectedProbability = 1.0 / 6.0,
        comingSoon = false
    )
    
    private val testConfigB = GameConfig(
        gameType = "game_type_b",
        displayName = "Game Type B",
        outcomes = listOf("Red", "Blue", "Green", "Yellow", "Purple", "Orange"),
        hitsPerRound = 3,
        expectedProbability = 1.0 / 6.0,
        comingSoon = false
    )
    
    // Arbitrary for generating valid card names from game config A
    private fun arbCardA(): Arb<String> = Arb.of(testConfigA.outcomes)
    
    // Arbitrary for generating valid card names from game config B
    private fun arbCardB(): Arb<String> = Arb.of(testConfigB.outcomes)
    
    // Arbitrary for generating valid hits arrays for game A
    private fun arbValidHitsA(): Arb<List<String>> = 
        Arb.list(arbCardA(), testConfigA.hitsPerRound..testConfigA.hitsPerRound)
    
    // Arbitrary for generating valid hits arrays for game B
    private fun arbValidHitsB(): Arb<List<String>> = 
        Arb.list(arbCardB(), testConfigB.hitsPerRound..testConfigB.hitsPerRound)
    
    // Helper to create a ViewModel with test registry and repository
    private fun createTestViewModel(): Triple<GameSessionViewModel, DefaultGameRegistry, InMemorySessionRepository> {
        val registry = DefaultGameRegistry()
        registry.register(testConfigA)
        registry.register(testConfigB)
        
        val repository = InMemorySessionRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        
        val viewModel = GameSessionViewModel(registry, repository, scope)
        
        return Triple(viewModel, registry, repository)
    }
    
    // Feature: perya-odds-mvp, Property 8
    @Test
    fun `Property 8 - Dataset isolation between game types`() {
        /**
         * **Validates: Requirements 1.7**
         * 
         * For any two distinct game types A and B with separate GameSession objects,
         * recording any valid observation for game type A SHALL leave game type B's
         * session (totalRounds, hitCounts, observations) completely unchanged.
         */
        runBlocking {
            checkAll(100, arbValidHitsA(), arbValidHitsB()) { hitsA, hitsB ->
                // Create a fresh ViewModel for each iteration
                val (viewModel, _, _) = createTestViewModel()
                
                // Wait for initial load to complete
                delay(50)
                
                // Select game A and record an observation
                viewModel.selectGame(testConfigA.gameType)
                val resultA1 = viewModel.recordObservation(hitsA)
                
                // Property 8a: Recording for game A should succeed
                assertTrue(
                    resultA1 is Result.Success,
                    "Recording valid observation for game A should succeed"
                )
                
                // Wait for persistence to complete
                delay(50)
                
                // Capture game A's state after first observation
                val sessionA1 = viewModel.sessions.value[testConfigA.gameType]
                assertTrue(
                    sessionA1 != null,
                    "Game A session should exist after recording observation"
                )
                
                // Property 8b: Game A should have exactly 1 round
                assertEquals(
                    1,
                    sessionA1.totalRounds,
                    "Game A should have exactly 1 round after first observation"
                )
                
                // Capture game B's state before any observations (should be empty or non-existent)
                val sessionB0 = viewModel.sessions.value[testConfigB.gameType]
                val sessionB0Rounds = sessionB0?.totalRounds ?: 0
                
                // Now select game B and record an observation
                viewModel.selectGame(testConfigB.gameType)
                val resultB1 = viewModel.recordObservation(hitsB)
                
                // Property 8c: Recording for game B should succeed
                assertTrue(
                    resultB1 is Result.Success,
                    "Recording valid observation for game B should succeed"
                )
                
                // Wait for persistence to complete
                delay(50)
                
                // Capture game B's state after first observation
                val sessionB1 = viewModel.sessions.value[testConfigB.gameType]
                assertTrue(
                    sessionB1 != null,
                    "Game B session should exist after recording observation"
                )
                
                // Property 8d: Game B should have exactly 1 round
                assertEquals(
                    sessionB0Rounds + 1,
                    sessionB1.totalRounds,
                    "Game B should have incremented by 1 round after observation"
                )
                
                // Property 8e: Game A's state should remain unchanged after game B observation
                val sessionA2 = viewModel.sessions.value[testConfigA.gameType]
                assertTrue(
                    sessionA2 != null,
                    "Game A session should still exist after game B observation"
                )
                
                assertEquals(
                    sessionA1.totalRounds,
                    sessionA2.totalRounds,
                    "Game A totalRounds should be unchanged after game B observation"
                )
                
                assertEquals(
                    sessionA1.hitCounts,
                    sessionA2.hitCounts,
                    "Game A hitCounts should be unchanged after game B observation"
                )
                
                assertEquals(
                    sessionA1.observations.size,
                    sessionA2.observations.size,
                    "Game A observations size should be unchanged after game B observation"
                )
                
                // Property 8f: Game A and Game B should have completely independent data
                // Verify that no outcome from game A appears in game B's hitCounts
                sessionB1.hitCounts.keys.forEach { outcome ->
                    assertTrue(
                        outcome in testConfigB.outcomes,
                        "Game B hitCounts should only contain outcomes from game B's config, found: $outcome"
                    )
                }
                
                // Verify that no outcome from game B appears in game A's hitCounts
                sessionA2.hitCounts.keys.forEach { outcome ->
                    assertTrue(
                        outcome in testConfigA.outcomes,
                        "Game A hitCounts should only contain outcomes from game A's config, found: $outcome"
                    )
                }
                
                // Property 8g: Record another observation for game A and verify game B remains unchanged
                viewModel.selectGame(testConfigA.gameType)
                val resultA2 = viewModel.recordObservation(hitsA)
                
                assertTrue(
                    resultA2 is Result.Success,
                    "Recording second observation for game A should succeed"
                )
                
                // Wait for persistence to complete
                delay(50)
                
                val sessionA3 = viewModel.sessions.value[testConfigA.gameType]
                val sessionB2 = viewModel.sessions.value[testConfigB.gameType]
                
                // Game A should now have 2 rounds
                assertEquals(
                    2,
                    sessionA3!!.totalRounds,
                    "Game A should have 2 rounds after second observation"
                )
                
                // Game B should still have only 1 round
                assertEquals(
                    sessionB1.totalRounds,
                    sessionB2!!.totalRounds,
                    "Game B totalRounds should remain unchanged after second game A observation"
                )
                
                assertEquals(
                    sessionB1.hitCounts,
                    sessionB2.hitCounts,
                    "Game B hitCounts should remain unchanged after second game A observation"
                )
                
                assertEquals(
                    sessionB1.observations.size,
                    sessionB2.observations.size,
                    "Game B observations size should remain unchanged after second game A observation"
                )
            }
        }
    }
}
