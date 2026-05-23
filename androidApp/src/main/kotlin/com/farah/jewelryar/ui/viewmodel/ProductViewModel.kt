package com.farah.jewelryar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farah.jewelryar.shared.model.Product
import com.farah.jewelryar.shared.repository.ProductRepository
import com.farah.jewelryar.shared.usecase.FilterProductsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProductUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProductViewModel(
    private val repository: ProductRepository,
    private val filterUseCase: FilterProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val products = repository.getAll()
                _uiState.value = _uiState.value.copy(
                    products = products,
                    filteredProducts = products,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun setCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilter(category)
    }

    private fun applyFilter(category: String?) {
        val filtered = filterUseCase(
            products = _uiState.value.products,
            category = category
        )
        _uiState.value = _uiState.value.copy(filteredProducts = filtered)
    }
}
