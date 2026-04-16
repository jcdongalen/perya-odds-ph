import SwiftUI

/// ProbabilityBarView displays observed vs expected frequency side-by-side.
///
/// Validates: Requirements 9.1
///
/// - Parameters:
///   - label: The card/outcome label
///   - observedProbability: The observed probability (0.0 to 1.0)
///   - expectedProbability: The expected probability (0.0 to 1.0)
struct ProbabilityBarView: View {
    let label: String
    let observedProbability: Double
    let expectedProbability: Double
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Label
            Text(label)
                .font(.body)
                .padding(.bottom, 4)
            
            HStack(spacing: 8) {
                // Observed bar
                VStack(alignment: .leading, spacing: 4) {
                    Text("Observed: \(Int(observedProbability * 100))%")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    GeometryReader { geometry in
                        ZStack(alignment: .leading) {
                            // Background
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color.gray.opacity(0.2))
                                .frame(height: 24)
                            
                            // Filled portion
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color.blue)
                                .frame(
                                    width: geometry.size.width * CGFloat(observedProbability),
                                    height: 24
                                )
                        }
                    }
                    .frame(height: 24)
                }
                .frame(maxWidth: .infinity)
                
                // Expected bar
                VStack(alignment: .leading, spacing: 4) {
                    Text("Expected: \(Int(expectedProbability * 100))%")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    GeometryReader { geometry in
                        ZStack(alignment: .leading) {
                            // Background
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color.gray.opacity(0.2))
                                .frame(height: 24)
                            
                            // Filled portion
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color.orange)
                                .frame(
                                    width: geometry.size.width * CGFloat(expectedProbability),
                                    height: 24
                                )
                        }
                    }
                    .frame(height: 24)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Preview
#Preview {
    VStack(spacing: 16) {
        ProbabilityBarView(
            label: "Ace",
            observedProbability: 0.25,
            expectedProbability: 0.1667
        )
        
        ProbabilityBarView(
            label: "King",
            observedProbability: 0.10,
            expectedProbability: 0.1667
        )
        
        ProbabilityBarView(
            label: "Queen",
            observedProbability: 0.1667,
            expectedProbability: 0.1667
        )
    }
    .padding()
}
