import SwiftUI

/// WinProbabilityDisplayView shows the computed win probability as a percentage.
///
/// Validates: Requirements 6.4, 7.4, 8.4
///
/// - Parameter winProbability: The win probability (0.0 to 1.0)
struct WinProbabilityDisplayView: View {
    let winProbability: Double
    
    var body: some View {
        VStack(spacing: 8) {
            Text("Win Probability")
                .font(.headline)
                .foregroundColor(.primary)
            
            Text("\(Int(winProbability * 100))%")
                .font(.system(size: 56, weight: .bold))
                .foregroundColor(.blue)
            
            Text("Chance of winning in 3 rounds")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.blue.opacity(0.1))
        )
    }
}

// MARK: - Preview
#Preview("Various Probabilities") {
    VStack(spacing: 16) {
        WinProbabilityDisplayView(winProbability: 0.15)
        WinProbabilityDisplayView(winProbability: 0.45)
        WinProbabilityDisplayView(winProbability: 0.75)
        WinProbabilityDisplayView(winProbability: 0.95)
    }
    .padding()
}

#Preview("In Context") {
    ScrollView {
        VStack(spacing: 16) {
            Text("Strategy Recommendation")
                .font(.title2)
                .fontWeight(.bold)
            
            WinProbabilityDisplayView(winProbability: 0.68)
            
            Text("Selected Cards: Ace, King, Queen")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}
