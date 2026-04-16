package com.jcdongalen.peryaodds.shared.domain.registry

import com.jcdongalen.peryaodds.shared.domain.models.GameConfig

/**
 * Central registry of all supported game types.
 * Each game registers a GameConfig object.
 */
interface GameRegistry {
    /**
     * Returns all registered game configurations.
     */
    fun getAll(): List<GameConfig>

    /**
     * Returns the game configuration for the specified game type.
     * @param gameType The game type identifier (e.g., "three_ball_drop")
     * @return The GameConfig if found, null otherwise
     */
    fun getById(gameType: String): GameConfig?

    /**
     * Registers a new game configuration.
     * @param config The game configuration to register
     */
    fun register(config: GameConfig)
}

/**
 * Default implementation of GameRegistry using a MutableMap as backing store.
 */
class DefaultGameRegistry : GameRegistry {
    private val games: MutableMap<String, GameConfig> = mutableMapOf()

    override fun getAll(): List<GameConfig> {
        return games.values.toList()
    }

    override fun getById(gameType: String): GameConfig? {
        return games[gameType]
    }

    override fun register(config: GameConfig) {
        games[config.gameType] = config
    }
}
