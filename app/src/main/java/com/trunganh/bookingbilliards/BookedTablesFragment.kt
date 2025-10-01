package com.trunganh.bookingbilliards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.NavOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.trunganh.bookingbilliards.adapter.BookedTableAdapter
import com.trunganh.bookingbilliards.databinding.FragmentBookedTablesBinding
import com.trunganh.bookingbilliards.manager.UserManager
import com.trunganh.bookingbilliards.model.BookedTable
import com.trunganh.bookingbilliards.network.RetrofitClient
import kotlinx.coroutines.*
import android.widget.TextView
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import com.trunganh.bookingbilliards.model.Booking
import com.trunganh.bookingbilliards.model.ReviewRequest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BookedTablesFragment : Fragment() {
    private var _binding: FragmentBookedTablesBinding? = null
    private val binding get() = _binding!!
    private lateinit var bookedTableAdapter: BookedTableAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookedTablesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (!UserManager.isLoggedIn()) {
            showLoginRequiredDialog()
            return
        }
        
        setupRecyclerView()
        loadBookedTables()
    }

    private fun setupRecyclerView() {
        binding.recyclerBookedTables.layoutManager = LinearLayoutManager(requireContext())
        bookedTableAdapter = BookedTableAdapter(
            bookedTables = emptyList(), // Khởi tạo với danh sách rỗng
            onItemClick = { bookedTable ->
                // Xử lý khi nhấn vào item (ví dụ: mở chi tiết)
                showBookingDetailsDialog(bookedTable)
            },
            onReviewClick = { bookedTable ->
                // Xử lý khi nhấn nút Đánh giá
                showReviewDialog(bookedTable)
            },
            onBookingCancelled = {
                // Tải lại danh sách khi có booking bị hủy
                loadBookedTables()
            },
            context = requireContext()
        )
        binding.recyclerBookedTables.adapter = bookedTableAdapter
    }

    private fun loadBookedTables() {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getBookedTables()
                if (response.isSuccessful) {
                    response.body()?.let { bookedTables ->
                        Log.d("BookedTables", "Received ${bookedTables.size} booked tables from API.")
                        bookedTables.forEach { 
                            Log.d("BookedTables", """
                                BookedTable details:
                                - ID: ${it.id}
                                - TableId: ${it.tableId}
                                - StartTime: ${it.startTime}
                                - Duration: ${it.duration}
                                - Status: ${it.status}
                                - Review: ${it.review}
                                - ContactName: ${it.contactName}
                                - ContactPhone: ${it.contactPhone}
                            """.trimIndent())
                        }
                        
                        val tablesWithNames = bookedTables.map { bookedTable ->
                            try {
                                val tableResponse = RetrofitClient.apiService.getTableInfo(bookedTable.tableId)
                                if (tableResponse.isSuccessful) {
                                    tableResponse.body()?.let { table ->
                                        Log.d("BookedTables", "Fetched table info for ${bookedTable.tableId}: TableName=${table.name}")
                                        bookedTable.copy(tableName = table.name)
                                    } ?: run {
                                        Log.w("BookedTables", "getTableInfo body is null for tableId: ${bookedTable.tableId}")
                                        bookedTable.copy(tableName = "Không lấy được tên bàn")
                                    }
                                } else {
                                    Log.e("BookedTables", "getTableInfo failed for tableId: ${bookedTable.tableId} with code: ${tableResponse.code()}")
                                    bookedTable.copy(tableName = "Không lấy được tên bàn")
                                }
                            } catch (e: Exception) {
                                Log.e("API_ERROR", "Error loading table info for tableId: ${bookedTable.tableId}", e)
                                bookedTable.copy(tableName = "Không lấy được tên bàn")
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            bookedTableAdapter.updateData(tablesWithNames)
                            binding.progressBar.visibility = View.GONE
                            binding.emptyState.visibility = if (tablesWithNames.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("BookedTables", "getBookedTables failed with code: ${response.code()} - $errorBody")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Lỗi khi tải danh sách bàn đã đặt", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error loading booked tables", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun showLoginRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yêu cầu đăng nhập")
            .setMessage("Vui lòng đăng nhập để xem danh sách bàn đã đặt")
            .setPositiveButton("Đăng nhập") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigate(R.id.loginFragment, null, NavOptions.Builder()
                     .setPopUpTo(findNavController().currentDestination?.id ?: R.id.navigation_home, true)
                    .build())
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigate(R.id.navigation_home)
            }
            .setCancelable(false)
            .show()
    }

    private fun showBookingDetailsDialog(bookedTable: BookedTable) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chi tiết đặt bàn")
            .setView(R.layout.dialog_booking_details)
            .setPositiveButton("Đóng", null)
            .create()

        dialog.show()

        val dialogView = dialog.findViewById<View>(android.R.id.content) as ViewGroup
        val detailsView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_booking_details, dialogView, false)
        dialogView.removeAllViews()
        dialogView.addView(detailsView)

        // Bind data
        detailsView.findViewById<TextView>(R.id.tvTableNumber).text = "Bàn số ${bookedTable.tableNumber}"
        detailsView.findViewById<TextView>(R.id.tvBookingTime).text = "Thời gian: ${bookedTable.bookingTime}"
        detailsView.findViewById<TextView>(R.id.tvDuration).text = "Thời lượng: ${bookedTable.duration} giờ"
        detailsView.findViewById<TextView>(R.id.tvTotalPrice).text = "Tổng tiền: ${bookedTable.totalPrice}đ"
        detailsView.findViewById<TextView>(R.id.tvStatus).text = "Trạng thái: ${bookedTable.status}"

        // Thêm nút đánh giá
        val btnReview = detailsView.findViewById<Button>(R.id.btnReview)
        btnReview.visibility = if (bookedTable.status == "Hoàn thành") View.VISIBLE else View.GONE
        btnReview.setOnClickListener {
            showReviewDialog(bookedTable)
        }
    }

    private fun showReviewDialog(bookedTable: BookedTable) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_review, null)
        val etReview = dialogView.findViewById<TextInputEditText>(R.id.etReview)
        etReview.setText(bookedTable.review) // Hiển thị đánh giá cũ nếu có

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Gửi") { dialog, _ ->
                val review = etReview.text.toString().trim()
                if (review.isNotEmpty()) {
                    submitReview(bookedTable, review)
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập đánh giá", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun submitReview(bookedTable: BookedTable, review: String) {
        val bookingId = bookedTable.id ?: run {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy ID đặt bàn.", Toast.LENGTH_SHORT).show()
            return
        }

        // Tạo đối tượng ReviewRequest để gửi lên backend
        val reviewRequest = ReviewRequest(review = review)

        lifecycleScope.launch {
            try {
                // Gọi API PATCH chuyên biệt cho review
                val response = RetrofitClient.apiService.updateBookingReview(bookingId, reviewRequest)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Cảm ơn đánh giá của bạn!", Toast.LENGTH_SHORT).show()
                    // Tải lại danh sách để cập nhật UI với đánh giá mới
                    loadBookedTables()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("BookedTables", "Lỗi khi gửi đánh giá: ${response.code()} - $errorBody")
                    Toast.makeText(requireContext(), "Lỗi khi gửi đánh giá: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("BookedTables", "Lỗi kết nối khi gửi đánh giá", e)
                Toast.makeText(requireContext(), "Lỗi kết nối: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::bookedTableAdapter.isInitialized) {
            // bookedTableAdapter.cancelAllTimers()
        }
        _binding = null
    }
} 