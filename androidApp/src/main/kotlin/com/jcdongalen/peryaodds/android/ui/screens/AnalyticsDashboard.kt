package com.jcdongalen.peryaodds.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcdongalen.peryaodds.android.ui.components.BiasIndicator
import com.jcdongalen.peryaodds.android.ui.components.ProbabilityBar
import com.jcdongalen.peryaodds.shared.domain.engines.BiasDetectionEngine
import com.jcdongalen.peryaodds.shared.domain.engines.ProbabilityEngine
import com.jcdongalen.peryaodds.shared.presentation.GameSessionViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboard(viewModel: GameSessionViewModel) {
    val probabilityEngine: ProbabilityEngine = koinInject()
    val currentSession = viewModel.getCurrentSession()
    val gameConfig = viewModel.getCurrentGameConfig()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") }
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
        } else if (currentSession.totalRounds == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No data recorded yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Record some observations to see analytics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val probabilityResult = probabilityEngine.computeProbabilities(currentSession, gameConfig)
            val biasResult = BiasDetectionEngine.detectBias(probabilityResult)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Summary",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Total Rounds: ${currentSession.totalRounds}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Confidence: ${probabilityResult.confidenceLevel}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Outcome analytics
                items(gameConfig.outcomes) { outcome ->
                    val outcomeProbability = probabilityResult.perOutcome[outcome]
                    val outcomeBias = biasResult.perOutcome[outcome]

                    if (outcomeProbability != null && outcomeBias != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = outcome,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    BiasIndicator(biasType = outcomeBias.classification)
                                }

                                ProbabilityBar(
                                    label = "Probability",
                                    observedProbability = outcomeProbability.observed,
                                    expectedProbability = outcomeProbability.expected
                                )

                                Text(
                                    text = "Deviation: ${String.format("%.2f", outcomeBias.deviation * 100)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
