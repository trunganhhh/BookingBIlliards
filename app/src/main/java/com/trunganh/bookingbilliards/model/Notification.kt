package com.trunganh.bookingbilliards.model

import java.util.Date

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val createdAt: Date,
    val isRead: Boolean = false,
    val type: String? = null // Có thể là "BOOKING", "SYSTEM", etc.
) 