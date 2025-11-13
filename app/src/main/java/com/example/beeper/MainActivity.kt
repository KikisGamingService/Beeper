package com.example.beeper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.beeper.ui.theme.BeeperTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel: ShotTimerViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.onStartClick()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            BeeperTheme(darkTheme = uiState.isDarkMode) {
                var showMenu by remember { mutableStateOf(false) }
                var showStartDelayDialog by remember { mutableStateOf(false) }
                var showRandomBeepDialog by remember { mutableStateOf(false) }
                var showSensitivityDialog by remember { mutableStateOf(false) }

                if (showStartDelayDialog) {
                    StartDelayDialog(
                        currentDelay = uiState.startDelay,
                        onDismiss = { showStartDelayDialog = false },
                        onConfirm = {
                            viewModel.setStartDelay(it)
                            showStartDelayDialog = false
                        }
                    )
                }

                if (showRandomBeepDialog) {
                    RandomBeepDialog(
                        uiState = uiState,
                        onDismiss = { showRandomBeepDialog = false },
                        onConfirm = { enabled, min, max ->
                            viewModel.setRandomBeepLoop(enabled, min, max)
                            showRandomBeepDialog = false
                        }
                    )
                }

                if (showSensitivityDialog) {
                    SensitivityDialog(
                        currentSensitivity = uiState.sensitivity,
                        onDismiss = { showSensitivityDialog = false },
                        onConfirm = {
                            viewModel.setSensitivity(it)
                            showSensitivityDialog = false
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Beeper") },
                            actions = {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, "Options")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Beep on start") },
                                        onClick = { viewModel.setBeepOnStart(!uiState.beepOnStart) },
                                        leadingIcon = { Icon(Icons.Filled.MusicNote, "Beep on start") },
                                        trailingIcon = {
                                            Checkbox(
                                                checked = uiState.beepOnStart,
                                                onCheckedChange = { viewModel.setBeepOnStart(it) }
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Start delay") },
                                        onClick = {
                                            showStartDelayDialog = true
                                            showMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Filled.Timer, "Start delay") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Random beep loop") },
                                        onClick = {
                                            showRandomBeepDialog = true
                                            showMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Filled.Shuffle, "Random beep loop") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sensitivity") },
                                        onClick = {
                                            showSensitivityDialog = true
                                            showMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Filled.Tune, "Sensitivity") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Dark Mode") },
                                        onClick = { viewModel.setDarkMode(!uiState.isDarkMode) },
                                        leadingIcon = { Icon(Icons.Filled.DarkMode, "Dark Mode") },
                                        trailingIcon = {
                                            Switch(
                                                checked = uiState.isDarkMode,
                                                onCheckedChange = { viewModel.setDarkMode(it) }
                                            )
                                        }
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    ShotTimerScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onStartClick = ::onStartClick
                    )
                }
            }
        }
    }

    private fun onStartClick() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.onStartClick()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
private fun StartDelayDialog(
    currentDelay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var delay by remember { mutableStateOf(currentDelay.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Delay") },
        text = {
            TextField(
                value = delay,
                onValueChange = { delay = it },
                label = { Text("Delay in seconds") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(delay.toIntOrNull() ?: 0) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RandomBeepDialog(
    uiState: ShotTimerUiState,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Int, Int) -> Unit
) {
    var enabled by remember { mutableStateOf(uiState.randomBeepLoop) }
    var min by remember { mutableStateOf(uiState.randomBeepMin.toString()) }
    var max by remember { mutableStateOf(uiState.randomBeepMax.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Random Beep Loop") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                    Text("Enabled")
                }
                TextField(
                    value = min,
                    onValueChange = { min = it },
                    label = { Text("Min seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = max,
                    onValueChange = { max = it },
                    label = { Text("Max seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(enabled, min.toIntOrNull() ?: 0, max.toIntOrNull() ?: 0) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SensitivityDialog(
    currentSensitivity: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sensitivity by remember { mutableStateOf(currentSensitivity.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sensitivity") },
        text = {
            Column {
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0f..100f,
                    steps = 100
                )
                Text(text = "${sensitivity.toInt()}%")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sensitivity.toInt()) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun ShotTimerScreen(
    modifier: Modifier = Modifier,
    uiState: ShotTimerUiState,
    onStartClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = uiState.timer,
            fontSize = 80.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 32.dp)
        )
        Button(
            onClick = onStartClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (uiState.isRunning) "Stop" else "Start",
                fontSize = 24.sp
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        if (uiState.shotTimes.isNotEmpty()) {
            Text(
                text = "Shot Times",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(uiState.shotTimes) { index, time ->
                    ShotTimeCard(shotNumber = index + 1, time = time)
                }
            }
        }
    }
}

@Composable
fun ShotTimeCard(shotNumber: Int, time: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$shotNumber",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShotTimerScreenPreview() {
    BeeperTheme {
        ShotTimerScreen(
            uiState = ShotTimerUiState(shotTimes = listOf("1.23", "2.45", "3.67")),
            onStartClick = {}
        )
    }
}
