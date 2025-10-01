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
import androidx.navigation.NavOptions
import com.trunganh.bookingbilliards.databinding.FragmentLoginBinding
import com.trunganh.bookingbilliards.manager.UserManager
import com.trunganh.bookingbilliards.model.LoginRequest
import com.trunganh.bookingbilliards.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Kiểm tra nếu đã đăng nhập thì chuyển đến màn hình chính
        if (UserManager.isLoggedIn()) {
            navigateToMainScreen()
        }
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener { authenticateUser() }
        binding.tvRegister.setOnClickListener { navigateToRegister() }
    }

    private fun authenticateUser() {
        val username = binding.edtUsername.text.toString()
        val password = binding.edtPassword.text.toString()

        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!currentCoroutineContext().isActive) return@launch

                val loginRequest = LoginRequest(username, password)
                val response = RetrofitClient.apiService.login(loginRequest)

                withContext(Dispatchers.Main) {
                    if (!currentCoroutineContext().isActive) return@withContext

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            UserManager.saveToken(loginResponse.token)

                            try {
                                val userResponse = RetrofitClient.apiService.getCurrentUser()
                                if (!currentCoroutineContext().isActive) return@withContext

                                if (userResponse.isSuccessful) {
                                    val user = userResponse.body()
                                    if (user != null) {
                                        UserManager.saveUser(user)
                                        Toast.makeText(requireContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                        navigateToMainScreen()
                                    } else {
                                        handleLoginError("Đăng nhập thất bại: Không lấy được thông tin người dùng")
                                    }
                                } else {
                                    val errorBody = userResponse.errorBody()?.string()
                                    Log.e("Login", "Failed to get user info: ${userResponse.code()} - $errorBody")
                                    handleLoginError("Đăng nhập thành công nhưng không lấy được thông tin người dùng")
                                }
                            } catch (e: Exception) {
                                Log.e("Login", "Error fetching user info", e)
                                handleLoginError("Đăng nhập thành công nhưng gặp lỗi khi lấy thông tin người dùng")
                            }
                        } else {
                            handleLoginError("Đăng nhập thất bại: Phản hồi trống")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Login", "Login failed: ${response.code()} - $errorBody")
                        handleLoginError("Đăng nhập thất bại: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("Login", "Login cancelled")
                    return@launch
                }
                Log.e("Login", "Login error", e)
                withContext(Dispatchers.Main) {
                    handleLoginError("Lỗi kết nối: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                    }
                }
            }
        }
    }

    private fun handleLoginError(message: String) {
        if (_binding != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
        }
    }

    private fun navigateToRegister() {
        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
    }

    private fun navigateToMainScreen() {
        if (_binding != null) {
        val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
            .build()
        findNavController().navigate(R.id.navigation_home, null, navOptions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}