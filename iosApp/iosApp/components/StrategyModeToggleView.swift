import SwiftUI

/// StrategyModeToggleView provides a 3-way toggle for strategy modes 1, 2, and 3.
/// Allows users to select between 1-card, 2-card, or 3-card strategy modes.
///
/// Requirements: 7.1, 8.1
///
/// - Parameters:
///   - selectedMode: The currently selected mode (1, 2, or 3)
///   - onModeSelected: Callback when a mode is selected
struct StrategyModeToggleView: View {
    let selectedMode: Int
    let onModeSelected: (Int) -> Void
    
    var body: some View {
        HStack(spacing: 8) {
            ForEach([1, 2, 3], id: \.self) { mode in
                ModeButton(
                    mode: mode,
                    isSelected: selectedMode == mode,
                    onTap: {
                        onModeSelected(mode)
                    }
                )
            }
        }
    }
}

/// Individual mode button
private struct ModeButton: View {
    let mode: Int
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            Text(modeLabel)
                .font(.subheadline)
                .fontWeight(.medium)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(isSelected ? Color.accentColor : Color(.systemGray5))
                )
                .foregroundColor(isSelected ? .white : .primary)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.accentColor, lineWidth: isSelected ? 2 : 0)
                )
        }
    }
    
    private var modeLabel: String {
        switch mode {
        case 1: return "1 Card"
        case 2: return "2 Cards"
        case 3: return "3 Cards"
        default: return "\(mode) Cards"
        }
    }
}

// MARK: - Preview
#Preview("Mode 1 Selected") {
    StrategyModeToggleView(
        selectedMode: 1,
        onModeSelected: { mode in
            print("Selected mode: \(mode)")
        }
    )
    .padding()
}

#Preview("Mode 2 Selected") {
    StrategyModeToggleView(
        selectedMode: 2,
        onModeSelected: { mode in
            print("Selected mode: \(mode)")
        }
    )
    .padding()
}

#Preview("Mode 3 Selected") {
    StrategyModeToggleView(
        selectedMode: 3,
        onModeSelected: { mode in
            print("Selected mode: \(mode)")
        }
    )
    .padding()
}

#Preview("In Context") {
    VStack(alignment: .leading, spacing: 16) {
        Text("Strategy Mode")
            .font(.headline)
        
        StrategyModeToggleView(
            selectedMode: 2,
            onModeSelected: { mode in
                print("Selected mode: \(mode)")
            }
        )
        
        Text("Select how many cards to include in your strategy")
            .font(.caption)
            .foregroundColor(.secondary)
    }
    .padding()
}
