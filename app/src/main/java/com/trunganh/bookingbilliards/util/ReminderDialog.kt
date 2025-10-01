package com.trunganh.bookingbilliards.util

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import com.trunganh.bookingbilliards.R

class ReminderDialog(
    context: Context,
    private val onReminderSelected: (Int?) -> Unit
) : Dialog(context) {

    private var selectedReminderMinutes: Int? = ReminderManager.REMINDER_15_MIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_reminder_setting)

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupReminder)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        // Xử lý khi user chọn radio button
        radioGroup?.setOnCheckedChangeListener { _, checkedId ->
            selectedReminderMinutes = when (checkedId) {
                R.id.radio15min -> ReminderManager.REMINDER_15_MIN
                R.id.radio30min -> ReminderManager.REMINDER_30_MIN
                R.id.radio1hour -> ReminderManager.REMINDER_1_HOUR
                R.id.radioNoReminder -> null
                else -> ReminderManager.REMINDER_15_MIN
            }
        }

        // Xử lý nút Hủy
        btnCancel?.setOnClickListener {
            dismiss()
        }

        // Xử lý nút Xác nhận
        btnConfirm?.setOnClickListener {
            onReminderSelected(selectedReminderMinutes)
            dismiss()
        }

        // Không cho phép đóng dialog khi click bên ngoài
        setCanceledOnTouchOutside(false)
    }
} 