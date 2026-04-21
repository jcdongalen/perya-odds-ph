package com.jcdongalen.peryaodds.android.di

import com.jcdongalen.peryaodds.android.BuildConfig
import com.jcdongalen.peryaodds.shared.data.repository.SessionRepository
import com.jcdongalen.peryaodds.shared.data.repository.createSessionRepository
import com.jcdongalen.peryaodds.shared.domain.engines.DefaultProbabilityEngine
import com.jcdongalen.peryaodds.shared.domain.engines.ProbabilityEngine
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

    // Provide ProbabilityEngine with reduced thresholds on debug builds
    // Debug:   Low < 10, Medium < 20, High < 50  (reach VeryHigh in ~50 rounds)
    // Release: Low < 100, Medium < 200, High < 500
    single<ProbabilityEngine> {
        if (BuildConfig.DEBUG) {
            DefaultProbabilityEngine(
                lowThreshold    = 10,
                mediumThreshold = 20,
                highThreshold   = 50
            )
        } else {
            DefaultProbabilityEngine(
                lowThreshold    = 100,
                mediumThreshold = 200,
                highThreshold   = 500
            )
        }
    }

    // Provide GameSessionViewModel
    single {
        GameSessionViewModel(
            gameRegistry = get(),
            repository   = get(),
            scope        = get()
        )
    }
}
