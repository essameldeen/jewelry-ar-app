package com.farah.jewelryar.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val nameAr: String = "",
    val category: String,
    val image: String,
    val overlay: String = "",
    val price: Double = 0.0,
    val originalPrice: Double? = null,
    val description: String = "",
    val descriptionAr: String = "",
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val isNew: Boolean = false,
    val isBestseller: Boolean = false
)
