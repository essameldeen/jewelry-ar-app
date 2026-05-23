package com.farah.jewelryar.ar

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PoseLandmarkPoint(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)

data class PoseLandmarks(
    val nose: PoseLandmarkPoint = PoseLandmarkPoint(),
    val leftShoulder: PoseLandmarkPoint = PoseLandmarkPoint(),
    val rightShoulder: PoseLandmarkPoint = PoseLandmarkPoint(),
    val isDetected: Boolean = false
)

class PoseDetectionManager(context: Context) {
    private var poseLandmarker: PoseLandmarker? = null

    init {
        try {
            val baseOptions = com.google.mediapipe.tasks.core.BaseOptions.builder()
                .setModelAssetPath("pose_landmarker.task")
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            Log.d("PoseDetectionManager", "MediaPipe PoseLandmarker initialized")
        } catch (e: Exception) {
            Log.e("PoseDetectionManager", "Failed to initialize PoseLandmarker: ${e.message}", e)
        }
    }

    suspend fun detectPose(bitmap: Bitmap): PoseLandmarks = withContext(Dispatchers.Default) {
        return@withContext try {
            val landmarker = poseLandmarker ?: return@withContext PoseLandmarks()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result: PoseLandmarkerResult = landmarker.detect(mpImage)

            if (result.landmarks().isEmpty()) {
                return@withContext PoseLandmarks()
            }

            val landmarks = result.landmarks()[0]
            if (landmarks.size < 13) return@withContext PoseLandmarks()

            // 0=nose, 11=leftShoulder, 12=rightShoulder
            val nose = landmarks[0]
            val leftShoulder = landmarks[11]
            val rightShoulder = landmarks[12]

            PoseLandmarks(
                nose = PoseLandmarkPoint(nose.x(), nose.y(), nose.z()),
                leftShoulder = PoseLandmarkPoint(leftShoulder.x(), leftShoulder.y(), leftShoulder.z()),
                rightShoulder = PoseLandmarkPoint(rightShoulder.x(), rightShoulder.y(), rightShoulder.z()),
                isDetected = true
            )
        } catch (e: Exception) {
            Log.e("PoseDetectionManager", "Error detecting pose: ${e.message}")
            PoseLandmarks()
        }
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }
}
