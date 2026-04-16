package com.jcdongalen.peryaodds.shared.data.repository

import com.jcdongalen.peryaodds.shared.domain.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of SessionRepository using UserDefaults.
 */
class SessionRepositoryImpl : SessionRepository {
    
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    override suspend fun loadAll(gameTypes: List<GameType>): Result<Map<GameType, GameSession>, ValidationError> {
        return try {
            val sessions = mutableMapOf<GameType, GameSession>()
            
            gameTypes.forEach { gameType ->
                val key = getStorageKey(gameType)
                val sessionJson = userDefaults.stringForKey(key)
                
                if (sessionJson != null) {
                    try {
                        val session = json.decodeFromString<GameSession>(sessionJson)
                        sessions[gameType] = session
                    } catch (e: Exception) {
                        // If deserialization fails, return empty session for this game type
                        sessions[gameType] = createEmptySession(gameType)
                    }
                } else {
                    // No saved data, return empty session
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
            
            userDefaults.setObject(sessionJson, forKey = key)
            userDefaults.synchronize()
            
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
            
            userDefaults.removeObjectForKey(key)
            userDefaults.synchronize()
            
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
}

/**
 * iOS actual implementation of createSessionRepository.
 */
actual fun createSessionRepository(): SessionRepository {
    return SessionRepositoryImpl()
}
