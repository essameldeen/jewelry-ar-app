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
    val confidence: Float = 0f,
    val handedness: String = ""
)

data class HandLandmarks(
    val indexFingerTip: FingerPosition,
    val middleFingerTip: FingerPosition,
    val wristPosition: FingerPosition,
    val isDetected: Boolean = false
)

class HandDetectionManager(context: Context) {
    private var handLandmarker: HandLandmarker? = null
    private val context = context

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
            Log.d("HandDetectionManager", "MediaPipe HandLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e("HandDetectionManager", "Failed to initialize MediaPipe HandLandmarker: ${e.message}", e)
        }
    }

    suspend fun detectHands(bitmap: Bitmap): HandLandmarks = withContext(Dispatchers.Default) {
        return@withContext try {
            val landmarker = handLandmarker ?: return@withContext getEmptyHandLandmarks()

            // Create MediaPipe image from bitmap
            val mpImage = BitmapImageBuilder(bitmap).build()

            // Detect hand landmarks
            val result: HandLandmarkerResult = landmarker.detect(mpImage)

            if (result.landmarks().isEmpty()) {
                Log.d("HandDetectionManager", "No hands detected")
                return@withContext getEmptyHandLandmarks()
            }

            Log.d("HandDetectionManager", "Detected ${result.landmarks().size} hands")
            // Extract landmarks from first detected hand
            val landmarks = result.landmarks()[0]
            val handedness = if (result.handedness().isNotEmpty()) {
                result.handedness()[0][0].categoryName()
            } else {
                "Unknown"
            }

            // Get specific landmark positions (indices from MediaPipe hand landmarks)
            // Index finger tip is landmark 8
            // Middle finger tip is landmark 12
            // Wrist is landmark 0
            val indexFingerTip = getLandmarkPosition(landmarks, 8, handedness, bitmap)
            val middleFingerTip = getLandmarkPosition(landmarks, 12, handedness, bitmap)
            val wristPosition = getLandmarkPosition(landmarks, 0, handedness, bitmap)

            HandLandmarks(
                indexFingerTip = indexFingerTip,
                middleFingerTip = middleFingerTip,
                wristPosition = wristPosition,
                isDetected = true
            )
        } catch (e: Exception) {
            Log.e("HandDetectionManager", "Error detecting hands: ${e.message}", e)
            getEmptyHandLandmarks()
        }
    }

    private fun getLandmarkPosition(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        index: Int,
        handedness: String,
        bitmap: Bitmap
    ): FingerPosition {
        return try {
            if (index >= landmarks.size) {
                return FingerPosition()
            }

            val landmark = landmarks[index]

            // Get x, y coordinates directly (NormalizedLandmark returns Float directly)
            val x = landmark.x()
            val y = landmark.y()

            // Get presence/confidence score with safe null handling
            // NormalizedLandmark.presence() returns Optional<Float> in Java which becomes Any? in Kotlin
            val confidence = try {
                val presenceValue = landmark.presence()
                if (presenceValue is Float) {
                    presenceValue.coerceIn(0f, 1f)
                } else {
                    0.9f
                }
            } catch (e: Exception) {
                0.9f
            }

            // Coordinates are already normalized to 0-1 range by MediaPipe
            FingerPosition(
                x = x,  // Already normalized 0-1
                y = y,  // Already normalized 0-1
                confidence = confidence,
                handedness = handedness
            )
        } catch (e: Exception) {
            Log.e("HandDetectionManager", "Error getting landmark position: ${e.message}")
            FingerPosition()
        }
    }

    private fun getEmptyHandLandmarks(): HandLandmarks {
        return HandLandmarks(
            indexFingerTip = FingerPosition(),
            middleFingerTip = FingerPosition(),
            wristPosition = FingerPosition(),
            isDetected = false
        )
    }

    fun close() {
        handLandmarker?.close()
        handLandmarker = null
    }
}
