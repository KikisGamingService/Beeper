package com.example.beeper

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.concurrent.thread

private const val TAG = "AudioProcessor"

class AudioProcessor(
    private val onShotDetected: (Long) -> Unit,
    private val getSensitivity: () -> Int
) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun start() {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        isRecording = true
        audioRecord?.startRecording()

        thread {
            val buffer = ShortArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    process(buffer)
                }
            }
        }
    }

    fun stop() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun process(buffer: ShortArray) {
        val maxAmplitude = buffer.maxOfOrNull { Math.abs(it.toDouble()) } ?: 0.0

        // The threshold is inversely proportional to the sensitivity.
        // A higher sensitivity means a lower threshold.
        val threshold = 32767 * (1 - getSensitivity() / 100.0)

        if (maxAmplitude > threshold) {
            Log.d(TAG, "Shot detected with amplitude: $maxAmplitude and threshold: $threshold")
            onShotDetected(System.currentTimeMillis())
        }
    }
}
