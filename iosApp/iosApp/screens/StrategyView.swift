import SwiftUI
import shared

/// StrategyView displays strategy recommendations based on observed data.
/// Derives StrategyResult using StrategyEngine and renders:
/// - Strategy mode toggle (1, 2, or 3 cards)
/// - Win probability display
/// - Confidence badge
/// - Recommended cards with individual probabilities
/// - High-exposure warning when mode == 3
/// - Disclaimer banner
///
/// Requirements: 1.4, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 8.1, 8.2, 8.3, 8.4, 8.5, 10.1, 10.2, 10.3
struct StrategyView: View {
    @ObservedObject var viewModel: GameSessionViewModelWrapper
    
    @State private var showResetAlert = false
    
    var body: some View {
        NavigationStack {
            ScrollView {
                if let session = viewModel.getCurrentSession(),
                   let config = viewModel.getCurrentGameConfig() {
                    // Compute probabilities and strategy
                    let probabilityEngine = DefaultProbabilityEngine()
                    let strategyEngine = DefaultStrategyEngine(probabilityEngine: probabilityEngine)
                    let strategyResult = strategyEngine.recommend(
                        session: session,
                        config: config,
                        mode: viewModel.strategyMode
                    )
                    
                    VStack(spacing: 20) {
                        // Strategy mode toggle
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Select Strategy Mode")
                                .font(.headline)
                            
                            StrategyModeToggleView(
                                selectedMode: Int(viewModel.strategyMode),
                                onModeSelected: { mode in
                                    viewModel.setStrategyMode(mode: Int32(mode))
                                }
                            )
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color(.systemBackground))
                                .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
                        )
                        
                        // Win probability display
                        WinProbabilityDisplayView(
                            winProbability: strategyResult.winProbability
                        )
                        
                        // Confidence badge
                        HStack {
                            Spacer()
                            ConfidenceBadgeView(
                                confidenceLevel: mapConfidenceLevel(strategyResult.confidenceLevel)
                            )
                            Spacer()
                        }
                        
                        // Recommended cards
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Recommended Cards")
                                .font(.headline)
                            
                            ForEach(strategyResult.selectedCards, id: \.self) { card in
                                let probability = strategyResult.individualProbabilities[card] ?? 0.0
                                
                                HStack {
                                    Text(card)
                                        .font(.body)
                                        .fontWeight(.medium)
                                    
                                    Spacer()
                                    
                                    Text("\(Int(probability * 100))%")
                                        .font(.body)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.blue)
                                }
                                .padding(.vertical, 8)
                            }
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color(.systemBackground))
                                .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
                        )
                        
                        // High exposure warning for mode 3
                        if viewModel.strategyMode == 3 {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("⚠️ High Exposure Warning")
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.red)
                                
                                Text("Selecting 3 cards significantly increases your risk. You are betting on multiple outcomes simultaneously, which can lead to higher losses.")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(Color(.systemRed).opacity(0.1))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Color(.systemRed), lineWidth: 1)
                            )
                        }
                        
                        // Disclaimer banner
                        DisclaimerBannerView(strategyMode: Int(viewModel.strategyMode))
                    }
                    .padding()
                } else {
                    // No game selected
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        
                        Text("No game selected")
                            .font(.headline)
                            .foregroundColor(.secondary)
                        
                        Text("Please select a game from the Game Selection screen to view strategy recommendations.")
                            .font(.body)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .padding()
                }
            }
            .navigationTitle("Strategy")
            .navigationBarTitleDisplayMode(.large)
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
                }
            } message: {
                Text("Are you sure you want to reset all game data? This action cannot be undone.")
            }
        }
    }
    
    /// Maps Kotlin ConfidenceLevel enum to Swift ConfidenceLevelType
    private func mapConfidenceLevel(_ level: ConfidenceLevel) -> ConfidenceLevelType {
        switch level {
        case .low:
            return .low
        case .medium:
            return .medium
        case .high:
            return .high
        case .veryhigh:
            return .veryHigh
        default:
            return .low
        }
    }
}

// MARK: - Preview
#Preview("With Data") {
    // Mock wrapper with sample data
    let mockViewModel = GameSessionViewModelWrapper(
        viewModel: GameSessionViewModel(
            gameRegistry: GameRegistryProvider.shared.registry,
            repository: SessionRepositoryImpl(),
            scope: Kotlinx_coroutines_coreMainScope()
        )
    )
    
    return StrategyView(viewModel: mockViewModel)
}

#Preview("No Game Selected") {
    // Mock wrapper with no active game
    let mockViewModel = GameSessionViewModelWrapper(
        viewModel: GameSessionViewModel(
            gameRegistry: GameRegistryProvider.shared.registry,
            repository: SessionRepositoryImpl(),
            scope: Kotlinx_coroutines_coreMainScope()
        )
    )
    
    return StrategyView(viewModel: mockViewModel)
}
