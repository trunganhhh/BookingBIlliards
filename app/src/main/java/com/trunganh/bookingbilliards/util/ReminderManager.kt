package com.trunganh.bookingbilliards.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.trunganh.bookingbilliards.receiver.ReminderReceiver
import java.text.SimpleDateFormat
import java.util.*

class ReminderManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ReminderManager"
        const val REMINDER_15_MIN = 15
        const val REMINDER_30_MIN = 30
        const val REMINDER_1_HOUR = 60
    }
    
    /**
     * Đặt nhắc lịch cho booking
     * @param bookingId ID của booking
     * @param tableName Tên bàn
     * @param startTime Thời gian bắt đầu (format: "yyyy-MM-dd'T'HH:mm:ss" hoặc "HH:mm")
     * @param reminderMinutes Số phút nhắc trước (15, 30, 60)
     */
    fun setReminder(bookingId: String, tableName: String, startTime: String, reminderMinutes: Int) {
        try {
            // Parse thời gian bắt đầu
            val startTimeDate: Date
            if (startTime.contains("T")) {
                // Format: "yyyy-MM-dd'T'HH:mm:ss"
                val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                startTimeDate = timeFormat.parse(startTime) ?: throw IllegalArgumentException("Invalid date format")
            } else {
                // Format: "HH:mm" - giả sử là hôm nay
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeOnly = timeFormat.parse(startTime) ?: throw IllegalArgumentException("Invalid time format")
                
                val calendar = Calendar.getInstance()
                calendar.time = timeOnly
                calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                startTimeDate = calendar.time
            }
            
            // Tính thời gian nhắc lịch (trước startTime reminderMinutes phút)
            val calendar = Calendar.getInstance()
            calendar.time = startTimeDate
            calendar.add(Calendar.MINUTE, -reminderMinutes)
            
            // Nếu thời gian nhắc đã qua, không đặt
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                Log.w(TAG, "Thời gian nhắc đã qua: ${calendar.time}")
                return
            }
            
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("booking_id", bookingId)
                putExtra("table_name", tableName)
                putExtra("start_time", startTime)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                bookingId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            
            Log.d(TAG, "Đã đặt nhắc lịch cho booking $bookingId vào lúc ${calendar.time}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi đặt nhắc lịch: ${e.message}")
        }
    }
    
    /**
     * Hủy nhắc lịch cho booking
     * @param bookingId ID của booking
     */
    fun cancelReminder(bookingId: String) {
        try {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                bookingId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            
            Log.d(TAG, "Đã hủy nhắc lịch cho booking $bookingId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hủy nhắc lịch: ${e.message}")
        }
    }
    
    /**
     * Kiểm tra xem có nhắc lịch nào đang hoạt động không
     * @param bookingId ID của booking
     */
    fun hasReminder(bookingId: String): Boolean {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            bookingId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }
} 