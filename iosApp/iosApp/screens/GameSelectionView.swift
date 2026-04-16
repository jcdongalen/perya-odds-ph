import SwiftUI
import shared

/// Game selection screen that displays all registered game types
/// Validates: Requirements 1.1, 1.2, 1.3, 1.8
struct GameSelectionView: View {
    @ObservedObject var viewModel: GameSessionViewModelWrapper
    
    var body: some View {
        List {
            ForEach(GameRegistryProvider.shared.provideRegistry().getAll(), id: \.gameType) { game in
                GameCard(game: game, viewModel: viewModel)
            }
        }
        .navigationTitle("Select a Game")
    }
}

/// Individual game card component
private struct GameCard: View {
    let game: GameConfig
    @ObservedObject var viewModel: GameSessionViewModelWrapper
    
    var body: some View {
        Button(action: {
            if !game.comingSoon {
                viewModel.selectGame(gameType: game.gameType)
            }
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(game.displayName)
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                    
                    Text("\(game.outcomes.count) outcomes, \(game.hitsPerRound) hits per round")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                if game.comingSoon {
                    Text("Coming Soon")
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.secondary.opacity(0.2))
                        .foregroundColor(.secondary)
                        .cornerRadius(4)
                }
            }
            .padding(.vertical, 8)
        }
        .disabled(game.comingSoon)
        .opacity(game.comingSoon ? 0.6 : 1.0)
    }
}
