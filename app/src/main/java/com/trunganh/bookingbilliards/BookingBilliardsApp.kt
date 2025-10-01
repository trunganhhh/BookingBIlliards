package com.trunganh.bookingbilliards

import android.app.Application
import com.trunganh.bookingbilliards.manager.UserManager

class BookingBilliardsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserManager.init(this)
    }
} 