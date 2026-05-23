package com.farah.jewelryar.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.farah.jewelryar.shared.repository.ProductRepository
import com.farah.jewelryar.shared.usecase.FilterProductsUseCase

class ProductViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repository = ProductRepository { loadJsonProducts(context) }
    private val filterUseCase = FilterProductsUseCase()

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ProductViewModel(repository, filterUseCase) as T
    }

    companion object {
        private fun loadJsonProducts(context: Context): String {
            return context.assets.open("products.json").bufferedReader().use { it.readText() }
        }
    }
}
