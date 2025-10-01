package com.trunganh.bookingbilliards.network

import com.trunganh.bookingbilliards.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth endpoints
    @POST("authenticate")
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<User>

    // Table endpoints
    @GET("billiards-tables")
    suspend fun getTables(): List<Table>

    @POST("billiards-tables")
    suspend fun addTable(@Body table: Table): Response<Table>

    @PUT("billiards-tables/{id}")
    suspend fun updateTable(@Path("id") tableId: String, @Body table: Table): Response<Table>

    @DELETE("billiards-tables/{id}")
    suspend fun deleteTable(@Path("id") tableId: String): Response<Unit>

    @GET("billiards-tables/{id}")
    suspend fun getTableInfo(@Path("id") id: String): Response<Table>

    // Booking endpoints
    @POST("bookings")
    suspend fun createBooking(@Body booking: Booking): Response<Void>

    @GET("bookings")
    suspend fun getBookings(@Query("user") userId: String): List<Booking>

    @DELETE("bookings/{id}")
    suspend fun deleteBooking(@Path("id") bookingId: String): Response<Void>

    @DELETE("bookings/{id}")
    suspend fun cancelBooking(@Path("id") bookingId: String): Response<Void>

    @PUT("bookings/{id}")
    suspend fun updateBooking(@Path("id") bookingId: String, @Body booking: Booking): Response<Booking>

    @PATCH("bookings/{id}/review")
    suspend fun updateBookingReview(@Path("id") bookingId: String, @Body reviewRequest: ReviewRequest): Response<Booking>

    @GET("bookings")
    suspend fun getBookedTables(): Response<List<BookedTable>>

    @GET("bookings/{tableId}")
    suspend fun getBookedTableById(@Path("tableId") tableId: String): Response<BookedTable>

    // User endpoints
    @GET("account")
    suspend fun getCurrentUser(): Response<User>

    @PUT("account")
    suspend fun updateUser(@Body user: User): Response<User>

    @GET("admin/users")
    suspend fun getAllUsers(): Response<List<User>>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") userId: String): Response<Unit>

    // Notification endpoints
    @GET("notifications")
    suspend fun getNotifications(): Response<List<Notification>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") notificationId: String): Response<Void>

    @GET("notifications/unread/count")
    suspend fun getUnreadNotificationCount(): Response<Int>

    @GET("authorities")
    suspend fun getAuthorities(): Response<List<Authority>>
}
