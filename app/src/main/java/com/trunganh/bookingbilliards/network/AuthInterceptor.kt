package com.trunganh.bookingbilliards.network

import com.trunganh.bookingbilliards.manager.UserManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = UserManager.getToken()

        // Add Authorization header if token exists
        val requestBuilder = originalRequest.newBuilder()
        if (token != null) {
            // Thêm token cho tất cả các request trừ authenticate
            if (!originalRequest.url.toString().contains("/authenticate")) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
} 