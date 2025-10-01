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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.trunganh.bookingbilliards.databinding.DialogEditProfileBinding
import com.trunganh.bookingbilliards.databinding.FragmentAccountBinding
import com.trunganh.bookingbilliards.manager.UserManager
import com.trunganh.bookingbilliards.model.User
import com.trunganh.bookingbilliards.model.isAdmin
import com.trunganh.bookingbilliards.model.isUser
import com.trunganh.bookingbilliards.network.RetrofitClient
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AccountFragment : Fragment() {
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Kiểm tra đăng nhập
        if (!UserManager.isLoggedIn()) {
            findNavController().navigate(R.id.action_accountFragment_to_loginFragment)
            return
        }

        setupUserInfo()
        setupLogoutButton()
        setupAccountManagementButton()
    }

    private fun setupUserInfo() {
        val user = UserManager.getUser()
        user?.let {
            binding.apply {
                tvUsername.text = it.login
                tvFullName.text = "${it.firstName ?: ""} ${it.lastName ?: ""}"
                tvEmail.text = it.email ?: "Chưa cập nhật"
                tvRole.text = if (UserManager.isAdmin()) "Quản trị viên" else "Người dùng"
                btnEdit.setOnClickListener { showEditProfileDialog() }
            }
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            // Xóa thông tin đăng nhập
            UserManager.clearUserData()
            Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show()
            // Chuyển về màn hình đăng nhập
            findNavController().navigate(R.id.action_accountFragment_to_loginFragment)
        }
    }

    private fun setupAccountManagementButton() {
        val currentUser = UserManager.getUser()
        Log.d("AccountFragment", "Current User Role: ${currentUser?.authorities}")
        if (currentUser?.isAdmin() == true) {
            binding.btnAccountManagement.visibility = View.VISIBLE
            binding.btnAccountManagement.setOnClickListener {
                findNavController().navigate(R.id.action_accountFragment_to_accountManagementFragment)
            }
        } else {
            binding.btnAccountManagement.visibility = View.GONE
        }
    }

    private fun showEditProfileDialog() {
        val user = UserManager.getUser() ?: return
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        
        // Điền thông tin hiện tại vào dialog
        dialogBinding.apply {
            edtFullName.setText("${user.firstName ?: ""} ${user.lastName ?: ""}")
            edtEmail.setText(user.email ?: "")
            // Phone không có trong model User hiện tại
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chỉnh sửa thông tin")
            .setView(dialogBinding.root)
            .setPositiveButton("Lưu", null) // Set null để tránh dialog tự đóng
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val fullName = dialogBinding.edtFullName.text.toString()
                val email = dialogBinding.edtEmail.text.toString()
                val phone = dialogBinding.edtPhone.text.toString()

                if (fullName.isBlank()) {
                    dialogBinding.tilFullName.error = "Vui lòng nhập họ và tên"
                    return@setOnClickListener
                }

                if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    dialogBinding.tilEmail.error = "Email không hợp lệ"
                    return@setOnClickListener
                }

                if (phone.isNotBlank() && !phone.matches(Regex("^[0-9]{10}$"))) {
                    dialogBinding.tilPhone.error = "Số điện thoại không hợp lệ"
                    return@setOnClickListener
                }

                // Ẩn các thông báo lỗi
                dialogBinding.tilFullName.error = null
                dialogBinding.tilEmail.error = null
                dialogBinding.tilPhone.error = null

                // Hiển thị progress bar
                dialogBinding.progressBar.visibility = View.VISIBLE
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = false

                // Tách họ và tên
                val nameParts = fullName.trim().split(" ")
                val firstName = nameParts.firstOrNull() ?: ""
                val lastName = nameParts.drop(1).joinToString(" ")

                // Cập nhật thông tin
                lifecycleScope.launch {
                    try {
                        val updatedUser = user.copy(
                            firstName = firstName,
                            lastName = lastName,
                            email = email.takeIf { it.isNotBlank() }
                        )

                        val response = RetrofitClient.apiService.updateUser(updatedUser)
                        if (response.isSuccessful) {
                            response.body()?.let { newUser ->
                                UserManager.saveUser(newUser)
                                setupUserInfo() // Cập nhật UI
                                Toast.makeText(requireContext(), "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("Account", "Update failed: ${response.code()} - $errorBody")
                            Toast.makeText(requireContext(), "Cập nhật thất bại: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("Account", "Update error", e)
                        Toast.makeText(requireContext(), "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        dialogBinding.progressBar.visibility = View.GONE
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                    }
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
