package com.jcdongalen.peryaodds.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * ProbabilityBar displays observed vs expected frequency side-by-side.
 * 
 * @param label The card/outcome label
 * @param observedProbability The observed probability (0.0 to 1.0)
 * @param expectedProbability The expected probability (0.0 to 1.0)
 */
@Composable
fun ProbabilityBar(
    label: String,
    observedProbability: Double,
    expectedProbability: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Observed bar
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Observed: ${(observedProbability * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(observedProbability.toFloat())
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small
                            )
                    )
                }
            }
            
            // Expected bar
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Expected: ${(expectedProbability * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(expectedProbability.toFloat())
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}
