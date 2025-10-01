package com.trunganh.bookingbilliards.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.trunganh.bookingbilliards.R
import com.trunganh.bookingbilliards.databinding.ItemAccountBinding
import com.trunganh.bookingbilliards.model.User
import com.trunganh.bookingbilliards.model.isAdmin
import com.trunganh.bookingbilliards.model.isUser

class AccountAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : ListAdapter<User, AccountAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AccountViewHolder(
        private val binding: ItemAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvUsername.text = user.login
                tvFullName.text = "${user.firstName ?: ""} ${user.lastName ?: ""}"
                tvEmail.text = user.email ?: "Chưa cập nhật"

                val roles = user.authorities ?: emptyList()
                val isAdmin = roles.contains("ROLE_ADMIN")
                val isUser = roles.contains("ROLE_USER")

                chipRole.apply {
                    text = when {
                        isAdmin -> "Quản trị viên"
                        isUser -> "Người dùng"
                        else -> "Khách hàng"
                    }
                    setChipBackgroundColorResource(
                        when {
                            isAdmin -> R.color.admin_role
                            isUser -> R.color.staff_role
                            else -> R.color.customer_role
                        }
                    )
                }

                btnEdit.setOnClickListener { onEditClick(user) }
                btnDelete.setOnClickListener { onDeleteClick(user) }
            }
        }
    }

    private class AccountDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
} 