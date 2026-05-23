package com.farah.jewelryar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.farah.jewelryar.ar.ARViewModel
import com.farah.jewelryar.ar.CameraARScreen
import com.farah.jewelryar.shared.model.Product
import com.farah.jewelryar.ui.screen.ProductListScreen
import com.farah.jewelryar.ui.theme.KmmJewelryARTheme
import com.farah.jewelryar.ui.viewmodel.ProductViewModel
import com.farah.jewelryar.ui.viewmodel.ProductViewModelFactory

class MainActivity : ComponentActivity() {
    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory(this)
    }

    private val arViewModel: ARViewModel by viewModels()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        arViewModel.setCameraPermission(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request camera permission
        requestCameraPermission()
        setContent {
            KmmJewelryARTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentScreen = remember { mutableStateOf<Screen>(Screen.ProductList) }

                    when (currentScreen.value) {
                        Screen.ProductList -> {
                            val uiState = productViewModel.uiState.collectAsState().value
                            ProductListScreen(
                                uiState = uiState,
                                onCategorySelected = { productViewModel.setCategory(it) },
                                onTryOn = { product ->
                                    arViewModel.setProduct(product)
                                    currentScreen.value = Screen.AR
                                }
                            )
                        }
                        Screen.AR -> {
                            val arState = arViewModel.uiState.collectAsState().value
                            CameraARScreen(
                                uiState = arState,
                                onHandDetected = { arViewModel.updateHandLandmarks(it) },
                                onBackClick = { currentScreen.value = Screen.ProductList },
                                onRingStyleChange = { arViewModel.setRingStyle(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                arViewModel.setCameraPermission(true)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

enum class Screen {
    ProductList,
    AR
}
