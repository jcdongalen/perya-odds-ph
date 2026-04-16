package com.jcdongalen.peryaodds.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * DisclaimerBanner displays disclaimer text.
 * Shows base disclaimer always, and additional risk note when mode > 1.
 * 
 * @param strategyMode The current strategy mode
 */
@Composable
fun DisclaimerBanner(
    strategyMode: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚠️ Disclaimer",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Text(
                text = "This app provides statistical analysis for entertainment purposes only. " +
                        "Past outcomes do not guarantee future results. Gambling involves risk. " +
                        "Please play responsibly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            if (strategyMode > 1) {
                Text(
                    text = "\n⚠️ Higher Risk: Selecting ${strategyMode} cards increases your exposure. " +
                            "The more cards you select, the higher your potential losses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
