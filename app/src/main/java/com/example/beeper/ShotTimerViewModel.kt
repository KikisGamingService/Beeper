package com.example.beeper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class ShotTimerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ShotTimerUiState())
    val uiState: StateFlow<ShotTimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var randomBeepJob: Job? = null
    private val audioProcessor = AudioProcessor(::onShotDetected) { _uiState.value.sensitivity }
    private val beeper = Beeper()
    private val settingsRepository = SettingsRepository(application)

    init {
        viewModelScope.launch {
            settingsRepository.isDarkMode.collect { isDarkMode ->
                _uiState.update { it.copy(isDarkMode = isDarkMode) }
            }
        }
    }

    fun onStartClick() {
        if (_uiState.value.isRunning) {
            stop()
        } else {
            start()
        }
    }

    private fun start() {
        val startTime = System.currentTimeMillis() + _uiState.value.startDelay * 1000
        _uiState.update {
            it.copy(
                isRunning = true,
                shotTimes = emptyList(),
                startTime = startTime
            )
        }
        viewModelScope.launch {
            delay(_uiState.value.startDelay * 1000L)

            if (_uiState.value.beepOnStart) {
                audioProcessor.ignoreSoundsFor(250)
                beeper.beep()
                delay(200)
            }

            audioProcessor.start()

            if (_uiState.value.randomBeepLoop) {
                randomBeepJob = viewModelScope.launch {
                    while (isActive) {
                        val min = _uiState.value.randomBeepMin
                        val max = _uiState.value.randomBeepMax
                        if (min < max) {
                            val delay = Random.nextInt(min, max + 1)
                            delay(delay * 1000L)
                            viewModelScope.launch {
                                audioProcessor.ignoreSoundsFor(250)
                                beeper.beep()
                            }
                        } else {
                            break
                        }
                    }
                }
            }

            timerJob = viewModelScope.launch {
                while (_uiState.value.isRunning) {
                    val elapsedTime = System.currentTimeMillis() - _uiState.value.startTime
                    if (elapsedTime >= 0) {
                        _uiState.update { it.copy(timer = "%.2f".format(elapsedTime / 1000.0)) }
                    }
                    delay(10)
                }
            }
        }
    }

    private fun stop() {
        _uiState.update { it.copy(isRunning = false) }
        audioProcessor.stop()
        timerJob?.cancel()
        randomBeepJob?.cancel()
    }

    private fun onShotDetected(time: Long) {
        if (time < _uiState.value.startTime) return
        val shotTime = "%.2f".format((time - _uiState.value.startTime) / 1000.0)
        _uiState.update {
            val newShotTimes = it.shotTimes + shotTime
            it.copy(shotTimes = newShotTimes)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioProcessor.stop()
        beeper.release()
    }

    fun setBeepOnStart(beepOnStart: Boolean) {
        _uiState.update { it.copy(beepOnStart = beepOnStart) }
    }

    fun setStartDelay(delay: Int) {
        _uiState.update { it.copy(startDelay = delay) }
    }

    fun setRandomBeepLoop(enabled: Boolean, min: Int, max: Int) {
        _uiState.update { it.copy(randomBeepLoop = enabled, randomBeepMin = min, randomBeepMax = max) }
    }

    fun setSensitivity(sensitivity: Int) {
        _uiState.update { it.copy(sensitivity = sensitivity) }
    }

    fun setDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(isDarkMode)
        }
    }
}

data class ShotTimerUiState(
    val timer: String = "0.00",
    val isRunning: Boolean = false,
    val shotTimes: List<String> = emptyList(),
    val startTime: Long = 0,
    val beepOnStart: Boolean = false,
    val startDelay: Int = 0,
    val randomBeepLoop: Boolean = false,
    val randomBeepMin: Int = 0,
    val randomBeepMax: Int = 0,
    val sensitivity: Int = 50,
    val isDarkMode: Boolean = false
)
