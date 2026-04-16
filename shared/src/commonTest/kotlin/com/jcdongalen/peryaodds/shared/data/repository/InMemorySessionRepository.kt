package com.jcdongalen.peryaodds.shared.data.repository

import com.jcdongalen.peryaodds.shared.domain.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * In-memory mock implementation of SessionRepository for testing.
 * Simulates persistence using a mutable map.
 */
class InMemorySessionRepository : SessionRepository {
    
    private val storage = mutableMapOf<String, String>()
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    override suspend fun loadAll(gameTypes: List<GameType>): Result<Map<GameType, GameSession>, ValidationError> {
        return try {
            val sessions = mutableMapOf<GameType, GameSession>()
            
            gameTypes.forEach { gameType ->
                val key = getStorageKey(gameType)
                val sessionJson = storage[key]
                
                if (sessionJson != null) {
                    try {
                        val session = json.decodeFromString<GameSession>(sessionJson)
                        sessions[gameType] = session
                    } catch (e: Exception) {
                        sessions[gameType] = createEmptySession(gameType)
                    }
                } else {
                    sessions[gameType] = createEmptySession(gameType)
                }
            }
            
            Result.Success(sessions)
        } catch (e: Exception) {
            Result.Error(
                ValidationError(
                    code = ValidationErrorCode.STORAGE_ERROR,
                    message = "Failed to load sessions: ${e.message}"
                )
            )
        }
    }
    
    override suspend fun save(session: GameSession): Result<Unit, ValidationError> {
        return try {
            val key = getStorageKey(session.gameType)
            val sessionJson = json.encodeToString(session)
            storage[key] = sessionJson
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                ValidationError(
                    code = ValidationErrorCode.STORAGE_ERROR,
                    message = "Failed to save session: ${e.message}"
                )
            )
        }
    }
    
    override suspend fun delete(gameType: GameType): Result<Unit, ValidationError> {
        return try {
            val key = getStorageKey(gameType)
            storage.remove(key)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                ValidationError(
                    code = ValidationErrorCode.STORAGE_ERROR,
                    message = "Failed to delete session: ${e.message}"
                )
            )
        }
    }
    
    private fun getStorageKey(gameType: GameType): String {
        return "perya_odds_session_$gameType"
    }
    
    private fun createEmptySession(gameType: GameType): GameSession {
        return GameSession(
            gameType = gameType,
            totalRounds = 0,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )
    }
    
    /**
     * Test helper to clear all stored data.
     */
    fun clear() {
        storage.clear()
    }
}
