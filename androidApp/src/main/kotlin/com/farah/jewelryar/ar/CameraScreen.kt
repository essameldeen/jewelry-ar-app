package com.farah.jewelryar.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import androidx.camera.view.PreviewView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun CameraARScreen(
    uiState: ARUiState,
    onHandDetected: (HandLandmarks) -> Unit,
    onBackClick: () -> Unit,
    onRingStyleChange: (RingStyle) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val handDetectionManager = remember { HandDetectionManager(context) }
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val ringImageManager = remember { RingImageManager(context) }
    val ringImage = remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val isLoadingImage = remember { mutableStateOf(false) }

    // Load ring image when ring style changes
    LaunchedEffect(uiState.ringStyle) {
        isLoadingImage.value = true
        ringImage.value = ringImageManager.loadRingImage(uiState.ringStyle)
        isLoadingImage.value = false
        Log.d("CameraARScreen", "Loaded ring image for style: ${uiState.ringStyle.name}")
    }

    // Initialize camera only if permission is granted (front camera for hand detection)
    LaunchedEffect(uiState.cameraPermissionGranted) {
        if (uiState.cameraPermissionGranted) {
            try {
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    try {
                        val provider = future.get()
                        cameraProvider.value = provider
                        // Bind camera on main thread (required for CameraX)
                        try {
                            bindCamera(
                                provider,
                                lifecycleOwner,
                                previewView,
                                handDetectionManager,
                                onHandDetected,
                                vibrator,
                                uiState
                            )
                        } catch (e: Exception) {
                            Log.e("CameraARScreen", "Camera binding failed: ${e.message}", e)
                        }
                    } catch (e: Exception) {
                        Log.e("CameraARScreen", "Failed to initialize camera: ${e.message}", e)
                    }
                }, { runnable -> android.os.Handler(android.os.Looper.getMainLooper()).post(runnable) })
            } catch (e: Exception) {
                Log.e("CameraARScreen", "Error setting up camera: ${e.message}", e)
            }
        } else {
            Log.w("CameraARScreen", "Camera permission not granted")
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            handDetectionManager.close()
            cameraProvider.value?.unbindAll()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Ring image overlay on finger position
        Log.d("CameraARScreen", "Render check - isDetected: ${uiState.handLandmarks.isDetected}, product: ${uiState.product?.name}")
        if (uiState.handLandmarks.isDetected && uiState.product != null) {
            Log.d("CameraARScreen", "Rendering ring image at position (${uiState.ringPosition.x}, ${uiState.ringPosition.y}), style: ${uiState.ringStyle}")
            if (isLoadingImage.value) {
                // Show loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading ring...", color = Color.White)
                }
            } else if (ringImage.value != null) {
                // Display ring product image
                RingImageOverlay(
                    bitmap = ringImage.value!!,
                    ringPosition = uiState.ringPosition,
                    detectedFinger = uiState.detectedFinger
                )
            } else {
                // Fallback: show Canvas-based ring
                RingOverlay(
                    uiState = uiState,
                    ringStyle = uiState.ringStyle,
                    detectedFinger = uiState.detectedFinger
                )
            }
        }

        // Product info
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), shape = MaterialTheme.shapes.medium)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.product?.name ?: "Trying on ring",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Position your finger in the center",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Ring style selector
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color.Black.copy(alpha = 0.7f), shape = MaterialTheme.shapes.medium)
                .padding(8.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RingStyle.values().forEach { style ->
                Button(
                    onClick = { onRingStyleChange(style) },
                    modifier = Modifier.padding(4.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (uiState.ringStyle == style) {
                            Color(0xFFFFD700)
                        } else {
                            Color.DarkGray
                        }
                    )
                ) {
                    Text(
                        text = style.name.replace("_", " "),
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Back button
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}

private fun bindCamera(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    handDetectionManager: HandDetectionManager,
    onHandDetected: (HandLandmarks) -> Unit,
    vibrator: Vibrator?,
    uiState: ARUiState
) {
    try {
        cameraProvider.unbindAll()

        // Create preview use case
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // Create image analysis use case
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer(
                    Executors.newSingleThreadExecutor()
                ) { imageProxy ->
                    analyzeImage(imageProxy, handDetectionManager, onHandDetected, vibrator, uiState)
                }
            }

        // Use front camera for optimal hand detection
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        Log.d("CameraARScreen", "Binding FRONT camera for hand detection")

        // Bind use cases
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
    } catch (e: Exception) {
        Log.e("CameraARScreen", "Use case binding failed: ${e.message}", e)
    }
}

private fun analyzeImage(
    imageProxy: ImageProxy,
    handDetectionManager: HandDetectionManager,
    onHandDetected: (HandLandmarks) -> Unit,
    vibrator: Vibrator?,
    uiState: ARUiState
) {
    try {
        // Convert ImageProxy to Bitmap
        val bitmap = imageProxyToBitmap(imageProxy) ?: run {
            imageProxy.close()
            return
        }

        // Run hand detection asynchronously
        GlobalScope.launch(Dispatchers.Default) {
            try {
                val handLandmarks = handDetectionManager.detectHands(bitmap)

                // Update UI on main thread
                GlobalScope.launch(Dispatchers.Main) {
                    onHandDetected(handLandmarks)

                    // Add haptic feedback when hand is detected with high confidence
                    if (handLandmarks.isDetected && handLandmarks.indexFingerTip.confidence > 0.7f) {
                        vibrator?.vibrate(10) // Short haptic pulse
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraARScreen", "Error in hand detection: ${e.message}")
            }
        }
    } catch (e: Exception) {
        Log.e("CameraARScreen", "Error analyzing image: ${e.message}", e)
    } finally {
        imageProxy.close()
    }
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val width = imageProxy.width
        val height = imageProxy.height

        // Handle YUV_420_888 format
        if (imageProxy.format == ImageFormat.YUV_420_888) {
            val planes = imageProxy.planes

            // Get Y, U, V planes
            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]

            // Get the actual data from buffers
            val ySize = yPlane.buffer.remaining()
            val yData = ByteArray(ySize)
            yPlane.buffer.get(yData)

            // For U and V, handle both packed and semi-planar formats
            val uvPixelStride = uPlane.pixelStride

            val uData: ByteArray
            val vData: ByteArray

            if (uvPixelStride == 1) {
                // Planar format
                uData = ByteArray(uPlane.buffer.remaining())
                vData = ByteArray(vPlane.buffer.remaining())
                uPlane.buffer.get(uData)
                vPlane.buffer.get(vData)
            } else {
                // Interleaved format (NV21/NV12)
                val uvSize = uPlane.buffer.remaining()
                val uvBuffer = ByteArray(uvSize)
                uPlane.buffer.get(uvBuffer)

                uData = ByteArray(uvSize / 2)
                vData = ByteArray(uvSize / 2)

                for (i in uData.indices) {
                    uData[i] = uvBuffer[i * 2]
                    vData[i] = uvBuffer[i * 2 + 1]
                }
            }

            // Convert YUV to ARGB
            val rgb = IntArray(width * height)
            yuvToRGBOptimized(rgb, yData, uData, vData, width, height)

            Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888)
        } else {
            Log.w("CameraARScreen", "Unsupported format: ${imageProxy.format}")
            null
        }
    } catch (e: Exception) {
        Log.e("CameraARScreen", "Image conversion error: ${e.message}", e)
        null
    }
}

private fun yuvToRGBOptimized(
    rgb: IntArray,
    yData: ByteArray,
    uData: ByteArray,
    vData: ByteArray,
    width: Int,
    height: Int
) {
    val frameSize = width * height

    for (i in 0 until frameSize) {
        val y = (yData[i].toInt() and 0xFF).toFloat()

        // Calculate UV index for 4:2:0 sampling (each UV sample covers 2x2 Y samples)
        val uvIndex = (i / width shr 1) * (width shr 1) + (i % width shr 1)

        // Bounds check
        if (uvIndex < uData.size && uvIndex < vData.size) {
            val u = ((uData[uvIndex].toInt() and 0xFF) - 128).toFloat()
            val v = ((vData[uvIndex].toInt() and 0xFF) - 128).toFloat()

            // YUV to RGB conversion using standard BT.601 coefficients
            val r = (y + 1.402f * v).toInt().coerceIn(0, 255)
            val g = (y - 0.344136f * u - 0.714136f * v).toInt().coerceIn(0, 255)
            val b = (y + 1.772f * u).toInt().coerceIn(0, 255)

            rgb[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        } else {
            // Fallback to white if UV data is unavailable
            rgb[i] = (0xFFFFFFFF.toInt())
        }
    }
}


@Composable
private fun RingImageOverlay(
    bitmap: android.graphics.Bitmap,
    ringPosition: RingPosition,
    detectedFinger: DetectedFinger
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val imagePainter = remember(imageBitmap) { androidx.compose.ui.graphics.painter.BitmapPainter(imageBitmap) }

    // Use BoxWithConstraints to get actual screen dimensions
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val ringSize = (ringPosition.scale * 180).dp

        // Convert normalized (0-1) coordinates to actual screen coordinates
        val posX = screenWidth * ringPosition.smoothedX - ringSize / 2
        val posY = screenHeight * ringPosition.smoothedY - ringSize / 2

        Image(
            painter = imagePainter,
            contentDescription = "Ring Product Image",
            modifier = Modifier
                .size(ringSize)
                .offset(x = posX, y = posY)
                .alpha(0.95f),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )

        // Finger indicator
        val fingerIndicatorColor = when (detectedFinger) {
            DetectedFinger.THUMB -> Color(0xFFFF6B6B)
            DetectedFinger.INDEX -> Color(0xFF4ECDC4)
            DetectedFinger.MIDDLE -> Color(0xFF45B7D1)
            DetectedFinger.RING -> Color(0xFFFFA07A)
            DetectedFinger.PINKY -> Color(0xFFDDA0DD)
            DetectedFinger.UNKNOWN -> Color.Gray
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = ringPosition.smoothedX * size.width
            val centerY = ringPosition.smoothedY * size.height
            drawCircle(
                color = fingerIndicatorColor,
                radius = 6f,
                center = Offset(centerX, centerY - ringPosition.scale * 50f - 15f)
            )
        }
    }
}

@Composable
private fun RingOverlay(
    uiState: ARUiState,
    ringStyle: RingStyle = RingStyle.GOLD,
    detectedFinger: DetectedFinger = DetectedFinger.INDEX
) {
    val ringPos = uiState.ringPosition
    val colors = ringStyle.getColors()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = ringPos.smoothedX * size.width
        val centerY = ringPos.smoothedY * size.height

        // Scale ring based on hand distance
        val ringWidth = ringPos.scale * 55f    // Width of ring band
        val ringHeight = ringPos.scale * 70f   // Height with perspective
        val holeWidth = ringPos.scale * 35f    // Inner hole width
        val holeHeight = ringPos.scale * 45f   // Inner hole height

        // ========== BACKGROUND SHADOW (drop shadow for depth) ==========
        drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(centerX - ringWidth - 4f, centerY - ringHeight + 4f),
            size = Size(ringWidth * 2 + 8f, ringHeight * 2 + 8f)
        )

        // ========== OUTER RING BAND (main metal color) ==========
        drawOval(
            color = Color(colors.primaryColor),
            topLeft = Offset(centerX - ringWidth, centerY - ringHeight),
            size = Size(ringWidth * 2, ringHeight * 2)
        )

        // ========== INNER HOLE (cutout to create ring shape) ==========
        drawOval(
            color = Color(0xFF1A1A1A),
            topLeft = Offset(centerX - holeWidth, centerY - holeHeight),
            size = Size(holeWidth * 2, holeHeight * 2)
        )

        // ========== TOP HIGHLIGHT (light reflection on outer edge) ==========
        drawOval(
            color = Color(colors.highlightColor).copy(alpha = 0.8f),
            topLeft = Offset(centerX - ringWidth * 0.7f, centerY - ringHeight * 0.6f),
            size = Size(ringWidth * 1.4f, ringHeight * 0.5f)
        )

        // ========== BOTTOM SHADOW (shadow on inner edge) ==========
        drawOval(
            color = Color(colors.shadowColor).copy(alpha = 0.6f),
            topLeft = Offset(centerX - holeWidth * 0.8f, centerY + holeHeight * 0.3f),
            size = Size(holeWidth * 1.6f, holeHeight * 0.6f)
        )

        // ========== MATERIAL-SPECIFIC EFFECTS ==========
        when (ringStyle) {
            RingStyle.DIAMOND -> {
                // Diamond: Bright sparkles for faceted effect
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    radius = ringWidth * 0.15f,
                    center = Offset(centerX - ringWidth * 0.6f, centerY - ringHeight * 0.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = ringWidth * 0.1f,
                    center = Offset(centerX + ringWidth * 0.5f, centerY - ringHeight * 0.4f)
                )
                drawCircle(
                    color = Color(colors.accentColor).copy(alpha = 0.7f),
                    radius = ringWidth * 0.08f,
                    center = Offset(centerX, centerY + ringHeight * 0.3f)
                )
            }
            RingStyle.PLATINUM -> {
                // Platinum: Subtle cool-toned highlights
                drawOval(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(centerX - ringWidth * 0.6f, centerY - ringHeight * 0.4f),
                    size = Size(ringWidth * 1.2f, ringHeight * 0.35f)
                )
                drawCircle(
                    color = Color(colors.accentColor).copy(alpha = 0.4f),
                    radius = ringWidth * 0.12f,
                    center = Offset(centerX + ringWidth * 0.4f, centerY - ringHeight * 0.2f)
                )
            }
            RingStyle.SILVER -> {
                // Silver: Bright mirror-like reflections
                drawOval(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(centerX - ringWidth * 0.65f, centerY - ringHeight * 0.45f),
                    size = Size(ringWidth * 1.3f, ringHeight * 0.4f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = ringWidth * 0.1f,
                    center = Offset(centerX + ringWidth * 0.45f, centerY - ringHeight * 0.15f)
                )
            }
            RingStyle.ROSE_GOLD -> {
                // Rose Gold: Warm romantic highlights
                drawOval(
                    color = Color(colors.highlightColor).copy(alpha = 0.75f),
                    topLeft = Offset(centerX - ringWidth * 0.6f, centerY - ringHeight * 0.4f),
                    size = Size(ringWidth * 1.2f, ringHeight * 0.35f)
                )
                drawCircle(
                    color = Color(colors.accentColor).copy(alpha = 0.55f),
                    radius = ringWidth * 0.1f,
                    center = Offset(centerX + ringWidth * 0.4f, centerY - ringHeight * 0.2f)
                )
            }
            else -> {
                // GOLD: Warm yellow highlights
                drawOval(
                    color = Color.White.copy(alpha = 0.7f),
                    topLeft = Offset(centerX - ringWidth * 0.65f, centerY - ringHeight * 0.45f),
                    size = Size(ringWidth * 1.3f, ringHeight * 0.4f)
                )
                drawCircle(
                    color = Color(colors.accentColor).copy(alpha = 0.5f),
                    radius = ringWidth * 0.1f,
                    center = Offset(centerX + ringWidth * 0.4f, centerY - ringHeight * 0.2f)
                )
            }
        }

        // ========== RING BAND EDGES (definition lines) ==========
        drawOval(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(centerX - ringWidth, centerY - ringHeight),
            size = Size(ringWidth * 2, ringHeight * 2),
            style = Stroke(width = 1.5f)
        )

        drawOval(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(centerX - holeWidth, centerY - holeHeight),
            size = Size(holeWidth * 2, holeHeight * 2),
            style = Stroke(width = 1f)
        )

        // ========== FINGER INDICATOR (shows detected finger type) ==========
        val fingerIndicatorColor = when (detectedFinger) {
            DetectedFinger.THUMB -> Color(0xFFFF6B6B)
            DetectedFinger.INDEX -> Color(0xFF4ECDC4)
            DetectedFinger.MIDDLE -> Color(0xFF45B7D1)
            DetectedFinger.RING -> Color(0xFFFFA07A)
            DetectedFinger.PINKY -> Color(0xFFDDA0DD)
            DetectedFinger.UNKNOWN -> Color.Gray
        }
        drawCircle(
            color = fingerIndicatorColor,
            radius = 6f,
            center = Offset(centerX, centerY - ringHeight - 20f)
        )
    }
}
