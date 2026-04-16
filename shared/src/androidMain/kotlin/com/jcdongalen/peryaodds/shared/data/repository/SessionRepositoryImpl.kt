package com.jcdongalen.peryaodds.shared.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jcdongalen.peryaodds.shared.domain.models.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android implementation of SessionRepository using DataStore Preferences.
 */
class SessionRepositoryImpl(private val context: Context) : SessionRepository {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "perya_odds_sessions")
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    override suspend fun loadAll(gameTypes: List<GameType>): Result<Map<GameType, GameSession>, ValidationError> {
        return try {
            val sessions = mutableMapOf<GameType, GameSession>()
            
            val preferences = context.dataStore.data.first()
            
            gameTypes.forEach { gameType ->
                val key = stringPreferencesKey(getStorageKey(gameType))
                val sessionJson = preferences[key]
                
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
            val key = stringPreferencesKey(getStorageKey(session.gameType))
            val sessionJson = json.encodeToString(session)
            
            context.dataStore.edit { preferences ->
                preferences[key] = sessionJson
            }
            
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
            val key = stringPreferencesKey(getStorageKey(gameType))
            
            context.dataStore.edit { preferences ->
                preferences.remove(key)
            }
            
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
 * Android actual implementation of createSessionRepository.
 * Requires Android Context to be provided.
 */
actual fun createSessionRepository(): SessionRepository {
    throw IllegalStateException(
        "createSessionRepository() requires Android Context. " +
        "Use createSessionRepository(context) instead."
    )
}

/**
 * Android-specific factory function that accepts Context.
 */
fun createSessionRepository(context: Context): SessionRepository {
    return SessionRepositoryImpl(context)
}
