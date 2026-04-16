import SwiftUI

/// DisclaimerBannerView displays disclaimer text.
/// Shows base disclaimer always, and additional risk note when mode > 1.
///
/// Requirements: 10.1, 10.2, 10.3
///
/// - Parameters:
///   - strategyMode: The current strategy mode (1, 2, or 3)
struct DisclaimerBannerView: View {
    let strategyMode: Int
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Disclaimer header
            Text("⚠️ Disclaimer")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.primary)
            
            // Base disclaimer text
            Text("This app provides statistical analysis for entertainment purposes only. Past outcomes do not guarantee future results. Gambling involves risk. Please play responsibly.")
                .font(.caption)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            
            // Risk note when mode > 1
            if strategyMode > 1 {
                Text("⚠️ Higher Risk: Selecting \(strategyMode) cards increases your exposure. The more cards you select, the higher your potential losses.")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.red)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 4)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.systemRed).opacity(0.1))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color(.systemRed).opacity(0.3), lineWidth: 1)
        )
    }
}

// MARK: - Preview
#Preview("Mode 1 - Base Disclaimer Only") {
    DisclaimerBannerView(strategyMode: 1)
        .padding()
}

#Preview("Mode 2 - With Risk Note") {
    DisclaimerBannerView(strategyMode: 2)
        .padding()
}

#Preview("Mode 3 - With Risk Note") {
    DisclaimerBannerView(strategyMode: 3)
        .padding()
}

#Preview("In Context - Strategy Screen") {
    ScrollView {
        VStack(spacing: 16) {
            Text("Strategy Recommendations")
                .font(.title2)
                .fontWeight(.bold)
            
            // Mock strategy content
            VStack(alignment: .leading, spacing: 8) {
                Text("Recommended Cards")
                    .font(.headline)
                
                HStack {
                    Text("Ace")
                        .padding()
                        .background(Color.blue.opacity(0.2))
                        .cornerRadius(8)
                    
                    Text("King")
                        .padding()
                        .background(Color.blue.opacity(0.2))
                        .cornerRadius(8)
                }
                
                Text("Win Probability: 45.2%")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
            
            // Disclaimer banner
            DisclaimerBannerView(strategyMode: 2)
        }
        .padding()
    }
}
