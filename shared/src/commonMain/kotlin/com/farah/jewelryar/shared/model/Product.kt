package com.farah.jewelryar.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val image: String,
    val overlay: String,
    val price: Double = 0.0,
    val description: String = ""
)
