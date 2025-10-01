package com.trunganh.bookingbilliards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.trunganh.bookingbilliards.adapter.AccountAdapter
import com.trunganh.bookingbilliards.databinding.DialogEditProfileBinding
import com.trunganh.bookingbilliards.databinding.FragmentAccountManagementBinding
import com.trunganh.bookingbilliards.model.User
import com.trunganh.bookingbilliards.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountManagementFragment : Fragment() {
    private var _binding: FragmentAccountManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var accountAdapter: AccountAdapter
    private var authorities: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadAccounts()
    }

    private fun setupRecyclerView() {
        accountAdapter = AccountAdapter(
            onEditClick = { user -> showEditDialog(user) },
            onDeleteClick = { user -> showDeleteConfirmation(user) }
        )
        binding.recyclerAccounts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = accountAdapter
        }
    }

    private fun loadAccounts() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val usersResponse = RetrofitClient.apiService.getAllUsers()
                var authoritiesList: List<String> = emptyList()
                try {
                    val authoritiesResponse = RetrofitClient.apiService.getAuthorities()
                    if (authoritiesResponse.isSuccessful) {
                        authoritiesList = authoritiesResponse.body()?.map { it.id } ?: emptyList()
                    }
                } catch (e: Exception) {
                    Log.e("AccountManagement", "Error loading authorities", e)
                }
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (usersResponse.isSuccessful) {
                        val users = usersResponse.body()
                        Log.d("AccountManagement", "Users: $users")
                        authorities = authoritiesList
                        if (users.isNullOrEmpty()) {
                            binding.tvEmpty.visibility = View.VISIBLE
                        } else {
                            accountAdapter.submitList(users)
                        }
                    } else {
                        Toast.makeText(context, "Lỗi khi tải danh sách tài khoản", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEditDialog(user: User) {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chỉnh sửa tài khoản")
            .setView(dialogBinding.root)
            .setPositiveButton("Lưu", null)
            .setNegativeButton("Hủy", null)
            .create()

        dialogBinding.apply {
            edtFullName.setText("${user.firstName ?: ""} ${user.lastName ?: ""}")
            edtEmail.setText(user.email ?: "")
            edtPhone.setText("") // Không có trường phone trong model User
        }

        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val fullName = dialogBinding.edtFullName.text.toString().trim()
            val email = dialogBinding.edtEmail.text.toString().trim()

            if (fullName.isEmpty()) {
                dialogBinding.edtFullName.error = "Vui lòng nhập họ tên"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                dialogBinding.edtEmail.error = "Vui lòng nhập email"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                dialogBinding.edtEmail.error = "Email không hợp lệ"
                return@setOnClickListener
            }

            dialogBinding.progressBar.visibility = View.VISIBLE
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Tách họ và tên
                    val nameParts = fullName.trim().split(" ")
                    val firstName = nameParts.firstOrNull() ?: ""
                    val lastName = nameParts.drop(1).joinToString(" ")

                    val updatedUser = user.copy(
                        firstName = firstName,
                        lastName = lastName,
                        email = email
                    )

                    val response = RetrofitClient.apiService.updateUser(updatedUser)
                    withContext(Dispatchers.Main) {
                        dialogBinding.progressBar.visibility = View.GONE
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadAccounts()
                        } else {
                            Toast.makeText(context, "Lỗi khi cập nhật: ${response.message()}", Toast.LENGTH_SHORT).show()
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        dialogBinding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmation(user: User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa tài khoản")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản ${user.login}?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteUser(user)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteUser(user: User) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.deleteUser(user._id ?: "")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Xóa tài khoản thành công", Toast.LENGTH_SHORT).show()
                        loadAccounts()
                    } else {
                        Toast.makeText(context, "Lỗi khi xóa tài khoản: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 