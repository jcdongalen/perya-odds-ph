package com.jcdongalen.peryaodds.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jcdongalen.peryaodds.shared.domain.models.BiasClassification

/**
 * BiasIndicator displays a color-coded badge for bias classification.
 * - Hot: Red
 * - Cold: Blue
 * - Neutral: Grey
 *
 * @param biasType The bias classification
 */
@Composable
fun BiasIndicator(
    biasType: BiasClassification,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (biasType) {
        BiasClassification.Hot -> Color(0xFFEF5350) to "Hot"
        BiasClassification.Cold -> Color(0xFF42A5F5) to "Cold"
        BiasClassification.Neutral -> Color(0xFF9E9E9E) to "Neutral"
    }

    Text(
        text = text,
        modifier = modifier
            .background(color, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White
    )
}
