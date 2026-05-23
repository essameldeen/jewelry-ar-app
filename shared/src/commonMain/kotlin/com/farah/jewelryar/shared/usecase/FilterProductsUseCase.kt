package com.farah.jewelryar.shared.usecase

import com.farah.jewelryar.shared.model.Product

class FilterProductsUseCase {
    operator fun invoke(products: List<Product>, category: String?): List<Product> =
        if (category.isNullOrBlank()) products
        else products.filter { it.category.equals(category, ignoreCase = true) }
}
