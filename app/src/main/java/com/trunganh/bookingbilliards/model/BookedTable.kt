package com.trunganh.bookingbilliards.model

data class BookedTable(
    val id: String? = null,
    val tableId: String,
    val tableName: String,
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