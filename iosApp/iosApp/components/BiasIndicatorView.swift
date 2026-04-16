import SwiftUI

/// BiasIndicatorView displays a color-coded badge for bias classification.
/// - Hot: Red (#EF5350)
/// - Cold: Blue (#42A5F5)
/// - Neutral: Grey (#9E9E9E)
///
/// Validates: Requirements 4.5, 9.2
///
/// - Parameter biasType: The bias classification (Hot, Cold, or Neutral)
struct BiasIndicatorView: View {
    let biasType: BiasType
    
    var body: some View {
        Text(biasType.displayText)
            .font(.caption)
            .fontWeight(.medium)
            .foregroundColor(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(
                RoundedRectangle(cornerRadius: 4)
                    .fill(biasType.color)
            )
    }
}

/// Bias classification type matching the shared Kotlin BiasClassification enum
enum BiasType {
    case hot
    case cold
    case neutral
    
    var displayText: String {
        switch self {
        case .hot: return "Hot"
        case .cold: return "Cold"
        case .neutral: return "Neutral"
        }
    }
    
    var color: Color {
        switch self {
        case .hot: return Color(red: 0xEF / 255, green: 0x53 / 255, blue: 0x50 / 255)
        case .cold: return Color(red: 0x42 / 255, green: 0xA5 / 255, blue: 0xF5 / 255)
        case .neutral: return Color(red: 0x9E / 255, green: 0x9E / 255, blue: 0x9E / 255)
        }
    }
}

// MARK: - Preview
#Preview("All Bias Types") {
    VStack(spacing: 16) {
        BiasIndicatorView(biasType: .hot)
        BiasIndicatorView(biasType: .cold)
        BiasIndicatorView(biasType: .neutral)
    }
    .padding()
}

#Preview("In Context") {
    VStack(alignment: .leading, spacing: 12) {
        HStack {
            Text("Ace")
                .font(.body)
                .fontWeight(.medium)
            Spacer()
            BiasIndicatorView(biasType: .hot)
        }
        
        HStack {
            Text("King")
                .font(.body)
                .fontWeight(.medium)
            Spacer()
            BiasIndicatorView(biasType: .cold)
        }
        
        HStack {
            Text("Queen")
                .font(.body)
                .fontWeight(.medium)
            Spacer()
            BiasIndicatorView(biasType: .neutral)
        }
    }
    .padding()
}
