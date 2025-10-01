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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.trunganh.bookingbilliards.adapter.NotificationAdapter
import com.trunganh.bookingbilliards.databinding.FragmentNotificationsBinding
import com.trunganh.bookingbilliards.manager.UserManager
import com.trunganh.bookingbilliards.model.Notification
import com.trunganh.bookingbilliards.network.RetrofitClient
import kotlinx.coroutines.*

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationAdapter: NotificationAdapter
    private var notifications: List<Notification> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (!UserManager.isLoggedIn()) {
            showLoginRequiredDialog()
            return
        }
        
        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
        notificationAdapter = NotificationAdapter(
            notifications = notifications,
            onItemClick = { notification ->
                if (!notification.isRead) {
                    markNotificationAsRead(notification)
                }
            }
        )
        binding.recyclerNotifications.adapter = notificationAdapter
    }

    private fun loadNotifications() {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getNotifications()
                if (response.isSuccessful) {
                    response.body()?.let { newNotifications ->
                        notifications = newNotifications
                        withContext(Dispatchers.Main) {
                            notificationAdapter.updateData(notifications)
                            binding.progressBar.visibility = View.GONE
                            binding.emptyState.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("Notifications", "getNotifications failed with code: ${response.code()} - $errorBody")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Lỗi khi tải danh sách thông báo", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("Notifications", "Error loading notifications", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun markNotificationAsRead(notification: Notification) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.markNotificationAsRead(notification.id)
                if (response.isSuccessful) {
                    // Update local data
                    notifications = notifications.map { existingNotification ->
                        if (existingNotification.id == notification.id) {
                            existingNotification.copy(isRead = true)
                        } else {
                            existingNotification
                        }
                    }
                    withContext(Dispatchers.Main) {
                        notificationAdapter.updateData(notifications)
                    }
                } else {
                    Log.e("Notifications", "markNotificationAsRead failed for id: ${notification.id}")
                }
            } catch (e: Exception) {
                Log.e("Notifications", "Error marking notification as read", e)
            }
        }
    }

    private fun showLoginRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yêu cầu đăng nhập")
            .setMessage("Vui lòng đăng nhập để xem thông báo")
            .setPositiveButton("Đăng nhập") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigate(R.id.loginFragment)
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigate(R.id.navigation_home)
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 