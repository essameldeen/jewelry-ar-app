package com.farah.jewelryar.shared.repository

import com.farah.jewelryar.shared.model.Product
import kotlinx.serialization.json.Json

class ProductRepository(private val jsonProvider: () -> String) {

    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): List<Product> = json.decodeFromString(jsonProvider())
}
