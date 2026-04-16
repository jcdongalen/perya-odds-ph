package com.jcdongalen.peryaodds.shared.domain.engines

import com.jcdongalen.peryaodds.shared.domain.models.*
import com.jcdongalen.peryaodds.shared.domain.models.Result
import kotlin.random.Random

/**
 * ObservationEngine is responsible for validating and recording round observations.
 * 
 * It validates that hits match the game configuration and returns updated sessions
 * following an immutable update pattern.
 */
object ObservationEngine {
    
    /**
     * Records a new observation for the given session.
     * 
     * Validates that:
     * - hits.size == config.hitsPerRound
     * - all hits are valid outcomes from config.outcomes
     * 
     * On success, returns an updated GameSession with:
     * - totalRounds incremented by 1
     * - hitCounts updated for each card in hits
     * - new observation appended to observations list
     * - lastUpdated timestamp updated
     * 
     * On failure, returns a ValidationError without mutating the session.
     * 
     * @param session The current game session
     * @param config The game configuration
     * @param hits The list of card hits for this round
     * @return Result containing either the updated session or a validation error
     */
    fun recordObservation(
        session: GameSession,
        config: GameConfig,
        hits: List<String>
    ): Result<GameSession, ValidationError> {
        // Validate hits count
        when {
            hits.size < config.hitsPerRound -> {
                return Result.Error(
                    ValidationError(
                        code = ValidationErrorCode.TOO_FEW_HITS,
                        message = "Expected ${config.hitsPerRound} hits, but got ${hits.size}"
                    )
                )
            }
            hits.size > config.hitsPerRound -> {
                return Result.Error(
                    ValidationError(
                        code = ValidationErrorCode.TOO_MANY_HITS,
                        message = "Expected ${config.hitsPerRound} hits, but got ${hits.size}"
                    )
                )
            }
        }
        
        // Validate all hits are valid outcomes
        val invalidHits = hits.filter { it !in config.outcomes }
        if (invalidHits.isNotEmpty()) {
            return Result.Error(
                ValidationError(
                    code = ValidationErrorCode.INVALID_OUTCOME,
                    message = "Invalid outcomes: ${invalidHits.joinToString(", ")}"
                )
            )
        }
        
        // Create new observation
        val observation = Observation(
            id = generateObservationId(),
            timestamp = currentTimeMillis(),
            hits = hits
        )
        
        // Update hit counts (immutable)
        val updatedHitCounts = session.hitCounts.toMutableMap()
        hits.forEach { card ->
            updatedHitCounts[card] = (updatedHitCounts[card] ?: 0) + 1
        }
        
        // Return updated session
        return Result.Success(
            session.copy(
                totalRounds = session.totalRounds + 1,
                hitCounts = updatedHitCounts.toMap(),
                observations = session.observations + observation,
                lastUpdated = currentTimeMillis()
            )
        )
    }
    
    /**
     * Resets the session for the given game type, returning a fresh GameSession
     * with all statistics cleared.
     * 
     * @param gameType The game type identifier
     * @return A fresh GameSession with totalRounds = 0, zeroed hitCounts, empty observations
     */
    fun resetSession(gameType: GameType): GameSession {
        return GameSession(
            gameType = gameType,
            totalRounds = 0,
            hitCounts = emptyMap(),
            observations = emptyList(),
            lastUpdated = currentTimeMillis()
        )
    }
    
    /**
     * Returns the current time in milliseconds since Unix epoch.
     * This is a separate function to allow for easier testing/mocking if needed.
     */
    private fun currentTimeMillis(): Long {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }
    
    /**
     * Generates a unique observation ID using timestamp and random number.
     * Format: {timestamp}-{random}
     */
    private fun generateObservationId(): String {
        val timestamp = currentTimeMillis()
        val random = Random.nextInt(100000, 999999)
        return "$timestamp-$random"
    }
}
