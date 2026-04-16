import SwiftUI
import shared

/// Root view for the Perya Odds iOS app
/// Implements NavigationStack + TabView navigation structure
struct ContentView: View {
    @StateObject private var viewModel: GameSessionViewModelWrapper
    
    init(sharedViewModel: GameSessionViewModel) {
        // Initialize the wrapper with the shared Kotlin ViewModel
        _viewModel = StateObject(wrappedValue: GameSessionViewModelWrapper(viewModel: sharedViewModel))
    }
    
    var body: some View {
        NavigationStack {
            if viewModel.activeGameType == nil {
                // No game selected - show game selection
                GameSelectionView(viewModel: viewModel)
            } else {
                // Game selected - show main tab navigation
                MainTabView(viewModel: viewModel)
            }
        }
        .alert("Error", isPresented: .constant(viewModel.error != nil)) {
            Button("OK") {
                viewModel.clearError()
            }
        } message: {
            if let error = viewModel.error {
                Text(error)
            }
        }
    }
}

// GameSelectionView is now implemented in screens/GameSelectionView.swift

/// Main tab view containing the three primary screens
struct MainTabView: View {
    @ObservedObject var viewModel: GameSessionViewModelWrapper
    
    var body: some View {
        TabView {
            ObservationView(viewModel: viewModel)
                .tabItem {
                    Label("Observe", systemImage: "pencil")
                }
            
            AnalyticsDashboardView(viewModel: viewModel)
                .tabItem {
                    Label("Analytics", systemImage: "chart.bar")
                }
            
            StrategyView(viewModel: viewModel)
                .tabItem {
                    Label("Strategy", systemImage: "lightbulb")
                }
        }
    }
}

// ObservationView is now implemented in screens/ObservationView.swift
// AnalyticsDashboardView is now implemented in screens/AnalyticsDashboardView.swift
// StrategyView is now implemented in screens/StrategyView.swift

#Preview {
    // Preview with mock shared ViewModel
    // Note: This requires the shared framework to be built
    ContentView(sharedViewModel: GameSessionViewModel(
        gameRegistry: GameRegistryProvider.shared.registry,
        repository: SessionRepositoryImpl(),
        scope: Kotlinx_coroutines_coreMainScope()
    ))
}
