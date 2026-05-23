package com.farah.jewelryar.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.farah.jewelryar.shared.model.Product
import com.farah.jewelryar.ui.viewmodel.ProductUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    uiState: ProductUiState,
    onCategorySelected: (String?) -> Unit,
    onTryOn: (Product) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Jewelry Store") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            FilterChips(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = onCategorySelected
            )

            // Products list or loading state
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 32.dp)
                    )
                }
                uiState.error != null -> {
                    Text(
                        text = "Error: ${uiState.error}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                uiState.filteredProducts.isEmpty() -> {
                    Text(
                        text = "No products found",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredProducts) { product ->
                            ProductCard(
                                product = product,
                                onTryOn = onTryOn
                            )
                        }
                    }
                }
            }
        }
    }
}
