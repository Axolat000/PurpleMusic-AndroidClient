package com.randomfilm.purplemusic20.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun AudioVisualizer(audioSessionId: Int, isPlaying: Boolean, color: Color, modifier: Modifier = Modifier) {
    var magnitudesState by remember { mutableStateOf(FloatArray(0)) }
    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    
    val visualizer = remember(audioSessionId, hasPermission) {
        if (audioSessionId == 0 || !hasPermission) null
        else {
            try {
                val bands = 32
                val currentMagnitudes = FloatArray(bands)
                android.media.audiofx.Visualizer(audioSessionId).apply {
                    captureSize = android.media.audiofx.Visualizer.getCaptureSizeRange()[1]
                    setDataCaptureListener(object : android.media.audiofx.Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: android.media.audiofx.Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                        override fun onFftDataCapture(v: android.media.audiofx.Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft != null && isPlaying) {
                                val newMagnitudes = FloatArray(bands)
                                val maxBin = fft.size / 2
                                val logMax = kotlin.math.log2(maxBin.toDouble())
                                var currentBin = 1
                                for (i in 0 until bands) {
                                    val nextBin = kotlin.math.max(currentBin + 1, java.lang.Math.pow(2.0, (i + 1) * logMax / bands).toInt()).coerceAtMost(maxBin)
                                    var maxMagnitude = 0f
                                    for (j in currentBin until nextBin) {
                                        if (j * 2 + 1 < fft.size) {
                                            val rfk = fft[j * 2]
                                            val ifk = fft[j * 2 + 1]
                                            val mag = kotlin.math.hypot(rfk.toFloat(), ifk.toFloat())
                                            if (mag > maxMagnitude) maxMagnitude = mag
                                        }
                                    }
                                    val multiplier = 1f + (i.toFloat() / bands) * 5f
                                    newMagnitudes[i] = maxMagnitude * multiplier
                                    currentBin = nextBin
                                }
                                
                                for (i in 0 until bands) {
                                    val old = currentMagnitudes[i]
                                    val new = newMagnitudes[i]
                                    currentMagnitudes[i] = if (new > old) new * 0.6f + old * 0.4f else old * 0.8f + new * 0.2f
                                }
                                magnitudesState = currentMagnitudes.copyOf()
                            } else if (!isPlaying) {
                                var active = false
                                for (i in 0 until bands) {
                                    currentMagnitudes[i] *= 0.8f
                                    if (currentMagnitudes[i] > 1f) active = true
                                }
                                if (active) magnitudesState = currentMagnitudes.copyOf()
                            }
                        }
                    }, android.media.audiofx.Visualizer.getMaxCaptureRate(), false, true)
                    enabled = true
                }
            } catch(e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    DisposableEffect(visualizer) {
        onDispose {
            visualizer?.enabled = false
            visualizer?.release()
        }
    }

    Canvas(modifier = modifier) {
        if (magnitudesState.isEmpty()) return@Canvas
        val bands = magnitudesState.size
        val barWidth = size.width / bands
        val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        
        for (i in 0 until bands) {
            val magnitude = magnitudesState[i]
            val height = (magnitude * size.height / 256f).coerceIn(0f, size.height)
            
            val x = i * barWidth + barWidth * 0.1f
            val w = barWidth * 0.8f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - height),
                size = Size(w, height.coerceAtLeast(barWidth * 0.8f)), // Assure un point minimum
                cornerRadius = cornerRadius
            )
        }
    }
}
