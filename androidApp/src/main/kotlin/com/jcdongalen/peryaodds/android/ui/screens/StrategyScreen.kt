package com.jcdongalen.peryaodds.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcdongalen.peryaodds.android.ui.components.ConfidenceBadge
import com.jcdongalen.peryaodds.android.ui.components.DisclaimerBanner
import com.jcdongalen.peryaodds.android.ui.components.StrategyModeToggle
import com.jcdongalen.peryaodds.android.ui.components.WinProbabilityDisplay
import com.jcdongalen.peryaodds.shared.domain.engines.DefaultStrategyEngine
import com.jcdongalen.peryaodds.shared.domain.engines.ProbabilityEngine
import com.jcdongalen.peryaodds.shared.presentation.GameSessionViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyScreen(viewModel: GameSessionViewModel) {
    val probabilityEngine: ProbabilityEngine = koinInject()
    val strategyEngine = remember(probabilityEngine) { DefaultStrategyEngine(probabilityEngine) }
    val currentSession = viewModel.getCurrentSession()
    val gameConfig = viewModel.getCurrentGameConfig()
    val strategyMode by viewModel.strategyMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Strategy") }
            )
        }
    ) { paddingValues ->
        if (currentSession == null || gameConfig == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No game selected")
            }
        } else {
            val probabilityResult = probabilityEngine.computeProbabilities(currentSession, gameConfig)
            val strategyResult = strategyEngine.recommend(currentSession, gameConfig, strategyMode)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Strategy mode toggle
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Select Strategy Mode",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            StrategyModeToggle(
                                selectedMode = strategyMode,
                                onModeSelected = { mode ->
                                    viewModel.setStrategyMode(mode)
                                }
                            )
                        }
                    }
                }

                // Win probability display
                item {
                    WinProbabilityDisplay(
                        winProbability = strategyResult.winProbability
                    )
                }

                // Confidence badge
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ConfidenceBadge(
                            confidenceLevel = strategyResult.confidenceLevel
                        )
                    }
                }

                // Recommended cards
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Recommended Cards",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            strategyResult.selectedCards.forEach { card ->
                                val probability = probabilityResult.perOutcome[card]?.observed ?: 0.0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = card,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${(probability * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // High exposure warning for mode 3
                if (strategyMode == 3) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "⚠️ High Exposure Warning",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Selecting 3 cards significantly increases your risk. " +
                                            "You are betting on multiple outcomes simultaneously, " +
                                            "which can lead to higher losses.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Disclaimer banner
                item {
                    DisclaimerBanner(strategyMode = strategyMode)
                }
            }
        }
    }
}
