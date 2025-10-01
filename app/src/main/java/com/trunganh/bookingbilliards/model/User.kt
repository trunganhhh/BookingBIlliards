package com.trunganh.bookingbilliards.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id") val _id: String? = null,
    @SerializedName("login") val login: String,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val email: String? = null,
    val activated: Boolean = false,
    @SerializedName("lang_key") val langKey: String? = null,
    @SerializedName("authorities") val authorities: List<String> = emptyList()
    // Không có trường phone trong cấu trúc DB User bạn cung cấp
)

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("rememberMe") val rememberMe: Boolean = false
)

data class LoginResponse(
    @SerializedName("id_token") val token: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val phone: String,
    val email: String? = null
)

// Extension function để kiểm tra role
fun User.isAdmin(): Boolean = authorities?.any { it == "ROLE_ADMIN" } ?: false
fun User.isUser(): Boolean = authorities?.any { it == "ROLE_USER" } ?: false 