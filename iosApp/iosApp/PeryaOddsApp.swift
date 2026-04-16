import SwiftUI
import shared

/// Main entry point for the Perya Odds iOS app
@main
struct PeryaOddsApp: App {
    
    // MARK: - Shared ViewModel
    
    /// The shared Kotlin ViewModel instance (initialized once for the app lifecycle)
    private let sharedViewModel: GameSessionViewModel
    
    // MARK: - Initialization
    
    init() {
        // Initialize the shared Kotlin ViewModel with dependencies
        let registry = GameRegistryProvider.shared.provideRegistry()
        let repository = SessionRepositoryImpl()
        let scope = Kotlinx_coroutines_coreMainScope()
        
        self.sharedViewModel = GameSessionViewModel(
            gameRegistry: registry,
            repository: repository,
            scope: scope
        )
    }
    
    // MARK: - Body
    
    var body: some Scene {
        WindowGroup {
            ContentView(sharedViewModel: sharedViewModel)
        }
    }
}
