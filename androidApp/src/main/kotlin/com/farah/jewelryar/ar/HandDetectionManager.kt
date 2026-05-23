package com.farah.jewelryar.ar

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FingerPosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val confidence: Float = 0f
)

data class HandLandmarks(
    val wrist: FingerPosition = FingerPosition(),
    val indexFingerTip: FingerPosition = FingerPosition(),
    val middleFingerTip: FingerPosition = FingerPosition(),
    val ringFingerBase: FingerPosition = FingerPosition(),
    val ringFingerTip: FingerPosition = FingerPosition(),
    val isDetected: Boolean = false
)

class HandDetectionManager(context: Context) {
    private var handLandmarker: HandLandmarker? = null

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.d("HandDetectionManager", "HandLandmarker initialized")
        } catch (e: Exception) {
            Log.e("HandDetectionManager", "Failed to init HandLandmarker: ${e.message}", e)
        }
    }

    suspend fun detectHands(bitmap: Bitmap): HandLandmarks = withContext(Dispatchers.Default) {
        return@withContext try {
            val landmarker = handLandmarker ?: return@withContext HandLandmarks()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result: HandLandmarkerResult = landmarker.detect(mpImage)

            if (result.landmarks().isEmpty()) return@withContext HandLandmarks()

            val lm = result.landmarks()[0]
            if (lm.size < 21) return@withContext HandLandmarks()

            fun get(i: Int): FingerPosition {
                val p = lm[i]
                val conf = try { val v = p.presence(); if (v is Float) v.coerceIn(0f, 1f) else 0.9f } catch (_: Exception) { 0.9f }
                return FingerPosition(p.x(), p.y(), conf)
            }

            HandLandmarks(
                wrist = get(0),
                indexFingerTip = get(8),
                middleFingerTip = get(12),
                ringFingerBase = get(13),
                ringFingerTip = get(16),
                isDetected = true
            )
        } catch (e: Exception) {
            Log.e("HandDetectionManager", "Error: ${e.message}")
            HandLandmarks()
        }
    }

    fun close() {
        handLandmarker?.close()
        handLandmarker = null
    }
}
