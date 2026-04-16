package com.jcdongalen.peryaodds.shared.presentation

import com.jcdongalen.peryaodds.shared.data.repository.SessionRepository
import com.jcdongalen.peryaodds.shared.domain.engines.ObservationEngine
import com.jcdongalen.peryaodds.shared.domain.models.*
import com.jcdongalen.peryaodds.shared.domain.registry.GameRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * GameSessionViewModel manages the shared state for the Perya Odds app.
 * 
 * State includes:
 * - activeGameType: Currently selected game (null if none selected)
 * - sessions: Map of all game sessions by game type
 * - strategyMode: Current strategy mode (1, 2, or 3)
 * 
 * This ViewModel is shared between Android and iOS platforms.
 */
class GameSessionViewModel(
    private val gameRegistry: GameRegistry,
    private val repository: SessionRepository,
    private val scope: CoroutineScope
) {
    
    // State flows
    private val _activeGameType = MutableStateFlow<GameType?>(null)
    val activeGameType: StateFlow<GameType?> = _activeGameType.asStateFlow()
    
    private val _sessions = MutableStateFlow<Map<GameType, GameSession>>(emptyMap())
    val sessions: StateFlow<Map<GameType, GameSession>> = _sessions.asStateFlow()
    
    private val _strategyMode = MutableStateFlow(1)
    val strategyMode: StateFlow<Int> = _strategyMode.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadSessions()
    }
    
    /**
     * Loads all game sessions from persistent storage.
     * Called automatically on initialization.
     */
    fun loadSessions() {
        scope.launch {
            val gameTypes = gameRegistry.getAll().map { it.gameType }
            
            when (val result = repository.loadAll(gameTypes)) {
                is Result.Success -> {
                    _sessions.value = result.value
                    _error.value = null
                }
                is Result.Error -> {
                    _error.value = result.error.message
                }
            }
        }
    }
    
    /**
     * Selects a game type as the active game.
     * 
     * @param gameType The game type to select
     */
    fun selectGame(gameType: GameType) {
        _activeGameType.value = gameType
    }
    
    /**
     * Records a new observation for the active game.
     * 
     * Validates the observation using ObservationEngine, updates the session,
     * and persists it to storage.
     * 
     * @param hits The list of card hits for this round
     * @return Result indicating success or validation error
     */
    fun recordObservation(hits: List<String>): Result<Unit, ValidationError> {
        val currentGameType = _activeGameType.value
            ?: return Result.Error(
                ValidationError(
                    code = ValidationErrorCode.INVALID_OUTCOME,
                    message = "No game selected"
                )
            )
        
        val config = gameRegistry.getById(currentGameType)
            ?: return Result.Error(
                ValidationError(
                    code = ValidationErrorCode.INVALID_OUTCOME,
                    message = "Game configuration not found"
                )
            )
        
        val currentSession = _sessions.value[currentGameType]
            ?: ObservationEngine.resetSession(currentGameType)
        
        // Validate and record observation
        return when (val result = ObservationEngine.recordObservation(currentSession, config, hits)) {
            is Result.Success -> {
                val updatedSession = result.value
                
                // Update local state
                _sessions.value = _sessions.value + (currentGameType to updatedSession)
                
                // Persist to storage
                scope.launch {
                    when (val saveResult = repository.save(updatedSession)) {
                        is Result.Error -> {
                            _error.value = saveResult.error.message
                        }
                        is Result.Success -> {
                            _error.value = null
                        }
                    }
                }
                
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }
    
    /**
     * Resets the session for the active game.
     * Clears all observations and statistics, and persists the empty session.
     */
    fun resetSession() {
        val currentGameType = _activeGameType.value ?: return
        
        val emptySession = ObservationEngine.resetSession(currentGameType)
        
        // Update local state
        _sessions.value = _sessions.value + (currentGameType to emptySession)
        
        // Persist to storage
        scope.launch {
            when (val result = repository.save(emptySession)) {
                is Result.Error -> {
                    _error.value = result.error.message
                }
                is Result.Success -> {
                    _error.value = null
                }
            }
        }
    }
    
    /**
     * Deletes the session for a specific game type.
     * Removes it from local state and persistent storage.
     * 
     * @param gameType The game type whose session should be deleted
     */
    fun deleteSession(gameType: GameType) {
        // Update local state
        _sessions.value = _sessions.value - gameType
        
        // Delete from storage
        scope.launch {
            when (val result = repository.delete(gameType)) {
                is Result.Error -> {
                    _error.value = result.error.message
                }
                is Result.Success -> {
                    _error.value = null
                }
            }
        }
    }
    
    /**
     * Sets the strategy mode (1, 2, or 3).
     * 
     * @param mode The strategy mode to set (1 = single card, 2 = two cards, 3 = three cards)
     */
    fun setStrategyMode(mode: Int) {
        if (mode in 1..3) {
            _strategyMode.value = mode
        }
    }
    
    /**
     * Gets the current session for the active game.
     * Returns null if no game is selected or no session exists.
     */
    fun getCurrentSession(): GameSession? {
        val currentGameType = _activeGameType.value ?: return null
        return _sessions.value[currentGameType]
    }
    
    /**
     * Gets the game configuration for the active game.
     * Returns null if no game is selected.
     */
    fun getCurrentGameConfig(): GameConfig? {
        val currentGameType = _activeGameType.value ?: return null
        return gameRegistry.getById(currentGameType)
    }
    
    /**
     * Clears any error message.
     */
    fun clearError() {
        _error.value = null
    }
}
