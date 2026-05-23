package com.farah.jewelryar.ar

import androidx.lifecycle.ViewModel
import com.farah.jewelryar.shared.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ARUiState(
    val product: Product? = null,
    val handLandmarks: HandLandmarks = HandLandmarks(
        FingerPosition(), FingerPosition(), FingerPosition(),
        FingerPosition(), FingerPosition(), false
    ),
    val poseLandmarks: PoseLandmarks = PoseLandmarks(),
    val cameraPermissionGranted: Boolean = false
)

class ARViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ARUiState())
    val uiState: StateFlow<ARUiState> = _uiState

    fun setProduct(product: Product) {
        _uiState.value = _uiState.value.copy(product = product)
    }

    fun setCameraPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(cameraPermissionGranted = granted)
    }

    fun updateHandLandmarks(landmarks: HandLandmarks) {
        _uiState.value = _uiState.value.copy(handLandmarks = landmarks)
    }

    fun updatePoseLandmarks(landmarks: PoseLandmarks) {
        _uiState.value = _uiState.value.copy(poseLandmarks = landmarks)
    }
}
