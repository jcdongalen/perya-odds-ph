package com.jcdongalen.peryaodds.shared.presentation

import com.jcdongalen.peryaodds.shared.data.repository.InMemorySessionRepository
import com.jcdongalen.peryaodds.shared.domain.models.*
import com.jcdongalen.peryaodds.shared.domain.registry.DefaultGameRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Unit tests for GameSessionViewModel.
 * Tests each function produces correct state updates using in-memory mock repository.
 * 
 * Requirements: 1.3, 1.6, 2.2, 11.4, 11.5
 */
class GameSessionViewModelTest {
    
    private val testConfig = GameConfig(
        gameType = "three_ball_drop",
        displayName = "3-Ball Drop Card Game",
        outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
        hitsPerRound = 3,
        expectedProbability = 1.0 / 6.0,
        comingSoon = false
    )
    
    private fun createTestViewModel(): Triple<GameSessionViewModel, DefaultGameRegistry, InMemorySessionRepository> {
        val registry = DefaultGameRegistry()
        registry.register(testConfig)
        
        val repository = InMemorySessionRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        
        val viewModel = GameSessionViewModel(registry, repository, scope)
        
        return Triple(viewModel, registry, repository)
    }
    
    @Test
    fun `selectGame updates activeGameType state`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        // Wait for initial load
        delay(50)
        
        // Initially no game selected
        assertNull(viewModel.activeGameType.value)
        
        // Select a game
        viewModel.selectGame(testConfig.gameType)
        
        // Verify state updated
        assertEquals(testConfig.gameType, viewModel.activeGameType.value)
    }
    
    @Test
    fun `selectGame can switch between different games`() = runBlocking {
        val registry = DefaultGameRegistry()
        registry.register(testConfig)
        
        val gameConfig2 = GameConfig(
            gameType = "another_game",
            displayName = "Another Game",
            outcomes = listOf("Red", "Blue", "Green"),
            hitsPerRound = 2,
            expectedProbability = 1.0 / 3.0,
            comingSoon = false
        )
        registry.register(gameConfig2)
        
        val repository = InMemorySessionRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = GameSessionViewModel(registry, repository, scope)
        
        delay(50)
        
        // Select first game
        viewModel.selectGame(testConfig.gameType)
        assertEquals(testConfig.gameType, viewModel.activeGameType.value)
        
        // Switch to second game
        viewModel.selectGame(gameConfig2.gameType)
        assertEquals(gameConfig2.gameType, viewModel.activeGameType.value)
    }
    
    @Test
    fun `recordObservation with valid hits updates session state`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        // Select game
        viewModel.selectGame(testConfig.gameType)
        
        // Record observation
        val hits = listOf("Ace", "King", "Queen")
        val result = viewModel.recordObservation(hits)
        
        // Verify success
        assertTrue(result is Result.Success)
        
        // Wait for state update
        delay(50)
        
        // Verify session state updated
        val session = viewModel.sessions.value[testConfig.gameType]
        assertNotNull(session)
        assertEquals(1, session.totalRounds)
        assertEquals(1, session.hitCounts["Ace"])
        assertEquals(1, session.hitCounts["King"])
        assertEquals(1, session.hitCounts["Queen"])
        assertEquals(1, session.observations.size)
    }
    
    @Test
    fun `recordObservation accumulates multiple rounds correctly`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        viewModel.selectGame(testConfig.gameType)
        
        // Record first observation
        val hits1 = listOf("Ace", "King", "Queen")
        viewModel.recordObservation(hits1)
        delay(50)
        
        // Record second observation
        val hits2 = listOf("Ace", "Ace", "Jack")
        viewModel.recordObservation(hits2)
        delay(50)
        
        // Verify accumulated state
        val session = viewModel.sessions.value[testConfig.gameType]
        assertNotNull(session)
        assertEquals(2, session.totalRounds)
        assertEquals(3, session.hitCounts["Ace"]) // 1 from first, 2 from second
        assertEquals(1, session.hitCounts["King"])
        assertEquals(1, session.hitCounts["Queen"])
        assertEquals(1, session.hitCounts["Jack"])
        assertEquals(2, session.observations.size)
    }
    
    @Test
    fun `recordObservation with no game selected returns error`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        // Don't select a game
        val hits = listOf("Ace", "King", "Queen")
        val result = viewModel.recordObservation(hits)
        
        // Verify error
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(ValidationErrorCode.INVALID_OUTCOME, error.code)
        assertTrue(error.message.contains("No game selected"))
    }
    
    @Test
    fun `recordObservation with invalid hits returns error and does not mutate state`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        viewModel.selectGame(testConfig.gameType)
        
        // Record valid observation first
        val validHits = listOf("Ace", "King", "Queen")
        viewModel.recordObservation(validHits)
        delay(50)
        
        val sessionBefore = viewModel.sessions.value[testConfig.gameType]
        assertNotNull(sessionBefore)
        val roundsBefore = sessionBefore.totalRounds
        
        // Try to record invalid observation (too few hits)
        val invalidHits = listOf("Ace", "King")
        val result = viewModel.recordObservation(invalidHits)
        
        // Verify error
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(ValidationErrorCode.TOO_FEW_HITS, error.code)
        
        delay(50)
        
        // Verify state unchanged
        val sessionAfter = viewModel.sessions.value[testConfig.gameType]
        assertNotNull(sessionAfter)
        assertEquals(roundsBefore, sessionAfter.totalRounds)
    }
    
    @Test
    fun `resetSession clears all data for active game`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        viewModel.selectGame(testConfig.gameType)
        
        // Record some observations
        viewModel.recordObservation(listOf("Ace", "King", "Queen"))
        delay(50)
        viewModel.recordObservation(listOf("Ace", "Ace", "Jack"))
        delay(50)
        
        // Verify data exists
        val sessionBefore = viewModel.sessions.value[testConfig.gameType]
        assertNotNull(sessionBefore)
        assertEquals(2, sessionBefore.totalRounds)
        assertTrue(sessionBefore.hitCounts.isNotEmpty())
        assertTrue(sessionBefore.observations.isNotEmpty())
        
        // Reset session
        viewModel.resetSession()
        delay(50)
        
        // Verify session cleared
        val sessionAfter = viewModel.sessions.value[testConfig.gameType]
        assertNotNull(sessionAfter)
        assertEquals(0, sessionAfter.totalRounds)
        assertTrue(sessionAfter.hitCounts.isEmpty())
        assertTrue(sessionAfter.observations.isEmpty())
    }
    
    @Test
    fun `resetSession with no game selected does nothing`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        // Don't select a game
        val sessionsBefore = viewModel.sessions.value
        
        // Try to reset
        viewModel.resetSession()
        delay(50)
        
        // Verify sessions unchanged
        val sessionsAfter = viewModel.sessions.value
        assertEquals(sessionsBefore, sessionsAfter)
    }
    
    @Test
    fun `setStrategyMode updates strategyMode state`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        // Initial mode is 1
        assertEquals(1, viewModel.strategyMode.value)
        
        // Set to mode 2
        viewModel.setStrategyMode(2)
        assertEquals(2, viewModel.strategyMode.value)
        
        // Set to mode 3
        viewModel.setStrategyMode(3)
        assertEquals(3, viewModel.strategyMode.value)
        
        // Set back to mode 1
        viewModel.setStrategyMode(1)
        assertEquals(1, viewModel.strategyMode.value)
    }
    
    @Test
    fun `setStrategyMode rejects invalid modes`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        // Initial mode is 1
        assertEquals(1, viewModel.strategyMode.value)
        
        // Try invalid modes
        viewModel.setStrategyMode(0)
        assertEquals(1, viewModel.strategyMode.value) // Should remain unchanged
        
        viewModel.setStrategyMode(4)
        assertEquals(1, viewModel.strategyMode.value) // Should remain unchanged
        
        viewModel.setStrategyMode(-1)
        assertEquals(1, viewModel.strategyMode.value) // Should remain unchanged
    }
    
    @Test
    fun `loadSessions initializes empty sessions for registered games`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        // Wait for initial load
        delay(50)
        
        // Verify session exists for registered game
        val sessions = viewModel.sessions.value
        assertTrue(sessions.containsKey(testConfig.gameType))
        
        val session = sessions[testConfig.gameType]
        assertNotNull(session)
        assertEquals(0, session.totalRounds)
        assertTrue(session.hitCounts.isEmpty())
        assertTrue(session.observations.isEmpty())
    }
    
    @Test
    fun `loadSessions restores previously saved sessions`() = runBlocking {
        val registry = DefaultGameRegistry()
        registry.register(testConfig)
        
        val repository = InMemorySessionRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        
        // Create first ViewModel and record data
        val viewModel1 = GameSessionViewModel(registry, repository, scope)
        delay(50)
        
        viewModel1.selectGame(testConfig.gameType)
        viewModel1.recordObservation(listOf("Ace", "King", "Queen"))
        delay(50)
        viewModel1.recordObservation(listOf("Ace", "Ace", "Jack"))
        delay(50)
        
        // Create second ViewModel (simulates app restart)
        val viewModel2 = GameSessionViewModel(registry, repository, scope)
        delay(50)
        
        // Verify data restored
        val session = viewModel2.sessions.value[testConfig.gameType]
        assertNotNull(session)
        assertEquals(2, session.totalRounds)
        assertEquals(3, session.hitCounts["Ace"])
        assertEquals(1, session.hitCounts["King"])
        assertEquals(1, session.hitCounts["Queen"])
        assertEquals(1, session.hitCounts["Jack"])
        assertEquals(2, session.observations.size)
    }
    
    @Test
    fun `getCurrentSession returns session for active game`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        // No game selected
        assertNull(viewModel.getCurrentSession())
        
        // Select game
        viewModel.selectGame(testConfig.gameType)
        
        // Record observation
        viewModel.recordObservation(listOf("Ace", "King", "Queen"))
        delay(50)
        
        // Get current session
        val session = viewModel.getCurrentSession()
        assertNotNull(session)
        assertEquals(testConfig.gameType, session.gameType)
        assertEquals(1, session.totalRounds)
    }
    
    @Test
    fun `getCurrentGameConfig returns config for active game`() = runBlocking {
        val (viewModel, _, _) = createTestViewModel()
        
        delay(50)
        
        // No game selected
        assertNull(viewModel.getCurrentGameConfig())
        
        // Select game
        viewModel.selectGame(testConfig.gameType)
        
        // Get current config
        val config = viewModel.getCurrentGameConfig()
        assertNotNull(config)
        assertEquals(testConfig.gameType, config.gameType)
        assertEquals(testConfig.displayName, config.displayName)
        assertEquals(testConfig.outcomes, config.outcomes)
        assertEquals(testConfig.hitsPerRound, config.hitsPerRound)
    }
    
    @Test
    fun `recordObservation persists data to repository`() = runBlocking {
        val (viewModel, _, repository) = createTestViewModel()
        
        delay(50)
        
        viewModel.selectGame(testConfig.gameType)
        viewModel.recordObservation(listOf("Ace", "King", "Queen"))
        delay(50)
        
        // Verify data persisted by loading directly from repository
        val result = repository.loadAll(listOf(testConfig.gameType))
        assertTrue(result is Result.Success)
        
        val sessions = (result as Result.Success).value
        val session = sessions[testConfig.gameType]
        assertNotNull(session)
        assertEquals(1, session.totalRounds)
        assertEquals(1, session.hitCounts["Ace"])
    }
    
    @Test
    fun `resetSession persists empty session to repository`() = runBlocking {
        val (viewModel, _, repository) = createTestViewModel()
        
        delay(50)
        
        viewModel.selectGame(testConfig.gameType)
        
        // Record data
        viewModel.recordObservation(listOf("Ace", "King", "Queen"))
        delay(50)
        
        // Reset
        viewModel.resetSession()
        delay(50)
        
        // Verify empty session persisted
        val result = repository.loadAll(listOf(testConfig.gameType))
        assertTrue(result is Result.Success)
        
        val sessions = (result as Result.Success).value
        val session = sessions[testConfig.gameType]
        assertNotNull(session)
        assertEquals(0, session.totalRounds)
        assertTrue(session.hitCounts.isEmpty())
        assertTrue(session.observations.isEmpty())
    }
}
