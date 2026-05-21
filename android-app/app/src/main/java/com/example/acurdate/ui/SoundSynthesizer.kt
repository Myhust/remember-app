package com.example.acurdate.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class AlertTone(val value: String, val displayName: String) {
    CHIME("chime", "Chime Clásico 🔔"),
    CELESTIAL("celestial", "Celestial Armónico ✨"),
    SOS("sos", "Alarma SOS 🚨"),
    BELL("bell", "Campana Tradicional 🔔"),
    PULSE("pulse", "Pulso Cyberpunk ⚡");

    companion object {
        fun fromValue(value: String): AlertTone {
            return values().firstOrNull { it.value == value } ?: CHIME
        }
    }
}

class SoundSynthesizer(private val context: android.content.Context? = null) {
    private val sampleRate = 44100
    
    fun playChime() {
        GlobalScope.launch(Dispatchers.Default) {
            val durationMs = 300
            val numSamples = durationMs * sampleRate / 1000
            val buffer = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val freq = 880.0 + (1320.0 - 880.0) * (i.toDouble() / numSamples)
                val angle = 2.0 * Math.PI * freq * t
                val envelope = 1.0 - (i.toDouble() / numSamples)
                val sampleValue = (sin(angle) * 32767.0 * 0.4 * envelope).toInt()
                buffer[i] = sampleValue.coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }
    
    fun playCelestialChord() {
        GlobalScope.launch(Dispatchers.Default) {
            val durationMs = 1500
            val numSamples = durationMs * sampleRate / 1000
            val buffer = ShortArray(numSamples)
            
            val eMaj7 = doubleArrayOf(329.63, 415.30, 493.88, 587.33)  // E4, G#4, B4, D#5
            val aMaj7 = doubleArrayOf(440.00, 554.37, 659.25, 830.61)  // A4, C#5, E5, G#5
            val bMaj7 = doubleArrayOf(493.88, 587.33, 698.46, 932.33)  // B4, D#5, F#5, A#5
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                
                val chordFreqs = when {
                    progress < 0.25 -> eMaj7
                    progress < 0.55 -> aMaj7
                    else -> bMaj7
                }
                
                var mix = 0.0
                val vibrato = 1.0 + 0.015 * sin(2.0 * Math.PI * 6.0 * t)
                
                for (freq in chordFreqs) {
                    mix += sin(2.0 * Math.PI * freq * vibrato * t)
                }
                mix /= chordFreqs.size
                
                val envelope = when {
                    progress < 0.05 -> progress / 0.05
                    progress > 0.7 -> 1.0 - (progress - 0.7) / 0.3
                    else -> 1.0
                }
                
                val sampleValue = (mix * 32767.0 * 0.35 * envelope).toInt()
                buffer[i] = sampleValue.coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }
    
    fun playSOSAlarm() {
        GlobalScope.launch(Dispatchers.Default) {
            val durationMs = 2400
            val numSamples = durationMs * sampleRate / 1000
            val buffer = ShortArray(numSamples)
            
            val dotMs = 100
            val dashMs = 300
            val spaceMs = 100
            
            val segments = listOf(
                Pair(true, dotMs), Pair(false, spaceMs), Pair(true, dotMs), Pair(false, spaceMs), Pair(true, dotMs), Pair(false, spaceMs),
                Pair(true, dashMs), Pair(false, spaceMs), Pair(true, dashMs), Pair(false, spaceMs), Pair(true, dashMs), Pair(false, spaceMs),
                Pair(true, dotMs), Pair(false, spaceMs), Pair(true, dotMs), Pair(false, spaceMs), Pair(true, dotMs)
            )
            
            var sampleIdx = 0
            for (seg in segments) {
                val segSamples = seg.second * sampleRate / 1000
                val active = seg.first
                
                for (j in 0 until segSamples) {
                    if (sampleIdx >= numSamples) break
                    val t = sampleIdx.toDouble() / sampleRate
                    
                    if (active) {
                        val vibratoFreq = 950.0 + 50.0 * sin(2.0 * Math.PI * 12.0 * t)
                        val angle = 2.0 * Math.PI * vibratoFreq * t
                        val sampleValue = (sin(angle) * 32767.0 * 0.4).toInt()
                        buffer[sampleIdx] = sampleValue.coerceIn(-32768, 32767).toShort()
                    } else {
                        buffer[sampleIdx] = 0
                    }
                    sampleIdx++
                }
            }
            playPcm(buffer)
        }
    }

    fun playBell() {
        GlobalScope.launch(Dispatchers.Default) {
            val durationMs = 1500
            val numSamples = durationMs * sampleRate / 1000
            val buffer = ShortArray(numSamples)
            
            // Classic bell frequency components with inharmonic structure
            val freqs = doubleArrayOf(350.0, 437.5, 525.0, 700.0, 875.0)
            val weights = doubleArrayOf(0.4, 0.25, 0.15, 0.1, 0.1)
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                
                var mix = 0.0
                for (j in freqs.indices) {
                    // Higher frequencies decay faster to replicate physical bell decay
                    val freqDecay = kotlin.math.exp(-progress * (3.0 + j.toDouble() * 2.0))
                    mix += sin(2.0 * Math.PI * freqs[j] * t) * weights[j] * freqDecay
                }
                
                val sampleValue = (mix * 32767.0 * 0.5).toInt()
                buffer[i] = sampleValue.coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }
    
    fun playCyberPulse() {
        GlobalScope.launch(Dispatchers.Default) {
            val durationMs = 450
            val numSamples = durationMs * sampleRate / 1000
            val buffer = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                
                // Retro cyberpunk frequency sweep down then rapid sweep up
                val phase = if (progress < 0.5) progress * 2.0 else (1.0 - progress) * 2.0
                val freq = 600.0 + 800.0 * sin(phase * Math.PI / 2.0)
                
                val angle = 2.0 * Math.PI * freq * t
                val sineVal = sin(angle)
                val angleMod = angle % (2.0 * Math.PI)
                val triVal = Math.abs((angleMod / Math.PI) - 1.0) * 2.0 - 1.0
                val waveMix = 0.6 * sineVal + 0.4 * triVal
                
                // Rapid double pulse envelope
                val pulseEnv = sin(progress * Math.PI * 2.0)
                val envelope = Math.abs(pulseEnv) * (1.0 - progress)
                
                val sampleValue = (waveMix * 32767.0 * 0.3 * envelope).toInt()
                buffer[i] = sampleValue.coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playTone(tone: AlertTone) {
        when (tone) {
            AlertTone.CHIME -> playChime()
            AlertTone.CELESTIAL -> playCelestialChord()
            AlertTone.SOS -> playSOSAlarm()
            AlertTone.BELL -> playBell()
            AlertTone.PULSE -> playCyberPulse()
        }
    }
    
    fun playSelectedTone() {
        val prefs = context?.getSharedPreferences("acurdate_prefs", android.content.Context.MODE_PRIVATE)
        val rawTone = prefs?.getString("selectedAlertTone", "chime") ?: "chime"
        val tone = AlertTone.fromValue(rawTone)
        playTone(tone)
    }
    
    private fun playPcm(buffer: ShortArray) {
        try {
            val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_ALARM,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )
            }
            
            track.write(buffer, 0, buffer.size)
            track.play()
            
            GlobalScope.launch(Dispatchers.Default) {
                val durationMs = (buffer.size.toDouble() / sampleRate * 1000).toLong()
                kotlinx.coroutines.delay(durationMs + 200)
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
