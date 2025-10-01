package com.trunganh.bookingbilliards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.trunganh.bookingbilliards.databinding.FragmentRegisterBinding
import com.trunganh.bookingbilliards.model.RegisterRequest
import com.trunganh.bookingbilliards.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            if (validateInput()) {
                register()
            }
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        // Validate username
        binding.tilUsername.error = if (binding.edtUsername.text.isNullOrBlank()) {
            isValid = false
            "Vui lòng nhập tên đăng nhập"
        } else null

        // Validate password
        binding.tilPassword.error = if (binding.edtPassword.text.isNullOrBlank()) {
            isValid = false
            "Vui lòng nhập mật khẩu"
        } else if (binding.edtPassword.text.toString().length < 6) {
            isValid = false
            "Mật khẩu phải có ít nhất 6 ký tự"
        } else null

        // Validate confirm password
        binding.tilConfirmPassword.error = if (binding.edtConfirmPassword.text.isNullOrBlank()) {
            isValid = false
            "Vui lòng xác nhận mật khẩu"
        } else if (binding.edtConfirmPassword.text.toString() != binding.edtPassword.text.toString()) {
            isValid = false
            "Mật khẩu xác nhận không khớp"
        } else null

        // Validate full name
        binding.tilFullName.error = if (binding.edtFullName.text.isNullOrBlank()) {
            isValid = false
            "Vui lòng nhập họ và tên"
        } else null

        // Validate phone
        binding.tilPhone.error = if (binding.edtPhone.text.isNullOrBlank()) {
            isValid = false
            "Vui lòng nhập số điện thoại"
        } else if (!binding.edtPhone.text.toString().matches(Regex("^[0-9]{10}$"))) {
            isValid = false
            "Số điện thoại không hợp lệ"
        } else null

        // Validate email (optional)
        binding.tilEmail.error = if (binding.edtEmail.text.isNullOrBlank()) {
            null // Email is optional
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.edtEmail.text.toString()).matches()) {
            isValid = false
            "Email không hợp lệ"
        } else null

        return isValid
    }

    private fun register() {
        val username = binding.edtUsername.text.toString()
        val password = binding.edtPassword.text.toString()
        val fullName = binding.edtFullName.text.toString()
        val phone = binding.edtPhone.text.toString()
        val email = binding.edtEmail.text.toString().takeIf { it.isNotBlank() }

        // Show progress
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(username, password, fullName, phone, email)
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.let { user ->
                            Log.d("REGISTER", "Register successful: ${user.login}")
                            Toast.makeText(
                                requireContext(),
                                "Đăng ký thành công! Vui lòng đăng nhập",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Đăng ký thất bại: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("REGISTER", "Register error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Lỗi kết nối: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 