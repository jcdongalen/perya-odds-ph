package com.jcdongalen.peryaodds.android.di

import com.jcdongalen.peryaodds.shared.data.repository.SessionRepository
import com.jcdongalen.peryaodds.shared.data.repository.createSessionRepository
import com.jcdongalen.peryaodds.shared.domain.registry.GameRegistry
import com.jcdongalen.peryaodds.shared.domain.registry.GameRegistryProvider
import com.jcdongalen.peryaodds.shared.presentation.GameSessionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    
    // Provide GameRegistry as singleton
    single<GameRegistry> {
        GameRegistryProvider.registry
    }
    
    // Provide SessionRepository as singleton
    single<SessionRepository> {
        createSessionRepository(androidContext())
    }
    
    // Provide CoroutineScope for ViewModel
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
    
    // Provide GameSessionViewModel
    single {
        GameSessionViewModel(
            gameRegistry = get(),
            repository = get(),
            scope = get()
        )
    }
}
