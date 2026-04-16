import SwiftUI

/// CardSelectorView allows users to select up to 3 cards from the available outcomes.
/// Supports multi-select with the same card being selectable multiple times.
/// Prevents selection of a 4th card (Requirement 2.4).
///
/// Requirements: 2.1, 2.4, 2.5
struct CardSelectorView: View {
    let outcomes: [String]
    @Binding var selectedCards: [String]
    let maxSelection: Int
    
    init(
        outcomes: [String],
        selectedCards: Binding<[String]>,
        maxSelection: Int = 3
    ) {
        self.outcomes = outcomes
        self._selectedCards = selectedCards
        self.maxSelection = maxSelection
    }
    
    var body: some View {
        LazyVGrid(
            columns: [
                GridItem(.flexible()),
                GridItem(.flexible()),
                GridItem(.flexible())
            ],
            spacing: 12
        ) {
            ForEach(outcomes, id: \.self) { card in
                CardButton(
                    card: card,
                    selectionCount: selectedCards.filter { $0 == card }.count,
                    isEnabled: selectedCards.count < maxSelection || selectedCards.contains(card),
                    onTap: {
                        handleCardTap(card)
                    }
                )
            }
        }
        .padding(8)
    }
    
    private func handleCardTap(_ card: String) {
        if let index = selectedCards.firstIndex(of: card) {
            // Card is already selected - remove one instance
            selectedCards.remove(at: index)
        } else if selectedCards.count < maxSelection {
            // Card not selected and we haven't reached max - add it
            selectedCards.append(card)
        }
        // If max reached and card not selected, do nothing (button is disabled)
    }
}

/// Individual card button with selection count indicator
private struct CardButton: View {
    let card: String
    let selectionCount: Int
    let isEnabled: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .topTrailing) {
                // Main card button
                Text(card)
                    .font(.title2)
                    .fontWeight(.medium)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(selectionCount > 0 ? Color.accentColor : Color(.systemGray5))
                    )
                    .foregroundColor(selectionCount > 0 ? .white : .primary)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.accentColor, lineWidth: selectionCount > 0 ? 2 : 0)
                    )
                
                // Selection count badge
                if selectionCount > 0 {
                    Text("\(selectionCount)")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(width: 20, height: 20)
                        .background(Circle().fill(Color.red))
                        .offset(x: 4, y: -4)
                }
            }
        }
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1.0 : 0.5)
    }
}

#Preview("Empty Selection") {
    CardSelectorView(
        outcomes: ["Ace", "King", "Queen", "Jack", "10", "9"],
        selectedCards: .constant([])
    )
    .padding()
}

#Preview("With Selection") {
    CardSelectorView(
        outcomes: ["Ace", "King", "Queen", "Jack", "10", "9"],
        selectedCards: .constant(["Ace", "King", "Ace"])
    )
    .padding()
}

#Preview("Max Selection") {
    CardSelectorView(
        outcomes: ["Ace", "King", "Queen", "Jack", "10", "9"],
        selectedCards: .constant(["Ace", "Ace", "Ace"])
    )
    .padding()
}
