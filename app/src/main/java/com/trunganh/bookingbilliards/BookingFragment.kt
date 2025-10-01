package com.trunganh.bookingbilliards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.trunganh.bookingbilliards.databinding.FragmentBookingBinding
import com.trunganh.bookingbilliards.manager.UserManager
import com.trunganh.bookingbilliards.model.Booking
import com.trunganh.bookingbilliards.network.RetrofitClient
import com.trunganh.bookingbilliards.util.ReminderDialog
import com.trunganh.bookingbilliards.util.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class BookingFragment : Fragment() {
    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!

    // Nhận đối số tableId
    private val args: BookingFragmentArgs by navArgs()
    private val calendar = Calendar.getInstance()
    private lateinit var reminderManager: ReminderManager
    private var selectedReminderMinutes: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ReminderManager
        reminderManager = ReminderManager(requireContext())

        // Lấy tableId từ arguments
        val tableId = args.tableId
        Log.d("BookingFragment", "Booking for Table ID: $tableId")

        setupListeners(tableId)
        setupDatePicker()
        setupReminderButton()
    }

    private fun setupListeners(tableId: String) {
        binding.btnBook.setOnClickListener {
            createBooking(tableId)
        }
    }

    private fun setupDatePicker() {
        binding.etStartTime.setOnClickListener { showDateTimePicker() }
    }

    private fun setupReminderButton() {
        binding.btnSetReminder.setOnClickListener {
            showReminderDialog()
        }
    }

    private fun showReminderDialog() {
        ReminderDialog(requireContext()) { reminderMinutes ->
            selectedReminderMinutes = reminderMinutes
            updateReminderButtonText()
        }.show()
    }

    private fun updateReminderButtonText() {
        val reminderText = when (selectedReminderMinutes) {
            ReminderManager.REMINDER_15_MIN -> "Nhắc lịch: 15 phút trước"
            ReminderManager.REMINDER_30_MIN -> "Nhắc lịch: 30 phút trước"
            ReminderManager.REMINDER_1_HOUR -> "Nhắc lịch: 1 giờ trước"
            null -> "Nhắc lịch: Không"
            else -> "Nhắc lịch: 15 phút trước"
        }
        binding.btnSetReminder.text = reminderText
    }

    private fun showDateTimePicker() {
        // Date picker
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Chọn ngày đặt bàn")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener {
            val selectedDate = Date(it)
            calendar.time = selectedDate

            // Time picker
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Chọn giờ đặt bàn")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)
                val dateFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                binding.etStartTime.setText(dateFormat.format(calendar.time))
            }

            timePicker.show(childFragmentManager, "TIME_PICKER_TAG")
        }

        datePicker.show(childFragmentManager, "DATE_PICKER_TAG")
    }

    private fun createBooking(tableId: String) {
        val startTimeString = binding.etStartTime.text.toString()
        val durationString = binding.etDuration.text.toString()
        val contactName = binding.etContactName.text.toString()
        val contactPhone = binding.etContactPhone.text.toString()
        val note = binding.etNote.text.toString().trim()

        // Validate input
        if (startTimeString.isBlank() || durationString.isBlank() || contactName.isBlank() || contactPhone.isBlank()) {
            Toast.makeText(requireContext(), "Vui lòng điền đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show()
            return
        }

        val duration = durationString.toIntOrNull()
        if (duration == null || duration <= 0) {
            Toast.makeText(requireContext(), "Thời lượng không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        // Format startTime to API expected format (assuming yyyy-MM-dd'T'HH:mm:ss)
        val inputFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        // Ensure timezone is handled correctly if necessary, defaulting to UTC or server's expected timezone
        // For now, assuming server handles 'T' and format correctly.
        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())

        val startTimeDate: Date
        try {
            startTimeDate = inputFormat.parse(startTimeString) ?: throw IllegalArgumentException("Invalid date format")
        } catch (e: Exception) {
            Log.e("Booking", "Date parsing error: ${e.message}", e)
            Toast.makeText(requireContext(), "Thời gian bắt đầu không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        val booking = Booking(
            tableId = tableId,
            startTime = outputFormat.format(startTimeDate),
            duration = duration,
            contactName = contactName,
            contactPhone = contactPhone,
            note = note.takeIf { it.isNotBlank() } // Send empty string if blank, not null
        )

        // Log booking data
        Log.d("Booking", """
            Creating booking with data:
            tableId: $tableId
            startTime: ${outputFormat.format(startTimeDate)}
            duration: $duration
            contactName: $contactName
            contactPhone: $contactPhone
            note: ${booking.note}
        """.trimIndent())

        binding.progressBar.visibility = View.VISIBLE
        binding.btnBook.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Assuming your API endpoint for creating booking is POST /bookings
                val response = RetrofitClient.apiService.createBooking(booking)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Đặt nhắc lịch nếu user đã chọn
                        if (selectedReminderMinutes != null) {
                            // Tạo bookingId từ tableId và timestamp để đảm bảo unique
                            val bookingId = "${tableId}_${System.currentTimeMillis()}"
                            
                            reminderManager.setReminder(
                                bookingId = bookingId,
                                tableName = "Bàn số $tableId",
                                startTime = outputFormat.format(startTimeDate),
                                reminderMinutes = selectedReminderMinutes!!
                            )
                            
                            Toast.makeText(requireContext(), "Đặt bàn thành công! Đã đặt nhắc lịch.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Đặt bàn thành công!", Toast.LENGTH_SHORT).show()
                        }
                        
                        findNavController().popBackStack(R.id.navigation_home, false)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Booking", "Booking failed: ${response.code()} - $errorBody")
                        Toast.makeText(requireContext(), "Đặt bàn thất bại: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Booking", "Booking error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                     if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                        binding.btnBook.isEnabled = true
                     }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
