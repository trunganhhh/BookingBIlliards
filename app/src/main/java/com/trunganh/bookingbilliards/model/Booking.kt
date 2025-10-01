package com.trunganh.bookingbilliards.model

data class Booking(
    val id: String? = null,
    val tableId: String,
    val startTime: String,
    val duration: Int,
    val contactName: String,
    val contactPhone: String,
    val note: String? = null,
    val tableNumber: Int? = null,
    val bookingTime: String? = null,
    val totalPrice: Double? = null,
    val status: String? = null,
    val review: String? = null
)

data class ReviewRequest(
    val review: String
)
