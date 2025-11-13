package com.example.beeper

import android.media.AudioManager
import android.media.ToneGenerator

class Beeper {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    @Synchronized
    fun beep() {
        toneGenerator.stopTone()
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 200)
    }

    fun release() {
        toneGenerator.release()
    }
}
