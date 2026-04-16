import SwiftUI
import shared

/// ObservationView allows users to record round observations for the active game.
/// Displays the active game name, renders CardSelectorView, and handles observation recording.
///
/// Requirements: 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
struct ObservationView: View {
    @ObservedObject var viewModel: GameSessionViewModelWrapper
    
    @State private var selectedCards: [String] = []
    @State private var errorMessage: String?
    @State private var showSuccessToast = false
    @State private var showResetAlert = false
    
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Game info card
                if let gameConfig = viewModel.getCurrentGameConfig(),
                   let session = viewModel.getCurrentSession() {
                    
                    VStack(alignment: .leading, spacing: 8) {
                        Text(gameConfig.displayName)
                            .font(.title2)
                            .fontWeight(.semibold)
                        
                        Text("Total Rounds: \(session.totalRounds)")
                            .font(.headline)
                            .foregroundColor(.secondary)
                        
                        Text("Select \(gameConfig.hitsPerRound) cards for this round")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(.systemGray6))
                    )
                    
                    // Selection status
                    Text("Selected: \(selectedCards.count) / \(gameConfig.hitsPerRound)")
                        .font(.headline)
                        .foregroundColor(
                            selectedCards.count == gameConfig.hitsPerRound
                                ? .accentColor
                                : .secondary
                        )
                    
                    // Error message (inline validation)
                    if let error = errorMessage {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                            Text(error)
                                .font(.subheadline)
                                .foregroundColor(.red)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color.red.opacity(0.1))
                        )
                    }
                    
                    // Card selector
                    CardSelectorView(
                        outcomes: gameConfig.outcomes,
                        selectedCards: $selectedCards,
                        maxSelection: Int(gameConfig.hitsPerRound)
                    )
                    
                    // Confirm button
                    Button(action: confirmSelection) {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                            Text("Confirm Round")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(
                                    selectedCards.count == gameConfig.hitsPerRound
                                        ? Color.accentColor
                                        : Color.gray
                                )
                        )
                        .foregroundColor(.white)
                    }
                    .disabled(selectedCards.count != gameConfig.hitsPerRound)
                    
                } else {
                    // No game selected
                    VStack(spacing: 16) {
                        Image(systemName: "questionmark.circle")
                            .font(.system(size: 60))
                            .foregroundColor(.secondary)
                        Text("No game selected")
                            .font(.title2)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxHeight: .infinity)
                    .padding()
                }
            }
            .padding()
        }
        .navigationTitle("Observe")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    showResetAlert = true
                }) {
                    Image(systemName: "trash")
                        .foregroundColor(.red)
                }
            }
        }
        .alert("Reset Data", isPresented: $showResetAlert) {
            Button("Cancel", role: .cancel) { }
            Button("Reset", role: .destructive) {
                viewModel.resetSession()
                selectedCards = []
                errorMessage = nil
            }
        } message: {
            Text("Are you sure you want to reset all game data? This action cannot be undone.")
        }
        .overlay(
            // Success toast
            Group {
                if showSuccessToast {
                    VStack {
                        Spacer()
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("Round recorded successfully!")
                                .font(.subheadline)
                                .fontWeight(.medium)
                        }
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color(.systemBackground))
                                .shadow(radius: 8)
                        )
                        .padding(.bottom, 32)
                    }
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .animation(.spring(), value: showSuccessToast)
                }
            }
        )
    }
    
    // MARK: - Actions
    
    /// Confirms the current card selection and records the observation
    private func confirmSelection() {
        guard let gameConfig = viewModel.getCurrentGameConfig() else {
            return
        }
        
        // Validate selection count
        if selectedCards.count < gameConfig.hitsPerRound {
            errorMessage = "Please select \(gameConfig.hitsPerRound) cards"
            return
        }
        
        if selectedCards.count > gameConfig.hitsPerRound {
            errorMessage = "Too many cards selected"
            return
        }
        
        // Record observation via ViewModel
        let result = viewModel.recordObservation(hits: selectedCards)
        
        // Handle result
        if result is Result_.Success {
            // Success - clear selection and show toast
            selectedCards = []
            errorMessage = nil
            showSuccessToast = true
            
            // Hide toast after 2 seconds
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                showSuccessToast = false
            }
        } else if let error = result as? Result_.Error {
            // Error - show validation message
            errorMessage = error.error.message
        }
    }
}

#Preview("With Active Game") {
    NavigationStack {
        ObservationView(
            viewModel: GameSessionViewModelWrapper(
                viewModel: GameSessionViewModel(
                    gameRegistry: GameRegistryProvider.shared.registry,
                    repository: SessionRepositoryImpl(),
                    scope: Kotlinx_coroutines_coreMainScope()
                )
            )
        )
    }
}
