package com.jcdongalen.peryaodds.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcdongalen.peryaodds.android.ui.components.CardSelector
import com.jcdongalen.peryaodds.shared.domain.models.Result
import com.jcdongalen.peryaodds.shared.presentation.GameSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationScreen(viewModel: GameSessionViewModel) {
    val activeGameType by viewModel.activeGameType.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    
    val gameConfig = viewModel.getCurrentGameConfig()
    val currentSession = viewModel.getCurrentSession()
    
    var selectedCards by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            successMessage = null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(gameConfig?.displayName ?: "Record Observations") 
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedCards.size == gameConfig?.hitsPerRound) {
                FloatingActionButton(
                    onClick = {
                        when (val result = viewModel.recordObservation(selectedCards)) {
                            is Result.Success -> {
                                successMessage = "Round recorded successfully!"
                                selectedCards = emptyList()
                                errorMessage = null
                            }
                            is Result.Error -> {
                                errorMessage = result.error.message
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm Selection")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Game info card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Total Rounds: ${currentSession?.totalRounds ?: 0}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Select ${gameConfig?.hitsPerRound ?: 3} cards for this round",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Selection status
            Text(
                text = "Selected: ${selectedCards.size} / ${gameConfig?.hitsPerRound ?: 3}",
                style = MaterialTheme.typography.titleSmall,
                color = if (selectedCards.size == gameConfig?.hitsPerRound) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Card selector
            gameConfig?.let { config ->
                CardSelector(
                    outcomes = config.outcomes,
                    selectedCards = selectedCards,
                    onSelectionChange = { newSelection ->
                        selectedCards = newSelection
                        errorMessage = null
                    },
                    maxSelection = config.hitsPerRound
                )
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No game selected")
                }
            }
        }
    }
}
