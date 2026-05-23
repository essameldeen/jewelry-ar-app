package com.farah.jewelryar.ar

import androidx.lifecycle.ViewModel
import com.farah.jewelryar.shared.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

// Ring style definitions with colors and properties
enum class RingStyle {
    GOLD,
    SILVER,
    ROSE_GOLD,
    DIAMOND,
    PLATINUM
}

data class RingColors(
    val primaryColor: Long,      // Main band color
    val highlightColor: Long,    // Light highlight
    val shadowColor: Long,       // Dark shadow
    val accentColor: Long        // Secondary accent
)

fun RingStyle.getColors(): RingColors = when (this) {
    RingStyle.GOLD -> RingColors(
        primaryColor = 0xFFD4AF37,      // Gold
        highlightColor = 0xFFFAE7C5,    // Light gold
        shadowColor = 0xFF8B6914,       // Dark gold
        accentColor = 0xFFFFED4E        // Yellow highlight
    )
    RingStyle.SILVER -> RingColors(
        primaryColor = 0xFFC0C0C0,      // Silver
        highlightColor = 0xFFFFFFFF,    // White
        shadowColor = 0xFF808080,       // Dark gray
        accentColor = 0xFFE8E8E8        // Light silver
    )
    RingStyle.ROSE_GOLD -> RingColors(
        primaryColor = 0xFFB76E79,      // Rose gold
        highlightColor = 0xFFD4A5AA,    // Light rose
        shadowColor = 0xFF9B5965,       // Dark rose
        accentColor = 0xFFE6C5CB        // Light pink
    )
    RingStyle.DIAMOND -> RingColors(
        primaryColor = 0xFFE0FFFF,      // Diamond white
        highlightColor = 0xFFFFFFFF,    // Bright white
        shadowColor = 0xFFB0D0D0,       // Shadow
        accentColor = 0xFF00FFFF        // Cyan accent
    )
    RingStyle.PLATINUM -> RingColors(
        primaryColor = 0xFFC5D3D3,      // Platinum
        highlightColor = 0xFFE8F0F0,    // Light platinum
        shadowColor = 0xFF6B7E7E,       // Dark platinum
        accentColor = 0xFFD0DCDC        // Light gray
    )
}

// Finger detection enum
enum class DetectedFinger {
    THUMB,
    INDEX,
    MIDDLE,
    RING,
    PINKY,
    UNKNOWN
}

data class RingPosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val smoothedX: Float = 0f,  // For smooth animation
    val smoothedY: Float = 0f   // For smooth animation
)

data class ARUiState(
    val product: Product? = null,
    val handLandmarks: HandLandmarks = HandLandmarks(
        FingerPosition(),
        FingerPosition(),
        FingerPosition(),
        false
    ),
    val ringPosition: RingPosition = RingPosition(),
    val cameraPermissionGranted: Boolean = false,
    val ringStyle: RingStyle = RingStyle.GOLD,
    val detectedFinger: DetectedFinger = DetectedFinger.INDEX
)

class ARViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ARUiState())
    val uiState: StateFlow<ARUiState> = _uiState

    // Smoothing factor for animation (0.0 = no smoothing, 1.0 = heavy smoothing)
    private val SMOOTHING_FACTOR = 0.7f

    fun setProduct(product: Product) {
        _uiState.value = _uiState.value.copy(product = product)
    }

    fun setCameraPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(cameraPermissionGranted = granted)
    }

    fun setRingStyle(style: RingStyle) {
        _uiState.value = _uiState.value.copy(ringStyle = style)
    }

    fun updateHandLandmarks(landmarks: HandLandmarks) {
        android.util.Log.d("ARViewModel", "updateHandLandmarks: isDetected=${landmarks.isDetected}, confidence=${landmarks.indexFingerTip.confidence}")
        _uiState.value = _uiState.value.copy(handLandmarks = landmarks)
        if (landmarks.isDetected) {
            android.util.Log.d("ARViewModel", "Hand detected, updating ring position")
            updateRingPosition(landmarks)
        } else {
            android.util.Log.d("ARViewModel", "Hand NOT detected")
        }
    }

    private fun updateRingPosition(landmarks: HandLandmarks) {
        // Use index finger tip as the ring position (primary)
        val fingerTip = landmarks.indexFingerTip
        val middleFinger = landmarks.middleFingerTip
        val wrist = landmarks.wristPosition

        // Ensure coordinates are in valid range (0-1)
        val validX = fingerTip.x.coerceIn(0f, 1f)
        val validY = fingerTip.y.coerceIn(0f, 1f)

        // Calculate scale based on hand distance (how close hand is to camera)
        val distanceX = (fingerTip.x - wrist.x)
        val distanceY = (fingerTip.y - wrist.y)
        val distance = sqrt(distanceX * distanceX + distanceY * distanceY)

        // Scale varies between 0.5 and 2.0 based on hand distance
        val scale = 0.5f + (1f - distance.coerceIn(0f, 0.5f)) * 3f

        // Calculate rotation based on finger angle
        val angleRad = atan2(
            middleFinger.y - fingerTip.y,
            middleFinger.x - fingerTip.x
        )
        val rotation = Math.toDegrees(angleRad.toDouble()).toFloat()

        // Apply smoothing for smooth animation
        val currentPosition = _uiState.value.ringPosition
        val smoothedX = currentPosition.smoothedX + (validX - currentPosition.smoothedX) * SMOOTHING_FACTOR
        val smoothedY = currentPosition.smoothedY + (validY - currentPosition.smoothedY) * SMOOTHING_FACTOR

        // Detect which finger is being tracked based on hand position
        val detectedFinger = detectFinger(fingerTip, wrist)

        // Update ring position with real hand coordinates and smooth animation
        _uiState.value = _uiState.value.copy(
            ringPosition = RingPosition(
                x = validX,
                y = validY,
                scale = scale.coerceIn(0.5f, 2.0f),
                rotation = rotation,
                smoothedX = smoothedX,
                smoothedY = smoothedY
            ),
            detectedFinger = detectedFinger
        )
    }

    private fun detectFinger(fingerTip: FingerPosition, wrist: FingerPosition): DetectedFinger {
        // Simple finger detection based on finger position relative to wrist
        val horizontalDistance = fingerTip.x - wrist.x
        val verticalDistance = fingerTip.y - wrist.y

        return when {
            // Thumb is typically to the left/right of the hand
            horizontalDistance < -0.15f -> DetectedFinger.THUMB
            // Pinky is on the far right
            horizontalDistance > 0.25f -> DetectedFinger.PINKY
            // Ring finger is right of center
            horizontalDistance > 0.08f -> DetectedFinger.RING
            // Middle finger is most common (center)
            horizontalDistance in -0.08f..0.08f -> DetectedFinger.MIDDLE
            // Index finger is left of center
            else -> DetectedFinger.INDEX
        }
    }
}
