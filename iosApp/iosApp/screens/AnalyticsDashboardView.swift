import SwiftUI
import shared

/// Analytics Dashboard displays frequency distributions, deviation scores,
/// and bias classifications for all outcomes in the active game.
///
/// Validates: Requirements 1.4, 4.5, 9.1, 9.2, 9.3, 9.4
///
/// - Parameter viewModel: The GameSessionViewModelWrapper providing access to session data
struct AnalyticsDashboardView: View {
    @ObservedObject var viewModel: GameSessionViewModelWrapper
    
    // Engines for computing analytics
    private let probabilityEngine = DefaultProbabilityEngine()
    
    @State private var showResetAlert = false
    
    var body: some View {
        NavigationStack {
            Group {
                if let session = viewModel.getCurrentSession(),
                   let gameConfig = viewModel.getCurrentGameConfig() {
                    
                    if session.totalRounds == 0 {
                        // Empty state - no data recorded yet
                        emptyStateView
                    } else {
                        // Analytics content
                        analyticsContent(session: session, gameConfig: gameConfig)
                    }
                } else {
                    // No game selected
                    noGameSelectedView
                }
            }
            .navigationTitle("Analytics")
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
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
        VStack(spacing: 12) {
            Image(systemName: "chart.bar.xaxis")
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text("No data recorded yet")
                .font(.title3)
                .fontWeight(.medium)
            
            Text("Record some observations to see analytics")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    // MARK: - No Game Selected
    
    private var noGameSelectedView: some View {
        VStack(spacing: 12) {
            Image(systemName: "gamecontroller")
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text("No game selected")
                .font(.title3)
                .fontWeight(.medium)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    // MARK: - Analytics Content
    
    private func analyticsContent(session: GameSession, gameConfig: GameConfig) -> some View {
        let probabilityResult = probabilityEngine.computeProbabilities(session: session, config: gameConfig)
        let biasResult = BiasDetectionEngine.shared.detectBias(probResult: probabilityResult)
        
        return ScrollView {
            VStack(spacing: 16) {
                // Summary card
                summaryCard(session: session, probabilityResult: probabilityResult)
                
                // Outcome analytics cards
                ForEach(gameConfig.outcomes, id: \.self) { outcome in
                    if let outcomeProbability = probabilityResult.perOutcome[outcome],
                       let outcomeBias = biasResult.perOutcome[outcome] {
                        outcomeCard(
                            outcome: outcome,
                            probability: outcomeProbability,
                            bias: outcomeBias
                        )
                    }
                }
            }
            .padding()
        }
    }
    
    // MARK: - Summary Card
    
    private func summaryCard(session: GameSession, probabilityResult: ProbabilityResult) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Summary")
                .font(.headline)
            
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Total Rounds")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(session.totalRounds)")
                        .font(.title2)
                        .fontWeight(.semibold)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text("Confidence")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(confidenceLevelText(probabilityResult.confidenceLevel))
                        .font(.title3)
                        .fontWeight(.medium)
                        .foregroundColor(confidenceLevelColor(probabilityResult.confidenceLevel))
                }
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
        )
    }
    
    // MARK: - Outcome Card
    
    private func outcomeCard(
        outcome: String,
        probability: OutcomeProbability,
        bias: OutcomeBias
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header with outcome name and bias indicator
            HStack {
                Text(outcome)
                    .font(.headline)
                
                Spacer()
                
                BiasIndicatorView(biasType: mapBiasClassification(bias.classification))
            }
            
            // Probability bar
            ProbabilityBarView(
                label: "Probability",
                observedProbability: probability.observed,
                expectedProbability: probability.expected
            )
            
            // Deviation score
            Text("Deviation: \(String(format: "%.2f", bias.deviation * 100))%")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
        )
    }
    
    // MARK: - Helper Functions
    
    /// Maps Kotlin BiasClassification to Swift BiasType
    private func mapBiasClassification(_ classification: BiasClassification) -> BiasType {
        switch classification {
        case .hot:
            return .hot
        case .cold:
            return .cold
        case .neutral:
            return .neutral
        default:
            return .neutral
        }
    }
    
    /// Returns display text for confidence level
    private func confidenceLevelText(_ level: ConfidenceLevel) -> String {
        switch level {
        case .low:
            return "Low"
        case .medium:
            return "Medium"
        case .high:
            return "High"
        case .veryhigh:
            return "Very High"
        default:
            return "Unknown"
        }
    }
    
    /// Returns color for confidence level
    private func confidenceLevelColor(_ level: ConfidenceLevel) -> Color {
        switch level {
        case .low:
            return .orange
        case .medium:
            return .yellow
        case .high:
            return .green
        case .veryhigh:
            return .blue
        default:
            return .gray
        }
    }
}

// MARK: - Preview
#Preview("With Data") {
    let viewModel = GameSessionViewModelWrapper(
        viewModel: GameSessionViewModel(
            gameRegistry: GameRegistryProvider.shared.registry,
            repository: SessionRepositoryImpl(),
            scope: Kotlinx_coroutines_coreMainScope()
        )
    )
    
    return AnalyticsDashboardView(viewModel: viewModel)
}

#Preview("Empty State") {
    let viewModel = GameSessionViewModelWrapper(
        viewModel: GameSessionViewModel(
            gameRegistry: GameRegistryProvider.shared.registry,
            repository: SessionRepositoryImpl(),
            scope: Kotlinx_coroutines_coreMainScope()
        )
    )
    
    return AnalyticsDashboardView(viewModel: viewModel)
}
