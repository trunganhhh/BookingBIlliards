package com.trunganh.bookingbilliards.model

data class Table(
    val id: String,
    val tableNumber: String,
    val name: String,
    val type: String,
    val ballType: String,
    val pricePerHour: Int,
    val imageUrl: String,
    val content: String,
    val status: String
)
