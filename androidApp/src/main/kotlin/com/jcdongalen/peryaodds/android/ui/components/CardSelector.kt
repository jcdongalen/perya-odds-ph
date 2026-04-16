package com.jcdongalen.peryaodds.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * CardSelector allows users to select up to 3 cards from the available outcomes.
 * Prevents selection of a 4th card (Requirement 2.4).
 * 
 * @param outcomes List of available card outcomes
 * @param selectedCards Currently selected cards
 * @param onSelectionChange Callback when selection changes
 * @param maxSelection Maximum number of cards that can be selected (default 3)
 */
@Composable
fun CardSelector(
    outcomes: List<String>,
    selectedCards: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    maxSelection: Int = 3,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(outcomes) { card ->
            CardButton(
                card = card,
                isSelected = card in selectedCards,
                isEnabled = card in selectedCards || selectedCards.size < maxSelection,
                onClick = {
                    val newSelection = if (card in selectedCards) {
                        selectedCards - card
                    } else {
                        if (selectedCards.size < maxSelection) {
                            selectedCards + card
                        } else {
                            selectedCards // Don't add if max reached
                        }
                    }
                    onSelectionChange(newSelection)
                }
            )
        }
    }
}

@Composable
private fun CardButton(
    card: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = card,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium
            )
        },
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}
