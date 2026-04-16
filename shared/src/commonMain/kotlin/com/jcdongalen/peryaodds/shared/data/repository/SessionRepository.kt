package com.jcdongalen.peryaodds.shared.data.repository

import com.jcdongalen.peryaodds.shared.domain.models.GameSession
import com.jcdongalen.peryaodds.shared.domain.models.GameType
import com.jcdongalen.peryaodds.shared.domain.models.Result
import com.jcdongalen.peryaodds.shared.domain.models.ValidationError

/**
 * SessionRepository defines the interface for persisting and retrieving game sessions.
 * 
 * Storage key format: perya_odds_session_{gameType}
 * 
 * Platform implementations:
 * - Android: Uses DataStore Preferences
 * - iOS: Uses UserDefaults
 */
interface SessionRepository {
    
    /**
     * Loads all game sessions for the registered game types.
     * Returns empty sessions for game types that have no saved data.
     * 
     * @param gameTypes List of game types to load
     * @return Result containing map of GameType to GameSession, or error if load fails
     */
    suspend fun loadAll(gameTypes: List<GameType>): Result<Map<GameType, GameSession>, ValidationError>
    
    /**
     * Saves a game session to persistent storage.
     * Serializes the session to JSON and stores it with key: perya_odds_session_{gameType}
     * 
     * @param session The game session to save
     * @return Result indicating success or failure
     */
    suspend fun save(session: GameSession): Result<Unit, ValidationError>
    
    /**
     * Deletes a game session from persistent storage.
     * Removes the key: perya_odds_session_{gameType}
     * 
     * @param gameType The game type whose session should be deleted
     * @return Result indicating success or failure
     */
    suspend fun delete(gameType: GameType): Result<Unit, ValidationError>
}

/**
 * Factory function to create platform-specific SessionRepository implementation.
 * This is an expect function that will be implemented differently on each platform.
 */
expect fun createSessionRepository(): SessionRepository
