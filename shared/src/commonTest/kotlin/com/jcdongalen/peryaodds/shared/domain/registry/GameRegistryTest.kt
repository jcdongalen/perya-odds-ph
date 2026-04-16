package com.jcdongalen.peryaodds.shared.domain.registry

import com.jcdongalen.peryaodds.shared.domain.models.GameConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRegistryTest {

    @Test
    fun `getAll returns empty list when no games registered`() {
        val registry = DefaultGameRegistry()
        assertTrue(registry.getAll().isEmpty())
    }

    @Test
    fun `register adds game to registry`() {
        val registry = DefaultGameRegistry()
        val config = GameConfig(
            gameType = "three_ball_drop",
            displayName = "3-Ball Drop Card Game",
            outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
            hitsPerRound = 3,
            expectedProbability = 1.0 / 6.0,
            comingSoon = false
        )

        registry.register(config)

        assertEquals(1, registry.getAll().size)
        assertEquals(config, registry.getAll().first())
    }

    @Test
    fun `getById returns correct game config`() {
        val registry = DefaultGameRegistry()
        val config = GameConfig(
            gameType = "three_ball_drop",
            displayName = "3-Ball Drop Card Game",
            outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
            hitsPerRound = 3,
            expectedProbability = 1.0 / 6.0,
            comingSoon = false
        )

        registry.register(config)

        val retrieved = registry.getById("three_ball_drop")
        assertEquals(config, retrieved)
    }

    @Test
    fun `getById returns null for non-existent game type`() {
        val registry = DefaultGameRegistry()
        assertNull(registry.getById("non_existent_game"))
    }

    @Test
    fun `register multiple games and getAll returns all`() {
        val registry = DefaultGameRegistry()
        val config1 = GameConfig(
            gameType = "three_ball_drop",
            displayName = "3-Ball Drop Card Game",
            outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
            hitsPerRound = 3,
            expectedProbability = 1.0 / 6.0,
            comingSoon = false
        )
        val config2 = GameConfig(
            gameType = "color_game",
            displayName = "Color Game",
            outcomes = listOf("Red", "Blue", "Yellow", "Green", "White", "Pink"),
            hitsPerRound = 1,
            expectedProbability = 1.0 / 6.0,
            comingSoon = true
        )

        registry.register(config1)
        registry.register(config2)

        assertEquals(2, registry.getAll().size)
        assertTrue(registry.getAll().contains(config1))
        assertTrue(registry.getAll().contains(config2))
    }

    @Test
    fun `register overwrites existing game with same gameType`() {
        val registry = DefaultGameRegistry()
        val config1 = GameConfig(
            gameType = "three_ball_drop",
            displayName = "3-Ball Drop Card Game",
            outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
            hitsPerRound = 3,
            expectedProbability = 1.0 / 6.0,
            comingSoon = false
        )
        val config2 = GameConfig(
            gameType = "three_ball_drop",
            displayName = "Updated 3-Ball Drop",
            outcomes = listOf("Ace", "King", "Queen", "Jack", "10", "9"),
            hitsPerRound = 3,
            expectedProbability = 1.0 / 6.0,
            comingSoon = true
        )

        registry.register(config1)
        registry.register(config2)

        assertEquals(1, registry.getAll().size)
        assertEquals("Updated 3-Ball Drop", registry.getById("three_ball_drop")?.displayName)
    }
}
