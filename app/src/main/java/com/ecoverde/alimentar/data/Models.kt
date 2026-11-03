package com.ecoverde.alimentar

data class Food(
    val id: Long = 0,
    val name: String,
    val quantity: String,
    val category: String,
    val date: String
)

data class Recipe(
    val id: Long = 0,
    val name: String,
    val ingredients: String,
    val description: String,
    val category: String
)
