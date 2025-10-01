package com.trunganh.bookingbilliards.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.trunganh.bookingbilliards.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tableName = intent.getStringExtra("table_name") ?: "Bàn"
        val bookingId = intent.getStringExtra("booking_id") ?: ""
        val startTime = intent.getStringExtra("start_time") ?: ""
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Nhắc lịch đặt bàn", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc lịch đặt bàn"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("⏰ Nhắc lịch đặt bàn")
            .setContentText("Sắp đến giờ đặt $tableName lúc $startTime, bạn đừng quên nhé!")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(bookingId.hashCode(), notification)
    }
} 