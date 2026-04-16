import Foundation
import Combine
import shared

/// ObservableObject wrapper around the shared Kotlin GameSessionViewModel.
/// Bridges Kotlin StateFlow to SwiftUI using Combine framework.
@MainActor
class GameSessionViewModelWrapper: ObservableObject {
    
    // MARK: - Published Properties
    
    /// Currently selected game type (nil if no game selected)
    @Published var activeGameType: String?
    
    /// Map of all game sessions by game type
    @Published var sessions: [String: GameSession] = [:]
    
    /// Current strategy mode (1, 2, or 3)
    @Published var strategyMode: Int32 = 1
    
    /// Error message (nil if no error)
    @Published var error: String?
    
    // MARK: - Private Properties
    
    /// The shared Kotlin ViewModel
    private let viewModel: GameSessionViewModel
    
    /// Handles for StateFlow observation (to cancel on deinit)
    private var observationHandles: [Closeable] = []
    
    // MARK: - Initialization
    
    init(viewModel: GameSessionViewModel) {
        self.viewModel = viewModel
        
        // Bridge Kotlin StateFlow to SwiftUI
        observeStateFlows()
    }
    
    deinit {
        // Cancel all StateFlow observations
        observationHandles.forEach { $0.close() }
    }
    
    // MARK: - StateFlow Observation
    
    /// Sets up observation of all StateFlows from the Kotlin ViewModel
    private func observeStateFlows() {
        // Observe activeGameType
        let activeGameTypeHandle = viewModel.activeGameType.watch { [weak self] (value: String?) in
            Task { @MainActor in
                self?.activeGameType = value
            }
        }
        observationHandles.append(activeGameTypeHandle)
        
        // Observe sessions
        let sessionsHandle = viewModel.sessions.watch { [weak self] (value: [String: GameSession]) in
            Task { @MainActor in
                self?.sessions = value
            }
        }
        observationHandles.append(sessionsHandle)
        
        // Observe strategyMode
        let strategyModeHandle = viewModel.strategyMode.watch { [weak self] (value: KotlinInt) in
            Task { @MainActor in
                self?.strategyMode = value.int32Value
            }
        }
        observationHandles.append(strategyModeHandle)
        
        // Observe error
        let errorHandle = viewModel.error.watch { [weak self] (value: String?) in
            Task { @MainActor in
                self?.error = value
            }
        }
        observationHandles.append(errorHandle)
    }
    
    // MARK: - Public Methods (Delegate to Kotlin ViewModel)
    
    /// Selects a game type as the active game
    /// - Parameter gameType: The game type to select
    func selectGame(gameType: String) {
        viewModel.selectGame(gameType: gameType)
    }
    
    /// Records a new observation for the active game
    /// - Parameter hits: The list of card hits for this round
    /// - Returns: Result indicating success or validation error
    func recordObservation(hits: [String]) -> Result_<KotlinUnit, ValidationError> {
        return viewModel.recordObservation(hits: hits)
    }
    
    /// Resets the session for the active game
    func resetSession() {
        viewModel.resetSession()
    }
    
    /// Deletes the session for a specific game type
    /// - Parameter gameType: The game type whose session should be deleted
    func deleteSession(gameType: String) {
        viewModel.deleteSession(gameType: gameType)
    }
    
    /// Sets the strategy mode (1, 2, or 3)
    /// - Parameter mode: The strategy mode to set
    func setStrategyMode(mode: Int32) {
        viewModel.setStrategyMode(mode: mode)
    }
    
    /// Gets the current session for the active game
    /// - Returns: The current game session, or nil if no game is selected
    func getCurrentSession() -> GameSession? {
        return viewModel.getCurrentSession()
    }
    
    /// Gets the game configuration for the active game
    /// - Returns: The game configuration, or nil if no game is selected
    func getCurrentGameConfig() -> GameConfig? {
        return viewModel.getCurrentGameConfig()
    }
    
    /// Clears any error message
    func clearError() {
        viewModel.clearError()
    }
}

