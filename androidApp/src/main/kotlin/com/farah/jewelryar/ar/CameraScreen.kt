package com.farah.jewelryar.ar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.farah.jewelryar.ui.theme.GreenDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun CameraARScreen(
    uiState: ARUiState,
    isArabic: Boolean,
    onHandDetected: (HandLandmarks) -> Unit,
    onPoseDetected: (PoseLandmarks) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val product = uiState.product
    val isNecklace = product?.category == "necklace" || product?.category == "luxury"

    val handDetectionManager = remember { HandDetectionManager(context) }
    val poseDetectionManager = remember { PoseDetectionManager(context) }

    // Load overlay image from assets
    var overlayBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(product?.overlay) {
        val overlayPath = product?.overlay ?: return@LaunchedEffect
        try {
            val bmp = context.assets.open(overlayPath).use { BitmapFactory.decodeStream(it) }
            overlayBitmap = bmp?.asImageBitmap()
        } catch (e: Exception) {
            Log.e("CameraARScreen", "Failed to load overlay: $overlayPath — ${e.message}")
        }
    }

    LaunchedEffect(uiState.cameraPermissionGranted) {
        if (!uiState.cameraPermissionGranted) return@LaunchedEffect
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            provider.unbindAll()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { analysis ->
                    analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        val bitmap = imageProxyToBitmap(imageProxy)
                        imageProxy.close()
                        if (bitmap != null) {
                            GlobalScope.launch(Dispatchers.Default) {
                                if (isNecklace) {
                                    val pose = poseDetectionManager.detectPose(bitmap)
                                    GlobalScope.launch(Dispatchers.Main) { onPoseDetected(pose) }
                                } else {
                                    val hand = handDetectionManager.detectHands(bitmap)
                                    GlobalScope.launch(Dispatchers.Main) { onHandDetected(hand) }
                                }
                            }
                        }
                    }
                }
            try {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraARScreen", "Camera binding failed: ${e.message}")
            }
        }, { android.os.Handler(android.os.Looper.getMainLooper()).post(it) })
    }

    DisposableEffect(Unit) {
        onDispose {
            handDetectionManager.close()
            poseDetectionManager.close()
        }
    }

    val isDetected = if (isNecklace) uiState.poseLandmarks.isDetected else uiState.handLandmarks.isDetected

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // AR overlay canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlay = overlayBitmap ?: return@Canvas
            if (isNecklace) {
                if (uiState.poseLandmarks.isDetected) {
                    drawNecklaceOverlay(overlay, uiState.poseLandmarks)
                }
            } else {
                if (uiState.handLandmarks.isDetected) {
                    val cat = product?.category ?: "ring"
                    drawHandOverlay(overlay, uiState.handLandmarks, cat)
                }
            }
        }

        // Back button
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isArabic) "رجوع ←" else "← Back", color = Color.White)
        }

        // Status badge
        val statusColor = if (isDetected) Color(0xFF10B981) else Color(0xFFF472B6)
        Text(
            text = when {
                isNecklace && isDetected -> if (isArabic) "تم الكشف عن الجسم" else "Body Detected"
                isNecklace -> if (isArabic) "ادخل في الإطار" else "Step into frame"
                isDetected -> if (isArabic) "تم الكشف عن اليد" else "Hand Detected"
                else -> if (isArabic) "أظهر يدك" else "Show your hand"
            },
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .background(statusColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // Product info bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isArabic) (product?.nameAr?.ifEmpty { product.name } ?: "") else (product?.name ?: ""),
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$${product?.price?.toInt()}",
                color = Color(0xFFD946EF),
                fontSize = 14.sp
            )
            Text(
                text = when {
                    isNecklace -> if (isArabic) "اعرض كتفيك وصدرك للكاميرا" else "Show your shoulders & chest"
                    product?.category == "bracelet" -> if (isArabic) "ضع معصمك في المركز" else "Position your wrist"
                    else -> if (isArabic) "ضع إصبع الخاتم في المركز" else "Position your ring finger"
                },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun DrawScope.drawNecklaceOverlay(overlay: ImageBitmap, pose: PoseLandmarks) {
    val w = size.width
    val h = size.height

    // Mirror: front camera is mirrored, so flip x coords
    val noseY = pose.nose.y * h
    // Mirror x: MediaPipe coords are already in camera space, front camera flips x
    val lsX = (1f - pose.leftShoulder.x) * w
    val rsX = (1f - pose.rightShoulder.x) * w
    val lsY = pose.leftShoulder.y * h
    val rsY = pose.rightShoulder.y * h

    val centerX = (lsX + rsX) / 2f
    val shoulderWidth = abs(rsX - lsX)
    val shoulderMidY = (lsY + rsY) / 2f
    val clavicleY = noseY + (shoulderMidY - noseY) * 0.65f

    val drawW = shoulderWidth * 1.05f
    val aspect = overlay.height.toFloat() / overlay.width.toFloat()
    val drawH = aspect * drawW

    drawImage(
        image = overlay,
        dstOffset = androidx.compose.ui.unit.IntOffset(
            (centerX - drawW / 2f).toInt(),
            clavicleY.toInt()
        ),
        dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt())
    )
}

private fun DrawScope.drawHandOverlay(overlay: ImageBitmap, hand: HandLandmarks, category: String) {
    val w = size.width
    val h = size.height
    val aspect = overlay.height.toFloat() / overlay.width.toFloat()

    val wristX = (1f - hand.wrist.x) * w
    val wristY = hand.wrist.y * h
    val midX = (1f - hand.middleFingerTip.x) * w
    val midY = hand.middleFingerTip.y * h
    val handSize = sqrt((midX - wristX) * (midX - wristX) + (midY - wristY) * (midY - wristY))
    val scale = handSize / 200f

    if (category == "bracelet") {
        val bx = wristX
        val by = wristY
        val baseX = (1f - hand.ringFingerBase.x) * w
        val baseY = hand.ringFingerBase.y * h
        val angle = atan2(baseY - by, baseX - bx) - (Math.PI / 2).toFloat()
        val drawW = 90f * scale
        val drawH = aspect * drawW

        withTransform({
            translate(bx, by)
            rotate(Math.toDegrees(angle.toDouble()).toFloat(), Offset.Zero)
        }) {
            drawImage(
                image = overlay,
                dstOffset = androidx.compose.ui.unit.IntOffset((-drawW / 2f).toInt(), (-drawH / 2f).toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt())
            )
        }
    } else {
        // ring
        val ringX = (1f - hand.ringFingerBase.x) * w
        val ringY = hand.ringFingerBase.y * h
        val tipX = (1f - hand.ringFingerTip.x) * w
        val tipY = hand.ringFingerTip.y * h
        val angle = atan2(tipY - ringY, tipX - ringX) - (Math.PI / 2).toFloat()
        val drawW = 65f * scale
        val drawH = aspect * drawW

        withTransform({
            translate(ringX, ringY)
            rotate(Math.toDegrees(angle.toDouble()).toFloat(), Offset.Zero)
        }) {
            drawImage(
                image = overlay,
                dstOffset = androidx.compose.ui.unit.IntOffset((-drawW / 2f).toInt(), (-drawH / 2f).toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt())
            )
        }
    }
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val width = imageProxy.width
        val height = imageProxy.height

        if (imageProxy.format == ImageFormat.YUV_420_888) {
            val planes = imageProxy.planes
            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]

            val yData = ByteArray(yPlane.buffer.remaining()).also { yPlane.buffer.get(it) }
            val uvPixelStride = uPlane.pixelStride

            val uData: ByteArray
            val vData: ByteArray

            if (uvPixelStride == 1) {
                uData = ByteArray(uPlane.buffer.remaining()).also { uPlane.buffer.get(it) }
                vData = ByteArray(vPlane.buffer.remaining()).also { vPlane.buffer.get(it) }
            } else {
                val uvSize = uPlane.buffer.remaining()
                val uvBuffer = ByteArray(uvSize).also { uPlane.buffer.get(it) }
                uData = ByteArray(uvSize / 2)
                vData = ByteArray(uvSize / 2)
                for (i in uData.indices) {
                    uData[i] = uvBuffer[i * 2]
                    vData[i] = uvBuffer[i * 2 + 1]
                }
            }

            val rgb = IntArray(width * height)
            for (i in 0 until width * height) {
                val y = (yData[i].toInt() and 0xFF).toFloat()
                val uvIndex = (i / width shr 1) * (width shr 1) + (i % width shr 1)
                if (uvIndex < uData.size) {
                    val u = ((uData[uvIndex].toInt() and 0xFF) - 128).toFloat()
                    val v = ((vData[uvIndex].toInt() and 0xFF) - 128).toFloat()
                    val r = (y + 1.402f * v).toInt().coerceIn(0, 255)
                    val g = (y - 0.344136f * u - 0.714136f * v).toInt().coerceIn(0, 255)
                    val b = (y + 1.772f * u).toInt().coerceIn(0, 255)
                    rgb[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                } else {
                    rgb[i] = 0xFFFFFFFF.toInt()
                }
            }
            Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888)
        } else null
    } catch (e: Exception) {
        null
    }
}
