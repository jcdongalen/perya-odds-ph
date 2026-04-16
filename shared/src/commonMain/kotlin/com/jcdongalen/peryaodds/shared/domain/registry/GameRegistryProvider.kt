package com.jcdongalen.peryaodds.shared.domain.registry

import com.jcdongalen.peryaodds.shared.domain.registry.games.ThreeBallDropConfig

/**
 * Bootstrap object that provides a pre-configured GameRegistry instance
 * with all supported game types registered.
 * 
 * This is the single source of truth for game registration in the app.
 */
object GameRegistryProvider {
    /**
     * Creates and returns a new GameRegistry instance with all games registered.
     */
    fun provideRegistry(): GameRegistry {
        val registry = DefaultGameRegistry()
        
        // Register MVP game
        registry.register(ThreeBallDropConfig)
        
        // Future games will be registered here with comingSoon = true
        
        return registry
    }
}
