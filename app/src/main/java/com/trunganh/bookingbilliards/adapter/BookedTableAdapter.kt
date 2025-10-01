package com.trunganh.bookingbilliards.adapter

import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.trunganh.bookingbilliards.databinding.ItemBookedTableBinding
import com.trunganh.bookingbilliards.model.BookedTable
import com.trunganh.bookingbilliards.network.RetrofitClient
import com.trunganh.bookingbilliards.util.ReminderManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.trunganh.bookingbilliards.manager.UserManager

class BookedTableAdapter(
    private var bookedTables: List<BookedTable>,
    private val onItemClick: (BookedTable) -> Unit,
    private val onReviewClick: (BookedTable) -> Unit,
    private val onBookingCancelled: () -> Unit,
    private val context: android.content.Context
) : RecyclerView.Adapter<BookedTableAdapter.BookedTableViewHolder>() {

    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val countdownTimers: MutableMap<String, CountDownTimer> = mutableMapOf()

    class BookedTableViewHolder(
        private val binding: ItemBookedTableBinding,
        private val onItemClick: (BookedTable) -> Unit,
        private val onReviewClick: (BookedTable) -> Unit,
        private val onCancelClick: (BookedTable) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bookedTable: BookedTable) {
            with(binding) {
                tvTableName.text = bookedTable.tableName
                
                // Tính toán và hiển thị thời gian
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val startTime = inputFormat.parse(bookedTable.startTime)
                    
                    if (startTime != null) {
                    // Tính thời gian kết thúc
                        val calendar = Calendar.getInstance()
                        calendar.time = startTime
                        calendar.add(Calendar.MINUTE, bookedTable.duration)
                        val endTime = calendar.time
                    
                        // Format thời gian để hiển thị
                        val outputFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                        tvTimeInfo.text = "${outputFormat.format(startTime)} - ${outputFormat.format(endTime)}"
                    } else {
                        tvTimeInfo.text = "Lỗi hiển thị thời gian"
                    }
                } catch (e: Exception) {
                    Log.e("BookedTableAdapter", "Error parsing date: ${e.message}")
                    tvTimeInfo.text = "Lỗi hiển thị thời gian"
                }

                tvDuration.text = "Thời lượng: ${bookedTable.duration} phút"
                tvContactName.text = "Người liên hệ: ${bookedTable.contactName}"
                tvContactPhone.text = "SĐT: ${bookedTable.contactPhone}"
                tvNote.text = "Ghi chú: ${bookedTable.note}"

                // Hiển thị review nếu có
                if (!bookedTable.review.isNullOrEmpty()) {
                    tvReview.visibility = View.VISIBLE
                    tvReview.text = "Đánh giá: ${bookedTable.review}"
                } else {
                    tvReview.visibility = View.GONE
                }

                // Logic hiển thị/ẩn nút Đánh giá dựa trên thời gian bắt đầu
                val showReviewButton = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val startTime = inputFormat.parse(bookedTable.startTime)
                    val currentTime = Calendar.getInstance().time
                    
                    // Hiển thị nút đánh giá nếu đã đến thời gian bắt đầu và chưa có review
                    startTime != null && currentTime.after(startTime) && bookedTable.review.isNullOrEmpty()
                } catch (e: Exception) {
                    Log.e("BookedTableAdapter", "Error parsing date: ${e.message}")
                    false
                }
                
                btnItemReview.visibility = if (showReviewButton) View.VISIBLE else View.GONE

                // Logic hiển thị nút Hủy booking
                val showCancelButton = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val startTime = inputFormat.parse(bookedTable.startTime)
                    val currentTime = Calendar.getInstance().time
                    
                    // Hiển thị nút hủy nếu chưa đến thời gian bắt đầu
                    startTime != null && currentTime.before(startTime)
                } catch (e: Exception) {
                    Log.e("BookedTableAdapter", "Error parsing date: ${e.message}")
                    false
                }
                
                btnCancelBooking.visibility = if (showCancelButton) View.VISIBLE else View.GONE

                btnItemReview.setOnClickListener {
                    onReviewClick.invoke(bookedTable)
                }

                btnCancelBooking.setOnClickListener {
                    onCancelClick.invoke(bookedTable)
                }

                root.setOnClickListener {
                    onItemClick.invoke(bookedTable)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookedTableViewHolder {
        val binding = ItemBookedTableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookedTableViewHolder(binding, onItemClick, onReviewClick) { bookedTable ->
            showCancelBookingDialog(bookedTable)
        }
    }

    override fun onBindViewHolder(holder: BookedTableViewHolder, position: Int) {
        holder.bind(bookedTables[position])
    }

    override fun getItemCount() = bookedTables.size

    fun updateData(newBookedTables: List<BookedTable>) {
        bookedTables = newBookedTables
        notifyDataSetChanged()
    }

    private fun showCancelBookingDialog(bookedTable: BookedTable) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Hủy đặt bàn")
            .setMessage("Bạn có chắc chắn muốn hủy đặt bàn này không?")
            .setPositiveButton("Hủy đặt bàn") { dialog, _ ->
                cancelBooking(bookedTable)
                dialog.dismiss()
            }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun cancelBooking(bookedTable: BookedTable) {
        val bookingId = bookedTable.id ?: run {
            Toast.makeText(context, "Lỗi: Không tìm thấy ID đặt bàn", Toast.LENGTH_SHORT).show()
            return
        }

        adapterScope.launch {
            try {
                val response = RetrofitClient.apiService.cancelBooking(bookingId)
                if (response.isSuccessful) {
                    // Hủy nhắc lịch nếu có
                    val reminderManager = ReminderManager(context)
                    reminderManager.cancelReminder(bookingId)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Đã hủy đặt bàn thành công", Toast.LENGTH_SHORT).show()
                        onBookingCancelled.invoke()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("BookedTableAdapter", "Lỗi khi hủy đặt bàn: ${response.code()} - $errorBody")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Lỗi khi hủy đặt bàn: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BookedTableAdapter", "Lỗi kết nối khi hủy đặt bàn", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi kết nối: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
} 