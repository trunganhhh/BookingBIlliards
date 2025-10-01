package com.trunganh.bookingbilliards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import com.trunganh.bookingbilliards.adapter.TableAdapter
import com.trunganh.bookingbilliards.databinding.FragmentTablesBinding
import com.trunganh.bookingbilliards.databinding.DialogAddEditTableBinding
import com.trunganh.bookingbilliards.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.trunganh.bookingbilliards.manager.UserManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import com.trunganh.bookingbilliards.model.Table
import androidx.lifecycle.Lifecycle

class TablesFragment : Fragment() {
    private var _binding: FragmentTablesBinding? = null
    private val binding get() = _binding!!
    private lateinit var tableAdapter: TableAdapter
    private var currentTable: Table? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTablesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fabAddTable.visibility = View.VISIBLE // TEST: luôn hiển thị FAB
        if (!UserManager.isLoggedIn()) {
            showLoginRequiredDialog()
            return
        }
        
        setupRecyclerView()
        loadTables()
        setupAdminFeatures()
        setupMenu()

        // Setup toolbar
        (requireActivity() as AppCompatActivity).apply {
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = "Danh sách bàn"
            }
            setHasOptionsMenu(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigate(R.id.action_tablesFragment_to_homeFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_tables, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_refresh -> {
                        loadTables()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupAdminFeatures() {
        // Thêm log để debug
        val isAdmin = UserManager.isAdmin()
        val currentUser = UserManager.getUser()
        Log.d("TablesFragment", "Current user: $currentUser")
        Log.d("TablesFragment", "Is admin: $isAdmin")
        Log.d("TablesFragment", "User authorities: ${currentUser?.authorities}")
        Log.d("TablesFragment", "FAB before visibility: ${binding.fabAddTable.visibility}")
        
        // Force hiển thị FAB nếu là admin
        if (isAdmin) {
            binding.fabAddTable.visibility = View.VISIBLE
            Log.d("TablesFragment", "Setting FAB to VISIBLE")
        } else {
            binding.fabAddTable.visibility = View.GONE
            Log.d("TablesFragment", "Setting FAB to GONE")
        }
        Log.d("TablesFragment", "FAB after visibility: ${binding.fabAddTable.visibility}")
        
        // Thêm sự kiện click cho nút thêm bàn
        binding.fabAddTable.setOnClickListener {
            Log.d("TablesFragment", "FAB clicked")
            showAddEditTableDialog()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerTables.layoutManager = LinearLayoutManager(requireContext())
        tableAdapter = TableAdapter(
            tables = emptyList(),
            onItemClick = { table ->
                if (UserManager.isAdmin()) {
                    currentTable = table
                    showAddEditTableDialog(table)
                }
            },
            onBookClick = { table ->
                val action = TablesFragmentDirections.actionTablesFragmentToBookingFragment(table.id)
                findNavController().navigate(action)
            },
            onDeleteClick = if (UserManager.isAdmin()) { table ->
                showDeleteTableDialog(table)
            } else null,
            onEditClick = if (UserManager.isAdmin()) { table ->
                showAddEditTableDialog(table)
            } else null
        )
        binding.recyclerTables.adapter = tableAdapter
    }

    private fun showLoginRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yêu cầu đăng nhập")
            .setMessage("Vui lòng đăng nhập để xem danh sách bàn")
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

    private fun showAddEditTableDialog(table: Table? = null) {
        val dialogBinding = DialogAddEditTableBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (table == null) "Thêm bàn mới" else "Chỉnh sửa bàn")
            .setView(dialogBinding.root)
            .setPositiveButton(if (table == null) "Thêm" else "Lưu", null)
            .setNegativeButton("Hủy", null)
            .create()

        // Nếu là chỉnh sửa, điền thông tin bàn hiện tại
        table?.let {
            dialogBinding.apply {
                etTableNumber.setText(it.tableNumber)
                etName.setText(it.name)
                etType.setText(it.type)
                etBallType.setText(it.ballType)
                etPricePerHour.setText(it.pricePerHour.toString())
                etImageUrl.setText(it.imageUrl)
                etContent.setText(it.content)
            }
        }

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val tableNumber = dialogBinding.etTableNumber.text.toString()
                val name = dialogBinding.etName.text.toString()
                val type = dialogBinding.etType.text.toString()
                val ballType = dialogBinding.etBallType.text.toString()
                val pricePerHour = dialogBinding.etPricePerHour.text.toString().toIntOrNull() ?: 0
                val imageUrl = dialogBinding.etImageUrl.text.toString()
                val content = dialogBinding.etContent.text.toString()

                if (validateTableInput(tableNumber, name, type, ballType, pricePerHour, dialogBinding)) {
                    if (table == null) {
                        // Thêm bàn mới
                        addNewTable(tableNumber, name, type, ballType, pricePerHour, imageUrl, content)
                    } else {
                        // Cập nhật bàn
                        updateTable(table.id, tableNumber, name, type, ballType, pricePerHour, imageUrl, content)
                    }
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun validateTableInput(
        tableNumber: String,
        name: String,
        type: String,
        ballType: String,
        pricePerHour: Int,
        binding: DialogAddEditTableBinding
    ): Boolean {
        var isValid = true

        binding.tilTableNumber.error = if (tableNumber.isBlank()) {
            isValid = false
            "Vui lòng nhập số bàn"
        } else null

        binding.tilName.error = if (name.isBlank()) {
            isValid = false
            "Vui lòng nhập tên bàn"
        } else null

        binding.tilType.error = if (type.isBlank()) {
            isValid = false
            "Vui lòng nhập loại bàn"
        } else null

        binding.tilBallType.error = if (ballType.isBlank()) {
            isValid = false
            "Vui lòng nhập loại bi"
        } else null

        binding.tilPricePerHour.error = if (pricePerHour <= 0) {
            isValid = false
            "Vui lòng nhập giá hợp lệ"
        } else null

        return isValid
    }

    private fun addNewTable(
        tableNumber: String,
        name: String,
        type: String,
        ballType: String,
        pricePerHour: Int,
        imageUrl: String,
        content: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newTable = Table(
                    id = "", // ID sẽ được server tạo
                    tableNumber = tableNumber,
                    name = name,
                    type = type,
                    ballType = ballType,
                    pricePerHour = pricePerHour,
                    imageUrl = imageUrl,
                    content = content,
                    status = "AVAILABLE"
                )
                Log.d("TableManagement", "Adding new table: $newTable")
                val response = RetrofitClient.apiService.addTable(newTable)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d("TableManagement", "Add table successful: ${response.body()}")
                        Toast.makeText(requireContext(), "Thêm bàn thành công", Toast.LENGTH_SHORT).show()
                        loadTables() // Tải lại danh sách bàn
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("TableManagement", "Add table failed: ${response.code()} - $errorBody")
                        Toast.makeText(requireContext(), "Thêm bàn thất bại: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("TableManagement", "Error adding table", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateTable(
        tableId: String,
        tableNumber: String,
        name: String,
        type: String,
        ballType: String,
        pricePerHour: Int,
        imageUrl: String,
        content: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updatedTable = Table(
                    id = tableId,
                    tableNumber = tableNumber,
                    name = name,
                    type = type,
                    ballType = ballType,
                    pricePerHour = pricePerHour,
                    imageUrl = imageUrl,
                    content = content,
                    status = "AVAILABLE"
                )
                Log.d("TableManagement", "Updating table: $updatedTable")
                val response = RetrofitClient.apiService.updateTable(tableId, updatedTable)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d("TableManagement", "Update table successful: ${response.body()}")
                        Toast.makeText(requireContext(), "Cập nhật bàn thành công", Toast.LENGTH_SHORT).show()
                        loadTables() // Tải lại danh sách bàn
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("TableManagement", "Update table failed: ${response.code()} - $errorBody")
                        Toast.makeText(requireContext(), "Cập nhật bàn thất bại: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("TableManagement", "Error updating table", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteTableDialog(table: Table) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa bàn")
            .setMessage("Bạn có chắc chắn muốn xóa bàn ${table.name}?")
            .setPositiveButton("Xóa") { dialog, _ ->
                deleteTable(table.id)
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteTable(tableId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("TableManagement", "Deleting table with ID: $tableId")
                val response = RetrofitClient.apiService.deleteTable(tableId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d("TableManagement", "Delete table successful")
                        Toast.makeText(requireContext(), "Xóa bàn thành công", Toast.LENGTH_SHORT).show()
                        loadTables() // Tải lại danh sách bàn
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("TableManagement", "Delete table failed: ${response.code()} - $errorBody")
                        Toast.makeText(requireContext(), "Xóa bàn thất bại: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("TableManagement", "Error deleting table", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadTables() {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getTables()
                // Log dữ liệu để kiểm tra
                response.forEach { table ->
                    Log.d("TABLE_DATA", """
                        Table: ${table.name}
                        Number: ${table.tableNumber}
                        Image URL: ${table.imageUrl}
                        Status: ${table.status} (Raw value)
                    """.trimIndent())
                }
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        tableAdapter.updateData(response)
                        binding.progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", e.message ?: "Unknown error", e)
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        Toast.makeText(requireContext(), "Lỗi khi tải danh sách bàn", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
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