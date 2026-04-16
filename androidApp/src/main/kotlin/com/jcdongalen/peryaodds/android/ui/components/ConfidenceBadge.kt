package com.jcdongalen.peryaodds.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcdongalen.peryaodds.shared.domain.models.ConfidenceLevel

/**
 * ConfidenceBadge displays the confidence level label.
 * 
 * @param confidenceLevel The confidence level to display
 */
@Composable
fun ConfidenceBadge(
    confidenceLevel: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val text = when (confidenceLevel) {
        ConfidenceLevel.LOW -> "Low Confidence"
        ConfidenceLevel.MEDIUM -> "Medium Confidence"
        ConfidenceLevel.HIGH -> "High Confidence"
        ConfidenceLevel.VERY_HIGH -> "Very High Confidence"
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
