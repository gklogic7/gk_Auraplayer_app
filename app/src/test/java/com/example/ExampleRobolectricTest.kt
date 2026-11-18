package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TrackEntity
import com.example.ui.components.formatTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify timestamp and waveform formatting`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Capsule", appName)
        assertEquals("0:06", formatTimestamp(6_000L, prefixMinus = false))
        assertEquals("-1:54", formatTimestamp(114_000L, prefixMinus = true))

        val sampleTrack = TrackEntity(
            title = "Piche Tere",
            artist = "Kunwarr, Dishant",
            durationMs = 120_000L,
            audioUriString = "file:///offline/piche_tere.wav",
            waveformCsv = "0.25,0.50,0.85,0.95"
        )
        assertEquals(4, sampleTrack.parsedWaveform().size)
        assertTrue(sampleTrack.parsedWaveform().all { it in 0.12f..1.0f })
    }
}
