package com.trunganh.bookingbilliards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.trunganh.bookingbilliards.databinding.ItemNotificationBinding
import com.trunganh.bookingbilliards.model.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private var notifications: List<Notification> = emptyList(),
    private val onItemClick: ((Notification) -> Unit)? = null
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(
        private val binding: ItemNotificationBinding,
        private val onItemClick: ((Notification) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(notification: Notification) {
            binding.apply {
                // Set title
                tvTitle.text = notification.title
                
                // Set message
                tvMessage.text = notification.message
                
                // Set time
                val dateFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                tvTime.text = dateFormat.format(notification.createdAt)
                
                // Show/hide unread indicator
                unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
                
                // Set click listener
                root.setOnClickListener {
                    onItemClick?.invoke(notification)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    fun updateData(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
} 