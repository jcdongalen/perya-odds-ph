package com.jcdongalen.peryaodds.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * StrategyModeToggle provides a 3-way toggle for strategy modes 1, 2, and 3.
 * 
 * @param selectedMode The currently selected mode (1, 2, or 3)
 * @param onModeSelected Callback when a mode is selected
 */
@Composable
fun StrategyModeToggle(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(1, 2, 3).forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                label = {
                    Text(
                        text = when (mode) {
                            1 -> "1 Card"
                            2 -> "2 Cards"
                            3 -> "3 Cards"
                            else -> "$mode Cards"
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
