import SwiftUI

/// ConfidenceBadgeView displays the confidence level label.
/// Confidence levels: Low, Medium, High, Very High
///
/// Validates: Requirements 5.5, 6.4
///
/// - Parameter confidenceLevel: The confidence level to display
struct ConfidenceBadgeView: View {
    let confidenceLevel: ConfidenceLevelType
    
    var body: some View {
        Text(confidenceLevel.displayText)
            .font(.caption)
            .fontWeight(.medium)
            .foregroundColor(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.accentColor.opacity(0.8))
            )
    }
}

/// Confidence level type matching the shared Kotlin ConfidenceLevel enum
enum ConfidenceLevelType {
    case low
    case medium
    case high
    case veryHigh
    
    var displayText: String {
        switch self {
        case .low: return "Low"
        case .medium: return "Medium"
        case .high: return "High"
        case .veryHigh: return "Very High"
        }
    }
}

// MARK: - Preview
#Preview("All Confidence Levels") {
    VStack(spacing: 16) {
        ConfidenceBadgeView(confidenceLevel: .low)
        ConfidenceBadgeView(confidenceLevel: .medium)
        ConfidenceBadgeView(confidenceLevel: .high)
        ConfidenceBadgeView(confidenceLevel: .veryHigh)
    }
    .padding()
}

#Preview("In Context") {
    VStack(alignment: .leading, spacing: 12) {
        HStack {
            Text("Confidence:")
                .font(.body)
                .fontWeight(.medium)
            Spacer()
            ConfidenceBadgeView(confidenceLevel: .low)
        }
        
        HStack {
            Text("Data Quality:")
                .font(.body)
                .fontWeight(.medium)
            Spacer()
            ConfidenceBadgeView(confidenceLevel: .veryHigh)
        }
    }
    .padding()
}
