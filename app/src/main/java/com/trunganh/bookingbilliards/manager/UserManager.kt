package com.trunganh.bookingbilliards.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.trunganh.bookingbilliards.model.User

object UserManager {
    private const val PREF_NAME = "user_pref"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER = "user"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        Log.d("UserManager", "Saving user: $userJson")
        prefs.edit().putString(KEY_USER, userJson).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUser(): User? {
        val userJson = prefs.getString(KEY_USER, null)
        Log.d("UserManager", "Retrieved user JSON: $userJson")
        if (userJson == null) return null
        return try {
            val user = gson.fromJson(userJson, User::class.java)
            Log.d("UserManager", "Parsed user: $user")
            Log.d("UserManager", "User authorities: ${user.authorities}")
            user
        } catch (e: Exception) {
            Log.e("UserManager", "Error parsing user JSON", e)
            null
        }
    }

    fun isLoggedIn(): Boolean = getToken() != null && getUser() != null

    fun isAdmin(): Boolean {
        val user = getUser()
        val isAdmin = user?.authorities?.any { it == "ROLE_ADMIN" } == true
        Log.d("UserManager", "Checking admin status - User: $user, Is admin: $isAdmin")
        return isAdmin
    }

    fun isUser(): Boolean {
        val user = getUser()
        val isUser = user?.authorities?.any { it == "ROLE_USER" } == true
        Log.d("UserManager", "Checking user status - User: $user, Is user: $isUser")
        return isUser
    }

    fun clearUserData() {
        prefs.edit().clear().apply()
    }
} 